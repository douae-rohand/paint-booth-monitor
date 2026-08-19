package com.projet.auth.exception;

import java.util.List;

/**
 * Levée lorsque le mot de passe ne respecte pas les règles de robustesse.
 * Le message contient la liste précise des règles non satisfaites,
 * ce qui permet au GlobalExceptionHandler de la renvoyer telle quelle au client.
 */
public class MotDePasseTropFaibleException extends RuntimeException {

    private final List<String> violations;

    public MotDePasseTropFaibleException(List<String> violations) {
        super(String.join(" | ", violations));
        this.violations = violations;
    }

    public List<String> getViolations() {
        return violations;
    }
}
