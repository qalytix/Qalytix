package com.qalytix.websocket;

import com.qalytix.entity.enums.BuildStatus;

import java.time.Instant;

public record BuildEventPayload(
        Long buildId,
        Long jobId,
        Long orgId,
        String jobName,
        Integer buildNumber,
        BuildStatus status,
        Long durationMs,
        Instant startedAt,
        Instant finishedAt
) {}
