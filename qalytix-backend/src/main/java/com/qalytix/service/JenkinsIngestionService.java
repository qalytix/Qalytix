package com.qalytix.service;

import com.qalytix.entity.JenkinsConfig;

public interface JenkinsIngestionService {
    void syncConfig(JenkinsConfig config);
    void syncAll();
}
