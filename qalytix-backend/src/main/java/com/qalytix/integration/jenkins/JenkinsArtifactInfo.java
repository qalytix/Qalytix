package com.qalytix.integration.jenkins;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JenkinsArtifactInfo(String fileName, String relativePath) {}
