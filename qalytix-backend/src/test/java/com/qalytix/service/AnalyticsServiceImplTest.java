package com.qalytix.service;

import com.qalytix.billing.PlanGuard;
import com.qalytix.dto.response.AnalyticsSummaryResponse;
import com.qalytix.repository.TestResultRepository;
import com.qalytix.repository.projection.FailureTrendProjection;
import com.qalytix.repository.projection.ModuleStabilityProjection;
import com.qalytix.repository.projection.TestAggProjection;
import com.qalytix.security.TenantContext;
import com.qalytix.service.impl.AnalyticsServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceImplTest {

    @Mock TestResultRepository testResultRepository;
    @Mock PlanGuard            planGuard;

    @InjectMocks AnalyticsServiceImpl service;

    private static final Long ORG_ID = 10L;

    @BeforeEach void setup()   { TenantContext.setOrgId(ORG_ID); }
    @AfterEach  void cleanup() { TenantContext.clear(); }

    private FailureTrendProjection trendPoint(String date, long total, long failed, long passed) {
        return new FailureTrendProjection() {
            public String getDate()   { return date;   }
            public Long   getTotal()  { return total;  }
            public Long   getFailed() { return failed; }
            public Long   getPassed() { return passed; }
        };
    }

    private TestAggProjection aggRow(String suite, String name, long total, long fail, long pass) {
        return new TestAggProjection() {
            public String getTestSuite() { return suite; }
            public String getTestName()  { return name;  }
            public Long   getTotalRuns() { return total; }
            public Long   getFailCount() { return fail;  }
            public Long   getPassCount() { return pass;  }
        };
    }

    private ModuleStabilityProjection moduleRow(String module, long total, long passed) {
        return new ModuleStabilityProjection() {
            public String getModuleName() { return module; }
            public Long   getTotal()      { return total;  }
            public Long   getPassed()     { return passed; }
        };
    }

    @Test
    void getSummary_mapsFailureTrend() {
        when(planGuard.clampDataRetentionDays(ORG_ID, 30)).thenReturn(30);
        when(testResultRepository.findFailureTrend(eq(ORG_ID), isNull(), any(Instant.class)))
                .thenReturn(List.of(trendPoint("2026-05-01", 100L, 10L, 90L)));
        when(testResultRepository.findTopFailing(any(), any(), any(), anyInt())).thenReturn(List.of());
        when(testResultRepository.findFlakyTests(any(), any(), any(), anyInt())).thenReturn(List.of());
        when(testResultRepository.findModuleStability(any(), any(), any())).thenReturn(List.of());

        AnalyticsSummaryResponse resp = service.getSummary(null, 30);

        assertThat(resp.failureTrend()).hasSize(1);
        assertThat(resp.failureTrend().get(0).date()).isEqualTo("2026-05-01");
        assertThat(resp.failureTrend().get(0).failed()).isEqualTo(10L);
    }

    @Test
    void getSummary_computesFlakinessScore() {
        when(planGuard.clampDataRetentionDays(ORG_ID, 30)).thenReturn(30);
        when(testResultRepository.findFailureTrend(any(), any(), any())).thenReturn(List.of());
        when(testResultRepository.findTopFailing(any(), any(), any(), anyInt())).thenReturn(List.of());
        when(testResultRepository.findFlakyTests(eq(ORG_ID), isNull(), any(Instant.class), anyInt()))
                .thenReturn(List.of(aggRow("com.example.A", "testFoo", 10L, 4L, 6L)));
        when(testResultRepository.findModuleStability(any(), any(), any())).thenReturn(List.of());

        AnalyticsSummaryResponse resp = service.getSummary(null, 30);

        assertThat(resp.flakyTests()).hasSize(1);
        // score = min(4,6)/10 = 0.4
        assertThat(resp.flakyTests().get(0).flakinessScore()).isEqualTo(0.4);
    }

    @Test
    void getSummary_computesModulePassRate() {
        when(planGuard.clampDataRetentionDays(ORG_ID, 30)).thenReturn(30);
        when(testResultRepository.findFailureTrend(any(), any(), any())).thenReturn(List.of());
        when(testResultRepository.findTopFailing(any(), any(), any(), anyInt())).thenReturn(List.of());
        when(testResultRepository.findFlakyTests(any(), any(), any(), anyInt())).thenReturn(List.of());
        when(testResultRepository.findModuleStability(eq(ORG_ID), isNull(), any(Instant.class)))
                .thenReturn(List.of(moduleRow("com.example", 200L, 180L)));

        AnalyticsSummaryResponse resp = service.getSummary(null, 30);

        assertThat(resp.moduleStability()).hasSize(1);
        assertThat(resp.moduleStability().get(0).passRate()).isEqualTo(90.0);
    }

    @Test
    void getSummary_clampsRetentionDays() {
        // FREE plan caps at 7 days
        when(planGuard.clampDataRetentionDays(ORG_ID, 90)).thenReturn(7);
        when(testResultRepository.findFailureTrend(any(), any(), any())).thenReturn(List.of());
        when(testResultRepository.findTopFailing(any(), any(), any(), anyInt())).thenReturn(List.of());
        when(testResultRepository.findFlakyTests(any(), any(), any(), anyInt())).thenReturn(List.of());
        when(testResultRepository.findModuleStability(any(), any(), any())).thenReturn(List.of());

        // Should not throw — PlanGuard clamped it silently
        AnalyticsSummaryResponse resp = service.getSummary(null, 90);
        assertThat(resp).isNotNull();
    }
}
