package com.projet.auth.exception;

public class EmailDejaUtiliseException extends RuntimeException {
    public EmailDejaUtiliseException(String message) {
        super(message);
    }
}
