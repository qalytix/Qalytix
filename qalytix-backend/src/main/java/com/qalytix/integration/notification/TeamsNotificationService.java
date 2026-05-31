package com.qalytix.integration.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Posts Adaptive Card messages to a Microsoft Teams Incoming Webhook.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeamsNotificationService {

    private final RestTemplate restTemplate;

    public void send(String webhookUrl, NotificationPayload payload) {
        String body = buildAdaptiveCard(payload);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            restTemplate.postForObject(webhookUrl, new HttpEntity<>(body, headers), String.class);
            log.info("[Teams] Notification sent — trigger={} job={} build=#{}",
                    payload.triggerLabel(), payload.jobName(), payload.buildNumber());
        } catch (Exception e) {
            log.error("[Teams] Failed to send notification to webhook: {}", e.getMessage());
            throw new RuntimeException("Teams webhook call failed: " + e.getMessage(), e);
        }
    }

    private String buildAdaptiveCard(NotificationPayload p) {
        String statusColor = p.failed() ? "attention" : "good";
        String statusIcon  = p.failed() ? "🔴" : "🟡";
        return """
                {
                  "@type": "MessageCard",
                  "@context": "https://schema.org/extensions",
                  "themeColor": "%s",
                  "summary": "%s",
                  "sections": [{
                    "activityTitle": "%s **%s** — Build #%d",
                    "activitySubtitle": "%s",
                    "activityText": "%s",
                    "facts": [
                      { "name": "Job",    "value": "%s" },
                      { "name": "Build",  "value": "#%d" },
                      { "name": "Status", "value": "%s" }
                    ]
                  }]
                }
                """.formatted(
                p.failed() ? "FF0000" : "FFA500",
                p.summary(),
                statusIcon, p.jobName(), p.buildNumber(),
                p.triggerLabel(),
                p.details(),
                p.jobName(),
                p.buildNumber(),
                p.status()
        );
    }
}
