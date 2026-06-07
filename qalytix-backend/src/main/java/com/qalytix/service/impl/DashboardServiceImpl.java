package com.qalytix.service.impl;

import com.qalytix.dto.response.DashboardStatsResponse;
import com.qalytix.dto.response.DashboardStatsResponse.RecentBuild;
import com.qalytix.dto.response.JobBuildHistoryResponse;
import com.qalytix.dto.response.JobBuildHistoryResponse.DayStatus;
import com.qalytix.entity.enums.BuildStatus;
import com.qalytix.repository.BuildRepository;
import com.qalytix.repository.JobRepository;
import com.qalytix.repository.projection.JobDailyStatusProjection;
import com.qalytix.security.TenantContext;
import com.qalytix.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final BuildRepository buildRepository;
    private final JobRepository   jobRepository;

    @Override
    public DashboardStatsResponse getStats(boolean testJobsOnly) {
        Long orgId     = TenantContext.getOrgId();
        Instant todayStart = Instant.now().truncatedTo(ChronoUnit.DAYS);

        // A job that ran more than once today only counts once — by its latest execution.
        List<String> latestStatuses = buildRepository.findLatestBuildStatusPerJobSince(orgId, todayStart, testJobsOnly);
        long todayTotal   = latestStatuses.size();
        long todaySuccess = latestStatuses.stream().filter(BuildStatus.SUCCESS.name()::equals).count();
        long todayFailure = latestStatuses.stream().filter(BuildStatus.FAILURE.name()::equals).count();

        long activeBuilds;
        List<RecentBuild> recentBuilds;

        if (testJobsOnly) {
            activeBuilds = buildRepository.countByOrgIdAndStatusAndTestJobs(orgId, BuildStatus.IN_PROGRESS);
            recentBuilds = buildRepository
                    .findRecentByOrgIdTestJobsOnly(orgId, PageRequest.of(0, 10))
                    .stream()
                    .map(b -> toRecentBuild(b.getId(), b.getJobId(), b.getBuildNumber(),
                            b.getStatus(), b.getDurationMs(), b.getStartedAt()))
                    .toList();
        } else {
            activeBuilds = buildRepository.countByOrgIdAndStatus(orgId, BuildStatus.IN_PROGRESS);
            recentBuilds = buildRepository
                    .findRecentByOrgId(orgId, PageRequest.of(0, 10))
                    .stream()
                    .map(b -> toRecentBuild(b.getId(), b.getJobId(), b.getBuildNumber(),
                            b.getStatus(), b.getDurationMs(), b.getStartedAt()))
                    .toList();
        }

        return new DashboardStatsResponse(activeBuilds, todayTotal, todaySuccess, todayFailure, recentBuilds);
    }

    @Override
    public List<JobBuildHistoryResponse> getBuildHistory(boolean testJobsOnly, int days) {
        Long orgId = TenantContext.getOrgId();
        int clampedDays = Math.min(Math.max(days, 1), 30);
        Instant since = Instant.now().truncatedTo(ChronoUnit.DAYS).minus(clampedDays - 1L, ChronoUnit.DAYS);

        List<JobDailyStatusProjection> rows = buildRepository.findDailyStatusHistory(orgId, since, testJobsOnly);

        Map<Long, JobBuildHistoryResponse> byJob = new LinkedHashMap<>();
        for (JobDailyStatusProjection row : rows) {
            JobBuildHistoryResponse job = byJob.computeIfAbsent(row.getJobId(),
                    id -> new JobBuildHistoryResponse(id, row.getJobName(), new ArrayList<>()));
            BuildStatus status = row.getStatus() != null ? BuildStatus.valueOf(row.getStatus()) : null;
            job.history().add(new DayStatus(row.getDay(), status));
        }

        return List.copyOf(byJob.values());
    }

    private RecentBuild toRecentBuild(Long id, Long jobId, int buildNumber,
                                      BuildStatus status, Long durationMs, Instant startedAt) {
        String jobName = jobRepository.findById(jobId)
                .map(j -> j.getDisplayName() != null ? j.getDisplayName() : j.getJenkinsJobName())
                .orElse("Unknown");
        return new RecentBuild(id, jobId, jobName, buildNumber, status, durationMs, startedAt);
    }
}
