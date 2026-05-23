package com.qalytix.controller;

import com.qalytix.dto.request.AcceptInvitationRequest;
import com.qalytix.dto.request.InviteRequest;
import com.qalytix.dto.response.ApiResponse;
import com.qalytix.dto.response.AuthResponse;
import com.qalytix.dto.response.InvitationResponse;
import com.qalytix.security.AuthenticatedUser;
import com.qalytix.service.InvitationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class InvitationController {

    private final InvitationService invitationService;

    @PostMapping("/api/v1/orgs/me/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ApiResponse<InvitationResponse> sendInvitation(
            @Valid @RequestBody InviteRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return ApiResponse.ok(invitationService.sendInvitation(request, currentUser), "Invitation sent");
    }

    @GetMapping("/api/v1/orgs/me/invitations")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ApiResponse<List<InvitationResponse>> listPendingInvitations() {
        return ApiResponse.ok(invitationService.listPendingInvitations());
    }

    @DeleteMapping("/api/v1/orgs/me/invitations/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public void revokeInvitation(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        invitationService.revokeInvitation(id, currentUser);
    }

    // Public endpoint — user clicks link from email
    @PostMapping("/api/v1/invitations/accept")
    public ApiResponse<AuthResponse> acceptInvitation(@Valid @RequestBody AcceptInvitationRequest request) {
        return ApiResponse.ok(invitationService.acceptInvitation(request), "Invitation accepted");
    }
}
