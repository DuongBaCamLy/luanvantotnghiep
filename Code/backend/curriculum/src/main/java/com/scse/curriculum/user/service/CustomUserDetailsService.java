package com.scse.curriculum.user.service;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.scse.curriculum.user.entity.UserAccount;
import com.scse.curriculum.user.repository.UserAccountRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserAccountRepository repository;

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        UserAccount user = repository.findByUsernameIgnoreCaseOrEmailIgnoreCase(username, username)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "User not found: " + username));

        String[] phaseRoles = switch (user.getRole()) {
            case ADMIN -> new String[] { "ADMIN" };
            case INSTRUCTOR -> new String[] { "INSTRUCTOR" };
            default -> new String[] { user.getRole().name(), "ADMIN" };
        };

        return User.builder()
                .username(user.getUsername())
                .password(user.getPasswordHash())
                // Instructor permissions are now enforced by their real role.
                // Dean/DeptHead retain the phase baseline authorities until
                // their dedicated permission phases are implemented.
                .roles(phaseRoles)
                .disabled(!user.getIsActive())
                .build();
    }
}
