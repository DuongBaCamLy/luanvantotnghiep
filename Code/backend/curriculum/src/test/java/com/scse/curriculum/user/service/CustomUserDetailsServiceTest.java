package com.scse.curriculum.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.scse.curriculum.user.entity.UserAccount;
import com.scse.curriculum.user.entity.UserRole;
import com.scse.curriculum.user.repository.UserAccountRepository;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {
    @Mock UserAccountRepository repository;
    @InjectMocks CustomUserDetailsService service;

    @Test
    void instructorReceivesOnlyInstructorAuthority() {
        when(repository.findByUsernameIgnoreCaseOrEmailIgnoreCase("faculty", "faculty"))
                .thenReturn(Optional.of(user(UserRole.INSTRUCTOR)));

        assertThat(service.loadUserByUsername("faculty").getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_INSTRUCTOR");
    }

    @Test
    void otherRolesRetainCurrentPhaseAuthorities() {
        for (UserRole role : new UserRole[] { UserRole.ADMIN, UserRole.DEAN, UserRole.DEPT_HEAD }) {
            String login = role.name().toLowerCase();
            when(repository.findByUsernameIgnoreCaseOrEmailIgnoreCase(login, login))
                    .thenReturn(Optional.of(user(role)));
            var authorities = service.loadUserByUsername(login).getAuthorities();
            assertThat(authorities).extracting(Object::toString).contains("ROLE_ADMIN");
            if (role != UserRole.ADMIN) {
                assertThat(authorities).extracting(Object::toString).contains("ROLE_" + role.name());
            }
        }
    }

    private UserAccount user(UserRole role) {
        return UserAccount.builder().username(role.name().toLowerCase())
                .passwordHash("hash").role(role).isActive(true).build();
    }
}
