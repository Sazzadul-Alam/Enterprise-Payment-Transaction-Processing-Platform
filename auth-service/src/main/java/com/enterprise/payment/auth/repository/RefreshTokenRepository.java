package com.enterprise.payment.auth.repository;

import com.enterprise.payment.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findByAuthUserIdAndRevokedFalse(UUID authUserId);

    /** Bulk-revoke all active tokens for a user (e.g., on password change or forced logout) */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true, rt.revokedAt = :now WHERE rt.authUser.id = :userId AND rt.revoked = false")
    int revokeAllByAuthUserId(@Param("userId") UUID userId, @Param("now") OffsetDateTime now);

    /** Cleanup: remove expired tokens older than the given cutoff */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") OffsetDateTime cutoff);
}
