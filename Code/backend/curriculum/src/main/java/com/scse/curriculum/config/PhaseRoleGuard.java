package com.scse.curriculum.config;

import com.scse.curriculum.user.entity.UserRole;
import com.scse.curriculum.user.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/** Checks the persisted role, unaffected by temporary phase authorities. */
@Component("phaseRoleGuard")
@RequiredArgsConstructor
public class PhaseRoleGuard {
    private final UserAccountRepository users;

    public boolean isAdmin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return users.findByUsernameIgnoreCaseOrEmailIgnoreCase(
                        authentication.getName(), authentication.getName())
                .map(user -> user.getIsActive() && user.getRole() == UserRole.ADMIN)
                .orElse(false);
    }
}
