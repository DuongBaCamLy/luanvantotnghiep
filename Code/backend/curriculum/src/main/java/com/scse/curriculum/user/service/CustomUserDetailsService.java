package com.scse.curriculum.user.service;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.scse.curriculum.user.entity.UserAccount;
import com.scse.curriculum.user.entity.UserRole;
import com.scse.curriculum.user.repository.UserAccountRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserAccountRepository repository;

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        UserAccount user =
                repository.findByUsernameIgnoreCaseOrEmailIgnoreCase(
                                username,
                                username)
                        .orElseThrow(() ->
                                new UsernameNotFoundException(
                                        "User not found: " + username));

        /*
         * DEAN_SECRETARY là một actor riêng trong database.
         *
         * Tuy nhiên ở tầng Spring Security,
         * thư ký được thêm ROLE_DEAN để có thể sử dụng
         * các API thuộc quyền Dean, đặc biệt là Final Review.
         *
         * ROLE_DEAN_SECRETARY vẫn được giữ để hệ thống biết
         * chính xác actor thật đang đăng nhập.
         */
        String[] securityRoles;

        if (user.getRole() == UserRole.DEAN_SECRETARY) {

            securityRoles = new String[] {
                    "DEAN_SECRETARY",
                    "DEAN"
            };

        } else {

            securityRoles = new String[] {
                    user.getRole().name()
            };
        }

        return User.builder()
                .username(user.getUsername())
                .password(user.getPasswordHash())
                .roles(securityRoles)
                .disabled(!Boolean.TRUE.equals(user.getIsActive()))
                .build();
    }
}