package com.qalytix.dto.response;

import com.qalytix.entity.enums.BuildStatus;

import java.time.Instant;
import java.util.List;

public record JobResponse(
        Long id,
        Long jenkinsConfigId,
        String jenkinsJobName,
        String displayName,
        String url,
        Integer lastBuildNumber,
        BuildStatus lastBuildStatus,
        Instant lastBuildAt,
        Instant createdAt,
        boolean isTestJob,
        List<String> viewNames
) {}
