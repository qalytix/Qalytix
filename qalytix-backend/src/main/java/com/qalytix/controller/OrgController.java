package com.qalytix.controller;

import com.qalytix.dto.response.ApiResponse;
import com.qalytix.dto.response.OrgResponse;
import com.qalytix.service.OrgService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orgs")
@RequiredArgsConstructor
public class OrgController {

    private final OrgService orgService;

    @GetMapping("/me")
    public ApiResponse<OrgResponse> getCurrentOrg() {
        return ApiResponse.ok(orgService.getCurrentOrg());
    }
}
