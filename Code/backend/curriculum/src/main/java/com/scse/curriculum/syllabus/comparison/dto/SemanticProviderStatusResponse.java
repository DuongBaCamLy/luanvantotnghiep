package com.scse.curriculum.syllabus.comparison.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Safe public status of the configured semantic-AI provider.
 *
 * API keys and other secrets must never be included here.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SemanticProviderStatusResponse {

    public enum Status {
        DISABLED,
        NOT_CONFIGURED,
        READY,
        MODEL_UNAVAILABLE,
        UNREACHABLE
    }

    private String provider;

    private Status status;

    private boolean enabled;

    private boolean configured;

    /**
     * Null means no live provider probe was attempted.
     */
    private Boolean reachable;

    /**
     * Null means model availability could not be determined.
     */
    private Boolean modelAvailable;

    private String model;

    /**
     * Public provider endpoint only. Never contains credentials.
     */
    private String baseUrl;

    private String message;
}
