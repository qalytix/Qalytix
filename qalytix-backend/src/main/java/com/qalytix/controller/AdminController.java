package com.qalytix.controller;

import com.qalytix.dto.request.AdminChangePlanRequest;
import com.qalytix.dto.response.*;
import com.qalytix.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AdminPlatformStatsResponse>> platformStats() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getPlatformStats()));
    }

    @GetMapping("/orgs")
    public ResponseEntity<ApiResponse<List<AdminOrgResponse>>> listOrgs(
            @RequestParam(required = false) String plan,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.listOrgs(plan, status)));
    }

    @GetMapping("/orgs/{orgId}")
    public ResponseEntity<ApiResponse<AdminOrgDetailResponse>> orgDetail(@PathVariable Long orgId) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getOrgDetail(orgId)));
    }

    @PatchMapping("/orgs/{orgId}/plan")
    public ResponseEntity<ApiResponse<AdminOrgResponse>> changePlan(
            @PathVariable Long orgId,
            @Valid @RequestBody AdminChangePlanRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.changePlan(orgId, request)));
    }
}
