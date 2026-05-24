package com.qalytix.dto.request;

import jakarta.validation.constraints.Pattern;

public record UpdateJenkinsConfigRequest(
        String name,
        String url,
        String username,
        String apiToken,

        @Pattern(regexp = "^(\\*|[0-9,\\-*/]+)( (\\*|[0-9,\\-*/]+)){4}$",
                 message = "Must be a valid 5-field cron expression")
        String pollInterval,

        Boolean active
) {}
