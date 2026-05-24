package com.qalytix.integration.jenkins;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JenkinsJobsResponse(List<JenkinsJobInfo> jobs) {}
