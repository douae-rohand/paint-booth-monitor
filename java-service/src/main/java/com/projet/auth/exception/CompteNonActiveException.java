package com.projet.auth.exception;

public class CompteNonActiveException extends RuntimeException {
    public CompteNonActiveException(String message) {
        super(message);
    }
}
