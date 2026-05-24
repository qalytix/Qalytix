package com.qalytix.service.impl;

import com.qalytix.dto.response.JobResponse;
import com.qalytix.entity.Job;
import com.qalytix.exception.ResourceNotFoundException;
import com.qalytix.repository.JobRepository;
import com.qalytix.security.TenantContext;
import com.qalytix.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;

    @Override
    public List<JobResponse> listForCurrentOrg() {
        return jobRepository.findAllByOrgId(TenantContext.getOrgId())
                .stream().map(this::toResponse).toList();
    }

    @Override
    public JobResponse getById(Long id) {
        return toResponse(findOwned(id));
    }

    private Job findOwned(Long id) {
        return jobRepository.findByIdAndOrgId(id, TenantContext.getOrgId())
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
    }

    private JobResponse toResponse(Job j) {
        return new JobResponse(
                j.getId(), j.getJenkinsConfigId(), j.getJenkinsJobName(),
                j.getDisplayName(), j.getUrl(), j.getLastBuildNumber(),
                j.getLastBuildStatus(), j.getLastBuildAt(), j.getCreatedAt());
    }
}
