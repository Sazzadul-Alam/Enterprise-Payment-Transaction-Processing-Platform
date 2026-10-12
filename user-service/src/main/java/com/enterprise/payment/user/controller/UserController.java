package com.enterprise.payment.user.controller;

import com.enterprise.payment.common.constant.HeaderConstants;
import com.enterprise.payment.common.dto.ApiResponse;
import com.enterprise.payment.user.dto.*;
import com.enterprise.payment.user.service.AddressService;
import com.enterprise.payment.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AddressService addressService;

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody CreateUserRequest request,
            @RequestHeader(value = HeaderConstants.CORRELATION_ID, required = false) String correlationId) {

        String cid = resolveCorrelationId(correlationId);
        UserResponse response = userService.createUser(request, cid);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "User profile created successfully", cid));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(
            @PathVariable UUID userId,
            @RequestHeader(value = HeaderConstants.CORRELATION_ID, required = false) String correlationId) {

        String cid = resolveCorrelationId(correlationId);
        UserResponse response = userService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success(response, "User retrieved successfully", cid));
    }

    @GetMapping("/by-auth/{authUserId}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserByAuthUserId(
            @PathVariable UUID authUserId,
            @RequestHeader(value = HeaderConstants.CORRELATION_ID, required = false) String correlationId) {

        String cid = resolveCorrelationId(correlationId);
        UserResponse response = userService.getUserByAuthUserId(authUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "User retrieved successfully", cid));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserRequest request,
            @RequestHeader(value = HeaderConstants.CORRELATION_ID, required = false) String correlationId) {

        String cid = resolveCorrelationId(correlationId);
        UserResponse response = userService.updateUser(userId, request, cid);
        return ResponseEntity.ok(ApiResponse.success(response, "User updated successfully", cid));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable UUID userId,
            @RequestHeader(value = HeaderConstants.CORRELATION_ID, required = false) String correlationId) {

        String cid = resolveCorrelationId(correlationId);
        userService.deleteUser(userId, cid);
        return ResponseEntity.ok(ApiResponse.success(null, "User deleted successfully", cid));
    }

    @PatchMapping("/{userId}/kyc")
    public ResponseEntity<ApiResponse<UserResponse>> updateKycStatus(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateKycRequest request,
            @RequestHeader(value = HeaderConstants.CORRELATION_ID, required = false) String correlationId) {

        String cid = resolveCorrelationId(correlationId);
        UserResponse response = userService.updateKycStatus(userId, request, cid);
        return ResponseEntity.ok(ApiResponse.success(response, "KYC status updated successfully", cid));
    }

    @PostMapping("/{userId}/addresses")
    public ResponseEntity<ApiResponse<AddressResponse>> addAddress(
            @PathVariable UUID userId,
            @Valid @RequestBody CreateAddressRequest request,
            @RequestHeader(value = HeaderConstants.CORRELATION_ID, required = false) String correlationId) {

        String cid = resolveCorrelationId(correlationId);
        AddressResponse response = addressService.addAddress(userId, request, cid);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Address added successfully", cid));
    }

    @GetMapping("/{userId}/addresses")
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getAddresses(
            @PathVariable UUID userId,
            @RequestHeader(value = HeaderConstants.CORRELATION_ID, required = false) String correlationId) {

        String cid = resolveCorrelationId(correlationId);
        List<AddressResponse> response = addressService.getAddressesByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Addresses retrieved successfully", cid));
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, String>>> getStatus(
            @RequestHeader(value = HeaderConstants.CORRELATION_ID, required = false) String correlationId) {

        String cid = resolveCorrelationId(correlationId);
        Map<String, String> status = Map.of(
                "service", "user-service",
                "status", "OPERATIONAL"
        );
        return ResponseEntity.ok(ApiResponse.success(status, "User service operational", cid));
    }

    private String resolveCorrelationId(String correlationId) {
        return StringUtils.hasText(correlationId) ? correlationId : UUID.randomUUID().toString();
    }
}
