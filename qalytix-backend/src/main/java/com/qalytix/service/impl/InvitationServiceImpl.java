package com.qalytix.service.impl;

import com.qalytix.config.AppProperties;
import com.qalytix.dto.request.AcceptInvitationRequest;
import com.qalytix.dto.request.InviteRequest;
import com.qalytix.dto.response.AuthResponse;
import com.qalytix.dto.response.InvitationResponse;
import com.qalytix.entity.*;
import com.qalytix.entity.enums.MemberRole;
import com.qalytix.entity.enums.MemberStatus;
import com.qalytix.exception.BadRequestException;
import com.qalytix.exception.ConflictException;
import com.qalytix.exception.ResourceNotFoundException;
import com.qalytix.repository.*;
import com.qalytix.security.AuthenticatedUser;
import com.qalytix.security.JwtUtil;
import com.qalytix.service.EmailService;
import com.qalytix.service.InvitationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvitationServiceImpl implements InvitationService {

    private static final int INVITATION_EXPIRY_DAYS = 7;

    private final InvitationRepository invitationRepository;
    private final OrganizationRepository orgRepository;
    private final OrganizationMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties appProperties;

    @Override
    @Transactional
    public InvitationResponse sendInvitation(InviteRequest request, AuthenticatedUser caller) {
        Long orgId = caller.orgId();
        String email = request.email().toLowerCase().strip();

        if (memberRepository.existsByOrganizationIdAndUserId(orgId,
                userRepository.findByEmail(email).map(User::getId).orElse(-1L))) {
            throw new ConflictException("This user is already a member of the organization");
        }

        boolean alreadyPending = invitationRepository
                .findByOrganizationIdAndAcceptedAtIsNull(orgId)
                .stream()
                .anyMatch(inv -> inv.getEmail().equalsIgnoreCase(email) && !inv.isExpired());

        if (alreadyPending) {
            throw new ConflictException("A pending invitation already exists for this email");
        }

        Organization org = orgRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        User inviter = userRepository.findById(caller.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Invitation invitation = invitationRepository.save(Invitation.builder()
                .organization(org)
                .email(email)
                .role(request.role())
                .token(UUID.randomUUID())
                .invitedBy(inviter)
                .expiresAt(OffsetDateTime.now().plusDays(INVITATION_EXPIRY_DAYS))
                .build());

        String acceptUrl = appProperties.getBaseUrl() + "/invitations/accept?token=" + invitation.getToken();
        emailService.sendInvitationEmail(email, org.getName(), inviter.getFullName(), acceptUrl);

        return toResponse(invitation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvitationResponse> listPendingInvitations() {
        return invitationRepository
                .findByOrganizationIdAndAcceptedAtIsNull(caller_orgId())
                .stream()
                .filter(inv -> !inv.isExpired())
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void revokeInvitation(Long invitationId, AuthenticatedUser caller) {
        Invitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));

        if (!invitation.getOrganization().getId().equals(caller.orgId())) {
            throw new ResourceNotFoundException("Invitation not found");
        }

        invitationRepository.delete(invitation);
    }

    @Override
    @Transactional
    public AuthResponse acceptInvitation(AcceptInvitationRequest request) {
        Invitation invitation = invitationRepository.findByToken(request.token())
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found or has expired"));

        if (invitation.isExpired()) {
            throw new BadRequestException("This invitation has expired");
        }
        if (invitation.isAccepted()) {
            throw new BadRequestException("This invitation has already been accepted");
        }

        User user = userRepository.findByEmail(invitation.getEmail())
                .orElseGet(() -> registerNewUser(request, invitation.getEmail()));

        Organization org = invitation.getOrganization();

        if (memberRepository.existsByOrganizationIdAndUserId(org.getId(), user.getId())) {
            throw new ConflictException("You are already a member of this organization");
        }

        memberRepository.save(OrganizationMember.builder()
                .organization(org)
                .user(user)
                .role(invitation.getRole())
                .status(MemberStatus.ACTIVE)
                .invitedBy(invitation.getInvitedBy())
                .build());

        invitation.setAcceptedAt(OffsetDateTime.now());
        invitationRepository.save(invitation);

        return buildAuthResponse(user, org, invitation.getRole());
    }

    // ------------------------------------------------------------------ helpers

    private User registerNewUser(AcceptInvitationRequest request, String email) {
        if (!StringUtils.hasText(request.fullName())) {
            throw new BadRequestException("Full name is required to create a new account");
        }
        if (!StringUtils.hasText(request.password()) || request.password().length() < 8) {
            throw new BadRequestException("Password must be at least 8 characters");
        }
        return userRepository.save(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName().strip())
                .build());
    }

    private AuthResponse buildAuthResponse(User user, Organization org, MemberRole role) {
        String accessToken  = jwtUtil.generateAccessToken(user.getId(), org.getId(), user.getEmail(), role);
        String refreshToken = jwtUtil.generateRefreshToken();

        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .organization(org)
                .tokenHash(hashToken(refreshToken))
                .expiresAt(OffsetDateTime.now().plusSeconds(
                        appProperties.getJwt().getRefreshExpiryMs() / 1000))
                .build());

        return new AuthResponse(
                accessToken,
                refreshToken,
                new AuthResponse.UserInfo(user.getId(), user.getEmail(), user.getFullName()),
                new AuthResponse.OrgInfo(org.getId(), org.getName(), org.getSlug(), org.getPlan()),
                role
        );
    }

    private Long caller_orgId() {
        return com.qalytix.security.TenantContext.getOrgId();
    }

    private InvitationResponse toResponse(Invitation inv) {
        return new InvitationResponse(
                inv.getId(),
                inv.getEmail(),
                inv.getRole(),
                inv.getToken(),
                inv.getExpiresAt(),
                inv.getCreatedAt()
        );
    }

    private String hashToken(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
