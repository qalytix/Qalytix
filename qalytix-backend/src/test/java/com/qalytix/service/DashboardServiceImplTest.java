package com.qalytix.service;

import com.qalytix.dto.response.DashboardStatsResponse;
import com.qalytix.entity.Build;
import com.qalytix.entity.Job;
import com.qalytix.entity.enums.BuildStatus;
import com.qalytix.repository.BuildRepository;
import com.qalytix.repository.JobRepository;
import com.qalytix.security.TenantContext;
import com.qalytix.service.impl.DashboardServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock BuildRepository buildRepository;
    @Mock JobRepository   jobRepository;

    @InjectMocks DashboardServiceImpl service;

    private static final Long ORG_ID = 10L;

    @BeforeEach void setup()   { TenantContext.setOrgId(ORG_ID); }
    @AfterEach  void cleanup() { TenantContext.clear(); }

    @Test
    void getStats_returnsAggregatedCounts() {
        when(buildRepository.countByOrgIdAndStatus(ORG_ID, BuildStatus.IN_PROGRESS)).thenReturn(2L);
        when(buildRepository.countByOrgIdAndStartedAtAfter(eq(ORG_ID), any(Instant.class))).thenReturn(15L);
        when(buildRepository.countByOrgIdAndStatusAndStartedAtAfter(eq(ORG_ID), eq(BuildStatus.SUCCESS), any())).thenReturn(10L);
        when(buildRepository.countByOrgIdAndStatusAndStartedAtAfter(eq(ORG_ID), eq(BuildStatus.FAILURE), any())).thenReturn(3L);
        when(buildRepository.findRecentByOrgId(eq(ORG_ID), any(Pageable.class))).thenReturn(List.of());

        DashboardStatsResponse stats = service.getStats(false);

        assertThat(stats.activeBuilds()).isEqualTo(2L);
        assertThat(stats.todayTotal()).isEqualTo(15L);
        assertThat(stats.todaySuccess()).isEqualTo(10L);
        assertThat(stats.todayFailure()).isEqualTo(3L);
        assertThat(stats.recentBuilds()).isEmpty();
    }

    @Test
    void getStats_recentBuildsIncludeJobName() {
        Build build = Build.builder()
                .id(1L).jobId(5L).orgId(ORG_ID).buildNumber(42)
                .status(BuildStatus.SUCCESS).startedAt(Instant.now()).build();
        Job job = Job.builder().id(5L).jenkinsJobName("my-job").displayName("My Job").build();

        when(buildRepository.countByOrgIdAndStatus(any(), any())).thenReturn(0L);
        when(buildRepository.countByOrgIdAndStartedAtAfter(any(), any())).thenReturn(0L);
        when(buildRepository.countByOrgIdAndStatusAndStartedAtAfter(any(), any(), any())).thenReturn(0L);
        when(buildRepository.findRecentByOrgId(eq(ORG_ID), any(Pageable.class))).thenReturn(List.of(build));
        when(jobRepository.findById(5L)).thenReturn(Optional.of(job));

        DashboardStatsResponse stats = service.getStats(false);

        assertThat(stats.recentBuilds()).hasSize(1);
        assertThat(stats.recentBuilds().get(0).jobName()).isEqualTo("My Job");
        assertThat(stats.recentBuilds().get(0).buildNumber()).isEqualTo(42);
        assertThat(stats.recentBuilds().get(0).status()).isEqualTo(BuildStatus.SUCCESS);
    }

    @Test
    void getStats_fallsBackToJobNameWhenNoDisplayName() {
        Build build = Build.builder()
                .id(1L).jobId(5L).orgId(ORG_ID).buildNumber(1)
                .status(BuildStatus.FAILURE).build();
        Job job = Job.builder().id(5L).jenkinsJobName("raw-job").displayName(null).build();

        when(buildRepository.countByOrgIdAndStatus(any(), any())).thenReturn(0L);
        when(buildRepository.countByOrgIdAndStartedAtAfter(any(), any())).thenReturn(0L);
        when(buildRepository.countByOrgIdAndStatusAndStartedAtAfter(any(), any(), any())).thenReturn(0L);
        when(buildRepository.findRecentByOrgId(eq(ORG_ID), any(Pageable.class))).thenReturn(List.of(build));
        when(jobRepository.findById(5L)).thenReturn(Optional.of(job));

        DashboardStatsResponse stats = service.getStats(false);

        assertThat(stats.recentBuilds().get(0).jobName()).isEqualTo("raw-job");
    }
}
