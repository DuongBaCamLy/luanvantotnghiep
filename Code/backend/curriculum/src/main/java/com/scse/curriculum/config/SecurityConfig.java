package com.scse.curriculum.config;

import com.scse.curriculum.auth.security.JwtAuthFilter;
import com.scse.curriculum.user.service.CustomUserDetailsService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final JwtAuthFilter jwtAuthFilter;
    private final HttpsSecurityConfig httpsSecurityConfig;
    private final List<String> corsAllowedOrigins;

    public SecurityConfig(
            CustomUserDetailsService customUserDetailsService,
            JwtAuthFilter jwtAuthFilter,
            HttpsSecurityConfig httpsSecurityConfig,
            @Value("${app.cors.allowed-origins:http://localhost:5173}")
            List<String> corsAllowedOrigins) {

        this.customUserDetailsService = customUserDetailsService;
        this.jwtAuthFilter = jwtAuthFilter;
        this.httpsSecurityConfig = httpsSecurityConfig;
        this.corsAllowedOrigins = corsAllowedOrigins;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        /*
         * NFR-03.6:
         * Local development:
         * REQUIRE_HTTPS=false
         *
         * Production:
         * REQUIRE_HTTPS=true
         */
        if (httpsSecurityConfig.isRequireHttps()) {
            http.requiresChannel(channel ->
                    channel
                            .anyRequest()
                            .requiresSecure()
            );
        }

        http
                .csrf(csrf -> csrf.disable())

                .cors(cors ->
                        cors.configurationSource(
                                corsConfigurationSource()
                        )
                )

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authenticationProvider(
                        authenticationProvider()
                )

                .authorizeHttpRequests(auth -> auth

                        /*
                         * Public infrastructure endpoints
                         */
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/metrics",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        )
                        .permitAll()

                        /*
                         * Allow browser CORS preflight requests
                         */
                        .requestMatchers(
                                HttpMethod.OPTIONS,
                                "/**"
                        )
                        .permitAll()

                        /*
                         * Authentication endpoints must be public.
                         *
                         * Includes:
                         * /auth/login
                         * /auth/google
                         * /auth/refresh
                         * /auth/logout
                         */
                        .requestMatchers(
                                "/auth/**",
                                "/error"
                        )
                        .permitAll()

                        /*
                         * ADMIN
                         */
                        .requestMatchers(
                                "/api/admin/**"
                        )
                        .hasRole("ADMIN")

                        /*
                         * DEAN
                         */
                        .requestMatchers(
                                "/api/dean/**"
                        )
                        .hasAnyRole("ADMIN", "DEAN")

                        /*
                         * DEPARTMENT HEAD
                         */
                        .requestMatchers(
                                "/api/dept/**"
                        )
                        .hasAnyRole("ADMIN", "DEPT_HEAD")

                        /*
                         * FACULTY / INSTRUCTOR
                         */
                        .requestMatchers(
                                "/api/instructor/**"
                        )
                        .hasAnyRole("ADMIN", "INSTRUCTOR")

                        /*
                         * Legacy/out-of-scope endpoints.
                         *
                         * SRS only uses:
                         * ADMIN
                         * DEAN
                         * DEPT_HEAD
                         * INSTRUCTOR
                         */
                        .requestMatchers(
                                "/api/coord/**",
                                "/api/student/**"
                        )
                        .denyAll()

                        /*
                         * All remaining API requests
                         * require authentication.
                         *
                         * Fine-grained permissions are additionally
                         * enforced through @PreAuthorize where applicable.
                         */
                        .anyRequest()
                        .authenticated()
                )

                /*
                 * JSON responses for authentication/authorization errors
                 */
                .exceptionHandling(ex -> ex

                        .authenticationEntryPoint(
                                (request,
                                 response,
                                 authException) -> {

                                    response.setStatus(
                                            HttpStatus.UNAUTHORIZED.value()
                                    );

                                    response.setContentType(
                                            MediaType.APPLICATION_JSON_VALUE
                                    );

                                    response.setCharacterEncoding("UTF-8");

                                    response.getWriter().write(
                                            "{\"message\":\"Bạn cần đăng nhập để truy cập chức năng này\"}"
                                    );
                                }
                        )

                        .accessDeniedHandler(
                                (request,
                                 response,
                                 accessDeniedException) -> {

                                    response.setStatus(
                                            HttpStatus.FORBIDDEN.value()
                                    );

                                    response.setContentType(
                                            MediaType.APPLICATION_JSON_VALUE
                                    );

                                    response.setCharacterEncoding("UTF-8");

                                    response.getWriter().write(
                                            "{\"message\":\"Bạn không có quyền thực hiện thao tác này\"}"
                                    );
                                }
                        )
                )

                /*
                 * JWT authentication filter must execute
                 * before UsernamePasswordAuthenticationFilter.
                 */
                .addFilterBefore(
                        jwtAuthFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config =
                new CorsConfiguration();

        /*
         * Local development defaults to the Vite dev server.
         * Deployments set APP_CORS_ALLOWED_ORIGINS to the URL the
         * browser actually uses, e.g. http://10.8.102.56:8088
         * (comma-separated when there is more than one).
         */
        config.setAllowedOrigins(corsAllowedOrigins);

        config.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"
                )
        );

        config.setAllowedHeaders(
                List.of(
                        "Authorization",
                        "Content-Type"
                )
        );

        config.setExposedHeaders(
                List.of(
                        "Content-Disposition"
                )
        );

        /*
         * Required because refresh_token
         * is stored in HttpOnly cookie.
         */
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                config
        );

        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {

        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider();

        provider.setUserDetailsService(
                customUserDetailsService
        );

        provider.setPasswordEncoder(
                passwordEncoder()
        );

        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration)
            throws Exception {

        return configuration
                .getAuthenticationManager();
    }
}