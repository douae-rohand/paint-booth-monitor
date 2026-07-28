package com.projet.auth.exception;

public class TokenInvalideOuExpireException extends RuntimeException {
    public TokenInvalideOuExpireException(String message) {
        super(message);
    }
}
