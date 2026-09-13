package com.projet.config;

import java.time.LocalDateTime;

/**
 * Structure de réponse d'erreur uniforme retournée par le GlobalExceptionHandler.
 *
 * Champs :
 *   - httpStatus : code HTTP numérique (400, 404, 500…)
 *   - code       : identifiant technique métier SNAKE_CASE (ex. "POINT_MESURE_INACTIF"),
 *                  null pour les erreurs non-BusinessException
 *   - message    : message lisible en français destiné à l'affichage utilisateur
 *   - timestamp  : date/heure de l'erreur
 */
public class ApiErrorResponse {

    private final int httpStatus;
    private final String code;
    private final String message;
    private final LocalDateTime timestamp;

    /** Constructeur pour BusinessException (avec code métier). */
    public ApiErrorResponse(int httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    /** Constructeur pour les autres exceptions (sans code métier). */
    public ApiErrorResponse(int httpStatus, String message) {
        this(httpStatus, null, message);
    }

    public int getHttpStatus()        { return httpStatus; }
    public String getCode()           { return code; }
    public String getMessage()        { return message; }
    public LocalDateTime getTimestamp() { return timestamp; }
}
