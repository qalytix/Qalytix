package com.qalytix.dto.response;

import com.qalytix.entity.NotificationEvent;
import com.qalytix.entity.enums.NotificationChannel;
import com.qalytix.entity.enums.TriggerEvent;

import java.time.OffsetDateTime;

public record NotificationEventResponse(
        Long                id,
        NotificationChannel channel,
        TriggerEvent        triggerEvent,
        String              jobName,
        Integer             buildNumber,
        String              payloadSummary,
        boolean             success,
        String              errorMessage,
        OffsetDateTime      sentAt
) {
    public static NotificationEventResponse from(NotificationEvent e) {
        return new NotificationEventResponse(
                e.getId(), e.getChannel(), e.getTriggerEvent(),
                e.getJobName(), e.getBuildNumber(), e.getPayloadSummary(),
                e.isSuccess(), e.getErrorMessage(), e.getSentAt()
        );
    }
}
