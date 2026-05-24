package com.qalytix.service;

import com.qalytix.billing.PlanGuard;
import com.qalytix.dto.request.CreateJenkinsConfigRequest;
import com.qalytix.dto.request.UpdateJenkinsConfigRequest;
import com.qalytix.dto.response.JenkinsConfigResponse;
import com.qalytix.entity.JenkinsConfig;
import com.qalytix.exception.ResourceNotFoundException;
import com.qalytix.integration.jenkins.JenkinsClient;
import com.qalytix.repository.JenkinsConfigRepository;
import com.qalytix.security.TenantContext;
import com.qalytix.service.impl.JenkinsConfigServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JenkinsConfigServiceImplTest {

    @Mock JenkinsConfigRepository jenkinsConfigRepository;
    @Mock JenkinsClient           jenkinsClient;
    @Mock JenkinsIngestionService jenkinsIngestionService;
    @Mock PlanGuard               planGuard;

    @InjectMocks JenkinsConfigServiceImpl service;

    private static final Long ORG_ID = 1L;

    @BeforeEach
    void setup() { TenantContext.setOrgId(ORG_ID); }

    @AfterEach
    void cleanup() { TenantContext.clear(); }

    private JenkinsConfig config(Long id) {
        return JenkinsConfig.builder()
                .id(id).orgId(ORG_ID).name("CI").url("http://jenkins")
                .username("admin").apiToken("token").pollInterval("*/5 * * * *")
                .build();
    }

    @Test
    void create_savesAndReturnsResponse() {
        var req = new CreateJenkinsConfigRequest("CI", "http://jenkins/", "admin", "token", null);
        var saved = config(10L);
        when(jenkinsConfigRepository.save(any())).thenReturn(saved);

        JenkinsConfigResponse resp = service.create(req);

        verify(planGuard).checkJenkinsConnectionLimit(ORG_ID);
        verify(jenkinsConfigRepository).save(any());
        assertThat(resp.id()).isEqualTo(10L);
        assertThat(resp.name()).isEqualTo("CI");
    }

    @Test
    void create_stripsTrailingSlashFromUrl() {
        var req = new CreateJenkinsConfigRequest("CI", "http://jenkins/", "admin", "token", null);
        when(jenkinsConfigRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.create(req);

        verify(jenkinsConfigRepository).save(argThat(c -> c.getUrl().equals("http://jenkins")));
    }

    @Test
    void list_returnsOnlyOrgConfigs() {
        when(jenkinsConfigRepository.findAllByOrgId(ORG_ID)).thenReturn(List.of(config(1L), config(2L)));

        var result = service.listForCurrentOrg();

        assertThat(result).hasSize(2);
    }

    @Test
    void update_patchesFieldsAndSaves() {
        var existing = config(5L);
        when(jenkinsConfigRepository.findByIdAndOrgId(5L, ORG_ID)).thenReturn(Optional.of(existing));
        when(jenkinsConfigRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var req = new UpdateJenkinsConfigRequest("NewName", null, null, null, null, null);
        var resp = service.update(5L, req);

        assertThat(resp.name()).isEqualTo("NewName");
    }

    @Test
    void update_throwsWhenConfigNotOwned() {
        when(jenkinsConfigRepository.findByIdAndOrgId(99L, ORG_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, new UpdateJenkinsConfigRequest(null, null, null, null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_removesConfig() {
        var existing = config(5L);
        when(jenkinsConfigRepository.findByIdAndOrgId(5L, ORG_ID)).thenReturn(Optional.of(existing));

        service.delete(5L);

        verify(jenkinsConfigRepository).delete(existing);
    }

    @Test
    void testConnection_delegatesToClient() {
        var existing = config(5L);
        when(jenkinsConfigRepository.findByIdAndOrgId(5L, ORG_ID)).thenReturn(Optional.of(existing));

        service.testConnection(5L);

        verify(jenkinsClient).testConnection(existing);
    }

    @Test
    void triggerSync_delegatesToIngestionService() {
        var existing = config(5L);
        when(jenkinsConfigRepository.findByIdAndOrgId(5L, ORG_ID)).thenReturn(Optional.of(existing));

        service.triggerSync(5L);

        verify(jenkinsIngestionService).syncConfig(existing);
    }
}
