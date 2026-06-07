package com.qalytix.dto.response;

import com.qalytix.entity.enums.BuildStatus;

import java.util.List;

public record JobBuildHistoryResponse(
        Long jobId,
        String jobName,
        List<DayStatus> history
) {
    public record DayStatus(String date, BuildStatus status) {}
}
