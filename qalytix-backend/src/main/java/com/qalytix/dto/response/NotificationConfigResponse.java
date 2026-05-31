package com.qalytix.dto.response;

import com.qalytix.entity.NotificationConfig;
import com.qalytix.entity.enums.NotificationChannel;

import java.time.OffsetDateTime;

public record NotificationConfigResponse(
        Long                id,
        String              name,
        NotificationChannel channel,
        boolean             onBuildFailure,
        boolean             onConsecutiveFailures,
        int                 consecutiveThreshold,
        boolean             onFlakyThreshold,
        double              flakyScoreThreshold,
        boolean             enabled,
        OffsetDateTime      createdAt
) {
    public static NotificationConfigResponse from(NotificationConfig c) {
        return new NotificationConfigResponse(
                c.getId(), c.getName(), c.getChannel(),
                c.isOnBuildFailure(), c.isOnConsecutiveFailures(), c.getConsecutiveThreshold(),
                c.isOnFlakyThreshold(), c.getFlakyScoreThreshold(),
                c.isEnabled(), c.getCreatedAt()
        );
    }
}
