package com.enterprise.payment.user.service;

import com.enterprise.payment.user.dto.AccountResponse;
import com.enterprise.payment.user.dto.CreateAccountRequest;
import com.enterprise.payment.user.dto.UpdateAccountStatusRequest;

import java.util.List;
import java.util.UUID;

public interface AccountService {

    AccountResponse createAccount(UUID userId, CreateAccountRequest request, String correlationId);

    AccountResponse getAccountById(UUID accountId);

    AccountResponse getAccountByNumber(String accountNumber);

    List<AccountResponse> getAccountsByUserId(UUID userId);

    AccountResponse updateAccountStatus(UUID accountId, UpdateAccountStatusRequest request, String correlationId);
}
