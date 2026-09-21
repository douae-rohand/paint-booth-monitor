package com.projet.config;

import com.projet.auth.exception.EmailDejaUtiliseException;
import com.projet.auth.exception.InvalidTokenException;
import com.projet.auth.exception.MotDePasseTropFaibleException;
import com.projet.auth.exception.MotsDePasseNeCorrespondentPasException;
import com.projet.auth.exception.SuperviseurNonTrouveException;
import com.projet.auth.exception.TokenInvalideOuExpireException;
import com.projet.auth.exception.CompteNonActiveException;
import com.projet.config.exception.ConfigurationPLCNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
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

    /**
     * Compte désactivé par un administrateur (Superviseur.actif = false).
     * isEnabled() et isAccountNonLocked() retournent false → DaoAuthenticationProvider
     * lève DisabledException avant LockedException.
     */
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiErrorResponse> handleDisabled(DisabledException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiErrorResponse(401, "COMPTE_DESACTIVE",
                        "Votre compte a été désactivé. Contactez un administrateur."));
    }

    /**
     * Compte verrouillé (isAccountNonLocked() = false).
     * Actuellement même origine que DisabledException (actif = false),
     * traité séparément pour couvrir un futur mécanisme de verrouillage distinct
     * (ex. tentatives d'échec répétées).
     */
    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ApiErrorResponse> handleLocked(LockedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiErrorResponse(401, "COMPTE_VERROUILLE",
                        "Votre compte est verrouillé. Contactez un administrateur."));
    }

    /**
     * Compte expiré (isAccountNonExpired() = false, i.e. deletedAt != null).
     */
    @ExceptionHandler(AccountExpiredException.class)
    public ResponseEntity<ApiErrorResponse> handleAccountExpired(AccountExpiredException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiErrorResponse(401, "COMPTE_EXPIRE",
                        "Votre compte a expiré. Contactez un administrateur."));
    }

    /**
     * Identifiants expirés (isCredentialsNonExpired() = false).
     * Actuellement toujours true dans l'implémentation, prévu pour un futur
     * mécanisme d'expiration de mot de passe.
     */
    @ExceptionHandler(CredentialsExpiredException.class)
    public ResponseEntity<ApiErrorResponse> handleCredentialsExpired(CredentialsExpiredException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiErrorResponse(401, "IDENTIFIANTS_EXPIRES",
                        "Vos identifiants ont expiré. Veuillez renouveler votre mot de passe."));
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

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadable(org.springframework.http.converter.HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorResponse(400, "CHATBOT_PARAMETRE_INVALIDE", "Un paramètre fourni dans la requête est invalide ou mal formaté."));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessException(BusinessException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(new ApiErrorResponse(ex.getStatus().value(), ex.getCode(), ex.getMessage()));
    }

    /**
     * Violation de contrainte base de données (unicité, clé étrangère, etc.).
     * Handler générique — couvre tous les cas de DataIntegrityViolationException,
     * quelle que soit la table ou la contrainte concernée.
     * Retourne 409 plutôt que 500 pour exposer une erreur exploitable au client.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiErrorResponse(409, "CONFLIT_DONNEES",
                        "Cette ressource existe déjà ou entre en conflit avec une contrainte existante."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiErrorResponse(500, "An unexpected error occurred"));
    }
}
