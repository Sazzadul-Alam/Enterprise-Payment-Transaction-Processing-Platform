package com.enterprise.payment.user.repository;

import com.enterprise.payment.user.entity.Account;
import com.enterprise.payment.user.entity.enums.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {

    Optional<Account> findByAccountNumber(String accountNumber);

    List<Account> findByUserIdAndStatus(UUID userId, AccountStatus status);

    List<Account> findByUserId(UUID userId);

    boolean existsByAccountNumber(String accountNumber);
}
