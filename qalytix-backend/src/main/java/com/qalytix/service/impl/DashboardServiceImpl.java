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
    public DashboardStatsResponse getStats(boolean testJobsOnly) {
        Long orgId     = TenantContext.getOrgId();
        Instant todayStart = Instant.now().truncatedTo(ChronoUnit.DAYS);

        long activeBuilds, todayTotal, todaySuccess, todayFailure;
        List<RecentBuild> recentBuilds;

        if (testJobsOnly) {
            activeBuilds = buildRepository.countByOrgIdAndStatusAndTestJobs(orgId, BuildStatus.IN_PROGRESS);
            todayTotal   = buildRepository.countByOrgIdAndStartedAtAfterAndTestJobs(orgId, todayStart);
            todaySuccess = buildRepository.countByOrgIdAndStatusAndStartedAtAfterAndTestJobs(orgId, BuildStatus.SUCCESS, todayStart);
            todayFailure = buildRepository.countByOrgIdAndStatusAndStartedAtAfterAndTestJobs(orgId, BuildStatus.FAILURE, todayStart);
            recentBuilds = buildRepository
                    .findRecentByOrgIdTestJobsOnly(orgId, PageRequest.of(0, 10))
                    .stream()
                    .map(b -> toRecentBuild(b.getId(), b.getJobId(), b.getBuildNumber(),
                            b.getStatus(), b.getDurationMs(), b.getStartedAt()))
                    .toList();
        } else {
            activeBuilds = buildRepository.countByOrgIdAndStatus(orgId, BuildStatus.IN_PROGRESS);
            todayTotal   = buildRepository.countByOrgIdAndStartedAtAfter(orgId, todayStart);
            todaySuccess = buildRepository.countByOrgIdAndStatusAndStartedAtAfter(orgId, BuildStatus.SUCCESS, todayStart);
            todayFailure = buildRepository.countByOrgIdAndStatusAndStartedAtAfter(orgId, BuildStatus.FAILURE, todayStart);
            recentBuilds = buildRepository
                    .findRecentByOrgId(orgId, PageRequest.of(0, 10))
                    .stream()
                    .map(b -> toRecentBuild(b.getId(), b.getJobId(), b.getBuildNumber(),
                            b.getStatus(), b.getDurationMs(), b.getStartedAt()))
                    .toList();
        }

        return new DashboardStatsResponse(activeBuilds, todayTotal, todaySuccess, todayFailure, recentBuilds);
    }

    private RecentBuild toRecentBuild(Long id, Long jobId, int buildNumber,
                                      BuildStatus status, Long durationMs, Instant startedAt) {
        String jobName = jobRepository.findById(jobId)
                .map(j -> j.getDisplayName() != null ? j.getDisplayName() : j.getJenkinsJobName())
                .orElse("Unknown");
        return new RecentBuild(id, jobId, jobName, buildNumber, status, durationMs, startedAt);
    }
}
