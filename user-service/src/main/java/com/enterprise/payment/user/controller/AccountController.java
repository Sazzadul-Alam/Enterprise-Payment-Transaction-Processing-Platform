package com.enterprise.payment.user.controller;

import com.enterprise.payment.common.constant.HeaderConstants;
import com.enterprise.payment.common.dto.ApiResponse;
import com.enterprise.payment.user.dto.AccountResponse;
import com.enterprise.payment.user.dto.CreateAccountRequest;
import com.enterprise.payment.user.dto.UpdateAccountStatusRequest;
import com.enterprise.payment.user.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping("/api/v1/users/{userId}/accounts")
    public ResponseEntity<ApiResponse<AccountResponse>> createAccount(
            @PathVariable UUID userId,
            @Valid @RequestBody CreateAccountRequest request,
            @RequestHeader(value = HeaderConstants.CORRELATION_ID, required = false) String correlationId) {

        String cid = resolveCorrelationId(correlationId);
        AccountResponse response = accountService.createAccount(userId, request, cid);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Account created successfully", cid));
    }

    @GetMapping("/api/v1/users/{userId}/accounts")
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getAccountsByUserId(
            @PathVariable UUID userId,
            @RequestHeader(value = HeaderConstants.CORRELATION_ID, required = false) String correlationId) {

        String cid = resolveCorrelationId(correlationId);
        List<AccountResponse> response = accountService.getAccountsByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Accounts retrieved successfully", cid));
    }

    @GetMapping("/api/v1/accounts/{accountId}")
    public ResponseEntity<ApiResponse<AccountResponse>> getAccountById(
            @PathVariable UUID accountId,
            @RequestHeader(value = HeaderConstants.CORRELATION_ID, required = false) String correlationId) {

        String cid = resolveCorrelationId(correlationId);
        AccountResponse response = accountService.getAccountById(accountId);
        return ResponseEntity.ok(ApiResponse.success(response, "Account retrieved successfully", cid));
    }

    @GetMapping("/api/v1/accounts/by-number/{accountNumber}")
    public ResponseEntity<ApiResponse<AccountResponse>> getAccountByNumber(
            @PathVariable String accountNumber,
            @RequestHeader(value = HeaderConstants.CORRELATION_ID, required = false) String correlationId) {

        String cid = resolveCorrelationId(correlationId);
        AccountResponse response = accountService.getAccountByNumber(accountNumber);
        return ResponseEntity.ok(ApiResponse.success(response, "Account retrieved successfully", cid));
    }

    @PatchMapping("/api/v1/accounts/{accountId}/status")
    public ResponseEntity<ApiResponse<AccountResponse>> updateAccountStatus(
            @PathVariable UUID accountId,
            @Valid @RequestBody UpdateAccountStatusRequest request,
            @RequestHeader(value = HeaderConstants.CORRELATION_ID, required = false) String correlationId) {

        String cid = resolveCorrelationId(correlationId);
        AccountResponse response = accountService.updateAccountStatus(accountId, request, cid);
        return ResponseEntity.ok(ApiResponse.success(response, "Account status updated successfully", cid));
    }

    private String resolveCorrelationId(String correlationId) {
        return StringUtils.hasText(correlationId) ? correlationId : UUID.randomUUID().toString();
    }
}
