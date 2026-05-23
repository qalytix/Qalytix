package com.qalytix.service.impl;

import com.qalytix.dto.response.OrgResponse;
import com.qalytix.entity.Organization;
import com.qalytix.exception.ResourceNotFoundException;
import com.qalytix.repository.OrganizationRepository;
import com.qalytix.security.TenantContext;
import com.qalytix.service.OrgService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrgServiceImpl implements OrgService {

    private final OrganizationRepository orgRepository;

    @Override
    @Transactional(readOnly = true)
    public OrgResponse getCurrentOrg() {
        Organization org = orgRepository.findById(TenantContext.getOrgId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        return toResponse(org);
    }

    private OrgResponse toResponse(Organization org) {
        return new OrgResponse(
                org.getId(),
                org.getName(),
                org.getSlug(),
                org.getPlan(),
                org.getStatus(),
                org.getCreatedAt()
        );
    }
}
