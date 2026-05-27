package com.qalytix.service;

import com.qalytix.dto.response.JobResponse;

import java.util.List;

public interface JobService {
    /** List all jobs for the current org, optionally filtered by view name. */
    List<JobResponse> listForCurrentOrg(String view);
    JobResponse getById(Long id);
    /** Return distinct Jenkins view names for all jobs in the current org. */
    List<String> getDistinctViews();
}
