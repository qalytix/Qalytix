package com.qalytix.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateJenkinsConfigRequest(

        @NotBlank String name,
        @NotBlank String url,
        @NotBlank String username,
        @NotBlank String apiToken,

        @Pattern(regexp = "^(\\*|[0-9,\\-*/]+)( (\\*|[0-9,\\-*/]+)){4}$",
                 message = "Must be a valid 5-field cron expression")
        String pollInterval
) {}
