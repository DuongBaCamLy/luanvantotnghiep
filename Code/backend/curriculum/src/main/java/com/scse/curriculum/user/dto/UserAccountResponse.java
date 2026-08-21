package com.scse.curriculum.user.dto;

import com.scse.curriculum.user.entity.UserAccount;
import com.scse.curriculum.user.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class UserAccountResponse {

    private Integer id;
    private String username;
    private String email;
    private UserRole role;
    private Integer instructorId;
    private Boolean isActive;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;

    public static UserAccountResponse fromEntity(UserAccount u) {
        return UserAccountResponse.builder()
                .id(u.getId())
                .username(u.getUsername())
                .email(u.getEmail())
                .role(u.getRole())
                .instructorId(u.getInstructorId())
                .isActive(u.getIsActive())
                .lastLogin(u.getLastLogin())
                .createdAt(u.getCreatedAt())
                .build();
    }
}