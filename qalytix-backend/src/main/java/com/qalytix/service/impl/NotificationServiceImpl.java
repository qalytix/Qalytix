package com.qalytix.service.impl;

import com.qalytix.dto.request.CreateNotificationConfigRequest;
import com.qalytix.dto.response.NotificationConfigResponse;
import com.qalytix.dto.response.NotificationEventResponse;
import com.qalytix.entity.NotificationConfig;
import com.qalytix.exception.BadRequestException;
import com.qalytix.integration.notification.NotificationDispatcher;
import com.qalytix.repository.NotificationConfigRepository;
import com.qalytix.repository.NotificationEventRepository;
import com.qalytix.security.TenantContext;
import com.qalytix.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationConfigRepository configRepository;
    private final NotificationEventRepository  eventRepository;
    private final NotificationDispatcher       dispatcher;

    @Override
    public List<NotificationConfigResponse> listConfigs() {
        return configRepository
                .findAllByOrgIdOrderByCreatedAtDesc(TenantContext.getOrgId())
                .stream()
                .map(NotificationConfigResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public NotificationConfigResponse createConfig(CreateNotificationConfigRequest req) {
        NotificationConfig config = NotificationConfig.builder()
                .orgId(TenantContext.getOrgId())
                .name(req.name())
                .channel(req.channel())
                .webhookUrl(req.webhookUrl())
                .onBuildFailure(req.onBuildFailure())
                .onConsecutiveFailures(req.onConsecutiveFailures())
                .consecutiveThreshold(req.consecutiveThreshold() > 0 ? req.consecutiveThreshold() : 3)
                .onFlakyThreshold(req.onFlakyThreshold())
                .flakyScoreThreshold(req.flakyScoreThreshold() > 0 ? req.flakyScoreThreshold() : 0.5)
                .build();
        return NotificationConfigResponse.from(configRepository.save(config));
    }

    @Override
    @Transactional
    public NotificationConfigResponse updateConfig(Long id, CreateNotificationConfigRequest req) {
        NotificationConfig config = findOwned(id);
        config.setName(req.name());
        config.setChannel(req.channel());
        config.setWebhookUrl(req.webhookUrl());
        config.setOnBuildFailure(req.onBuildFailure());
        config.setOnConsecutiveFailures(req.onConsecutiveFailures());
        config.setConsecutiveThreshold(req.consecutiveThreshold() > 0 ? req.consecutiveThreshold() : 3);
        config.setOnFlakyThreshold(req.onFlakyThreshold());
        config.setFlakyScoreThreshold(req.flakyScoreThreshold() > 0 ? req.flakyScoreThreshold() : 0.5);
        return NotificationConfigResponse.from(configRepository.save(config));
    }

    @Override
    @Transactional
    public void deleteConfig(Long id) {
        configRepository.delete(findOwned(id));
    }

    @Override
    public void testConfig(Long id) {
        dispatcher.sendTest(findOwned(id));
    }

    @Override
    public List<NotificationEventResponse> listHistory(int limit) {
        int clampedLimit = Math.min(limit, 100);
        return eventRepository
                .findAllByOrgIdOrderBySentAtDesc(TenantContext.getOrgId(), PageRequest.of(0, clampedLimit))
                .stream()
                .map(NotificationEventResponse::from)
                .toList();
    }

    private NotificationConfig findOwned(Long id) {
        NotificationConfig config = configRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Notification config not found"));
        if (!config.getOrgId().equals(TenantContext.getOrgId())) {
            throw new BadRequestException("Notification config not found");
        }
        return config;
    }
}
