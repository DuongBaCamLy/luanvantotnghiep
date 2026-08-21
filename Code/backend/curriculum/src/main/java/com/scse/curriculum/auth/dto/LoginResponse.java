package com.scse.curriculum.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.scse.curriculum.user.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class LoginResponse {

    private String accessToken;
    private String tokenType;
    private Integer userId;
    private String username;
    private String email;
    private UserRole role;
    private Integer instructorId;

    @JsonIgnore
    private String refreshToken;
}