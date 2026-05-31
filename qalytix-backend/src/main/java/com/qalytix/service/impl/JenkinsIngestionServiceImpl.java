package com.qalytix.service.impl;

import com.qalytix.entity.Build;
import com.qalytix.entity.Job;
import com.qalytix.entity.JenkinsConfig;
import com.qalytix.entity.enums.BuildStatus;
import com.qalytix.integration.jenkins.JenkinsBuildInfo;
import com.qalytix.integration.jenkins.JenkinsClient;
import com.qalytix.integration.jenkins.JenkinsJobInfo;
import com.qalytix.integration.notification.NotificationDispatcher;
import com.qalytix.repository.BuildRepository;
import com.qalytix.repository.JenkinsConfigRepository;
import com.qalytix.repository.JobRepository;
import com.qalytix.service.JenkinsIngestionService;
import com.qalytix.service.TestResultIngestionService;
import com.qalytix.websocket.BuildEventPayload;
import com.qalytix.websocket.BuildEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class JenkinsIngestionServiceImpl implements JenkinsIngestionService {

    private final JenkinsClient            jenkinsClient;
    private final JenkinsConfigRepository  jenkinsConfigRepository;
    private final JobRepository            jobRepository;
    private final BuildRepository             buildRepository;
    private final BuildEventPublisher         buildEventPublisher;
    private final TestResultIngestionService  testResultIngestionService;
    private final NotificationDispatcher      notificationDispatcher;

    @Override
    @Scheduled(fixedDelayString = "${app.jenkins.poll-delay-ms:300000}")
    public void syncAll() {
        List<JenkinsConfig> configs = jenkinsConfigRepository.findAllByActiveTrue();
        log.info("Jenkins sync started — {} active config(s)", configs.size());
        for (JenkinsConfig config : configs) {
            try {
                syncConfig(config);
            } catch (Exception e) {
                log.error("Sync failed for config [{}]: {}", config.getName(), e.getMessage());
            }
        }
    }

    @Override
    @Transactional
    public void syncConfig(JenkinsConfig config) {
        log.debug("Syncing Jenkins config [{}] for org {}", config.getName(), config.getOrgId());
        List<JenkinsJobInfo> remoteJobs = jenkinsClient.fetchJobs(config);

        // Fetch view membership once per config to avoid N+1 calls
        Map<String, List<String>> viewsByJob = jenkinsClient.fetchViewsByJob(config);

        for (JenkinsJobInfo jobInfo : remoteJobs) {
            Job job = upsertJob(config, jobInfo, viewsByJob);
            if (jobInfo.lastBuild() != null) {
                upsertBuild(config, job, jobInfo.lastBuild());
            }
        }

        config.setLastSyncedAt(Instant.now());
        jenkinsConfigRepository.save(config);
    }

    private Job upsertJob(JenkinsConfig config, JenkinsJobInfo info, Map<String, List<String>> viewsByJob) {
        String viewNames = buildViewNames(info.name(), viewsByJob);
        return jobRepository
                .findByJenkinsConfigIdAndJenkinsJobName(config.getId(), info.name())
                .map(existing -> {
                    existing.setDisplayName(info.displayName());
                    existing.setUrl(info.url());
                    existing.setViewNames(viewNames);
                    if (info.lastBuild() != null) {
                        existing.setLastBuildNumber(info.lastBuild().number());
                        existing.setLastBuildStatus(toStatus(info.lastBuild()));
                        existing.setLastBuildAt(Instant.ofEpochMilli(info.lastBuild().timestampMs()));
                    }
                    return jobRepository.save(existing);
                })
                .orElseGet(() -> {
                    Job.JobBuilder builder = Job.builder()
                            .orgId(config.getOrgId())
                            .jenkinsConfigId(config.getId())
                            .jenkinsJobName(info.name())
                            .displayName(info.displayName())
                            .url(info.url())
                            .viewNames(viewNames);
                    if (info.lastBuild() != null) {
                        builder.lastBuildNumber(info.lastBuild().number())
                               .lastBuildStatus(toStatus(info.lastBuild()))
                               .lastBuildAt(Instant.ofEpochMilli(info.lastBuild().timestampMs()));
                    }
                    return jobRepository.save(builder.build());
                });
    }

    /**
     * Build a pipe-delimited string of view names for the given job, e.g. "|All|Backend|".
     * Falls back to "|All|" if the job doesn't appear in any view mapping.
     */
    private String buildViewNames(String jobName, Map<String, List<String>> viewsByJob) {
        List<String> views = viewsByJob.getOrDefault(jobName, List.of());
        if (views.isEmpty()) return "|All|";
        StringBuilder sb = new StringBuilder("|");
        for (String v : views) sb.append(v).append("|");
        // Ensure "All" is always present
        if (!sb.toString().contains("|All|")) sb.insert(1, "All|");
        return sb.toString();
    }

    private void upsertBuild(JenkinsConfig config, Job job, JenkinsBuildInfo info) {
        BuildStatus status = toStatus(info);
        Build saved = buildRepository.findByJobIdAndBuildNumber(job.getId(), info.number())
                .map(existing -> {
                    existing.setStatus(status);
                    existing.setDurationMs(info.durationMs());
                    existing.setFinishedAt(info.building() ? null : Instant.ofEpochMilli(info.timestampMs() + info.durationMs()));
                    return buildRepository.save(existing);
                })
                .orElseGet(() -> buildRepository.save(Build.builder()
                        .orgId(config.getOrgId())
                        .jobId(job.getId())
                        .buildNumber(info.number())
                        .status(status)
                        .durationMs(info.durationMs())
                        .startedAt(Instant.ofEpochMilli(info.timestampMs()))
                        .finishedAt(info.building() ? null : Instant.ofEpochMilli(info.timestampMs() + info.durationMs()))
                        .url(info.url())
                        .triggeredBy(info.triggeredBy())
                        .build()));

        buildEventPublisher.publishBuildEvent(new BuildEventPayload(
                saved.getId(), job.getId(), config.getOrgId(),
                job.getJenkinsJobName(), saved.getBuildNumber(),
                status, saved.getDurationMs(), saved.getStartedAt(), saved.getFinishedAt()));

        testResultIngestionService.ingestForBuild(config, job, saved);
        notificationDispatcher.onBuildCompleted(config.getOrgId(), job.getId(), job.getJenkinsJobName(), saved);
    }

    private BuildStatus toStatus(JenkinsBuildInfo info) {
        if (info.building()) return BuildStatus.IN_PROGRESS;
        if (info.result() == null) return BuildStatus.UNKNOWN;
        return switch (info.result()) {
            case "SUCCESS"  -> BuildStatus.SUCCESS;
            case "FAILURE"  -> BuildStatus.FAILURE;
            case "UNSTABLE" -> BuildStatus.UNSTABLE;
            case "ABORTED"  -> BuildStatus.ABORTED;
            default         -> BuildStatus.UNKNOWN;
        };
    }
}
