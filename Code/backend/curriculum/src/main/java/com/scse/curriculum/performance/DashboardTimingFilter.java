package com.scse.curriculum.performance;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;


@Component
public class DashboardTimingFilter extends OncePerRequestFilter {


    private static final String DASHBOARD_PREFIX =
            "/api/dashboard";


    private static final String HEADER =
            "X-LVTN-Dashboard-Time";


    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    )
            throws ServletException, IOException {


        if (!request.getRequestURI()
                .startsWith(DASHBOARD_PREFIX)) {

            filterChain.doFilter(
                    request,
                    response
            );

            return;
        }


        Instant start =
                Instant.now();


        try {

            filterChain.doFilter(
                    request,
                    response
            );


        } finally {


            long duration =
                    Duration.between(
                            start,
                            Instant.now()
                    )
                    .toMillis();


            response.setHeader(
                    HEADER,
                    duration + "ms"
            );


        }

    }

}