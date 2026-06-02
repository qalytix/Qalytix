package com.qalytix.dto.response;

import java.util.List;

public record ReportSummaryResponse(
        String  fromDate,
        String  toDate,
        Long    totalRuns,
        Long    totalPassed,
        Long    totalFailed,
        Long    totalSkipped,
        double  overallPassRate,   // %
        List<JobReportRow>    jobRows,
        List<ModuleReportRow> moduleRows,
        List<FlakyReportRow>  flakyRows
) {
    public record JobReportRow(
            String jobName,
            long   totalTests,
            long   passed,
            long   failed,
            double passRate
    ) {}

    public record ModuleReportRow(
            String moduleName,
            long   totalTests,
            long   passed,
            double passRate
    ) {}

    public record FlakyReportRow(
            String testSuite,
            String testName,
            long   totalRuns,
            long   failCount,
            double flakinessScore
    ) {}
}
