package com.qalytix.integration.jenkins;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JenkinsViewInfo(String name, List<JenkinsViewJobRef> jobs) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record JenkinsViewJobRef(String name) {}
}
