package com.projet.auth.service;

import com.projet.auth.dto.ActivationCompteDTO;
import com.projet.auth.exception.MotsDePasseNeCorrespondentPasException;
import com.projet.auth.exception.TokenInvalideOuExpireException;
import com.projet.auth.model.Superviseur;
import com.projet.auth.model.TokenActivation;
import com.projet.auth.repository.RefreshTokenRepository;
import com.projet.auth.repository.SuperviseurRepository;
import com.projet.auth.repository.TokenActivationRepository;
import com.projet.auth.service.MotDePasseValidator;
import com.projet.audit.model.enums.ActionAudit;
import com.projet.audit.service.LogAuditService;
import com.projet.notifications.service.EmailService;
import com.projet.notifications.service.NotificationDispatchService;
import com.projet.auth.service.SuperviseurAdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de SuperviseurAdminService, centrés sur activerCompte().
 *
 * REMARQUE : Le prompt demande des tests sur "AdminService (activation compte)".
 * Le service réel est SuperviseurAdminService — il n'existe pas de classe AdminService.
 *
 * ÉCART CONSTATÉ :
 * La méthode findByTokenHashAndUtiliseFalseAndDateExpirationAfter filtre en une seule
 * requête les cas "token introuvable", "token déjà utilisé" ET "token expiré".
 * Il est impossible de distinguer "token expiré" de "token déjà utilisé" via cette
 * requête — les deux lèvent la même TokenInvalideOuExpireException avec le même message.
 * Les deux scénarios sont testés via le comportement observable (Optional.empty()),
 * pas via la cause interne.
 */
@ExtendWith(MockitoExtension.class)
class SuperviseurAdminServiceTest {

    @Mock
    private SuperviseurRepository superviseurRepository;

    @Mock
    private TokenActivationRepository tokenActivationRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @Mock
    private MotDePasseValidator motDePasseValidator;

    @Mock
    private NotificationDispatchService notificationDispatchService;

    @Mock
    private LogAuditService logAuditService;

    @InjectMocks
    private SuperviseurAdminService superviseurAdminService;

    private Superviseur superviseurNonActif;
    private static final String MOT_DE_PASSE_VALIDE = "MotDePasse123!";
    private static final String RAW_TOKEN_VALIDE = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

    @BeforeEach
    void setUp() {
        // Injecter les @Value — non accessibles via MockitoExtension seul
        ReflectionTestUtils.setField(superviseurAdminService, "frontendUrl", "http://localhost:5173");
        ReflectionTestUtils.setField(superviseurAdminService, "activationTokenExpirationHours", 24);

        superviseurNonActif = new Superviseur();
        superviseurNonActif.setIdSuperviseur(UUID.randomUUID());
        superviseurNonActif.setEmail("alice@exemple.com");
        superviseurNonActif.setNom("Dupont");
        superviseurNonActif.setPrenom("Alice");
        superviseurNonActif.setActif(true);
        superviseurNonActif.setCompteActive(false);
        superviseurNonActif.setMotDePasseHash(null);
    }

    // ── activerCompte — token valide ──────────────────────────────────────────

    @Test
    void activerCompte_tokenValide_activeLeCompteDuSuperviseur() {
        // Arrange
        TokenActivation tokenActivation = buildTokenActivation(false, LocalDateTime.now().plusHours(24));
        when(tokenActivationRepository.findByTokenHashAndUtiliseFalseAndDateExpirationAfter(
                anyString(), any(LocalDateTime.class)))
                .thenReturn(Optional.of(tokenActivation));
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashedMdp");
        when(superviseurRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(tokenActivationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(emailService.envoyerEmailBienvenue(anyString(), anyString()))
                .thenReturn(EmailService.EmailResult.succes());
        doNothing().when(logAuditService).logger(any(UUID.class), any(ActionAudit.class));

        ActivationCompteDTO dto = buildDto(RAW_TOKEN_VALIDE, MOT_DE_PASSE_VALIDE, MOT_DE_PASSE_VALIDE);

        // Act
        superviseurAdminService.activerCompte(dto);

        // Assert : le superviseur doit avoir compteActive = true
        assertThat(superviseurNonActif.isCompteActive()).isTrue();
    }

    @Test
    void activerCompte_tokenValide_motDePasseHasheEstPersiste() {
        // Arrange
        TokenActivation tokenActivation = buildTokenActivation(false, LocalDateTime.now().plusHours(24));
        when(tokenActivationRepository.findByTokenHashAndUtiliseFalseAndDateExpirationAfter(
                anyString(), any(LocalDateTime.class)))
                .thenReturn(Optional.of(tokenActivation));
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashedMdp");
        when(superviseurRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(tokenActivationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(emailService.envoyerEmailBienvenue(anyString(), anyString()))
                .thenReturn(EmailService.EmailResult.succes());
        doNothing().when(logAuditService).logger(any(UUID.class), any(ActionAudit.class));

        ActivationCompteDTO dto = buildDto(RAW_TOKEN_VALIDE, MOT_DE_PASSE_VALIDE, MOT_DE_PASSE_VALIDE);

        // Act
        superviseurAdminService.activerCompte(dto);

        // Assert
        assertThat(superviseurNonActif.getMotDePasseHash()).isEqualTo("$2a$10$hashedMdp");
        verify(passwordEncoder).encode(MOT_DE_PASSE_VALIDE);
    }

    @Test
    void activerCompte_tokenValide_tokenMarqueUtilise() {
        // Arrange
        TokenActivation tokenActivation = buildTokenActivation(false, LocalDateTime.now().plusHours(24));
        when(tokenActivationRepository.findByTokenHashAndUtiliseFalseAndDateExpirationAfter(
                anyString(), any(LocalDateTime.class)))
                .thenReturn(Optional.of(tokenActivation));
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashedMdp");
        when(superviseurRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(tokenActivationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(emailService.envoyerEmailBienvenue(anyString(), anyString()))
                .thenReturn(EmailService.EmailResult.succes());
        doNothing().when(logAuditService).logger(any(UUID.class), any(ActionAudit.class));

        ActivationCompteDTO dto = buildDto(RAW_TOKEN_VALIDE, MOT_DE_PASSE_VALIDE, MOT_DE_PASSE_VALIDE);

        // Act
        superviseurAdminService.activerCompte(dto);

        // Assert : le token doit être marqué utilisé
        assertThat(tokenActivation.isUtilise()).isTrue();
    }

    // ── activerCompte — token expiré ou déjà utilisé ──────────────────────────

    @Test
    void activerCompte_tokenIntrouvable_leveTokenInvalideOuExpireException() {
        // Arrange : la requête combinée retourne empty (token inexistant, utilisé, ou expiré)
        when(tokenActivationRepository.findByTokenHashAndUtiliseFalseAndDateExpirationAfter(
                anyString(), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        ActivationCompteDTO dto = buildDto("tokenInexistant", MOT_DE_PASSE_VALIDE, MOT_DE_PASSE_VALIDE);

        // Act / Assert
        assertThatThrownBy(() -> superviseurAdminService.activerCompte(dto))
                .isInstanceOf(TokenInvalideOuExpireException.class)
                .hasMessage("Token invalide ou expiré");
    }

    @Test
    void activerCompte_tokenExpire_leveTokenInvalideOuExpireException() {
        // Arrange : token expiré → la requête retourne empty (filtré par dateExpiration > now)
        when(tokenActivationRepository.findByTokenHashAndUtiliseFalseAndDateExpirationAfter(
                anyString(), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        ActivationCompteDTO dto = buildDto(RAW_TOKEN_VALIDE, MOT_DE_PASSE_VALIDE, MOT_DE_PASSE_VALIDE);

        // Act / Assert
        assertThatThrownBy(() -> superviseurAdminService.activerCompte(dto))
                .isInstanceOf(TokenInvalideOuExpireException.class)
                .hasMessage("Token invalide ou expiré");
    }

    @Test
    void activerCompte_tokenDejaUtilise_leveTokenInvalideOuExpireException() {
        // Arrange : token utilisé → la requête retourne empty (filtré par utilise=false)
        when(tokenActivationRepository.findByTokenHashAndUtiliseFalseAndDateExpirationAfter(
                anyString(), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        ActivationCompteDTO dto = buildDto(RAW_TOKEN_VALIDE, MOT_DE_PASSE_VALIDE, MOT_DE_PASSE_VALIDE);

        // Act / Assert
        assertThatThrownBy(() -> superviseurAdminService.activerCompte(dto))
                .isInstanceOf(TokenInvalideOuExpireException.class)
                .hasMessage("Token invalide ou expiré");

        // Aucune activation ne doit avoir eu lieu
        verify(superviseurRepository, never()).save(any());
    }

    // ── activerCompte — mots de passe ne correspondent pas ────────────────────

    @Test
    void activerCompte_motsDePasseDifferents_leveMotsDePasseNeCorrespondentPasException() {
        // Arrange : token valide, mais confirmationMotDePasse différente
        TokenActivation tokenActivation = buildTokenActivation(false, LocalDateTime.now().plusHours(24));
        when(tokenActivationRepository.findByTokenHashAndUtiliseFalseAndDateExpirationAfter(
                anyString(), any(LocalDateTime.class)))
                .thenReturn(Optional.of(tokenActivation));

        ActivationCompteDTO dto = buildDto(RAW_TOKEN_VALIDE, MOT_DE_PASSE_VALIDE, "AutreMdp456!");

        // Act / Assert
        assertThatThrownBy(() -> superviseurAdminService.activerCompte(dto))
                .isInstanceOf(MotsDePasseNeCorrespondentPasException.class)
                .hasMessage("Les mots de passe ne correspondent pas");
    }

    @Test
    void activerCompte_motsDePasseDifferents_nActivesPasLeCompte() {
        // Arrange
        TokenActivation tokenActivation = buildTokenActivation(false, LocalDateTime.now().plusHours(24));
        when(tokenActivationRepository.findByTokenHashAndUtiliseFalseAndDateExpirationAfter(
                anyString(), any(LocalDateTime.class)))
                .thenReturn(Optional.of(tokenActivation));

        ActivationCompteDTO dto = buildDto(RAW_TOKEN_VALIDE, MOT_DE_PASSE_VALIDE, "AutreMdp456!");

        // Act / Assert
        assertThatThrownBy(() -> superviseurAdminService.activerCompte(dto))
                .isInstanceOf(MotsDePasseNeCorrespondentPasException.class);

        // Le superviseur ne doit pas avoir été sauvegardé
        verify(superviseurRepository, never()).save(any());
        assertThat(superviseurNonActif.isCompteActive()).isFalse();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private TokenActivation buildTokenActivation(boolean utilise, LocalDateTime dateExpiration) {
        TokenActivation t = new TokenActivation();
        t.setSuperviseur(superviseurNonActif);
        t.setTokenHash("hash-fictif");
        t.setUtilise(utilise);
        t.setDateExpiration(dateExpiration);
        t.setCreatedAt(LocalDateTime.now());
        return t;
    }

    private ActivationCompteDTO buildDto(String token, String mdp, String confirmation) {
        ActivationCompteDTO dto = new ActivationCompteDTO();
        dto.setToken(token);
        dto.setNouveauMotDePasse(mdp);
        dto.setConfirmationMotDePasse(confirmation);
        return dto;
    }
}
