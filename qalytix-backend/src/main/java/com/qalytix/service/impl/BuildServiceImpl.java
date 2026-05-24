package com.qalytix.service.impl;

import com.qalytix.dto.response.BuildResponse;
import com.qalytix.entity.Build;
import com.qalytix.exception.ResourceNotFoundException;
import com.qalytix.repository.BuildRepository;
import com.qalytix.security.TenantContext;
import com.qalytix.service.BuildService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BuildServiceImpl implements BuildService {

    private final BuildRepository buildRepository;

    @Override
    public Page<BuildResponse> listForJob(Long jobId, Pageable pageable) {
        return buildRepository.findAllByJobIdAndOrgId(jobId, TenantContext.getOrgId(), pageable)
                .map(this::toResponse);
    }

    @Override
    public BuildResponse getById(Long id) {
        return toResponse(buildRepository.findByIdAndOrgId(id, TenantContext.getOrgId())
                .orElseThrow(() -> new ResourceNotFoundException("Build not found")));
    }

    private BuildResponse toResponse(Build b) {
        return new BuildResponse(
                b.getId(), b.getJobId(), b.getBuildNumber(), b.getStatus(),
                b.getDurationMs(), b.getStartedAt(), b.getFinishedAt(),
                b.getUrl(), b.getTriggeredBy());
    }
}
