package com.qalytix.service.impl;

import com.qalytix.billing.PlanGuard;
import com.qalytix.dto.response.AnalyticsSummaryResponse;
import com.qalytix.dto.response.AnalyticsSummaryResponse.*;
import com.qalytix.repository.TestResultRepository;
import com.qalytix.security.TenantContext;
import com.qalytix.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final TestResultRepository testResultRepository;
    private final PlanGuard            planGuard;

    @Override
    public AnalyticsSummaryResponse getSummary(Long jobId, int days) {
        Long orgId = TenantContext.getOrgId();
        int clampedDays = planGuard.clampDataRetentionDays(orgId, days);
        Instant since = Instant.now().minus(clampedDays, ChronoUnit.DAYS);

        List<FailureTrendPoint> trend = testResultRepository
                .findFailureTrend(orgId, jobId, since)
                .stream()
                .map(p -> new FailureTrendPoint(p.getDate(), p.getTotal(), p.getFailed(), p.getPassed()))
                .toList();

        List<TopFailingTest> topFailing = testResultRepository
                .findTopFailing(orgId, jobId, since, 10)
                .stream()
                .map(p -> new TopFailingTest(p.getTestSuite(), p.getTestName(), p.getFailCount()))
                .toList();

        List<FlakyTest> flaky = testResultRepository
                .findFlakyTests(orgId, jobId, since, 20)
                .stream()
                .map(p -> {
                    long fail = p.getFailCount();
                    long pass = p.getPassCount();
                    long total = p.getTotalRuns();
                    double score = total > 0 ? (Math.min(fail, pass) * 1.0 / total) : 0.0;
                    return new FlakyTest(p.getTestSuite(), p.getTestName(), total, fail, pass,
                            Math.round(score * 1000.0) / 1000.0);
                })
                .toList();

        List<ModuleStability> modules = testResultRepository
                .findModuleStability(orgId, jobId, since)
                .stream()
                .map(p -> {
                    long total = p.getTotal();
                    long passed = p.getPassed();
                    double rate = total > 0 ? (passed * 100.0 / total) : 0.0;
                    return new ModuleStability(p.getModuleName(), total, passed,
                            Math.round(rate * 10.0) / 10.0);
                })
                .toList();

        return new AnalyticsSummaryResponse(trend, topFailing, flaky, modules);
    }
}
