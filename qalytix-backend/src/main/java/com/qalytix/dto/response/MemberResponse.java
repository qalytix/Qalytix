package com.qalytix.dto.response;

import com.qalytix.entity.enums.MemberRole;
import com.qalytix.entity.enums.MemberStatus;

import java.time.OffsetDateTime;

public record MemberResponse(
        Long id,
        Long userId,
        String email,
        String fullName,
        MemberRole role,
        MemberStatus status,
        OffsetDateTime joinedAt
) {}
