package com.qalytix.integration.jenkins;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JenkinsJobInfo(
        String name,
        @JsonProperty("displayName") String displayName,
        String url,
        @JsonProperty("lastBuild") JenkinsBuildInfo lastBuild,
        @JsonProperty("jobs") List<JenkinsJobInfo> jobs
) {}
