package com.scse.curriculum.user.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scse.curriculum.user.dto.CreateUserRequest;
import com.scse.curriculum.user.dto.UpdateUserRequest;
import com.scse.curriculum.user.dto.UserAccountResponse;
import com.scse.curriculum.user.service.UserAccountService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserAccountController {

    private final UserAccountService service;

    @GetMapping
    public ResponseEntity<List<UserAccountResponse>> getAll() {

        return ResponseEntity.ok(
                service.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserAccountResponse> getById(
            @PathVariable Integer id) {

        return ResponseEntity.ok(
                service.getById(id));
    }

    @PostMapping
    public ResponseEntity<UserAccountResponse> create(
            @Valid
            @RequestBody
            CreateUserRequest request) {

        UserAccountResponse created =
                service.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserAccountResponse> update(
            @PathVariable Integer id,
            @Valid
            @RequestBody
            UpdateUserRequest request) {

        return ResponseEntity.ok(
                service.update(
                        id,
                        request));
    }

    @PatchMapping("/{id}/toggle-active")
    public ResponseEntity<UserAccountResponse> toggleActive(
            @PathVariable Integer id) {

        return ResponseEntity.ok(
                service.toggleActive(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Integer id) {

        service.delete(id);

        return ResponseEntity
                .noContent()
                .build();
    }
}