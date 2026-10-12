package com.scse.curriculum.classsection.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;

import com.scse.curriculum.auth.security.CurrentUserService;
import com.scse.curriculum.common.exception.ForbiddenOperationException;
import com.scse.curriculum.major.entity.Major;
import com.scse.curriculum.user.entity.UserAccount;
import com.scse.curriculum.user.entity.UserRole;
import com.scse.curriculum.user.repository.UserAccountRepository;

@ExtendWith(MockitoExtension.class)
class AssignableInstructorControllerTest {

    @Mock
    private UserAccountRepository repository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private AssignableInstructorController controller;

    @Test
    void adminCanLoadActiveInstructorOptions() {
        UserAccount admin = UserAccount.builder()
                .role(UserRole.ADMIN)
                .build();

        UserAccount instructor = UserAccount.builder()
                .id(201)
                .role(UserRole.INSTRUCTOR)
                .fullName("Test Instructor")
                .username("instructor01")
                .isActive(true)
                .build();

        when(currentUserService.getCurrentUser())
                .thenReturn(admin);

        when(repository.findByRoleAndIsActiveTrue(UserRole.INSTRUCTOR))
                .thenReturn(List.of(instructor));

        var result = controller.getAssignableInstructors();

        assertEquals(1, result.size());
        assertEquals(201, result.get(0).id());
        assertEquals("Test Instructor", result.get(0).fullName());
        assertEquals("instructor01", result.get(0).username());

        verify(repository)
                .findByRoleAndIsActiveTrue(UserRole.INSTRUCTOR);
    }

    @Test
    void departmentHeadWithManagedMajorCanLoadInstructors() {
        Major major = Major.builder()
                .id(7)
                .build();

        UserAccount head = UserAccount.builder()
                .role(UserRole.DEPT_HEAD)
                .managedMajor(major)
                .build();

        when(currentUserService.getCurrentUser())
                .thenReturn(head);

        when(repository.findByRoleAndIsActiveTrue(UserRole.INSTRUCTOR))
                .thenReturn(List.of());

        assertTrue(controller.getAssignableInstructors().isEmpty());

        verify(repository)
                .findByRoleAndIsActiveTrue(UserRole.INSTRUCTOR);
    }

    @Test
    void departmentHeadWithoutManagedMajorIsForbidden() {
        UserAccount head = UserAccount.builder()
                .role(UserRole.DEPT_HEAD)
                .build();

        when(currentUserService.getCurrentUser())
                .thenReturn(head);

        assertThrows(
                ForbiddenOperationException.class,
                () -> controller.getAssignableInstructors()
        );

        verifyNoInteractions(repository);
    }

    @Test
    void instructorCannotLoadInstructorOptions() {
        UserAccount instructor = UserAccount.builder()
                .role(UserRole.INSTRUCTOR)
                .build();

        when(currentUserService.getCurrentUser())
                .thenReturn(instructor);

        assertThrows(
                ForbiddenOperationException.class,
                () -> controller.getAssignableInstructors()
        );

        verifyNoInteractions(repository);
    }

    @Test
    void deanCannotLoadInstructorOptions() {
        UserAccount dean = UserAccount.builder()
                .role(UserRole.DEAN)
                .build();

        when(currentUserService.getCurrentUser())
                .thenReturn(dean);

        assertThrows(
                ForbiddenOperationException.class,
                () -> controller.getAssignableInstructors()
        );

        verifyNoInteractions(repository);
    }

    @Test
    void endpointHasExpectedRoleAnnotation() throws Exception {
        var method = AssignableInstructorController.class
                .getMethod("getAssignableInstructors");

        PreAuthorize annotation =
                method.getAnnotation(PreAuthorize.class);

        assertNotNull(annotation);
        assertEquals(
                "hasAnyRole('ADMIN', 'DEPT_HEAD')",
                annotation.value()
        );

        assertEquals(
                3,
                AssignableInstructorController.InstructorOption.class
                        .getRecordComponents().length
        );
    }
}