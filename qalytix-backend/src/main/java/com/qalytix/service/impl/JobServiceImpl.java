package com.qalytix.service.impl;

import com.qalytix.dto.response.JobResponse;
import com.qalytix.entity.Job;
import com.qalytix.exception.ResourceNotFoundException;
import com.qalytix.repository.JobRepository;
import com.qalytix.security.TenantContext;
import com.qalytix.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;

    @Override
    public List<JobResponse> listForCurrentOrg(String view) {
        Long orgId = TenantContext.getOrgId();
        List<Job> jobs;
        if (view == null || view.isBlank() || view.equalsIgnoreCase("All")) {
            jobs = jobRepository.findAllByOrgId(orgId);
        } else {
            // Pipe-delimited search: "|ViewName|" must appear in the column
            jobs = jobRepository.findAllByOrgIdAndView(orgId, "|" + view + "|");
        }
        return jobs.stream().map(this::toResponse).toList();
    }

    @Override
    public JobResponse getById(Long id) {
        return toResponse(findOwned(id));
    }

    @Override
    public List<String> getDistinctViews() {
        Long orgId = TenantContext.getOrgId();
        // Collect all pipe-delimited segments across all jobs, deduplicate, sort
        return jobRepository.findAllByOrgId(orgId).stream()
                .map(Job::getViewNames)
                .flatMap(vn -> Arrays.stream(vn.split("\\|")))
                .filter(s -> !s.isBlank())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    private Job findOwned(Long id) {
        return jobRepository.findByIdAndOrgId(id, TenantContext.getOrgId())
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
    }

    private JobResponse toResponse(Job j) {
        List<String> views = Arrays.stream(j.getViewNames().split("\\|"))
                .filter(s -> !s.isBlank())
                .toList();
        return new JobResponse(
                j.getId(), j.getJenkinsConfigId(), j.getJenkinsJobName(),
                j.getDisplayName(), j.getUrl(), j.getLastBuildNumber(),
                j.getLastBuildStatus(), j.getLastBuildAt(), j.getCreatedAt(),
                j.isTestJob(), views);
    }
}
