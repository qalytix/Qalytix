package com.qalytix.service;

import com.qalytix.dto.response.DashboardStatsResponse;
import com.qalytix.dto.response.JobBuildHistoryResponse;

import java.util.List;

public interface DashboardService {
    DashboardStatsResponse getStats(boolean testJobsOnly);

    List<JobBuildHistoryResponse> getBuildHistory(boolean testJobsOnly, int days);
}
