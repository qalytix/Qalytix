package com.qalytix.service;

import com.qalytix.dto.response.BuildResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BuildService {
    Page<BuildResponse> listForJob(Long jobId, Pageable pageable);
    BuildResponse getById(Long id);
}
