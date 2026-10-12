package com.enterprise.payment.auth.service;

import com.enterprise.payment.auth.dto.AuthResponse;
import com.enterprise.payment.auth.dto.AuthUserResponse;
import com.enterprise.payment.auth.dto.LoginRequest;
import com.enterprise.payment.auth.dto.RefreshTokenRequest;
import com.enterprise.payment.auth.dto.RegisterRequest;

import java.util.UUID;

public interface AuthService {

    /**
     * Registers a new user credentials record, assigns default ROLE_USER, and returns JWT tokens.
     */
    AuthResponse register(RegisterRequest request, String correlationId);

    /**
     * Authenticates a user by email & password with brute-force lockout protection, returns JWT tokens.
     */
    AuthResponse login(LoginRequest request, String correlationId);

    /**
     * Rotates refresh token and issues a new access token and refresh token.
     */
    AuthResponse refreshToken(RefreshTokenRequest request, String correlationId);

    /**
     * Revokes active refresh token(s) for the user session.
     */
    void logout(String refreshToken, UUID currentUserId, String correlationId);

    /**
     * Retrieves current authenticated user profile summary.
     */
    AuthUserResponse getCurrentUser(UUID currentUserId);
}
