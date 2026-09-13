package com.projet.config;

import org.springframework.http.HttpStatus;

/**
 * Exception métier générique transverse.
 *
 * Placée dans com.projet.config car elle est utilisée par plusieurs modules
 * (alerting, kpis, measures, reports, chatbot…) — ce n'est pas une
 * responsabilité propre au module alerting.
 *
 * Sépare explicitement deux notions :
 *   - {@code code}    : identifiant technique stable en SNAKE_CASE (ex. "POINT_MESURE_INACTIF"),
 *                       destiné au traitement programmatique (logs, i18n future, module chatbot).
 *   - {@code message} : message lisible en français, destiné à l'affichage utilisateur
 *                       (accessible via {@link #getMessage()}).
 */
public class BusinessException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    /**
     * @param code    Identifiant technique SNAKE_CASE (ex. "POINT_MESURE_INACTIF")
     * @param message Message lisible en français destiné à l'utilisateur final
     * @param status  Statut HTTP à retourner
     */
    public BusinessException(String code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
