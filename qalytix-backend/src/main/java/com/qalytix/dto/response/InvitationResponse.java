package com.qalytix.dto.response;

import com.qalytix.entity.enums.MemberRole;

import java.time.OffsetDateTime;
import java.util.UUID;

public record InvitationResponse(
        Long id,
        String email,
        MemberRole role,
        UUID token,
        OffsetDateTime expiresAt,
        OffsetDateTime createdAt
) {}
