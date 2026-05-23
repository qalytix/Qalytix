package com.qalytix.dto.request;

import com.qalytix.entity.enums.MemberRole;
import jakarta.validation.constraints.NotNull;

public record UpdateMemberRoleRequest(

        @NotNull(message = "Role is required")
        MemberRole role
) {}
