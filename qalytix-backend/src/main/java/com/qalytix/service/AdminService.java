package com.qalytix.service;

import com.qalytix.dto.request.AdminChangePlanRequest;
import com.qalytix.dto.response.AdminOrgDetailResponse;
import com.qalytix.dto.response.AdminOrgResponse;
import com.qalytix.dto.response.AdminPlatformStatsResponse;

import java.util.List;

public interface AdminService {

    AdminPlatformStatsResponse getPlatformStats();

    List<AdminOrgResponse> listOrgs(String planFilter, String statusFilter);

    AdminOrgDetailResponse getOrgDetail(Long orgId);

    AdminOrgResponse changePlan(Long orgId, AdminChangePlanRequest request);
}
