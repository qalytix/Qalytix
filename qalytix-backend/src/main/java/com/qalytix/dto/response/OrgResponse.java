package com.qalytix.dto.response;

import com.qalytix.entity.enums.OrgStatus;
import com.qalytix.entity.enums.Plan;

import java.time.OffsetDateTime;

public record OrgResponse(
        Long id,
        String name,
        String slug,
        Plan plan,
        OrgStatus status,
        OffsetDateTime createdAt
) {}
