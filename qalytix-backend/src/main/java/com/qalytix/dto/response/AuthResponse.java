package com.qalytix.dto.response;

import com.qalytix.entity.enums.MemberRole;
import com.qalytix.entity.enums.Plan;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        UserInfo user,
        OrgInfo org,
        MemberRole role
) {
    public record UserInfo(Long id, String email, String fullName) {}

    public record OrgInfo(Long id, String name, String slug, Plan plan) {}
}
