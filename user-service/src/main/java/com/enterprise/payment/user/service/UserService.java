package com.enterprise.payment.user.service;

import com.enterprise.payment.user.dto.CreateUserRequest;
import com.enterprise.payment.user.dto.UpdateKycRequest;
import com.enterprise.payment.user.dto.UpdateUserRequest;
import com.enterprise.payment.user.dto.UserResponse;

import java.util.UUID;

public interface UserService {

    UserResponse createUser(CreateUserRequest request, String correlationId);

    UserResponse getUserById(UUID userId);

    UserResponse getUserByAuthUserId(UUID authUserId);

    UserResponse updateUser(UUID userId, UpdateUserRequest request, String correlationId);

    void deleteUser(UUID userId, String correlationId);

    UserResponse updateKycStatus(UUID userId, UpdateKycRequest request, String correlationId);
}
