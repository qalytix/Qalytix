package com.qalytix.service;

import com.qalytix.entity.Build;
import com.qalytix.entity.JenkinsConfig;
import com.qalytix.entity.Job;

public interface TestResultIngestionService {
    void ingestForBuild(JenkinsConfig config, Job job, Build build);
}
