package com.qalytix.service;

import com.qalytix.config.AppProperties;
import com.qalytix.dto.request.AcceptInvitationRequest;
import com.qalytix.dto.request.InviteRequest;
import com.qalytix.dto.response.AuthResponse;
import com.qalytix.dto.response.InvitationResponse;
import com.qalytix.entity.*;
import com.qalytix.entity.enums.MemberRole;
import com.qalytix.entity.enums.OrgStatus;
import com.qalytix.entity.enums.Plan;
import com.qalytix.exception.BadRequestException;
import com.qalytix.exception.ConflictException;
import com.qalytix.exception.ResourceNotFoundException;
import com.qalytix.repository.*;
import com.qalytix.security.AuthenticatedUser;
import com.qalytix.security.JwtUtil;
import com.qalytix.security.TenantContext;
import com.qalytix.service.impl.InvitationServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InvitationServiceImplTest {

    @Mock InvitationRepository          invitationRepository;
    @Mock OrganizationRepository        orgRepository;
    @Mock OrganizationMemberRepository  memberRepository;
    @Mock UserRepository                userRepository;
    @Mock RefreshTokenRepository        refreshTokenRepository;
    @Mock EmailService                  emailService;
    @Mock JwtUtil                       jwtUtil;
    @Mock PasswordEncoder               passwordEncoder;
    @Mock AppProperties                 appProperties;

    @InjectMocks InvitationServiceImpl invitationService;

    // ------------------------------------------------------------------ fixtures

    private final Organization org = Organization.builder()
            .id(10L).name("Acme").slug("acme").plan(Plan.FREE).status(OrgStatus.ACTIVE).build();

    private final User inviterUser = User.builder()
            .id(1L).email("owner@example.com").fullName("Owner").build();

    private final User existingUser = User.builder()
            .id(2L).email("existing@example.com").fullName("Existing").passwordHash("hashed").build();

    private final AuthenticatedUser ownerCaller =
            new AuthenticatedUser(1L, 10L, "owner@example.com", MemberRole.OWNER);

    private AppProperties.Jwt jwtProps;

    @BeforeEach
    void setUp() {
        TenantContext.setOrgId(10L);
        jwtProps = new AppProperties.Jwt();
        jwtProps.setRefreshExpiryMs(604_800_000L);
        when(appProperties.getJwt()).thenReturn(jwtProps);
        when(appProperties.getBaseUrl()).thenReturn("http://localhost:3000");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ------------------------------------------------------------------ sendInvitation

    @Test
    void sendInvitation_success_createsInvitationAndSendsEmail() {
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(invitationRepository.findByOrganizationIdAndAcceptedAtIsNull(10L)).thenReturn(List.of());
        when(orgRepository.findById(10L)).thenReturn(Optional.of(org));
        when(userRepository.findById(1L)).thenReturn(Optional.of(inviterUser));

        UUID token = UUID.randomUUID();
        Invitation saved = Invitation.builder()
                .id(1L).organization(org).email("new@example.com")
                .role(MemberRole.MEMBER).token(token).invitedBy(inviterUser)
                .expiresAt(OffsetDateTime.now().plusDays(7))
                .createdAt(OffsetDateTime.now())
                .build();
        when(invitationRepository.save(any())).thenReturn(saved);

        InvitationResponse response = invitationService.sendInvitation(
                new InviteRequest("new@example.com", MemberRole.MEMBER), ownerCaller);

        assertThat(response.email()).isEqualTo("new@example.com");
        assertThat(response.role()).isEqualTo(MemberRole.MEMBER);
        verify(emailService).sendInvitationEmail(eq("new@example.com"), eq("Acme"), eq("Owner"), anyString());
    }

    @Test
    void sendInvitation_userAlreadyMember_throwsConflict() {
        when(userRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(existingUser));
        when(memberRepository.existsByOrganizationIdAndUserId(10L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> invitationService.sendInvitation(
                new InviteRequest("existing@example.com", MemberRole.MEMBER), ownerCaller))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already a member");
    }

    @Test
    void sendInvitation_pendingInvitationExists_throwsConflict() {
        when(userRepository.findByEmail("pending@example.com")).thenReturn(Optional.empty());

        Invitation pending = Invitation.builder()
                .email("pending@example.com").expiresAt(OffsetDateTime.now().plusDays(5)).build();
        when(invitationRepository.findByOrganizationIdAndAcceptedAtIsNull(10L))
                .thenReturn(List.of(pending));

        assertThatThrownBy(() -> invitationService.sendInvitation(
                new InviteRequest("pending@example.com", MemberRole.MEMBER), ownerCaller))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("pending invitation");
    }

    // ------------------------------------------------------------------ acceptInvitation

    @Test
    void acceptInvitation_existingUser_joinsOrg() {
        UUID token = UUID.randomUUID();
        Invitation invitation = Invitation.builder()
                .id(1L).organization(org).email("existing@example.com")
                .role(MemberRole.MEMBER).token(token).invitedBy(inviterUser)
                .expiresAt(OffsetDateTime.now().plusDays(3))
                .build();

        when(invitationRepository.findByToken(token)).thenReturn(Optional.of(invitation));
        when(userRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(existingUser));
        when(memberRepository.existsByOrganizationIdAndUserId(10L, 2L)).thenReturn(false);
        when(memberRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(invitationRepository.save(any())).thenReturn(invitation);
        when(jwtUtil.generateAccessToken(anyLong(), anyLong(), anyString(), any(), anyBoolean())).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken()).thenReturn("refresh-token");
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse response = invitationService.acceptInvitation(
                new AcceptInvitationRequest(token, null, null));

        assertThat(response.user().email()).isEqualTo("existing@example.com");
        assertThat(response.role()).isEqualTo(MemberRole.MEMBER);
        verify(memberRepository).save(any(OrganizationMember.class));
        assertThat(invitation.getAcceptedAt()).isNotNull();
    }

    @Test
    void acceptInvitation_newUser_registersAndJoinsOrg() {
        UUID token = UUID.randomUUID();
        Invitation invitation = Invitation.builder()
                .id(1L).organization(org).email("new@example.com")
                .role(MemberRole.MEMBER).token(token).invitedBy(inviterUser)
                .expiresAt(OffsetDateTime.now().plusDays(3))
                .build();

        User newUser = User.builder().id(5L).email("new@example.com").fullName("New User").build();

        when(invitationRepository.findByToken(token)).thenReturn(Optional.of(invitation));
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenReturn(newUser);
        when(memberRepository.existsByOrganizationIdAndUserId(10L, 5L)).thenReturn(false);
        when(memberRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(invitationRepository.save(any())).thenReturn(invitation);
        when(jwtUtil.generateAccessToken(anyLong(), anyLong(), anyString(), any(), anyBoolean())).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken()).thenReturn("refresh-token");
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse response = invitationService.acceptInvitation(
                new AcceptInvitationRequest(token, "New User", "password123"));

        assertThat(response.user().email()).isEqualTo("new@example.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void acceptInvitation_expiredToken_throwsBadRequest() {
        UUID token = UUID.randomUUID();
        Invitation expired = Invitation.builder()
                .organization(org).email("x@x.com")
                .expiresAt(OffsetDateTime.now().minusDays(1))
                .token(token).invitedBy(inviterUser)
                .build();

        when(invitationRepository.findByToken(token)).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> invitationService.acceptInvitation(
                new AcceptInvitationRequest(token, null, null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void acceptInvitation_alreadyAccepted_throwsBadRequest() {
        UUID token = UUID.randomUUID();
        Invitation accepted = Invitation.builder()
                .organization(org).email("x@x.com")
                .expiresAt(OffsetDateTime.now().plusDays(3))
                .acceptedAt(OffsetDateTime.now().minusHours(1))
                .token(token).invitedBy(inviterUser)
                .build();

        when(invitationRepository.findByToken(token)).thenReturn(Optional.of(accepted));

        assertThatThrownBy(() -> invitationService.acceptInvitation(
                new AcceptInvitationRequest(token, null, null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already been accepted");
    }

    @Test
    void acceptInvitation_newUserWithoutFullName_throwsBadRequest() {
        UUID token = UUID.randomUUID();
        Invitation invitation = Invitation.builder()
                .organization(org).email("new@example.com")
                .expiresAt(OffsetDateTime.now().plusDays(3))
                .token(token).invitedBy(inviterUser)
                .build();

        when(invitationRepository.findByToken(token)).thenReturn(Optional.of(invitation));
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invitationService.acceptInvitation(
                new AcceptInvitationRequest(token, null, "password123")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Full name");
    }

    @Test
    void acceptInvitation_newUserWithShortPassword_throwsBadRequest() {
        UUID token = UUID.randomUUID();
        Invitation invitation = Invitation.builder()
                .organization(org).email("new@example.com")
                .expiresAt(OffsetDateTime.now().plusDays(3))
                .token(token).invitedBy(inviterUser)
                .build();

        when(invitationRepository.findByToken(token)).thenReturn(Optional.of(invitation));
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invitationService.acceptInvitation(
                new AcceptInvitationRequest(token, "New User", "short")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("8 characters");
    }

    @Test
    void acceptInvitation_tokenNotFound_throwsNotFound() {
        UUID token = UUID.randomUUID();
        when(invitationRepository.findByToken(token)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invitationService.acceptInvitation(
                new AcceptInvitationRequest(token, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
