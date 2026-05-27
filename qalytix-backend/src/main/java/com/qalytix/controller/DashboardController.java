package com.qalytix.controller;

import com.qalytix.dto.response.ApiResponse;
import com.qalytix.dto.response.DashboardStatsResponse;
import com.qalytix.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
}
