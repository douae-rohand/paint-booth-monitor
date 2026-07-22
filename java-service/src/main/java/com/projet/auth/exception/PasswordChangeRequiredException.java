package com.projet.auth.exception;

public class PasswordChangeRequiredException extends RuntimeException {
    public PasswordChangeRequiredException(String message) {
        super(message);
    }
}
