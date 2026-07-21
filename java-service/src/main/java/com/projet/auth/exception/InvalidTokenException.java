package com.projet.auth.exception;

/**
 * Thrown when a refresh token is invalid, revoked, or expired.
 * Caught by GlobalExceptionHandler and returned as a uniform 401 ApiErrorResponse.
 */
public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException(String message) {
        super(message);
    }
}
