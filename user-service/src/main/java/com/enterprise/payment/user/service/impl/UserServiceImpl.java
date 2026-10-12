package com.enterprise.payment.user.service.impl;

import com.enterprise.payment.common.exception.BusinessValidationException;
import com.enterprise.payment.common.exception.DuplicateResourceException;
import com.enterprise.payment.common.exception.ResourceNotFoundException;
import com.enterprise.payment.user.dto.*;
import com.enterprise.payment.user.entity.Account;
import com.enterprise.payment.user.entity.User;
import com.enterprise.payment.user.entity.UserAddress;
import com.enterprise.payment.user.entity.enums.AccountStatus;
import com.enterprise.payment.user.entity.enums.AccountType;
import com.enterprise.payment.user.entity.enums.KycStatus;
import com.enterprise.payment.user.repository.AccountRepository;
import com.enterprise.payment.user.repository.UserAddressRepository;
import com.enterprise.payment.user.repository.UserRepository;
import com.enterprise.payment.user.service.UserService;
import com.enterprise.payment.user.util.AccountNumberGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final UserAddressRepository userAddressRepository;
    private final AccountNumberGenerator accountNumberGenerator;

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request, String correlationId) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmailAndDeletedAtIsNull(normalizedEmail)) {
            log.warn("User creation rejected - duplicate email {} [corr: {}]", normalizedEmail, correlationId);
            throw new DuplicateResourceException("User already exists with email: " + request.getEmail());
        }

        if (userRepository.findByAuthUserIdAndDeletedAtIsNull(request.getAuthUserId()).isPresent()) {
            log.warn("User creation rejected - duplicate auth_user_id {} [corr: {}]", request.getAuthUserId(), correlationId);
            throw new DuplicateResourceException("User profile already exists for authUserId: " + request.getAuthUserId());
        }

        User user = User.builder()
                .authUserId(request.getAuthUserId())
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .email(normalizedEmail)
                .phoneNumber(request.getPhoneNumber() != null ? request.getPhoneNumber().trim() : null)
                .dateOfBirth(request.getDateOfBirth())
                .kycStatus(KycStatus.PENDING)
                .build();

        user = userRepository.save(user);

        // Auto-provision initial default WALLET account
        String accountNumber = accountNumberGenerator.generateUniqueAccountNumber();
        Account defaultAccount = Account.builder()
                .user(user)
                .accountNumber(accountNumber)
                .accountType(AccountType.WALLET)
                .currency("USD")
                .balance(BigDecimal.ZERO)
                .status(AccountStatus.ACTIVE)
                .build();

        defaultAccount = accountRepository.save(defaultAccount);
        log.info("Created user profile {} with default WALLET account {} [corr: {}]",
                user.getId(), accountNumber, correlationId);

        return mapToResponse(user, List.of(defaultAccount), Collections.emptyList());
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        List<Account> accounts = accountRepository.findByUserId(userId);
        List<UserAddress> addresses = userAddressRepository.findByUserId(userId);

        return mapToResponse(user, accounts, addresses);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserByAuthUserId(UUID authUserId) {
        User user = userRepository.findByAuthUserIdAndDeletedAtIsNull(authUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User with authUserId '" + authUserId + "' was not found"));

        List<Account> accounts = accountRepository.findByUserId(user.getId());
        List<UserAddress> addresses = userAddressRepository.findByUserId(user.getId());

        return mapToResponse(user, accounts, addresses);
    }

    @Override
    @Transactional
    public UserResponse updateUser(UUID userId, UpdateUserRequest request, String correlationId) {
        User user = userRepository.findById(userId)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setPhoneNumber(request.getPhoneNumber() != null ? request.getPhoneNumber().trim() : null);
        user.setDateOfBirth(request.getDateOfBirth());

        user = userRepository.save(user);
        log.info("Updated user profile {} [corr: {}]", userId, correlationId);

        List<Account> accounts = accountRepository.findByUserId(userId);
        List<UserAddress> addresses = userAddressRepository.findByUserId(userId);

        return mapToResponse(user, accounts, addresses);
    }

    @Override
    @Transactional
    public void deleteUser(UUID userId, String correlationId) {
        User user = userRepository.findById(userId)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        user.setDeletedAt(OffsetDateTime.now());
        userRepository.save(user);

        // Suspend all active accounts belonging to the deleted user
        List<Account> accounts = accountRepository.findByUserId(userId);
        for (Account account : accounts) {
            if (account.getStatus() == AccountStatus.ACTIVE) {
                account.setStatus(AccountStatus.SUSPENDED);
                accountRepository.save(account);
            }
        }

        log.info("Soft-deleted user {} and suspended {} accounts [corr: {}]",
                userId, accounts.size(), correlationId);
    }

    @Override
    @Transactional
    public UserResponse updateKycStatus(UUID userId, UpdateKycRequest request, String correlationId) {
        User user = userRepository.findById(userId)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        KycStatus currentStatus = user.getKycStatus();
        KycStatus targetStatus = request.getKycStatus();

        // KYC state machine transitions
        validateKycTransition(currentStatus, targetStatus);

        user.setKycStatus(targetStatus);
        user = userRepository.save(user);
        log.info("Updated KYC status for user {} from {} to {} [corr: {}]",
                userId, currentStatus, targetStatus, correlationId);

        List<Account> accounts = accountRepository.findByUserId(userId);
        List<UserAddress> addresses = userAddressRepository.findByUserId(userId);

        return mapToResponse(user, accounts, addresses);
    }

    private void validateKycTransition(KycStatus current, KycStatus target) {
        if (current == target) {
            return; // Idempotent
        }

        // VERIFIED cannot transition back to PENDING directly
        if (current == KycStatus.VERIFIED && target == KycStatus.PENDING) {
            throw new BusinessValidationException(
                    "Cannot revert a VERIFIED KYC status back to PENDING",
                    "INVALID_KYC_TRANSITION"
            );
        }
    }

    private UserResponse mapToResponse(User user, List<Account> accounts, List<UserAddress> addresses) {
        List<AccountResponse> accountResponses = accounts != null
                ? accounts.stream().map(this::mapAccount).toList()
                : Collections.emptyList();

        List<AddressResponse> addressResponses = addresses != null
                ? addresses.stream().map(this::mapAddress).toList()
                : Collections.emptyList();

        return UserResponse.builder()
                .id(user.getId())
                .authUserId(user.getAuthUserId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .dateOfBirth(user.getDateOfBirth())
                .kycStatus(user.getKycStatus())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .accounts(accountResponses)
                .addresses(addressResponses)
                .build();
    }

    private AccountResponse mapAccount(Account account) {
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

    private AddressResponse mapAddress(UserAddress address) {
        return AddressResponse.builder()
                .id(address.getId())
                .userId(address.getUser().getId())
                .addressType(address.getAddressType())
                .street(address.getStreet())
                .city(address.getCity())
                .state(address.getState())
                .country(address.getCountry())
                .postalCode(address.getPostalCode())
                .primary(address.isPrimary())
                .createdAt(address.getCreatedAt())
                .build();
    }
}
