package com.qalytix.controller;

import com.qalytix.dto.response.ApiResponse;
import com.qalytix.dto.response.ReportSummaryResponse;
import com.qalytix.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /**
     * GET /api/v1/reports/summary?jobId=&from=YYYY-MM-DD&to=YYYY-MM-DD
     * Returns a JSON summary for the given date range and optional job filter.
     */
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<ReportSummaryResponse>> summary(
            @RequestParam(required = false) Long jobId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return ResponseEntity.ok(ApiResponse.ok(reportService.getSummary(jobId, from, to)));
    }

    /**
     * GET /api/v1/reports/export/csv?jobId=&from=YYYY-MM-DD&to=YYYY-MM-DD
     * Returns a CSV file download.
     */
    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(required = false) Long jobId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        String csv = reportService.exportCsv(jobId, from, to);
        String filename = "qalytix-report-" + (from != null ? from : "all") + "-to-" + (to != null ? to : "today") + ".csv";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());

        return ResponseEntity.ok().headers(headers).body(csv.getBytes());
    }
}
