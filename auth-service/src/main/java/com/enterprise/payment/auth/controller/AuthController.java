package com.enterprise.payment.auth.controller;

import com.enterprise.payment.auth.dto.*;
import com.enterprise.payment.auth.security.UserPrincipal;
import com.enterprise.payment.auth.service.AuthService;
import com.enterprise.payment.common.constant.HeaderConstants;
import com.enterprise.payment.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            @RequestHeader(value = HeaderConstants.CORRELATION_ID, required = false) String correlationId) {

        String cid = resolveCorrelationId(correlationId);
        AuthResponse response = authService.register(request, cid);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "User registered successfully", cid));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            @RequestHeader(value = HeaderConstants.CORRELATION_ID, required = false) String correlationId) {

        String cid = resolveCorrelationId(correlationId);
        AuthResponse response = authService.login(request, cid);
        return ResponseEntity.ok(ApiResponse.success(response, "Login successful", cid));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request,
            @RequestHeader(value = HeaderConstants.CORRELATION_ID, required = false) String correlationId) {

        String cid = resolveCorrelationId(correlationId);
        AuthResponse response = authService.refreshToken(request, cid);
        return ResponseEntity.ok(ApiResponse.success(response, "Token refreshed successfully", cid));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestBody(required = false) RefreshTokenRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = HeaderConstants.CORRELATION_ID, required = false) String correlationId) {

        String cid = resolveCorrelationId(correlationId);
        String refreshToken = (request != null) ? request.getRefreshToken() : null;
        UUID userId = (principal != null) ? principal.getId() : null;

        authService.logout(refreshToken, userId, cid);
        return ResponseEntity.ok(ApiResponse.success(null, "Logged out successfully", cid));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthUserResponse>> getCurrentUser(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = HeaderConstants.CORRELATION_ID, required = false) String correlationId) {

        String cid = resolveCorrelationId(correlationId);
        AuthUserResponse response = authService.getCurrentUser(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(response, "User profile retrieved successfully", cid));
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, String>>> getStatus(
            @RequestHeader(value = HeaderConstants.CORRELATION_ID, required = false) String correlationId) {

        String cid = resolveCorrelationId(correlationId);
        Map<String, String> status = Map.of(
                "service", "auth-service",
                "status", "OPERATIONAL"
        );
        return ResponseEntity.ok(ApiResponse.success(status, "Auth service operational", cid));
    }

    private String resolveCorrelationId(String correlationId) {
        return StringUtils.hasText(correlationId) ? correlationId : UUID.randomUUID().toString();
    }
}
