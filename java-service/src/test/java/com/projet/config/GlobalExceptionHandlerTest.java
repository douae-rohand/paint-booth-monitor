package com.projet.config;

import com.projet.auth.model.Superviseur;
import com.projet.auth.repository.RefreshTokenRepository;
import com.projet.auth.repository.SuperviseurRepository;
import com.projet.auth.service.AuthService;
import com.projet.auth.service.JwtUtil;
import com.projet.auth.service.MotDePasseValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires de GlobalExceptionHandler.
 *
 * Deux responsabilités distinctes testées ici :
 *
 * Partie 1 — GlobalExceptionHandler instancié directement.
 *   Pas de dépendances à injecter : les handlers sont de pures fonctions
 *   exception → ResponseEntity. On les appelle directement sans Spring MVC.
 *
 * Partie 2 — Maillon précédent : AuthService.loadUserByUsername sur un
 *   superviseur avec actif=false.
 *   Vérifie que UserDetails.isAccountNonLocked() et isEnabled() retournent
 *   false, ce qui est la condition exacte qui fera lever DisabledException
 *   par DaoAuthenticationProvider au moment de l'authentification.
 */
@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    // ── Partie 1 : handler instancié directement ──────────────────────────────

    // GlobalExceptionHandler n'a aucun champ à injecter — instanciation directe.
    private GlobalExceptionHandler handler;

    // ── Partie 2 : maillon UserDetails avec actif=false ───────────────────────

    @Mock
    private SuperviseurRepository superviseurRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private MotDePasseValidator motDePasseValidator;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    // ── handleDisabled ────────────────────────────────────────────────────────

    @Test
    void handleDisabled_exceptionLevee_retourne401AvecCodeCompteDesactive() {
        // Arrange
        DisabledException ex = new DisabledException("compte désactivé");

        // Act
        ResponseEntity<ApiErrorResponse> response = handler.handleDisabled(ex);

        // Assert — statut HTTP
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void handleDisabled_exceptionLevee_bodyContientCodeCompteDesactive() {
        // Arrange
        DisabledException ex = new DisabledException("compte désactivé");

        // Act
        ResponseEntity<ApiErrorResponse> response = handler.handleDisabled(ex);

        // Assert — code technique exact
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("COMPTE_DESACTIVE");
    }

    @Test
    void handleDisabled_exceptionLevee_messageNonVide() {
        // Arrange
        DisabledException ex = new DisabledException("compte désactivé");

        // Act
        ResponseEntity<ApiErrorResponse> response = handler.handleDisabled(ex);

        // Assert
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isNotBlank();
    }

    // ── handleLocked ──────────────────────────────────────────────────────────

    @Test
    void handleLocked_exceptionLevee_retourne401AvecCodeCompteVerrouille() {
        // Arrange
        LockedException ex = new LockedException("compte verrouillé");

        // Act
        ResponseEntity<ApiErrorResponse> response = handler.handleLocked(ex);

        // Assert — statut HTTP
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void handleLocked_exceptionLevee_bodyContientCodeCompteVerrouille() {
        // Arrange
        LockedException ex = new LockedException("compte verrouillé");

        // Act
        ResponseEntity<ApiErrorResponse> response = handler.handleLocked(ex);

        // Assert — code technique exact
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("COMPTE_VERROUILLE");
    }

    @Test
    void handleLocked_exceptionLevee_messageNonVide() {
        // Arrange
        LockedException ex = new LockedException("compte verrouillé");

        // Act
        ResponseEntity<ApiErrorResponse> response = handler.handleLocked(ex);

        // Assert
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isNotBlank();
    }

    // ── handleAccountExpired ──────────────────────────────────────────────────

    @Test
    void handleAccountExpired_exceptionLevee_retourne401AvecCodeCompteExpire() {
        // Arrange
        AccountExpiredException ex = new AccountExpiredException("compte expiré");

        // Act
        ResponseEntity<ApiErrorResponse> response = handler.handleAccountExpired(ex);

        // Assert — statut HTTP
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void handleAccountExpired_exceptionLevee_bodyContientCodeCompteExpire() {
        // Arrange
        AccountExpiredException ex = new AccountExpiredException("compte expiré");

        // Act
        ResponseEntity<ApiErrorResponse> response = handler.handleAccountExpired(ex);

        // Assert — code technique exact
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("COMPTE_EXPIRE");
    }

    @Test
    void handleAccountExpired_exceptionLevee_messageNonVide() {
        // Arrange
        AccountExpiredException ex = new AccountExpiredException("compte expiré");

        // Act
        ResponseEntity<ApiErrorResponse> response = handler.handleAccountExpired(ex);

        // Assert
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isNotBlank();
    }

    // ── handleCredentialsExpired ──────────────────────────────────────────────

    @Test
    void handleCredentialsExpired_exceptionLevee_retourne401AvecCodeIdentifiantsExpires() {
        // Arrange
        CredentialsExpiredException ex = new CredentialsExpiredException("identifiants expirés");

        // Act
        ResponseEntity<ApiErrorResponse> response = handler.handleCredentialsExpired(ex);

        // Assert — statut HTTP
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void handleCredentialsExpired_exceptionLevee_bodyContientCodeIdentifiantsExpires() {
        // Arrange
        CredentialsExpiredException ex = new CredentialsExpiredException("identifiants expirés");

        // Act
        ResponseEntity<ApiErrorResponse> response = handler.handleCredentialsExpired(ex);

        // Assert — code technique exact
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("IDENTIFIANTS_EXPIRES");
    }

    @Test
    void handleCredentialsExpired_exceptionLevee_messageNonVide() {
        // Arrange
        CredentialsExpiredException ex = new CredentialsExpiredException("identifiants expirés");

        // Act
        ResponseEntity<ApiErrorResponse> response = handler.handleCredentialsExpired(ex);

        // Assert
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isNotBlank();
    }

    // ── handleDataIntegrityViolation ──────────────────────────────────────────

    @Test
    void handleDataIntegrityViolation_exceptionLevee_retourne409ConflitDonnees() {
        // Arrange
        DataIntegrityViolationException ex = new DataIntegrityViolationException("Violation de contrainte d'unicité");

        // Act
        ResponseEntity<ApiErrorResponse> response = handler.handleDataIntegrityViolation(ex);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("CONFLIT_DONNEES");
        assertThat(response.getBody().getMessage()).isEqualTo("Cette ressource existe déjà ou entre en conflit avec une contrainte existante.");
    }

    // ── Partie 2 : maillon UserDetails — actif=false ──────────────────────────
    //
    // Ces tests vérifient que le Superviseur retourné par loadUserByUsername
    // expose bien isAccountNonLocked()=false et isEnabled()=false quand actif=false.
    // C'est le maillon qui déclenche DisabledException dans DaoAuthenticationProvider.

    @Test
    void loadUserByUsername_superviseurActifFalse_isAccountNonLockedRetourneFalse() {
        // Arrange
        Superviseur superviseurDesactive = buildSuperviseur(false, true);
        when(superviseurRepository.findByEmail("desactive@exemple.com"))
                .thenReturn(Optional.of(superviseurDesactive));

        // Act
        var userDetails = authService.loadUserByUsername("desactive@exemple.com");

        // Assert : isAccountNonLocked() = actif = false
        assertThat(userDetails.isAccountNonLocked()).isFalse();
    }

    @Test
    void loadUserByUsername_superviseurActifFalse_isEnabledRetourneFalse() {
        // Arrange
        Superviseur superviseurDesactive = buildSuperviseur(false, true);
        when(superviseurRepository.findByEmail("desactive@exemple.com"))
                .thenReturn(Optional.of(superviseurDesactive));

        // Act
        var userDetails = authService.loadUserByUsername("desactive@exemple.com");

        // Assert : isEnabled() = actif && deletedAt == null = false && true = false
        assertThat(userDetails.isEnabled()).isFalse();
    }

    @Test
    void loadUserByUsername_superviseurActifTrue_isAccountNonLockedRetourneTrue() {
        // Arrange : contre-test — superviseur actif normal
        Superviseur superviseurActif = buildSuperviseur(true, true);
        when(superviseurRepository.findByEmail("actif@exemple.com"))
                .thenReturn(Optional.of(superviseurActif));

        // Act
        var userDetails = authService.loadUserByUsername("actif@exemple.com");

        // Assert
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.isEnabled()).isTrue();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Superviseur buildSuperviseur(boolean actif, boolean compteActive) {
        Superviseur s = new Superviseur();
        s.setIdSuperviseur(UUID.randomUUID());
        s.setEmail(actif ? "actif@exemple.com" : "desactive@exemple.com");
        s.setNom("Test");
        s.setPrenom("Superviseur");
        s.setMotDePasseHash("$2a$10$hash");
        s.setActif(actif);
        s.setCompteActive(compteActive);
        s.setDeletedAt(null);
        return s;
    }
}
