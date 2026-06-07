package com.qalytix.controller;

import com.qalytix.dto.response.ApiResponse;
import com.qalytix.dto.response.DashboardStatsResponse;
import com.qalytix.dto.response.JobBuildHistoryResponse;
import com.qalytix.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * @param testJobsOnly when true, stats and recent builds are restricted to
     *                     jobs that have had at least one test result ingested.
     */
    @GetMapping("/stats")
    public ApiResponse<DashboardStatsResponse> stats(
            @RequestParam(defaultValue = "false") boolean testJobsOnly) {
        return ApiResponse.ok(dashboardService.getStats(testJobsOnly));
    }

    /**
     * Per-job build status history for the last {@code days} calendar days (max 30).
     * When a job ran more than once on a day, only its latest execution that day is reported.
     */
    @GetMapping("/build-history")
    public ApiResponse<List<JobBuildHistoryResponse>> buildHistory(
            @RequestParam(defaultValue = "false") boolean testJobsOnly,
            @RequestParam(defaultValue = "10") int days) {
        return ApiResponse.ok(dashboardService.getBuildHistory(testJobsOnly, days));
    }
}
