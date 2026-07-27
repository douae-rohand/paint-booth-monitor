package com.projet.alerting.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception métier générique pour le module alerting.
 * Elle permet de remonter des codes d'erreur spécifiques avec le statut HTTP approprié.
 */
public class BusinessException extends RuntimeException {
    private final HttpStatus status;

    public BusinessException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
