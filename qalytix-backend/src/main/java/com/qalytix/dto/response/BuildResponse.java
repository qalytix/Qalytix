package com.qalytix.dto.response;

import com.qalytix.entity.enums.BuildStatus;

import java.time.Instant;

public record BuildResponse(
        Long id,
        Long jobId,
        Integer buildNumber,
        BuildStatus status,
        Long durationMs,
        Instant startedAt,
        Instant finishedAt,
        String url,
        String triggeredBy
) {}
