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

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserAccountService {

    private final UserAccountRepository repository;

    private final PasswordEncoder passwordEncoder;

    public List<UserAccountResponse> getAll() {

        return repository
                .findAll()
                .stream()
                .map(UserAccountResponse::fromEntity)
                .toList();
    }

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

        user.setUsername(
                request.getUsername());

        user.setEmail(
                request.getEmail());

        user.setRole(
                request.getRole());

        user.setInstructorId(
                request.getInstructorId());

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

        user.setIsActive(
                !Boolean.TRUE.equals(
                        user.getIsActive()));

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
}