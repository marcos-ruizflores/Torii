package com.torii.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;

/** Access to the {@code auth_tokens} table. */
public interface AuthTokenRepository extends JpaRepository<AuthToken, Long> {

    Optional<AuthToken> findByTokenHashAndPurpose(String tokenHash, AuthTokenPurpose purpose);

    Optional<AuthToken> findFirstByUserIdAndPurposeOrderByCreatedAtDesc(Long userId, AuthTokenPurpose purpose);

    /** Burns every pending link of that kind, so only the newest one works. */
    @Modifying
    @Query("update AuthToken t set t.usedAt = :now "
            + "where t.user.id = :userId and t.purpose = :purpose and t.usedAt is null")
    int invalidatePending(Long userId, AuthTokenPurpose purpose, Instant now);
}
