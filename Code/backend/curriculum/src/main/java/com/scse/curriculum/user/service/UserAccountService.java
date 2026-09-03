package com.scse.curriculum.user.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scse.curriculum.common.exception.ResourceNotFoundException;
import com.scse.curriculum.user.dto.CreateUserRequest;
import com.scse.curriculum.user.dto.UpdateUserRequest;
import com.scse.curriculum.user.dto.UserAccountResponse;
import com.scse.curriculum.user.entity.UserAccount;
import com.scse.curriculum.user.repository.UserAccountRepository;
import com.scse.curriculum.major.entity.Major;
import com.scse.curriculum.major.repository.MajorRepository;
import com.scse.curriculum.user.entity.UserRole;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserAccountService {

    private final UserAccountRepository repository;

    private final PasswordEncoder passwordEncoder;
    private final MajorRepository majorRepository;

    @Transactional(readOnly = true)
    public List<UserAccountResponse> getAll() {

        return repository
                .findAll()
                .stream()
                .map(UserAccountResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserAccountResponse getById(
            Integer id) {

        UserAccount user =
                findEntityOrThrow(id);

        return UserAccountResponse
                .fromEntity(user);
    }

    @Transactional
    public UserAccountResponse create(
            CreateUserRequest request) {
        Major managedMajor = resolveManagedMajor(request.getRole(), request.getManagedMajorId(), null, true);

        if (repository
                .findByUsername(
                        request.getUsername())
                .isPresent()) {

            throw new DataIntegrityViolationException(
                    "Username already exists");
        }

        if (repository
                .findByEmail(
                        request.getEmail())
                .isPresent()) {

            throw new DataIntegrityViolationException(
                    "Email already exists");
        }

        UserAccount user =
                UserAccount
                        .builder()
                        .username(
                                request.getUsername())
                        .email(
                                request.getEmail())
                        .passwordHash(
                                passwordEncoder.encode(
                                        request.getPassword()))
                        .role(
                                request.getRole())
                        .instructorId(
                                request.getInstructorId())
                        .managedMajor(managedMajor)
                        .isActive(true)
                        .createdAt(
                                LocalDateTime.now())
                        .build();

        UserAccount saved =
                repository.save(user);

        return UserAccountResponse
                .fromEntity(saved);
    }

    @Transactional
    public UserAccountResponse update(
            Integer id,
            UpdateUserRequest request) {

        UserAccount user =
                findEntityOrThrow(id);
        boolean active = request.getIsActive() == null
                ? Boolean.TRUE.equals(user.getIsActive())
                : Boolean.TRUE.equals(request.getIsActive());
        Major managedMajor = resolveManagedMajor(
                request.getRole(), request.getManagedMajorId(), id, active);

        user.setUsername(
                request.getUsername());

        user.setEmail(
                request.getEmail());

        user.setRole(
                request.getRole());

        user.setInstructorId(
                request.getInstructorId());
        user.setManagedMajor(managedMajor);

        if (request.getIsActive() != null) {
            user.setIsActive(
                    request.getIsActive());
        }

        if (request.getPassword() != null
                && !request
                        .getPassword()
                        .isBlank()) {

            user.setPasswordHash(
                    passwordEncoder.encode(
                            request.getPassword()));
        }

        UserAccount saved =
                repository.save(user);

        return UserAccountResponse
                .fromEntity(saved);
    }

    @Transactional
    public void delete(
            Integer id) {

        UserAccount user =
                findEntityOrThrow(id);

        repository.delete(user);
    }

    @Transactional
    public UserAccountResponse toggleActive(
            Integer id) {

        UserAccount user =
                findEntityOrThrow(id);

        boolean activating = !Boolean.TRUE.equals(user.getIsActive());
        if (activating && user.getRole() == UserRole.DEPT_HEAD) {
            resolveManagedMajor(UserRole.DEPT_HEAD,
                    user.getManagedMajor() == null ? null : user.getManagedMajor().getId(),
                    user.getId(), true);
        }
        user.setIsActive(activating);

        UserAccount saved =
                repository.save(user);

        return UserAccountResponse
                .fromEntity(saved);
    }

    private UserAccount findEntityOrThrow(
            Integer id) {

        return repository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with id = "
                                        + id));
    }

    private Major resolveManagedMajor(
            UserRole role, Integer majorId, Integer excludedUserId, boolean active) {
        if (role != UserRole.DEPT_HEAD) {
            return null;
        }
        if (majorId == null) {
            throw new IllegalArgumentException("Managed Major is required for a Head of Department account.");
        }
        Major major = majorRepository.findById(majorId)
                .orElseThrow(() -> new ResourceNotFoundException("Managed Major not found."));
        int excludedId = excludedUserId == null ? -1 : excludedUserId;
        if (active && repository.existsByRoleAndManagedMajor_IdAndIsActiveTrueAndIdNot(
                UserRole.DEPT_HEAD, majorId, excludedId)) {
            throw new IllegalStateException(
                    major.getName() + " already has an active Head of Department.");
        }
        return major;
    }
}
