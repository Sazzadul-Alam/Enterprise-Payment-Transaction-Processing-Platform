package com.enterprise.payment.user.repository;

import com.enterprise.payment.user.entity.User;
import com.enterprise.payment.user.entity.enums.KycStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /** Find active (non-deleted) user by email */
    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    /** Find active user by auth_user_id cross-service reference */
    Optional<User> findByAuthUserIdAndDeletedAtIsNull(UUID authUserId);

    boolean existsByEmailAndDeletedAtIsNull(String email);

    List<User> findByKycStatusAndDeletedAtIsNull(KycStatus kycStatus);
}
