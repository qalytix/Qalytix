package com.qalytix.service;

import com.qalytix.dto.request.*;
import com.qalytix.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(RefreshRequest request);

    void logout(Long userId, Long orgId);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);

    void changePassword(Long userId, ChangePasswordRequest request);
}
