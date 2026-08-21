package com.scse.curriculum.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    private static final String TOKEN_TYPE_CLAIM = "tokenType";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms}")
    private long accessExpirationMs;

    @Value("${jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(
            String token,
            Function<Claims, T> resolver) {

        Claims claims = extractAllClaims(token);
        return resolver.apply(claims);
    }

    public String generateAccessToken(
            UserDetails userDetails,
            Map<String, Object> extraClaims) {

        return generateToken(
                userDetails,
                extraClaims,
                accessExpirationMs,
                ACCESS_TOKEN_TYPE);
    }

    public String generateRefreshToken(
            UserDetails userDetails,
            Map<String, Object> extraClaims) {

        return generateToken(
                userDetails,
                extraClaims,
                refreshExpirationMs,
                REFRESH_TOKEN_TYPE);
    }

    // Giữ tương thích với code access-token cũ
    public String generateToken(
            UserDetails userDetails,
            Map<String, Object> extraClaims) {

        return generateAccessToken(
                userDetails,
                extraClaims);
    }

    public boolean isAccessTokenValid(
            String token,
            UserDetails userDetails) {

        String username = extractUsername(token);
        String tokenType = extractTokenType(token);

        // Cho phép access token cũ chưa có tokenType.
        boolean validType =
                tokenType == null
                || ACCESS_TOKEN_TYPE.equals(tokenType);

        return username.equals(userDetails.getUsername())
                && validType
                && !isTokenExpired(token);
    }

    public boolean isRefreshTokenValid(
            String token,
            UserDetails userDetails) {

        String username = extractUsername(token);
        String tokenType = extractTokenType(token);

        return username.equals(userDetails.getUsername())
                && REFRESH_TOKEN_TYPE.equals(tokenType)
                && !isTokenExpired(token);
    }

    public boolean isTokenValid(
            String token,
            UserDetails userDetails) {

        return isAccessTokenValid(
                token,
                userDetails);
    }

    private String generateToken(
            UserDetails userDetails,
            Map<String, Object> extraClaims,
            long expirationMs,
            String tokenType) {

        Map<String, Object> claims =
                new HashMap<>(extraClaims);

        claims.put(
                TOKEN_TYPE_CLAIM,
                tokenType);

        long now = System.currentTimeMillis();

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(now))
                .setExpiration(
                        new Date(now + expirationMs))
                .signWith(
                        getSignInKey(),
                        SignatureAlgorithm.HS256)
                .compact();
    }

    private String extractTokenType(String token) {
        return extractAllClaims(token)
                .get(
                        TOKEN_TYPE_CLAIM,
                        String.class);
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(
                token,
                Claims::getExpiration)
                .before(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignInKey() {
        return Keys.hmacShaKeyFor(
                secret.getBytes(
                        StandardCharsets.UTF_8));
    }
}