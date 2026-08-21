package com.scse.curriculum.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "SCSE Curriculum Management System API",
                version = "1.0.0",
                description = """
                        REST API documentation for the
                        SCSE Curriculum Management System.

                        Main modules:
                        - Authentication
                        - User and Role Management
                        - Curriculum Management
                        - Syllabus Management
                        - CLO-PLO Mapping
                        - Approval Workflow
                        - Reports
                        - Notifications
                        - Audit Log
                        """,
                contact = @Contact(
                        name = "School of Computer Science and Engineering",
                        email = "scse@hcmiu.edu.vn"
                ),
                license = @License(
                        name = "Academic Project"
                )
        ),
        servers = {
                @Server(
                        url = "http://localhost:8080",
                        description = "Local Development Server"
                )
        },
        security = {
                @SecurityRequirement(
                        name = "bearerAuth"
                )
        }
)
@SecurityScheme(
        name = "bearerAuth",
        description = "JWT access token. Enter the access token without the Bearer prefix.",
        scheme = "bearer",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT"
)
public class OpenApiConfig {
}