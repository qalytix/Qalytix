package com.qalytix.integration.notification;

import com.qalytix.entity.Build;
import com.qalytix.entity.NotificationConfig;
import com.qalytix.entity.NotificationEvent;
import com.qalytix.entity.enums.BuildStatus;
import com.qalytix.entity.enums.NotificationChannel;
import com.qalytix.entity.enums.TriggerEvent;
import com.qalytix.repository.BuildRepository;
import com.qalytix.repository.NotificationConfigRepository;
import com.qalytix.repository.NotificationEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Evaluates trigger rules for a completed build and fires matching notifications.
 * Called asynchronously after each build ingestion so it never blocks the ingestion pipeline.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationDispatcher {

    private final NotificationConfigRepository configRepository;
    private final NotificationEventRepository  eventRepository;
    private final BuildRepository              buildRepository;
    private final TeamsNotificationService     teamsService;
    private final SlackNotificationService     slackService;

    @Async
    public void onBuildCompleted(Long orgId, Long jobId, String jobName, Build build) {
        if (build.getStatus() == BuildStatus.IN_PROGRESS || build.getStatus() == BuildStatus.UNKNOWN) {
            return;
        }

        List<NotificationConfig> configs = configRepository.findAllByOrgIdAndEnabledTrue(orgId);
        if (configs.isEmpty()) return;

        boolean isFailed = build.getStatus() == BuildStatus.FAILURE
                        || build.getStatus() == BuildStatus.UNSTABLE;

        // Check consecutive failures once (shared across configs that need it)
        int consecutiveCount = isFailed ? countConsecutiveFailures(orgId, jobId) : 0;

        for (NotificationConfig cfg : configs) {
            TriggerEvent trigger = resolveTrigger(cfg, isFailed, consecutiveCount);
            if (trigger == null) continue;

            NotificationPayload payload = buildPayload(trigger, jobName, build);
            dispatch(cfg, trigger, payload, jobName, build.getBuildNumber());
        }
    }

    /** Sends a test notification for the given config — ignores trigger rules. */
    public void sendTest(NotificationConfig cfg) {
        NotificationPayload payload = new NotificationPayload(
                "test-job", 0, "N/A",
                "Test Notification",
                "Qalytix test notification from " + cfg.getName(),
                "This is a test notification sent from the Qalytix notification settings page.",
                false
        );
        dispatch(cfg, TriggerEvent.TEST_SEND, payload, "test-job", 0);
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private TriggerEvent resolveTrigger(NotificationConfig cfg, boolean isFailed, int consecutiveCount) {
        // Consecutive failures takes priority over single-failure
        if (cfg.isOnConsecutiveFailures()
                && isFailed
                && consecutiveCount >= cfg.getConsecutiveThreshold()) {
            return TriggerEvent.CONSECUTIVE_FAILURES;
        }
        if (cfg.isOnBuildFailure() && isFailed) {
            return TriggerEvent.BUILD_FAILURE;
        }
        return null;
    }

    private int countConsecutiveFailures(Long orgId, Long jobId) {
        List<Build> recent = buildRepository.findRecentByJobId(jobId, orgId, PageRequest.of(0, 10));
        int count = 0;
        for (Build b : recent) {
            if (b.getStatus() == BuildStatus.FAILURE || b.getStatus() == BuildStatus.UNSTABLE) {
                count++;
            } else {
                break;
            }
        }
        return count;
    }

    private NotificationPayload buildPayload(TriggerEvent trigger, String jobName, Build build) {
        String triggerLabel = switch (trigger) {
            case BUILD_FAILURE         -> "Build Failed";
            case CONSECUTIVE_FAILURES  -> "Consecutive Failures";
            case FLAKY_THRESHOLD       -> "Flaky Test Threshold Exceeded";
            case TEST_SEND             -> "Test Notification";
        };
        String details = switch (trigger) {
            case CONSECUTIVE_FAILURES -> "Multiple consecutive failures detected on job **" + jobName + "**.";
            case BUILD_FAILURE        -> "Build #" + build.getBuildNumber() + " of **" + jobName + "** has failed.";
            default                   -> "Notification from Qalytix for job **" + jobName + "**.";
        };
        return new NotificationPayload(
                jobName,
                build.getBuildNumber(),
                build.getStatus().name(),
                triggerLabel,
                triggerLabel + " — " + jobName,
                details,
                build.getStatus() == BuildStatus.FAILURE || build.getStatus() == BuildStatus.UNSTABLE
        );
    }

    private void dispatch(NotificationConfig cfg, TriggerEvent trigger,
                          NotificationPayload payload, String jobName, int buildNumber) {
        boolean success = true;
        String  error   = null;
        try {
            if (cfg.getChannel() == NotificationChannel.TEAMS) {
                teamsService.send(cfg.getWebhookUrl(), payload);
            } else {
                slackService.send(cfg.getWebhookUrl(), payload);
            }
        } catch (Exception e) {
            success = false;
            error   = e.getMessage();
            log.warn("[NotificationDispatcher] Dispatch failed for config {}: {}", cfg.getId(), e.getMessage());
        }

        eventRepository.save(NotificationEvent.builder()
                .orgId(cfg.getOrgId())
                .configId(cfg.getId())
                .channel(cfg.getChannel())
                .triggerEvent(trigger)
                .jobName(jobName)
                .buildNumber(buildNumber)
                .payloadSummary(payload.summary())
                .success(success)
                .errorMessage(error)
                .build());
    }
}
