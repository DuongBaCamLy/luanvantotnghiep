package com.scse.curriculum.config;


import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.scse.curriculum.performance.SyllabusEditorPerformanceInterceptor;

import lombok.RequiredArgsConstructor;


@Configuration
@RequiredArgsConstructor
public class WebMvcPerformanceConfig
        implements WebMvcConfigurer {


    private final SyllabusEditorPerformanceInterceptor interceptor;


    @Override
    public void addInterceptors(
            InterceptorRegistry registry){

        registry.addInterceptor(interceptor)
                .addPathPatterns(
                    "/api/syllabuses/**"
                );
    }
}