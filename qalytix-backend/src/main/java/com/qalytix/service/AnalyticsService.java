package com.qalytix.service;

import com.qalytix.dto.response.AnalyticsSummaryResponse;

public interface AnalyticsService {
    AnalyticsSummaryResponse getSummary(Long jobId, int days);
}
