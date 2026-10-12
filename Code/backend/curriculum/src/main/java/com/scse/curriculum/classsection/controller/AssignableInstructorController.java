package com.scse.curriculum.classsection.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scse.curriculum.auth.security.CurrentUserService;
import com.scse.curriculum.common.exception.ForbiddenOperationException;
import com.scse.curriculum.user.entity.UserAccount;
import com.scse.curriculum.user.entity.UserRole;
import com.scse.curriculum.user.repository.UserAccountRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/class-sections/assignable-instructors")
public class AssignableInstructorController {

    private final UserAccountRepository userAccountRepository;
    private final CurrentUserService currentUserService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPT_HEAD')")
    public List<InstructorOption> getAssignableInstructors() {

        UserAccount actor = currentUserService.getCurrentUser();

        // Check the actual database role, not only JWT authorities.
        if (actor.getRole() != UserRole.ADMIN
                && actor.getRole() != UserRole.DEPT_HEAD) {
            throw new ForbiddenOperationException(
                    "You cannot view assignable instructors.");
        }

        if (actor.getRole() == UserRole.DEPT_HEAD
                && (actor.getManagedMajor() == null
                    || actor.getManagedMajor().getId() == null)) {
            throw new ForbiddenOperationException(
                    "The Head of Department has no Managed Major.");
        }

        return userAccountRepository
                .findByRoleAndIsActiveTrue(UserRole.INSTRUCTOR)
                .stream()
                .map(user -> new InstructorOption(
                        user.getId(),
                        user.getFullName(),
                        user.getUsername()))
                .toList();
    }

    public record InstructorOption(
            Integer id,
            String fullName,
            String username
    ) {}
}