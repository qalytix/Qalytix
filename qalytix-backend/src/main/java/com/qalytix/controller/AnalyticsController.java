package com.qalytix.controller;

import com.qalytix.dto.response.AnalyticsSummaryResponse;
import com.qalytix.dto.response.ApiResponse;
import com.qalytix.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/summary")
    public ApiResponse<AnalyticsSummaryResponse> summary(
            @RequestParam(required = false) Long jobId,
            @RequestParam(defaultValue = "30") int days) {
        return ApiResponse.ok(analyticsService.getSummary(jobId, days));
    }
}
