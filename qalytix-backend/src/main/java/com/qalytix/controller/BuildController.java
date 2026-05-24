package com.qalytix.controller;

import com.qalytix.dto.response.ApiResponse;
import com.qalytix.dto.response.BuildResponse;
import com.qalytix.service.BuildService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/jobs/{jobId}/builds")
@RequiredArgsConstructor
public class BuildController {

    private final BuildService buildService;

    @GetMapping
    public ApiResponse<Page<BuildResponse>> list(
            @PathVariable Long jobId,
            @PageableDefault(size = 20, sort = "buildNumber") Pageable pageable) {
        return ApiResponse.ok(buildService.listForJob(jobId, pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<BuildResponse> get(@PathVariable Long jobId, @PathVariable Long id) {
        return ApiResponse.ok(buildService.getById(id));
    }
}
