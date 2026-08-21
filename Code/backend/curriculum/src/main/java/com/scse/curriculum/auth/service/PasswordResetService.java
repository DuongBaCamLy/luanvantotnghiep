package com.scse.curriculum.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scse.curriculum.auth.entity.PasswordResetToken;
import com.scse.curriculum.auth.repository.PasswordResetTokenRepository;
import com.scse.curriculum.email.EmailOutboxService;
import com.scse.curriculum.user.entity.UserAccount;
import com.scse.curriculum.user.repository.UserAccountRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    private final UserAccountRepository userAccountRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailOutboxService emailOutboxService;

    @Value("${app.auth.password-reset-expiration-minutes:15}")
    private long expirationMinutes;

    /**
     * Always returns normally even when the email does not exist. This avoids
     * leaking which email addresses are registered in the system.
     */
    @Transactional
    public void requestReset(String email) {
        if (email == null || email.isBlank()) {
            return;
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        userAccountRepository
                .findByEmailIgnoreCase(normalizedEmail)
                .filter(user -> Boolean.TRUE.equals(user.getIsActive()))
                .ifPresent(this::createResetRequest);
    }

    @Transactional
    public void resetPassword(
            String rawToken,
            String newPassword) {

        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException(
                    "Password reset link is invalid or has expired.");
        }

        String tokenHash = hash(rawToken.trim());

        PasswordResetToken resetToken = tokenRepository
                .findByTokenHashAndUsedAtIsNull(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Password reset link is invalid or has expired."));

        LocalDateTime now = LocalDateTime.now();
        if (resetToken.getExpiresAt().isBefore(now)) {
            resetToken.setUsedAt(now);
            tokenRepository.save(resetToken);
            throw new IllegalArgumentException(
                    "Password reset link is invalid or has expired.");
        }

        UserAccount user = userAccountRepository
                .findById(resetToken.getUserId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Password reset link is invalid or has expired."));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new IllegalArgumentException(
                    "This account is currently inactive.");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userAccountRepository.save(user);

        resetToken.setUsedAt(now);
        tokenRepository.save(resetToken);

        // Invalidate any other reset links that may still exist for the user.
        tokenRepository.invalidateActiveTokens(user.getId(), now);
    }

    private void createResetRequest(UserAccount user) {
        LocalDateTime now = LocalDateTime.now();

        // A newly requested link invalidates previous unused links.
        tokenRepository.invalidateActiveTokens(user.getId(), now);

        String rawToken = generateRawToken();
        String tokenHash = hash(rawToken);

        PasswordResetToken token = PasswordResetToken.builder()
                .userId(user.getId())
                .tokenHash(tokenHash)
                .expiresAt(now.plusMinutes(Math.max(1, expirationMinutes)))
                .build();

        tokenRepository.save(token);

        emailOutboxService.enqueuePasswordReset(
                user,
                "/reset-password?token=" + rawToken,
                Math.max(1, expirationMinutes));
    }

    private String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(
                    value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(
                    "SHA-256 is not available", ex);
        }
    }
}
