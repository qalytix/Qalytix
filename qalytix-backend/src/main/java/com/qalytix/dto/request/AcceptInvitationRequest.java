package com.qalytix.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AcceptInvitationRequest(

        @NotNull(message = "Invitation token is required")
        UUID token,

        // Required only when accepting as a new (unregistered) user
        String fullName,
        String password
) {}
