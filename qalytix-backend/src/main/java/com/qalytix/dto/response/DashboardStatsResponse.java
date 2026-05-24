package com.qalytix.dto.response;

import com.qalytix.entity.enums.BuildStatus;

import java.time.Instant;
import java.util.List;

public record DashboardStatsResponse(
        long activeBuilds,
        long todayTotal,
        long todaySuccess,
        long todayFailure,
        List<RecentBuild> recentBuilds
) {
    public record RecentBuild(
            Long buildId,
            Long jobId,
            String jobName,
            Integer buildNumber,
            BuildStatus status,
            Long durationMs,
            Instant startedAt
    ) {}
}
