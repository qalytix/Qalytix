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
        MemberRole role,
        boolean superAdmin
) {
    /** Backwards-compatible constructor for non-admin tokens. */
    public AuthenticatedUser(Long userId, Long orgId, String email, MemberRole role) {
        this(userId, orgId, email, role, false);
    }
}
