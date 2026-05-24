package com.qalytix.controller;

import com.qalytix.dto.request.CreateJenkinsConfigRequest;
import com.qalytix.dto.request.UpdateJenkinsConfigRequest;
import com.qalytix.dto.response.ApiResponse;
import com.qalytix.dto.response.JenkinsConfigResponse;
import com.qalytix.service.JenkinsConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/jenkins")
@RequiredArgsConstructor
public class JenkinsController {

    private final JenkinsConfigService jenkinsConfigService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('OWNER', 'ADMIN')")
    public ApiResponse<JenkinsConfigResponse> create(@Valid @RequestBody CreateJenkinsConfigRequest request) {
        return ApiResponse.ok(jenkinsConfigService.create(request), "Jenkins config created");
    }

    @GetMapping
    public ApiResponse<List<JenkinsConfigResponse>> list() {
        return ApiResponse.ok(jenkinsConfigService.listForCurrentOrg());
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('OWNER', 'ADMIN')")
    public ApiResponse<JenkinsConfigResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateJenkinsConfigRequest request) {
        return ApiResponse.ok(jenkinsConfigService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyAuthority('OWNER', 'ADMIN')")
    public void delete(@PathVariable Long id) {
        jenkinsConfigService.delete(id);
    }

    @PostMapping("/{id}/test-connection")
    @PreAuthorize("hasAnyAuthority('OWNER', 'ADMIN')")
    public ApiResponse<Void> testConnection(@PathVariable Long id) {
        jenkinsConfigService.testConnection(id);
        return ApiResponse.ok("Connection successful");
    }

    @PostMapping("/{id}/sync")
    @PreAuthorize("hasAnyAuthority('OWNER', 'ADMIN')")
    public ApiResponse<Void> sync(@PathVariable Long id) {
        jenkinsConfigService.triggerSync(id);
        return ApiResponse.ok("Sync triggered");
    }
}
