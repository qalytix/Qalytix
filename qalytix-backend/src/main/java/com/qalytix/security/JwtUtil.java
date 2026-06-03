package com.qalytix.security;

import com.qalytix.config.AppProperties;
import com.qalytix.entity.enums.MemberRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private static final String CLAIM_USER_ID    = "userId";
    private static final String CLAIM_ORG_ID     = "orgId";
    private static final String CLAIM_ROLE       = "role";
    private static final String CLAIM_SUPER_ADMIN = "superAdmin";

    private final AppProperties properties;

    public String generateAccessToken(Long userId, Long orgId, String email, MemberRole role) {
        return generateAccessToken(userId, orgId, email, role, false);
    }

    public String generateAccessToken(Long userId, Long orgId, String email, MemberRole role, boolean superAdmin) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(email)
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_ORG_ID, orgId)
                .claim(CLAIM_ROLE, role.name())
                .claim(CLAIM_SUPER_ADMIN, superAdmin)
                .issuedAt(new Date(now))
                .expiration(new Date(now + properties.getJwt().getExpiryMs()))
                .signWith(signingKey())
                .compact();
    }

    /** Returns a raw random token string. The caller is responsible for hashing before persistence. */
    public String generateRefreshToken() {
        return UUID.randomUUID().toString();
    }

    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid JWT: {}", e.getMessage());
            return false;
        }
    }

    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public Long extractUserId(String token) {
        return parseClaims(token).get(CLAIM_USER_ID, Long.class);
    }

    public Long extractOrgId(String token) {
        return parseClaims(token).get(CLAIM_ORG_ID, Long.class);
    }

    public MemberRole extractRole(String token) {
        String role = parseClaims(token).get(CLAIM_ROLE, String.class);
        return MemberRole.valueOf(role);
    }

    public boolean extractSuperAdmin(String token) {
        Boolean flag = parseClaims(token).get(CLAIM_SUPER_ADMIN, Boolean.class);
        return Boolean.TRUE.equals(flag);
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(
                properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8)
        );
    }
}
