package com.qalytix.integration.notification;

/**
 * Channel-agnostic notification payload passed to Teams/Slack senders.
 */
public record NotificationPayload(
        String jobName,
        int    buildNumber,
        String status,
        String triggerLabel,
        String summary,
        String details,
        boolean failed
) {}
