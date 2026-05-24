package com.qalytix.dto.response;

import java.util.List;

public record AnalyticsSummaryResponse(
        List<FailureTrendPoint>    failureTrend,
        List<TopFailingTest>       topFailingTests,
        List<FlakyTest>            flakyTests,
        List<ModuleStability>      moduleStability,
        List<JobStat>              jobStats
) {
    public record FailureTrendPoint(String date, long total, long failed, long passed) {}

    public record TopFailingTest(String testSuite, String testName, long failCount) {}

    public record FlakyTest(
            String testSuite,
            String testName,
            long totalRuns,
            long failCount,
            long passCount,
            double flakinessScore
    ) {}

    public record ModuleStability(String moduleName, long total, long passed, double passRate) {}

    public record JobStat(
            String jobName,
            long totalTests,
            String latestBuildStatus,
            long yesterdayPassed,
            long todayPassed,
            String trend,
            double passPercentage,
            double failPercentage
    ) {}
}
