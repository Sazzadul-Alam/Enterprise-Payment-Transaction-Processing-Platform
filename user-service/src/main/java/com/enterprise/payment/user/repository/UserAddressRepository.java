package com.enterprise.payment.user.repository;

import com.enterprise.payment.user.entity.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserAddressRepository extends JpaRepository<UserAddress, UUID> {

    List<UserAddress> findByUserId(UUID userId);

    Optional<UserAddress> findByUserIdAndPrimaryTrue(UUID userId);
}
