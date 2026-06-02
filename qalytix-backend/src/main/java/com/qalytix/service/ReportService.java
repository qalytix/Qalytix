package com.qalytix.service;

import com.qalytix.dto.response.ReportSummaryResponse;

public interface ReportService {

    ReportSummaryResponse getSummary(Long jobId, String fromDate, String toDate);

    String exportCsv(Long jobId, String fromDate, String toDate);
}
