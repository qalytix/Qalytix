package com.qalytix.service;

import com.qalytix.dto.request.LoginRequest;
import com.qalytix.dto.request.RefreshRequest;
import com.qalytix.dto.request.RegisterRequest;
import com.qalytix.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(RefreshRequest request);

    void logout(Long userId, Long orgId);
}
