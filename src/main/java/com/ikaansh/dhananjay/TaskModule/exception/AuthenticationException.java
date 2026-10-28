package com.ikaansh.dhananjay.TaskModule.exception;

public class AuthenticationException extends RuntimeException {
    public AuthenticationException(String message) {
        super(message);
    }
}