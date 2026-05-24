package com.qalytix.integration.jenkins;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JenkinsTestReportResponse(
        List<Suite> suites
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Suite(
            String name,
            List<Case> cases
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Case(
            String className,
            String name,
            String status,
            double duration,
            String errorDetails
    ) {}
}
