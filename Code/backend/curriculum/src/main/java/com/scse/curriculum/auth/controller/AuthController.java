package com.scse.curriculum.auth.controller;

import com.scse.curriculum.auth.dto.ForgotPasswordRequest;
import com.scse.curriculum.auth.dto.GoogleLoginRequest;
import com.scse.curriculum.auth.dto.LoginRequest;
import com.scse.curriculum.auth.dto.LoginResponse;
import com.scse.curriculum.auth.dto.ResetPasswordRequest;
import com.scse.curriculum.auth.service.AuthService;
import com.scse.curriculum.auth.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_COOKIE_NAME =
            "refresh_token";

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @Value("${jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    @Value("${app.auth.refresh-cookie-secure:false}")
    private boolean refreshCookieSecure;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {

        LoginResponse response =
                authService.login(request);

        return withRefreshCookie(response);
    }

    @PostMapping("/google")
    public ResponseEntity<LoginResponse> googleLogin(
            @Valid @RequestBody GoogleLoginRequest request) {

        LoginResponse response =
                authService.googleLogin(request);

        return withRefreshCookie(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        passwordResetService.requestReset(request.getEmail());

        return ResponseEntity.ok(Map.of(
                "message",
                "If an account exists for this email, a password reset link has been sent."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        passwordResetService.resetPassword(
                request.getToken(),
                request.getNewPassword());

        return ResponseEntity.ok(Map.of(
                "message",
                "Password reset successfully. You can now sign in with your new password."));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
            @CookieValue(
                    name = REFRESH_COOKIE_NAME,
                    required = false)
            String refreshToken) {

        return ResponseEntity.ok(
                authService.refresh(refreshToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {

        ResponseCookie deleteCookie =
                ResponseCookie
                        .from(
                                REFRESH_COOKIE_NAME,
                                "")
                        .httpOnly(true)
                        .secure(refreshCookieSecure)
                        .sameSite("Lax")
                        .path("/auth")
                        .maxAge(Duration.ZERO)
                        .build();

        return ResponseEntity
                .noContent()
                .header(
                        HttpHeaders.SET_COOKIE,
                        deleteCookie.toString())
                .build();
    }

    private ResponseEntity<LoginResponse> withRefreshCookie(
            LoginResponse response) {

        ResponseCookie refreshCookie =
                ResponseCookie
                        .from(
                                REFRESH_COOKIE_NAME,
                                response.getRefreshToken())
                        .httpOnly(true)
                        .secure(refreshCookieSecure)
                        .sameSite("Lax")
                        .path("/auth")
                        .maxAge(
                                Duration.ofMillis(
                                        refreshExpirationMs))
                        .build();

        return ResponseEntity
                .ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshCookie.toString())
                .body(response);
    }
}