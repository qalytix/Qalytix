package com.qalytix.dto.response;

import java.time.Instant;

public record JenkinsConfigResponse(
        Long id,
        String name,
        String url,
        String username,
        String pollInterval,
        boolean active,
        Instant lastSyncedAt,
        Instant createdAt
) {}
