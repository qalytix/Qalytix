package com.qalytix.service;

import com.qalytix.dto.request.CreateNotificationConfigRequest;
import com.qalytix.dto.response.NotificationConfigResponse;
import com.qalytix.dto.response.NotificationEventResponse;

import java.util.List;

public interface NotificationService {

    List<NotificationConfigResponse> listConfigs();

    NotificationConfigResponse createConfig(CreateNotificationConfigRequest request);

    NotificationConfigResponse updateConfig(Long id, CreateNotificationConfigRequest request);

    void deleteConfig(Long id);

    void testConfig(Long id);

    List<NotificationEventResponse> listHistory(int limit);
}
