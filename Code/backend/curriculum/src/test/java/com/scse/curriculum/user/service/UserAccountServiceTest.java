package com.scse.curriculum.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.scse.curriculum.major.entity.Major;
import com.scse.curriculum.major.repository.MajorRepository;
import com.scse.curriculum.user.dto.CreateUserRequest;
import com.scse.curriculum.user.entity.UserAccount;
import com.scse.curriculum.user.entity.UserRole;
import com.scse.curriculum.user.repository.UserAccountRepository;

@ExtendWith(MockitoExtension.class)
class UserAccountServiceTest {

    @Mock private UserAccountRepository repository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private MajorRepository majorRepository;
    @InjectMocks private UserAccountService service;

    @Test
    void activeDepartmentHeadRequiresManagedMajor() {
        CreateUserRequest request = request(UserRole.DEPT_HEAD, null);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Managed Major is required");
        verify(repository, never()).save(any());
    }

    @Test
    void cannotCreateSecondActiveHeadForSameMajor() {
        Major cs = Major.builder().id(1).code("CS").name("Computer Science").build();
        CreateUserRequest request = request(UserRole.DEPT_HEAD, 1);
        when(majorRepository.findById(1)).thenReturn(Optional.of(cs));
        when(repository.existsByRoleAndManagedMajor_IdAndIsActiveTrueAndIdNot(
                UserRole.DEPT_HEAD, 1, -1)).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Computer Science already has an active Head of Department.");
        verify(repository, never()).save(any());
    }

    @Test
    void departmentHeadIsSavedWithRealMajorEntity() {
        Major cs = Major.builder().id(1).code("CS").name("Computer Science").build();
        CreateUserRequest request = request(UserRole.DEPT_HEAD, 1);
        when(majorRepository.findById(1)).thenReturn(Optional.of(cs));
        when(passwordEncoder.encode("secret1")).thenReturn("hash");
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(request);

        ArgumentCaptor<UserAccount> saved = ArgumentCaptor.forClass(UserAccount.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getManagedMajor()).isSameAs(cs);
    }

    @Test
    void deanHasFacultyWideScopeWithoutManagedMajor() {
        CreateUserRequest request = request(UserRole.DEAN, null);
        when(passwordEncoder.encode("secret1")).thenReturn("hash");
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(request);

        ArgumentCaptor<UserAccount> saved = ArgumentCaptor.forClass(UserAccount.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getManagedMajor()).isNull();
        verify(majorRepository, never()).findById(any());
    }

    private CreateUserRequest request(UserRole role, Integer managedMajorId) {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername(role.name().toLowerCase());
        request.setEmail(role.name().toLowerCase() + "@example.edu");
        request.setPassword("secret1");
        request.setRole(role);
        request.setManagedMajorId(managedMajorId);
        return request;
    }
}
