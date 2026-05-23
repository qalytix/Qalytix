package com.qalytix.security;

import com.qalytix.entity.enums.MemberRole;

/**
 * The principal stored in the SecurityContext for every authenticated request.
 * Populated directly from JWT claims — no DB lookup on the hot path.
 */
public record AuthenticatedUser(
        Long userId,
        Long orgId,
        String email,
        MemberRole role
) {}
