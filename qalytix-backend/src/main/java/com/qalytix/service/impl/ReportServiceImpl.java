package com.qalytix.service.impl;

import com.qalytix.billing.PlanGuard;
import com.qalytix.dto.response.ReportSummaryResponse;
import com.qalytix.dto.response.ReportSummaryResponse.*;
import com.qalytix.repository.TestResultRepository;
import com.qalytix.repository.projection.JobStatProjection;
import com.qalytix.repository.projection.ModuleStabilityProjection;
import com.qalytix.repository.projection.TestAggProjection;
import com.qalytix.security.TenantContext;
import com.qalytix.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final TestResultRepository testResultRepository;
    private final PlanGuard            planGuard;

    @Override
    public ReportSummaryResponse getSummary(Long jobId, String fromDate, String toDate) {
        Long orgId = TenantContext.getOrgId();

        Instant from = parseDate(fromDate, planGuard.clampDataRetentionDays(orgId, 365));
        Instant to   = parseEndDate(toDate);

        List<JobStatProjection> jobStats = testResultRepository
                .findJobStatsInRange(orgId, jobId, from, to);

        List<ModuleStabilityProjection> modules = testResultRepository
                .findModuleStabilityInRange(orgId, jobId, from, to);

        List<TestAggProjection> flaky = testResultRepository
                .findFlakyTestsInRange(orgId, jobId, from, to);

        // Aggregate totals from job rows
        long totalRuns    = jobStats.stream().mapToLong(j -> j.getTotalTests()  != null ? j.getTotalTests()  : 0L).sum();
        long totalPassed  = jobStats.stream().mapToLong(j -> j.getPassedTotal() != null ? j.getPassedTotal() : 0L).sum();
        long totalFailed  = jobStats.stream().mapToLong(j -> j.getFailedTotal() != null ? j.getFailedTotal() : 0L).sum();
        long totalSkipped = Math.max(0, totalRuns - totalPassed - totalFailed);
        double passRate   = totalRuns > 0 ? Math.round(totalPassed * 1000.0 / totalRuns) / 10.0 : 0.0;

        List<JobReportRow> jobRows = jobStats.stream().map(j -> {
            long total  = j.getTotalTests()  != null ? j.getTotalTests()  : 0L;
            long passed = j.getPassedTotal() != null ? j.getPassedTotal() : 0L;
            long failed = j.getFailedTotal() != null ? j.getFailedTotal() : 0L;
            double rate = total > 0 ? Math.round(passed * 1000.0 / total) / 10.0 : 0.0;
            return new JobReportRow(j.getJobName(), total, passed, failed, rate);
        }).toList();

        List<ModuleReportRow> moduleRows = modules.stream().map(m -> {
            long total  = m.getTotal()  != null ? m.getTotal()  : 0L;
            long passed = m.getPassed() != null ? m.getPassed() : 0L;
            double rate = total > 0 ? Math.round(passed * 1000.0 / total) / 10.0 : 0.0;
            return new ModuleReportRow(m.getModuleName(), total, passed, rate);
        }).toList();

        List<FlakyReportRow> flakyRows = flaky.stream().map(f -> {
            long total = f.getTotalRuns() != null ? f.getTotalRuns() : 0L;
            long fail  = f.getFailCount() != null ? f.getFailCount() : 0L;
            long pass  = f.getPassCount() != null ? f.getPassCount() : 0L;
            double score = total > 0 ? Math.round(Math.min(fail, pass) * 1000.0 / total) / 1000.0 : 0.0;
            return new FlakyReportRow(f.getTestSuite(), f.getTestName(), total, fail, score);
        }).toList();

        return new ReportSummaryResponse(
                fromDate, toDate,
                totalRuns, totalPassed, totalFailed, totalSkipped, passRate,
                jobRows, moduleRows, flakyRows
        );
    }

    @Override
    public String exportCsv(Long jobId, String fromDate, String toDate) {
        ReportSummaryResponse report = getSummary(jobId, fromDate, toDate);
        StringBuilder csv = new StringBuilder();

        // Header
        csv.append("Report: ").append(report.fromDate()).append(" to ").append(report.toDate()).append("\n");
        csv.append("Total Runs,Total Passed,Total Failed,Total Skipped,Overall Pass Rate\n");
        csv.append(report.totalRuns()).append(",")
                .append(report.totalPassed()).append(",")
                .append(report.totalFailed()).append(",")
                .append(report.totalSkipped()).append(",")
                .append(report.overallPassRate()).append("%\n\n");

        // Job breakdown
        csv.append("Job Summary\n");
        csv.append("Job Name,Total Tests,Passed,Failed,Pass Rate\n");
        for (var row : report.jobRows()) {
            csv.append(escapeCsv(row.jobName())).append(",")
                    .append(row.totalTests()).append(",")
                    .append(row.passed()).append(",")
                    .append(row.failed()).append(",")
                    .append(row.passRate()).append("%\n");
        }

        // Module breakdown
        csv.append("\nModule Stability\n");
        csv.append("Module,Total Tests,Passed,Pass Rate\n");
        for (var row : report.moduleRows()) {
            csv.append(escapeCsv(row.moduleName())).append(",")
                    .append(row.totalTests()).append(",")
                    .append(row.passed()).append(",")
                    .append(row.passRate()).append("%\n");
        }

        // Flaky tests
        csv.append("\nFlaky Tests\n");
        csv.append("Test Suite,Test Name,Total Runs,Fail Count,Flakiness Score\n");
        for (var row : report.flakyRows()) {
            csv.append(escapeCsv(row.testSuite())).append(",")
                    .append(escapeCsv(row.testName())).append(",")
                    .append(row.totalRuns()).append(",")
                    .append(row.failCount()).append(",")
                    .append(row.flakinessScore()).append("\n");
        }

        return csv.toString();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Instant parseDate(String date, int maxRetentionDays) {
        if (date == null || date.isBlank()) {
            return Instant.now().minusSeconds((long) maxRetentionDays * 86400);
        }
        try {
            return LocalDate.parse(date).atStartOfDay(ZoneOffset.UTC).toInstant();
        } catch (DateTimeParseException e) {
            return Instant.now().minusSeconds(30L * 86400);
        }
    }

    private Instant parseEndDate(String date) {
        if (date == null || date.isBlank()) {
            return Instant.now();
        }
        try {
            // End of the given day
            return LocalDate.parse(date).plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        } catch (DateTimeParseException e) {
            return Instant.now();
        }
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
