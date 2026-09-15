package com.scse.curriculum.config;

import com.scse.curriculum.user.entity.UserRole;
import com.scse.curriculum.user.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Checks the persisted account role.
 *
 * This guard intentionally reads the role from the database instead of relying
 * only on temporary Spring Security authorities.
 */
@Component("phaseRoleGuard")
@RequiredArgsConstructor
public class PhaseRoleGuard {

    private final UserAccountRepository users;

    public boolean isAdmin(Authentication authentication) {
        return activePersistedRole(authentication) == UserRole.ADMIN;
    }

    public boolean isAdminOrDean(Authentication authentication) {
        UserRole role = activePersistedRole(authentication);

        return role == UserRole.ADMIN
                || role == UserRole.DEAN;
    }

    private UserRole activePersistedRole(
            Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()) {
            return null;
        }

        return users
                .findByUsernameIgnoreCaseOrEmailIgnoreCase(
                        authentication.getName(),
                        authentication.getName())
                .map(user ->
                        Boolean.TRUE.equals(user.getIsActive())
                                ? user.getRole()
                                : null)
                .orElse(null);
    }
}