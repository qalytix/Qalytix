package com.qalytix.integration.jenkins;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JenkinsBuildInfo(
        int number,
        String result,
        @JsonProperty("duration") long durationMs,
        @JsonProperty("timestamp") long timestampMs,
        String url,
        @JsonProperty("building") boolean building,
        @JsonProperty("userId") String triggeredBy
) {}
