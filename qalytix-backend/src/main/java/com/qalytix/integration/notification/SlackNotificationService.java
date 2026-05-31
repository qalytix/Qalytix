package com.qalytix.integration.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Posts Block Kit messages to a Slack Incoming Webhook.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SlackNotificationService {

    private final RestTemplate restTemplate;

    public void send(String webhookUrl, NotificationPayload payload) {
        String body = buildBlocks(payload);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            restTemplate.postForObject(webhookUrl, new HttpEntity<>(body, headers), String.class);
            log.info("[Slack] Notification sent — trigger={} job={} build=#{}",
                    payload.triggerLabel(), payload.jobName(), payload.buildNumber());
        } catch (Exception e) {
            log.error("[Slack] Failed to send notification to webhook: {}", e.getMessage());
            throw new RuntimeException("Slack webhook call failed: " + e.getMessage(), e);
        }
    }

    private String buildBlocks(NotificationPayload p) {
        String icon = p.failed() ? ":red_circle:" : ":large_yellow_circle:";
        return """
                {
                  "blocks": [
                    {
                      "type": "header",
                      "text": { "type": "plain_text", "text": "%s %s — Build #%d" }
                    },
                    {
                      "type": "section",
                      "fields": [
                        { "type": "mrkdwn", "text": "*Trigger:*\\n%s" },
                        { "type": "mrkdwn", "text": "*Status:*\\n%s" },
                        { "type": "mrkdwn", "text": "*Job:*\\n%s" },
                        { "type": "mrkdwn", "text": "*Build:*\\n#%d" }
                      ]
                    },
                    {
                      "type": "section",
                      "text": { "type": "mrkdwn", "text": "%s" }
                    }
                  ]
                }
                """.formatted(
                icon, p.jobName(), p.buildNumber(),
                p.triggerLabel(),
                p.status(),
                p.jobName(),
                p.buildNumber(),
                p.details()
        );
    }
}
