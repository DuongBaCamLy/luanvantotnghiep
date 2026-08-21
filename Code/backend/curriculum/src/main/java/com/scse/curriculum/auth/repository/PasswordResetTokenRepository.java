package com.scse.curriculum.auth.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.scse.curriculum.auth.entity.PasswordResetToken;

public interface PasswordResetTokenRepository
        extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHashAndUsedAtIsNull(
            String tokenHash);

    @Modifying
    @Query("""
            UPDATE PasswordResetToken token
               SET token.usedAt = :usedAt
             WHERE token.userId = :userId
               AND token.usedAt IS NULL
            """)
    int invalidateActiveTokens(
            @Param("userId") Integer userId,
            @Param("usedAt") LocalDateTime usedAt);
}
