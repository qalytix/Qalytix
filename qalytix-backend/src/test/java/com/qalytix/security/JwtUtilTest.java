package com.qalytix.security;

import com.qalytix.config.AppProperties;
import com.qalytix.entity.enums.MemberRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private static final String SECRET = "test-secret-key-that-is-at-least-32-bytes-long-for-hmac";
    private static final Long USER_ID  = 1L;
    private static final Long ORG_ID   = 10L;
    private static final String EMAIL  = "user@example.com";
    private static final MemberRole ROLE = MemberRole.ADMIN;

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(propsWithExpiry(900_000L));
    }

    @Test
    void generatedToken_isValid() {
        String token = jwtUtil.generateAccessToken(USER_ID, ORG_ID, EMAIL, ROLE);
        assertThat(jwtUtil.isValid(token)).isTrue();
    }

    @Test
    void extractClaims_matchInput() {
        String token = jwtUtil.generateAccessToken(USER_ID, ORG_ID, EMAIL, ROLE);

        assertThat(jwtUtil.extractUserId(token)).isEqualTo(USER_ID);
        assertThat(jwtUtil.extractOrgId(token)).isEqualTo(ORG_ID);
        assertThat(jwtUtil.extractEmail(token)).isEqualTo(EMAIL);
        assertThat(jwtUtil.extractRole(token)).isEqualTo(ROLE);
    }

    @Test
    void expiredToken_isNotValid() {
        // Generate with negative expiry so it's already expired at creation
        JwtUtil expiredJwtUtil = new JwtUtil(propsWithExpiry(-1_000L));
        String expiredToken = expiredJwtUtil.generateAccessToken(USER_ID, ORG_ID, EMAIL, ROLE);

        assertThat(jwtUtil.isValid(expiredToken)).isFalse();
    }

    @Test
    void tamperedToken_isNotValid() {
        String token = jwtUtil.generateAccessToken(USER_ID, ORG_ID, EMAIL, ROLE);
        String tampered = token.substring(0, token.length() - 4) + "XXXX";

        assertThat(jwtUtil.isValid(tampered)).isFalse();
    }

    @Test
    void blankToken_isNotValid() {
        assertThat(jwtUtil.isValid("")).isFalse();
        assertThat(jwtUtil.isValid("not.a.jwt")).isFalse();
    }

    @Test
    void generateRefreshToken_returnsNonBlankString() {
        String token = jwtUtil.generateRefreshToken();
        assertThat(token).isNotBlank();
        // Two calls produce different tokens
        assertThat(token).isNotEqualTo(jwtUtil.generateRefreshToken());
    }

    // ------------------------------------------------------------------ helpers

    private AppProperties propsWithExpiry(long expiryMs) {
        AppProperties props = new AppProperties();
        AppProperties.Jwt jwt = new AppProperties.Jwt();
        jwt.setSecret(SECRET);
        jwt.setExpiryMs(expiryMs);
        jwt.setRefreshExpiryMs(604_800_000L);
        props.setJwt(jwt);
        return props;
    }
}
