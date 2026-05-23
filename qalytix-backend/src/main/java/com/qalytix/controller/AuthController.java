package com.qalytix.controller;

import com.qalytix.dto.request.LoginRequest;
import com.qalytix.dto.request.RefreshRequest;
import com.qalytix.dto.request.RegisterRequest;
import com.qalytix.dto.response.ApiResponse;
import com.qalytix.dto.response.AuthResponse;
import com.qalytix.security.AuthenticatedUser;
import com.qalytix.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(authService.register(request), "Account created successfully");
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request), "Login successful");
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        authService.logout(currentUser.userId(), currentUser.orgId());
        return ApiResponse.ok("Logged out successfully");
    }
}
