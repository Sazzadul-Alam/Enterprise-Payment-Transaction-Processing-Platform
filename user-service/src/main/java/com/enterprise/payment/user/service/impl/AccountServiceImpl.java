package com.enterprise.payment.user.service.impl;

import com.enterprise.payment.common.exception.BusinessValidationException;
import com.enterprise.payment.common.exception.ResourceNotFoundException;
import com.enterprise.payment.user.dto.AccountResponse;
import com.enterprise.payment.user.dto.CreateAccountRequest;
import com.enterprise.payment.user.dto.UpdateAccountStatusRequest;
import com.enterprise.payment.user.entity.Account;
import com.enterprise.payment.user.entity.User;
import com.enterprise.payment.user.entity.enums.AccountStatus;
import com.enterprise.payment.user.entity.enums.KycStatus;
import com.enterprise.payment.user.repository.AccountRepository;
import com.enterprise.payment.user.repository.UserRepository;
import com.enterprise.payment.user.service.AccountService;
import com.enterprise.payment.user.util.AccountNumberGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final AccountNumberGenerator accountNumberGenerator;

    @Override
    @Transactional
    public AccountResponse createAccount(UUID userId, CreateAccountRequest request, String correlationId) {
        User user = userRepository.findById(userId)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (user.getKycStatus() == KycStatus.REJECTED) {
            log.warn("Account creation rejected: user {} KYC is REJECTED [corr: {}]", userId, correlationId);
            throw new BusinessValidationException(
                    "Cannot open account for user with REJECTED KYC status",
                    "KYC_REJECTED"
            );
        }

        String accountNumber = accountNumberGenerator.generateUniqueAccountNumber();
        String currency = (request.getCurrency() != null && !request.getCurrency().isBlank())
                ? request.getCurrency().toUpperCase().trim()
                : "USD";

        Account account = Account.builder()
                .user(user)
                .accountNumber(accountNumber)
                .accountType(request.getAccountType())
                .currency(currency)
                .balance(BigDecimal.ZERO)
                .status(AccountStatus.ACTIVE)
                .build();

        account = accountRepository.save(account);
        log.info("Created account {} ({}) for user {} [corr: {}]",
                account.getId(), accountNumber, userId, correlationId);

        return mapToResponse(account);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getAccountById(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountId));
        return mapToResponse(account);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getAccountByNumber(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account with number '" + accountNumber + "' was not found"));
        return mapToResponse(account);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountResponse> getAccountsByUserId(UUID userId) {
        userRepository.findById(userId)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        return accountRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public AccountResponse updateAccountStatus(UUID accountId, UpdateAccountStatusRequest request, String correlationId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountId));

        AccountStatus currentStatus = account.getStatus();
        AccountStatus targetStatus = request.getStatus();

        if (currentStatus == AccountStatus.CLOSED) {
            log.warn("Cannot change status of CLOSED account {} [corr: {}]", accountId, correlationId);
            throw new BusinessValidationException(
                    "Cannot change status of an already CLOSED account",
                    "ACCOUNT_ALREADY_CLOSED"
            );
        }

        if (targetStatus == AccountStatus.CLOSED && account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            log.warn("Cannot close account {} with non-zero balance {} [corr: {}]",
                    accountId, account.getBalance(), correlationId);
            throw new BusinessValidationException(
                    "Cannot close account with non-zero balance: " + account.getBalance(),
                    "NON_ZERO_BALANCE"
            );
        }

        account.setStatus(targetStatus);
        account = accountRepository.save(account);
        log.info("Updated account {} status from {} to {} [corr: {}]",
                accountId, currentStatus, targetStatus, correlationId);

        return mapToResponse(account);
    }

    private AccountResponse mapToResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .userId(account.getUser().getId())
                .accountNumber(account.getAccountNumber())
                .accountType(account.getAccountType())
                .currency(account.getCurrency())
                .balance(account.getBalance())
                .status(account.getStatus())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }
}
