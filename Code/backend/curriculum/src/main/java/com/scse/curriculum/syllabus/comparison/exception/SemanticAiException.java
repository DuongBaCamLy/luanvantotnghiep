package com.scse.curriculum.syllabus.comparison.exception;

public class SemanticAiException extends RuntimeException {

    public SemanticAiException(String message) {
        super(message);
    }

    public SemanticAiException(
            String message,
            Throwable cause) {

        super(message, cause);
    }
}