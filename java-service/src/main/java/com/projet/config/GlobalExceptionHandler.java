package com.projet.config;

import com.projet.auth.exception.EmailDejaUtiliseException;
import com.projet.auth.exception.InvalidTokenException;
import com.projet.auth.exception.MotDePasseTropFaibleException;
import com.projet.auth.exception.MotsDePasseNeCorrespondentPasException;
import com.projet.auth.exception.SuperviseurNonTrouveException;
import com.projet.auth.exception.TokenInvalideOuExpireException;
import com.projet.auth.exception.CompteNonActiveException;
import com.projet.config.exception.ConfigurationPLCNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorResponse(400, ex.getMessage()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiErrorResponse(401, "Invalid username or password"));
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleUserNotFound(UsernameNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiErrorResponse(401, ex.getMessage()));
    }

    @ExceptionHandler(CompteNonActiveException.class)
    public ResponseEntity<ApiErrorResponse> handleCompteNonActive(CompteNonActiveException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiErrorResponse(401, ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiErrorResponse(403, "Access denied"));
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidToken(InvalidTokenException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiErrorResponse(401, ex.getMessage()));
    }

    @ExceptionHandler(TokenInvalideOuExpireException.class)
    public ResponseEntity<ApiErrorResponse> handleTokenInvalideOuExpire(TokenInvalideOuExpireException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorResponse(400, ex.getMessage()));
    }

    @ExceptionHandler(MotDePasseTropFaibleException.class)
    public ResponseEntity<ApiErrorResponse> handleMotDePasseTropFaible(MotDePasseTropFaibleException ex) {
        // Retourner chaque violation dans le message, séparées par un saut de ligne
        String message = String.join("\n", ex.getViolations());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorResponse(400, message));
    }

    @ExceptionHandler(MotsDePasseNeCorrespondentPasException.class)
    public ResponseEntity<ApiErrorResponse> handleMotsDePasseNeCorrespondentPas(MotsDePasseNeCorrespondentPasException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorResponse(400, ex.getMessage()));
    }

    @ExceptionHandler(EmailDejaUtiliseException.class)
    public ResponseEntity<ApiErrorResponse> handleEmailDejaUtilise(EmailDejaUtiliseException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiErrorResponse(409, ex.getMessage()));
    }

    @ExceptionHandler(SuperviseurNonTrouveException.class)
    public ResponseEntity<ApiErrorResponse> handleSuperviseurNonTrouve(SuperviseurNonTrouveException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiErrorResponse(404, ex.getMessage()));
    }

    @ExceptionHandler(ConfigurationPLCNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleConfigurationPLCNotFound(ConfigurationPLCNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiErrorResponse(404, ex.getMessage()));
    }

    @ExceptionHandler(com.projet.alerting.exception.BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessException(com.projet.alerting.exception.BusinessException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(new ApiErrorResponse(ex.getStatus().value(), ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiErrorResponse(500, "An unexpected error occurred"));
    }
}
