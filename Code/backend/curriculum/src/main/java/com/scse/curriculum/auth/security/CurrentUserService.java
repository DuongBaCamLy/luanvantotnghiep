package com.scse.curriculum.auth.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scse.curriculum.common.exception.ForbiddenOperationException;
import com.scse.curriculum.common.exception.ResourceNotFoundException;
import com.scse.curriculum.user.entity.UserAccount;
import com.scse.curriculum.user.repository.UserAccountRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CurrentUserService {

    private final UserAccountRepository userAccountRepository;

    public UserAccount getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(
                        authentication.getPrincipal())) {

            throw new ForbiddenOperationException(
                    "You must sign in to perform this action.");
        }

        return userAccountRepository
                .findByUsernameIgnoreCaseOrEmailIgnoreCase(
                        authentication.getName(),
                        authentication.getName())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "The signed-in account could not be found."));
    }
}