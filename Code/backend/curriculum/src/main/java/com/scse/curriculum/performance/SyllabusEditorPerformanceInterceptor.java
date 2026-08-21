package com.scse.curriculum.performance;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class SyllabusEditorPerformanceInterceptor
        implements HandlerInterceptor {


    private static final String START_TIME =
            "SYLLABUS_EDITOR_START";


    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) {

        String uri = request.getRequestURI();

        if(uri.contains("/api/syllabuses")) {
            request.setAttribute(
                    START_TIME,
                    System.nanoTime()
            );
        }

        return true;
    }


    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex) {


        Object start =
                request.getAttribute(START_TIME);


        if(start != null){

            long duration =
                    (System.nanoTime()
                    - (Long)start)
                    / 1_000_000;


            response.addHeader(
                    "X-Syllabus-API-Time-MS",
                    String.valueOf(duration)
            );


            response.addHeader(
                    "Server-Timing",
                    "syllabus-editor;dur="
                    + duration
            );
        }
    }
}