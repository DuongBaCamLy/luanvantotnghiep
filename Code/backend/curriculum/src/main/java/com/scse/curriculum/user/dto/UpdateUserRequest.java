package com.scse.curriculum.user.dto;

import com.scse.curriculum.user.entity.UserRole;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserRequest {

    @NotBlank(
            message = "Username is required")
    @Size(
            min = 3,
            max = 100,
            message = "Username must be between 3 and 100 characters")
    private String username;

    @NotBlank(
            message = "Email is required")
    @Email(
            message = "Email address is invalid")
    private String email;

    /*
     * Leave blank when the password
     * should remain unchanged.
     */
    @Size(
            min = 6,
            message = "Password must contain at least 6 characters")
    private String password;

    @NotNull(
            message = "Role is required")
    private UserRole role;

    private Integer instructorId;
    private Integer managedMajorId;

    private Boolean isActive;
}
