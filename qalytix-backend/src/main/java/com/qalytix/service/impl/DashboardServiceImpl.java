package com.qalytix.service.impl;

import com.qalytix.dto.response.DashboardStatsResponse;
import com.qalytix.dto.response.DashboardStatsResponse.RecentBuild;
import com.qalytix.entity.enums.BuildStatus;
import com.qalytix.repository.BuildRepository;
import com.qalytix.repository.JobRepository;
import com.qalytix.security.TenantContext;
import com.qalytix.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final BuildRepository buildRepository;
    private final JobRepository   jobRepository;

    @Override
    public DashboardStatsResponse getStats() {
        Long orgId     = TenantContext.getOrgId();
        Instant todayStart = Instant.now().truncatedTo(ChronoUnit.DAYS);

        long activeBuilds  = buildRepository.countByOrgIdAndStatus(orgId, BuildStatus.IN_PROGRESS);
        long todayTotal    = buildRepository.countByOrgIdAndStartedAtAfter(orgId, todayStart);
        long todaySuccess  = buildRepository.countByOrgIdAndStatusAndStartedAtAfter(orgId, BuildStatus.SUCCESS, todayStart);
        long todayFailure  = buildRepository.countByOrgIdAndStatusAndStartedAtAfter(orgId, BuildStatus.FAILURE, todayStart);

        List<RecentBuild> recentBuilds = buildRepository
                .findRecentByOrgId(orgId, PageRequest.of(0, 10))
                .stream()
                .map(b -> {
                    String jobName = jobRepository.findById(b.getJobId())
                            .map(j -> j.getDisplayName() != null ? j.getDisplayName() : j.getJenkinsJobName())
                            .orElse("Unknown");
                    return new RecentBuild(b.getId(), b.getJobId(), jobName,
                            b.getBuildNumber(), b.getStatus(), b.getDurationMs(), b.getStartedAt());
                })
                .toList();

        return new DashboardStatsResponse(activeBuilds, todayTotal, todaySuccess, todayFailure, recentBuilds);
    }
}
