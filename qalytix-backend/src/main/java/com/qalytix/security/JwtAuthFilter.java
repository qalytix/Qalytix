package com.qalytix.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {
        try {
            String token = extractToken(request);
            if (token != null && jwtUtil.isValid(token)) {
                authenticate(token);
            }
            chain.doFilter(request, response);
        } finally {
            // Only clear the tenant thread-local; Spring Security 7's
            // SecurityContextHolderFilter manages SecurityContext cleanup itself.
            TenantContext.clear();
        }
    }

    private void authenticate(String token) {
        Long userId    = jwtUtil.extractUserId(token);
        Long orgId     = jwtUtil.extractOrgId(token);
        String email   = jwtUtil.extractEmail(token);
        var role       = jwtUtil.extractRole(token);
        boolean isSuperAdmin = jwtUtil.extractSuperAdmin(token);

        TenantContext.setOrgId(orgId);

        var principal = new AuthenticatedUser(userId, orgId, email, role, isSuperAdmin);

        var authorities = isSuperAdmin
                ? List.of(new SimpleGrantedAuthority("ROLE_" + role.name()),
                          new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))
                : List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));

        var auth = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
