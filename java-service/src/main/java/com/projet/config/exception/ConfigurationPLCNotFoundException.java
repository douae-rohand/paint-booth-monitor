package com.projet.config.exception;

/**
 * Exception metier levee par ConfigurationPLCService lorsqu'une operation
 * sur une configuration PLC echoue pour une raison fonctionnelle.
 *
 * Mappee en 404 NOT FOUND par GlobalExceptionHandler.
 */
public class ConfigurationPLCNotFoundException extends RuntimeException {

    public ConfigurationPLCNotFoundException(String message) {
        super(message);
    }
}
