package com.scse.curriculum.auth.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import io.jsonwebtoken.JwtException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.scse.curriculum.auth.dto.GoogleLoginRequest;
import com.scse.curriculum.auth.dto.GoogleTokenInfo;
import com.scse.curriculum.auth.dto.LoginRequest;
import com.scse.curriculum.auth.dto.LoginResponse;
import com.scse.curriculum.auth.security.JwtService;
import com.scse.curriculum.user.entity.UserAccount;
import com.scse.curriculum.user.repository.UserAccountRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;

    private final UserAccountRepository userAccountRepository;

    private final JwtService jwtService;

    private final RestTemplateBuilder restTemplateBuilder;

    @Value("${google.oauth.client-id:}")
    private String googleClientId;

    @Value("${google.oauth.allowed-domain:}")
    private String googleAllowedDomain;

    public LoginResponse login(
            LoginRequest request) {

        String login =
                request
                        .getUsername()
                        .trim();

        UserAccount user =
                userAccountRepository
                        .findByUsernameIgnoreCaseOrEmailIgnoreCase(
                                login,
                                login)
                        .orElseThrow(() ->
                                new BadCredentialsException(
                                        "Invalid username/email or password"));

        var authToken =
                new UsernamePasswordAuthenticationToken(
                        login,
                        request.getPassword());

        authenticationManager
                .authenticate(authToken);

        return issueInitialTokens(user);
    }

    public LoginResponse googleLogin(
            GoogleLoginRequest request) {

        GoogleTokenInfo tokenInfo =
                verifyGoogleToken(
                        request.getIdToken());

        String email =
                tokenInfo.getEmail();

        if (email == null
                || email.isBlank()) {

            throw new BadCredentialsException(
                    "Google token does not contain a valid email address");
        }

        String normalizedEmail =
                email
                        .trim()
                        .toLowerCase(
                                Locale.ROOT);

        validateGoogleWorkspaceDomain(
                normalizedEmail,
                tokenInfo.getHd());

        UserAccount user =
                userAccountRepository
                        .findByEmailIgnoreCase(
                                normalizedEmail)
                        .orElseThrow(() ->
                                new BadCredentialsException(
                                        "This Google account is not authorized to access the system"));

        return issueInitialTokens(user);
    }

    
    public LoginResponse refresh(
        String refreshToken) {

    if (refreshToken == null
            || refreshToken.isBlank()) {

        throw new BadCredentialsException(
                "Refresh token is missing");
    }

    final String username;

    try {
        username =
                jwtService.extractUsername(
                        refreshToken);

    } catch (JwtException
            | IllegalArgumentException ex) {

        throw new BadCredentialsException(
                "Invalid or expired refresh token");
    }

    UserAccount user =
            userAccountRepository
                    .findByUsername(username)
                    .orElseThrow(() ->
                            new BadCredentialsException(
                                    "User account no longer exists"));

    if (!Boolean.TRUE.equals(
            user.getIsActive())) {

        throw new DisabledException(
                "This account has been locked");
    }

    UserDetails userDetails =
            buildUserDetails(user);

    try {

        if (!jwtService.isRefreshTokenValid(
                refreshToken,
                userDetails)) {

            throw new BadCredentialsException(
                    "Invalid or expired refresh token");
        }

    } catch (JwtException
            | IllegalArgumentException ex) {

        throw new BadCredentialsException(
                "Invalid or expired refresh token");
    }

    return issueRefreshedAccessToken(
            user,
            userDetails);
}
    private GoogleTokenInfo verifyGoogleToken(
            String idToken) {

        if (googleClientId == null
                || googleClientId.isBlank()) {

            throw new IllegalArgumentException(
                    "google.oauth.client-id is not configured in application.properties");
        }

        try {
            RestTemplate restTemplate =
                    restTemplateBuilder.build();

            GoogleTokenInfo tokenInfo =
                    restTemplate.getForObject(
                            "https://oauth2.googleapis.com/tokeninfo?id_token={idToken}",
                            GoogleTokenInfo.class,
                            idToken);

            if (tokenInfo == null) {

                throw new BadCredentialsException(
                        "Unable to verify the Google token");
            }

            boolean validIssuer =
                    "accounts.google.com"
                            .equals(
                                    tokenInfo.getIss())
                    || "https://accounts.google.com"
                            .equals(
                                    tokenInfo.getIss());

            boolean validAudience =
                    googleClientId.equals(
                            tokenInfo.getAud());

            boolean emailVerified =
                    "true"
                            .equalsIgnoreCase(
                                    tokenInfo
                                            .getEmailVerified());

            if (!validIssuer
                    || !validAudience
                    || !emailVerified) {

                throw new BadCredentialsException(
                        "Invalid Google token or the email address has not been verified");
            }

            return tokenInfo;

        } catch (RestClientException ex) {

            throw new BadCredentialsException(
                    "Unable to verify the Google token");
        }
    }

    private void validateGoogleWorkspaceDomain(
            String email,
            String hostedDomain) {

        if (googleAllowedDomain == null
                || googleAllowedDomain.isBlank()) {

            return;
        }

        String domain =
                googleAllowedDomain
                        .trim()
                        .toLowerCase(
                                Locale.ROOT);

        String hd =
                hostedDomain == null
                        ? ""
                        : hostedDomain
                                .trim()
                                .toLowerCase(
                                        Locale.ROOT);

        if (!email.endsWith(
                "@" + domain)
                || (!hd.isBlank()
                && !domain.equals(hd))) {

            throw new BadCredentialsException(
                    "Only Google Workspace accounts from the following domain are allowed: "
                            + domain);
        }
    }
private LoginResponse issueInitialTokens(
        UserAccount user) {

    if (!Boolean.TRUE.equals(
            user.getIsActive())) {

        throw new DisabledException(
                "This account has been locked");
    }

    UserDetails userDetails =
            buildUserDetails(user);

    Map<String, Object> extraClaims =
            buildClaims(user);

    String accessToken =
            jwtService.generateAccessToken(
                    userDetails,
                    extraClaims);

    String refreshToken =
            jwtService.generateRefreshToken(
                    userDetails,
                    extraClaims);

    user.setLastLogin(
            LocalDateTime.now());

    userAccountRepository.save(user);

    return buildLoginResponse(
            user,
            accessToken,
            refreshToken);
}

private LoginResponse issueRefreshedAccessToken(
        UserAccount user,
        UserDetails userDetails) {

    String accessToken =
            jwtService.generateAccessToken(
                    userDetails,
                    buildClaims(user));

    // Giữ nguyên refresh token ban đầu:
    // hết hạn đúng 7 ngày kể từ lúc login.
    return buildLoginResponse(
            user,
            accessToken,
            null);
}

private UserDetails buildUserDetails(
        UserAccount user) {

    return org.springframework
            .security
            .core
            .userdetails
            .User
            .builder()
            .username(
                    user.getUsername())
            .password(
                    user.getPasswordHash())
            .roles(
                    user.getRole().name())
            .disabled(
                    !Boolean.TRUE.equals(
                            user.getIsActive()))
            .build();
}

private Map<String, Object> buildClaims(
        UserAccount user) {

    Map<String, Object> extraClaims =
            new HashMap<>();

    extraClaims.put(
            "role",
            user.getRole().name());

    extraClaims.put(
            "userId",
            user.getId());

    return extraClaims;
}

private LoginResponse buildLoginResponse(
        UserAccount user,
        String accessToken,
        String refreshToken) {

    return LoginResponse
            .builder()
            .accessToken(accessToken)
            .tokenType("Bearer")
            .userId(
                    user.getId())
            .username(
                    user.getUsername())
            .email(
                    user.getEmail())
            .role(
                    user.getRole())
            .instructorId(
                    user.getInstructorId())
            .refreshToken(refreshToken)
            .build();
}
}