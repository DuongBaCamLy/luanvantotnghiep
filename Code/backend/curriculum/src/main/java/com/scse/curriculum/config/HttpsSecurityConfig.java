package com.scse.curriculum.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HttpsSecurityConfig {

    private final boolean requireHttps;

    public HttpsSecurityConfig(
            @Value("${app.security.require-https:false}")
            boolean requireHttps) {

        this.requireHttps = requireHttps;
    }

    public boolean isRequireHttps() {
        return requireHttps;
    }
}