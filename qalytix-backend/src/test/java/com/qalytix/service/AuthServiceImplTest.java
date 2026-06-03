package com.qalytix.service;

import com.qalytix.config.AppProperties;
import com.qalytix.dto.request.LoginRequest;
import com.qalytix.dto.request.RefreshRequest;
import com.qalytix.dto.request.RegisterRequest;
import com.qalytix.dto.response.AuthResponse;
import com.qalytix.entity.*;
import com.qalytix.entity.enums.MemberRole;
import com.qalytix.entity.enums.MemberStatus;
import com.qalytix.entity.enums.OrgStatus;
import com.qalytix.entity.enums.Plan;
import com.qalytix.exception.BadRequestException;
import com.qalytix.exception.ConflictException;
import com.qalytix.exception.UnauthorizedException;
import com.qalytix.repository.*;
import com.qalytix.security.JwtUtil;
import com.qalytix.service.impl.AuthServiceImpl;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceImplTest {

    @Mock UserRepository               userRepository;
    @Mock OrganizationRepository       orgRepository;
    @Mock OrganizationMemberRepository memberRepository;
    @Mock RefreshTokenRepository       refreshTokenRepository;
    @Mock PasswordEncoder              passwordEncoder;
    @Mock JwtUtil                      jwtUtil;
    @Mock AppProperties                appProperties;

    @InjectMocks AuthServiceImpl authService;

    private AppProperties.Jwt jwtProps;

    // ------------------------------------------------------------------ fixtures

    private final Organization org = Organization.builder()
            .id(10L).name("Acme").slug("acme").plan(Plan.FREE).status(OrgStatus.ACTIVE).build();

    private final User user = User.builder()
            .id(1L).email("alice@example.com").passwordHash("hashed").fullName("Alice").build();

    private final OrganizationMember ownerMembership = OrganizationMember.builder()
            .id(100L).organization(org).user(user).role(MemberRole.OWNER).status(MemberStatus.ACTIVE).build();

    @BeforeEach
    void setUp() {
        jwtProps = new AppProperties.Jwt();
        jwtProps.setRefreshExpiryMs(604_800_000L);
        when(appProperties.getJwt()).thenReturn(jwtProps);
        when(jwtUtil.generateAccessToken(anyLong(), anyLong(), anyString(), any(), anyBoolean())).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken()).thenReturn("raw-refresh-token");
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // ------------------------------------------------------------------ register

    @Test
    void register_success_createsUserOrgAndOwnerMembership() {
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(orgRepository.existsBySlug(anyString())).thenReturn(false);
        when(orgRepository.save(any(Organization.class))).thenReturn(org);
        when(memberRepository.save(any(OrganizationMember.class))).thenReturn(ownerMembership);

        AuthResponse response = authService.register(
                new RegisterRequest("alice@example.com", "password123", "Alice", "Acme"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.role()).isEqualTo(MemberRole.OWNER);
        assertThat(response.org().slug()).isEqualTo("acme");

        verify(userRepository).save(any(User.class));
        verify(orgRepository).save(any(Organization.class));
        verify(memberRepository).save(any(OrganizationMember.class));
    }

    @Test
    void register_duplicateEmail_throwsConflict() {
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("alice@example.com", "password123", "Alice", "Acme")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists");

        verify(userRepository, never()).save(any());
    }

    // ------------------------------------------------------------------ login

    @Test
    void login_success_returnsAuthResponse() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(memberRepository.findByUserIdAndStatus(1L, MemberStatus.ACTIVE))
                .thenReturn(List.of(ownerMembership));

        AuthResponse response = authService.login(new LoginRequest("alice@example.com", "password123"));

        assertThat(response.user().email()).isEqualTo("alice@example.com");
        assertThat(response.role()).isEqualTo(MemberRole.OWNER);
    }

    @Test
    void login_userNotFound_throwsUnauthorized() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@example.com", "pass")))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void login_wrongPassword_throwsUnauthorized() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("alice@example.com", "wrong")))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void login_noActiveMembership_throwsBadRequest() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(memberRepository.findByUserIdAndStatus(1L, MemberStatus.ACTIVE)).thenReturn(List.of());

        assertThatThrownBy(() -> authService.login(new LoginRequest("alice@example.com", "password123")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("No active organization");
    }

    // ------------------------------------------------------------------ refresh

    @Test
    void refresh_validToken_returnsNewAccessToken() {
        RefreshToken stored = RefreshToken.builder()
                .user(user).organization(org)
                .tokenHash("hashed-token")
                .expiresAt(OffsetDateTime.now().plusDays(7))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(stored));
        when(memberRepository.findByOrganizationIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(ownerMembership));

        AuthResponse response = authService.refresh(new RefreshRequest("raw-refresh-token"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("raw-refresh-token");
    }

    @Test
    void refresh_tokenNotFound_throwsUnauthorized() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("bogus")))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void refresh_expiredToken_throwsUnauthorized() {
        RefreshToken expired = RefreshToken.builder()
                .user(user).organization(org)
                .tokenHash("hashed")
                .expiresAt(OffsetDateTime.now().minusDays(1))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("raw")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void refresh_revokedToken_throwsUnauthorized() {
        RefreshToken revoked = RefreshToken.builder()
                .user(user).organization(org)
                .tokenHash("hashed")
                .expiresAt(OffsetDateTime.now().plusDays(7))
                .revoked(true)
                .build();

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("raw")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("expired");
    }

    // ------------------------------------------------------------------ logout

    @Test
    void logout_revokesAllRefreshTokensForUserAndOrg() {
        authService.logout(1L, 10L);
        verify(refreshTokenRepository).revokeAllByUserIdAndOrgId(1L, 10L);
    }
}
