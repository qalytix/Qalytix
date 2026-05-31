package com.qalytix.dto.request;

import com.qalytix.entity.enums.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateNotificationConfigRequest(
        @NotBlank String name,
        @NotNull  NotificationChannel channel,
        @NotBlank String webhookUrl,

        boolean onBuildFailure,
        boolean onConsecutiveFailures,
        int     consecutiveThreshold,
        boolean onFlakyThreshold,
        double  flakyScoreThreshold
) {}
