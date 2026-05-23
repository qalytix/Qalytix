package com.qalytix.service;

import com.qalytix.dto.request.AcceptInvitationRequest;
import com.qalytix.dto.request.InviteRequest;
import com.qalytix.dto.response.AuthResponse;
import com.qalytix.dto.response.InvitationResponse;
import com.qalytix.security.AuthenticatedUser;

import java.util.List;

public interface InvitationService {

    InvitationResponse sendInvitation(InviteRequest request, AuthenticatedUser caller);

    List<InvitationResponse> listPendingInvitations();

    void revokeInvitation(Long invitationId, AuthenticatedUser caller);

    AuthResponse acceptInvitation(AcceptInvitationRequest request);
}
