package com.enterprise.payment.auth.service.impl;

import com.enterprise.payment.auth.config.JwtProperties;
import com.enterprise.payment.auth.config.LockoutProperties;
import com.enterprise.payment.auth.dto.*;
import com.enterprise.payment.auth.entity.AuthUser;
import com.enterprise.payment.auth.entity.RefreshToken;
import com.enterprise.payment.auth.entity.Role;
import com.enterprise.payment.auth.entity.enums.RoleName;
import com.enterprise.payment.auth.repository.AuthUserRepository;
import com.enterprise.payment.auth.repository.RefreshTokenRepository;
import com.enterprise.payment.auth.repository.RoleRepository;
import com.enterprise.payment.auth.security.JwtTokenProvider;
import com.enterprise.payment.auth.security.TokenHashUtil;
import com.enterprise.payment.auth.service.AuthService;
import com.enterprise.payment.common.exception.DuplicateResourceException;
import com.enterprise.payment.common.exception.ResourceNotFoundException;
import com.enterprise.payment.common.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthUserRepository authUserRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final LockoutProperties lockoutProperties;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request, String correlationId) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (authUserRepository.existsByEmail(normalizedEmail)) {
            log.warn("Registration rejected - duplicate email: [corr: {}]", correlationId);
            throw new DuplicateResourceException("User already exists with email: " + request.getEmail());
        }

        Role userRole = roleRepository.findByName(RoleName.ROLE_USER.name())
                .orElseGet(() -> roleRepository.save(
                        Role.builder()
                                .name(RoleName.ROLE_USER.name())
                                .description("Standard authenticated user")
                                .build()
                ));

        Set<Role> roles = new HashSet<>();
        roles.add(userRole);

        AuthUser authUser = AuthUser.builder()
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .active(true)
                .locked(false)
                .failedAttempts(0)
                .roles(roles)
                .build();

        authUser = authUserRepository.save(authUser);
        log.info("Registered new user id={} email={} [corr: {}]", authUser.getId(), normalizedEmail, correlationId);

        return issueTokens(authUser);
    }

    @Override
    @Transactional(noRollbackFor = UnauthorizedException.class)
    public AuthResponse login(LoginRequest request, String correlationId) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        OffsetDateTime now = OffsetDateTime.now();

        AuthUser authUser = authUserRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> {
                    log.warn("Login failed: unknown email {} [corr: {}]", normalizedEmail, correlationId);
                    return new UnauthorizedException("Invalid email or password");
                });

        // Check and handle lockout state
        if (authUser.isLocked()) {
            if (authUser.getLockedUntil() != null && authUser.getLockedUntil().isAfter(now)) {
                log.warn("Login rejected: account is locked for user {} [corr: {}]", authUser.getId(), correlationId);
                throw new UnauthorizedException("Account is locked due to multiple failed login attempts. Try again later.");
            } else {
                // Lock duration has elapsed -> auto unlock
                authUser.setLocked(false);
                authUser.setFailedAttempts(0);
                authUser.setLockedUntil(null);
            }
        }

        if (!authUser.isActive()) {
            log.warn("Login rejected: account inactive for user {} [corr: {}]", authUser.getId(), correlationId);
            throw new UnauthorizedException("Account is inactive. Please contact support.");
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), authUser.getPasswordHash())) {
            int attempts = authUser.getFailedAttempts() + 1;
            authUser.setFailedAttempts(attempts);

            if (attempts >= lockoutProperties.getMaxAttempts()) {
                authUser.setLocked(true);
                authUser.setLockedUntil(now.plusMinutes(lockoutProperties.getLockDurationMinutes()));
                log.warn("User {} locked out after {} failed attempts [corr: {}]",
                        authUser.getId(), attempts, correlationId);
            }

            authUserRepository.saveAndFlush(authUser);
            throw new UnauthorizedException("Invalid email or password");
        }

        // Login successful -> reset counters
        authUser.setFailedAttempts(0);
        authUser.setLocked(false);
        authUser.setLockedUntil(null);
        authUser.setLastLoginAt(now);
        authUserRepository.saveAndFlush(authUser);

        log.info("User {} logged in successfully [corr: {}]", authUser.getId(), correlationId);
        return issueTokens(authUser);
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request, String correlationId) {
        String tokenHash = TokenHashUtil.hash(request.getRefreshToken());
        OffsetDateTime now = OffsetDateTime.now();

        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> {
                    log.warn("Token refresh failed: token not found [corr: {}]", correlationId);
                    return new UnauthorizedException("Invalid refresh token");
                });

        if (refreshToken.isRevoked()) {
            log.warn("Token refresh failed: token was previously revoked for user {} [corr: {}]",
                    refreshToken.getAuthUser().getId(), correlationId);
            throw new UnauthorizedException("Refresh token has been revoked");
        }

        if (refreshToken.getExpiresAt().isBefore(now)) {
            log.warn("Token refresh failed: token expired at {} [corr: {}]",
                    refreshToken.getExpiresAt(), correlationId);
            throw new UnauthorizedException("Refresh token has expired");
        }

        AuthUser user = refreshToken.getAuthUser();
        if (!user.isActive() || user.isLocked()) {
            throw new UnauthorizedException("User account is inactive or locked");
        }

        // Token rotation: revoke current token
        refreshToken.setRevoked(true);
        refreshToken.setRevokedAt(now);
        refreshTokenRepository.save(refreshToken);

        log.info("Refreshed tokens for user {} [corr: {}]", user.getId(), correlationId);
        return issueTokens(user);
    }

    @Override
    @Transactional
    public void logout(String refreshToken, UUID currentUserId, String correlationId) {
        OffsetDateTime now = OffsetDateTime.now();

        if (StringUtils.hasText(refreshToken)) {
            String tokenHash = TokenHashUtil.hash(refreshToken);
            refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
                token.setRevoked(true);
                token.setRevokedAt(now);
                refreshTokenRepository.save(token);
            });
        }

        if (currentUserId != null) {
            refreshTokenRepository.revokeAllByAuthUserId(currentUserId, now);
        }

        log.info("Logged out session for user {} [corr: {}]", currentUserId, correlationId);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthUserResponse getCurrentUser(UUID currentUserId) {
        AuthUser authUser = authUserRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUserId));

        Set<String> roleNames = authUser.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        return AuthUserResponse.builder()
                .id(authUser.getId())
                .email(authUser.getEmail())
                .active(authUser.isActive())
                .roles(roleNames)
                .createdAt(authUser.getCreatedAt())
                .lastLoginAt(authUser.getLastLoginAt())
                .build();
    }

    private AuthResponse issueTokens(AuthUser authUser) {
        Set<String> roleNames = authUser.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        String accessToken = jwtTokenProvider.generateAccessToken(
                authUser.getId(),
                authUser.getEmail(),
                roleNames
        );

        String rawRefreshToken = UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
        String tokenHash = TokenHashUtil.hash(rawRefreshToken);

        OffsetDateTime expiresAt = OffsetDateTime.now()
                .plus(Duration.ofMillis(jwtProperties.getRefreshTokenExpiration()));

        RefreshToken refreshToken = RefreshToken.builder()
                .authUser(authUser)
                .tokenHash(tokenHash)
                .expiresAt(expiresAt)
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getAccessTokenExpiration() / 1000)
                .userId(authUser.getId())
                .email(authUser.getEmail())
                .roles(roleNames)
                .build();
    }
}
