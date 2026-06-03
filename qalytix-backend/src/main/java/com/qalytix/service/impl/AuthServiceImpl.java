package com.qalytix.service.impl;

import com.qalytix.dto.request.*;
import com.qalytix.dto.response.AuthResponse;
import com.qalytix.entity.PasswordResetToken;
import com.qalytix.repository.PasswordResetTokenRepository;
import com.qalytix.entity.*;
import com.qalytix.entity.enums.MemberRole;
import com.qalytix.entity.enums.MemberStatus;
import com.qalytix.exception.BadRequestException;
import com.qalytix.exception.ConflictException;
import com.qalytix.exception.ResourceNotFoundException;
import com.qalytix.exception.UnauthorizedException;
import com.qalytix.repository.*;
import com.qalytix.security.JwtUtil;
import com.qalytix.service.AuthService;
import com.qalytix.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
public class AuthServiceImpl implements AuthService {

    private final UserRepository               userRepository;
    private final OrganizationRepository       orgRepository;
    private final OrganizationMemberRepository memberRepository;
    private final RefreshTokenRepository       refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder              passwordEncoder;
    private final JwtUtil                      jwtUtil;
    private final AppProperties                appProperties;
    private final com.qalytix.service.EmailService emailService;

    @Value("${app.base-url:http://localhost:3000}")
    private String appBaseUrl;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("An account with this email already exists");
        }

        User user = userRepository.save(User.builder()
                .email(request.email().toLowerCase().strip())
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName().strip())
                .build());

        Organization org = orgRepository.save(Organization.builder()
                .name(request.orgName().strip())
                .slug(uniqueSlug(request.orgName()))
                .build());

        memberRepository.save(OrganizationMember.builder()
                .organization(org)
                .user(user)
                .role(MemberRole.OWNER)
                .status(MemberStatus.ACTIVE)
                .build());

        return buildAuthResponse(user, org, MemberRole.OWNER);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email().toLowerCase().strip())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        List<OrganizationMember> memberships = memberRepository
                .findByUserIdAndStatus(user.getId(), MemberStatus.ACTIVE);

        if (memberships.isEmpty()) {
            throw new BadRequestException("No active organization found for this account");
        }

        // Pick first active membership; org switching handled by a separate endpoint later
        OrganizationMember membership = memberships.get(0);
        Organization org = membership.getOrganization();

        return buildAuthResponse(user, org, membership.getRole());
    }

    @Override
    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        String hash = hashToken(request.refreshToken());

        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (!stored.isValid()) {
            throw new UnauthorizedException("Refresh token has expired or been revoked");
        }

        User user = stored.getUser();
        Organization org = stored.getOrganization();

        OrganizationMember membership = memberRepository
                .findByOrganizationIdAndUserId(org.getId(), user.getId())
                .orElseThrow(() -> new UnauthorizedException("Membership no longer active"));

        String newAccessToken = jwtUtil.generateAccessToken(
                user.getId(), org.getId(), user.getEmail(), membership.getRole(), user.isSuperAdmin());

        return new AuthResponse(
                newAccessToken,
                request.refreshToken(),   // reuse same refresh token until it expires
                new AuthResponse.UserInfo(user.getId(), user.getEmail(), user.getFullName()),
                new AuthResponse.OrgInfo(org.getId(), org.getName(), org.getSlug(), org.getPlan()),
                membership.getRole(),
                user.isSuperAdmin()
        );
    }

    @Override
    @Transactional
    public void logout(Long userId, Long orgId) {
        refreshTokenRepository.revokeAllByUserIdAndOrgId(userId, orgId);
        log.debug("Revoked refresh tokens for user={} org={}", userId, orgId);
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        // Always return success to prevent email enumeration
        userRepository.findByEmail(request.email().toLowerCase().strip()).ifPresent(user -> {
            passwordResetTokenRepository.deleteAllByUserId(user.getId());

            String rawToken = UUID.randomUUID().toString();
            passwordResetTokenRepository.save(PasswordResetToken.builder()
                    .userId(user.getId())
                    .tokenHash(hashToken(rawToken))
                    .expiresAt(OffsetDateTime.now().plusMinutes(30))
                    .build());

            String resetUrl = appBaseUrl + "/reset-password?token=" + rawToken;
            emailService.sendPasswordResetEmail(user.getEmail(), resetUrl);
            log.info("Password reset email sent to user={}", user.getId());
        });
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String hash = hashToken(request.token());
        PasswordResetToken prt = passwordResetTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset link."));

        if (prt.isExpired()) throw new BadRequestException("This reset link has expired.");
        if (prt.isUsed())    throw new BadRequestException("This reset link has already been used.");

        User user = userRepository.findById(prt.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        prt.setUsedAt(OffsetDateTime.now());
        passwordResetTokenRepository.save(prt);

        // Revoke all active sessions so old passwords can't be used via refresh
        refreshTokenRepository.revokeAllByUserId(user.getId());
        log.info("Password reset completed for user={}", user.getId());
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        log.info("Password changed for user={}", userId);
    }

    // ------------------------------------------------------------------ helpers

    private AuthResponse buildAuthResponse(User user, Organization org, MemberRole role) {
        String accessToken  = jwtUtil.generateAccessToken(user.getId(), org.getId(), user.getEmail(), role, user.isSuperAdmin());
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
                role,
                user.isSuperAdmin()
        );
    }

    private String uniqueSlug(String name) {
        String base = name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .strip()
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");

        if (!orgRepository.existsBySlug(base)) {
            return base;
        }
        // Append short random suffix to resolve collision
        return base + "-" + UUID.randomUUID().toString().substring(0, 6);
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
