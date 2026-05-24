package com.qalytix.service;

import com.qalytix.dto.request.CreateJenkinsConfigRequest;
import com.qalytix.dto.request.UpdateJenkinsConfigRequest;
import com.qalytix.dto.response.JenkinsConfigResponse;

import java.util.List;

public interface JenkinsConfigService {
    JenkinsConfigResponse create(CreateJenkinsConfigRequest request);
    List<JenkinsConfigResponse> listForCurrentOrg();
    JenkinsConfigResponse update(Long id, UpdateJenkinsConfigRequest request);
    void delete(Long id);
    void testConnection(Long id);
    void triggerSync(Long id);
}
