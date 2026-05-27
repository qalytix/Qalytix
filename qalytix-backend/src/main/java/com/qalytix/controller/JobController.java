package com.qalytix.controller;

import com.qalytix.dto.response.ApiResponse;
import com.qalytix.dto.response.JobResponse;
import com.qalytix.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    /**
     * List jobs for the current org.
     * @param view optional Jenkins view name to filter by (e.g. "Backend"); omit or pass "All" for all jobs
     */
    @GetMapping
    public ApiResponse<List<JobResponse>> list(
            @RequestParam(required = false) String view) {
        return ApiResponse.ok(jobService.listForCurrentOrg(view));
    }

    /** Return the distinct Jenkins view names available for the current org's jobs. */
    @GetMapping("/views")
    public ApiResponse<List<String>> views() {
        return ApiResponse.ok(jobService.getDistinctViews());
    }

    @GetMapping("/{id}")
    public ApiResponse<JobResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(jobService.getById(id));
    }
}
