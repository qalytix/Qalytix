package com.qalytix.service;

import com.qalytix.dto.response.JobResponse;

import java.util.List;

public interface JobService {
    List<JobResponse> listForCurrentOrg();
    JobResponse getById(Long id);
}
