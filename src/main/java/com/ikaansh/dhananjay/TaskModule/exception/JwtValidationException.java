package com.ikaansh.dhananjay.TaskModule.exception;

public class JwtValidationException extends RuntimeException {
    public JwtValidationException(String message) {
        super(message);
    }
}