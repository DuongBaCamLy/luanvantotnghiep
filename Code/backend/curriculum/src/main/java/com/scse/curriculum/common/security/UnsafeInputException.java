package com.scse.curriculum.common.security;

public class UnsafeInputException extends RuntimeException {

    public UnsafeInputException(String message) {
        super(message);
    }
}