package com.qalytix.service.impl;

import com.qalytix.billing.PlanGuard;
import com.qalytix.dto.request.CreateJenkinsConfigRequest;
import com.qalytix.dto.request.UpdateJenkinsConfigRequest;
import com.qalytix.dto.response.JenkinsConfigResponse;
import com.qalytix.entity.JenkinsConfig;
import com.qalytix.exception.ResourceNotFoundException;
import com.qalytix.integration.jenkins.JenkinsClient;
import com.qalytix.repository.JenkinsConfigRepository;
import com.qalytix.security.TenantContext;
import com.qalytix.service.JenkinsConfigService;
import com.qalytix.service.JenkinsIngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class JenkinsConfigServiceImpl implements JenkinsConfigService {

    private final JenkinsConfigRepository jenkinsConfigRepository;
    private final JenkinsClient           jenkinsClient;
    private final JenkinsIngestionService jenkinsIngestionService;
    private final PlanGuard               planGuard;

    @Override
    @Transactional
    public JenkinsConfigResponse create(CreateJenkinsConfigRequest req) {
        Long orgId = TenantContext.getOrgId();
        planGuard.checkJenkinsConnectionLimit(orgId);

        JenkinsConfig config = JenkinsConfig.builder()
                .orgId(orgId)
                .name(req.name())
                .url(req.url().stripTrailing().replaceAll("/$", ""))
                .username(req.username())
                .apiToken(req.apiToken())
                .pollInterval(req.pollInterval() != null ? req.pollInterval() : "*/5 * * * *")
                .build();

        return toResponse(jenkinsConfigRepository.save(config));
    }

    @Override
    public List<JenkinsConfigResponse> listForCurrentOrg() {
        return jenkinsConfigRepository.findAllByOrgId(TenantContext.getOrgId())
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public JenkinsConfigResponse update(Long id, UpdateJenkinsConfigRequest req) {
        JenkinsConfig config = findOwned(id);
        if (req.name()         != null) config.setName(req.name());
        if (req.url()          != null) config.setUrl(req.url().stripTrailing().replaceAll("/$", ""));
        if (req.username()     != null) config.setUsername(req.username());
        if (req.apiToken()     != null) config.setApiToken(req.apiToken());
        if (req.pollInterval() != null) config.setPollInterval(req.pollInterval());
        if (req.active()       != null) config.setActive(req.active());
        return toResponse(jenkinsConfigRepository.save(config));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        jenkinsConfigRepository.delete(findOwned(id));
    }

    @Override
    public void testConnection(Long id) {
        jenkinsClient.testConnection(findOwned(id));
    }

    @Override
    public void triggerSync(Long id) {
        jenkinsIngestionService.syncConfig(findOwned(id));
    }

    private JenkinsConfig findOwned(Long id) {
        return jenkinsConfigRepository.findByIdAndOrgId(id, TenantContext.getOrgId())
                .orElseThrow(() -> new ResourceNotFoundException("Jenkins config not found"));
    }

    private JenkinsConfigResponse toResponse(JenkinsConfig c) {
        return new JenkinsConfigResponse(
                c.getId(), c.getName(), c.getUrl(), c.getUsername(),
                c.getPollInterval(), c.isActive(), c.getLastSyncedAt(), c.getCreatedAt());
    }
}
