package com.projet.auth.service;

import com.projet.auth.exception.CompteNonActiveException;
import com.projet.auth.exception.InvalidTokenException;
import com.projet.auth.model.RefreshToken;
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
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de AuthService.
 *
 * ÉCARTS CONSTATÉS PAR RAPPORT AU PROMPT :
 *
 * 1. Pas de méthode login() dans AuthService.
 *    L'authentification est déléguée à Spring Security (AuthenticationManager →
 *    DaoAuthenticationProvider → loadUserByUsername). Les scénarios "credentials
 *    valides → tokens retournés" et "mot de passe incorrect → BadCredentialsException"
 *    NE PEUVENT PAS être testés sur AuthService directement.
 *    Ce qui est testable est loadUserByUsername (gestion email introuvable,
 *    compte non activé) et generateToken.
 *
 * 2. Pas de vérification du compte désactivé (actif=false) dans loadUserByUsername.
 *    La méthode vérifie uniquement compteActive. Si actif=false, UserDetails est
 *    retourné normalement (isAccountNonLocked() retourne false, ce que Spring Security
 *    gère via un LockedException — en dehors du scope de loadUserByUsername).
 *    Ce scénario est documenté mais pas dans AuthService.
 *
 * 3. "Même message d'erreur entre utilisateur inexistant et mot de passe incorrect" :
 *    NON — loadUserByUsername lève UsernameNotFoundException avec le message
 *    "Superviseur not found with email: {email}". Spring Security wrappera ce message
 *    dans un 401 Bad Credentials côté HTTP, mais le message brut diffère.
 *    Aucune fuite d'information visible via l'API REST grâce au GlobalExceptionHandler
 *    qui retourne toujours "Invalid username or password" pour BadCredentialsException.
 *    Ce test n'est donc pas applicable à ce niveau de la couche service.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

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

    private Superviseur superviseurActif;
    private static final String EMAIL_TEST = "alice@exemple.com";

    @BeforeEach
    void setUp() {
        superviseurActif = new Superviseur();
        superviseurActif.setIdSuperviseur(UUID.randomUUID());
        superviseurActif.setEmail(EMAIL_TEST);
        superviseurActif.setNom("Dupont");
        superviseurActif.setPrenom("Alice");
        superviseurActif.setMotDePasseHash("$2a$10$hashedPassword");
        superviseurActif.setActif(true);
        superviseurActif.setCompteActive(true);
    }

    // ── loadUserByUsername ─────────────────────────────────────────────────────

    @Test
    void loadUserByUsername_emailExistantEtCompteActif_retourneSuperviseur() {
        // Arrange
        when(superviseurRepository.findByEmail(EMAIL_TEST))
                .thenReturn(Optional.of(superviseurActif));

        // Act
        var result = authService.loadUserByUsername(EMAIL_TEST);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo(EMAIL_TEST);
    }

    @Test
    void loadUserByUsername_emailInexistant_leveUsernameNotFoundException() {
        // Arrange
        when(superviseurRepository.findByEmail("inconnu@exemple.com"))
                .thenReturn(Optional.empty());

        // Act / Assert
        assertThatThrownBy(() -> authService.loadUserByUsername("inconnu@exemple.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("inconnu@exemple.com");
    }

    @Test
    void loadUserByUsername_compteNonActive_leveCompteNonActiveException() {
        // Arrange
        superviseurActif.setCompteActive(false);
        when(superviseurRepository.findByEmail(EMAIL_TEST))
                .thenReturn(Optional.of(superviseurActif));

        // Act / Assert
        assertThatThrownBy(() -> authService.loadUserByUsername(EMAIL_TEST))
                .isInstanceOf(CompteNonActiveException.class)
                .hasMessageContaining("Compte non activé");
    }

    @Test
    void loadUserByUsername_compteNonActive_neRetournePasDeToken() {
        // Arrange
        superviseurActif.setCompteActive(false);
        when(superviseurRepository.findByEmail(EMAIL_TEST))
                .thenReturn(Optional.of(superviseurActif));

        // Act / Assert : exception levée avant tout appel à jwtUtil
        assertThatThrownBy(() -> authService.loadUserByUsername(EMAIL_TEST))
                .isInstanceOf(CompteNonActiveException.class);

        verifyNoInteractions(jwtUtil);
    }

    // ── generateToken ──────────────────────────────────────────────────────────

    @Test
    void generateToken_superviseurSansAdmin_appelleJwtUtilAvecRoleSuperviseur() {
        // Arrange : admin null → getRole() = "ROLE_SUPERVISEUR"
        superviseurActif.setAdmin(null);
        when(jwtUtil.generateToken(superviseurActif, "ROLE_SUPERVISEUR"))
                .thenReturn("jwt.token.superviseur");

        // Act
        String token = authService.generateToken(superviseurActif);

        // Assert
        assertThat(token).isEqualTo("jwt.token.superviseur");
        verify(jwtUtil).generateToken(superviseurActif, "ROLE_SUPERVISEUR");
    }

    // ── rotateRefreshToken — token introuvable ─────────────────────────────────

    @Test
    void rotateRefreshToken_tokenIntrouvable_leveInvalidTokenException() {
        // Arrange : aucun token en DB correspondant au hash du rawToken
        when(refreshTokenRepository.findByTokenHash(anyString()))
                .thenReturn(Optional.empty());

        // Act / Assert
        assertThatThrownBy(() -> authService.rotateRefreshToken("rawTokenInvalide", "Mozilla"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Refresh token invalide");
    }

    // ── rotateRefreshToken — token révoqué ────────────────────────────────────

    @Test
    void rotateRefreshToken_tokenRevoque_leveInvalidTokenException() {
        // Arrange
        RefreshToken tokenRevoque = buildRefreshToken(true, LocalDateTime.now().plusDays(7));
        when(refreshTokenRepository.findByTokenHash(anyString()))
                .thenReturn(Optional.of(tokenRevoque));

        // Act / Assert
        assertThatThrownBy(() -> authService.rotateRefreshToken("rawTokenRevoque", "Mozilla"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Refresh token révoqué — veuillez vous reconnecter");
    }

    @Test
    void rotateRefreshToken_tokenRevoque_nEmetPasDeNouveauToken() {
        // Arrange
        RefreshToken tokenRevoque = buildRefreshToken(true, LocalDateTime.now().plusDays(7));
        when(refreshTokenRepository.findByTokenHash(anyString()))
                .thenReturn(Optional.of(tokenRevoque));

        // Act / Assert
        assertThatThrownBy(() -> authService.rotateRefreshToken("rawTokenRevoque", "Mozilla"))
                .isInstanceOf(InvalidTokenException.class);

        // Aucun nouveau token ne doit être persisté
        verify(refreshTokenRepository, never()).save(argThat(t -> !t.isRevoque()));
        verifyNoInteractions(jwtUtil);
    }

    // ── rotateRefreshToken — token expiré ─────────────────────────────────────

    @Test
    void rotateRefreshToken_tokenExpire_leveInvalidTokenException() {
        // Arrange : token non révoqué mais dateExpiration dans le passé
        RefreshToken tokenExpire = buildRefreshToken(false, LocalDateTime.now().minusSeconds(1));
        when(refreshTokenRepository.findByTokenHash(anyString()))
                .thenReturn(Optional.of(tokenExpire));

        // Act / Assert
        assertThatThrownBy(() -> authService.rotateRefreshToken("rawTokenExpire", "Mozilla"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Refresh token expiré — veuillez vous reconnecter");
    }

    // ── rotateRefreshToken — rotation correcte ────────────────────────────────

    @Test
    void rotateRefreshToken_tokenValide_ancienTokenEstRevoque() {
        // Arrange
        RefreshToken ancienToken = buildRefreshToken(false, LocalDateTime.now().plusDays(7));
        when(refreshTokenRepository.findByTokenHash(anyString()))
                .thenReturn(Optional.of(ancienToken));
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(jwtUtil.generateToken(any(Superviseur.class), anyString()))
                .thenReturn("nouveau.access.token");

        // Act
        authService.rotateRefreshToken("rawTokenValide", "Mozilla");

        // Assert : l'ancien token doit avoir été marqué révoqué
        assertThat(ancienToken.isRevoque()).isTrue();
    }

    @Test
    void rotateRefreshToken_tokenValide_retourneNouveauxTokens() {
        // Arrange
        RefreshToken ancienToken = buildRefreshToken(false, LocalDateTime.now().plusDays(7));
        when(refreshTokenRepository.findByTokenHash(anyString()))
                .thenReturn(Optional.of(ancienToken));
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(jwtUtil.generateToken(any(Superviseur.class), anyString()))
                .thenReturn("nouveau.access.token");

        // Act
        var result = authService.rotateRefreshToken("rawTokenValide", "Mozilla");

        // Assert
        assertThat(result.newAccessToken()).isEqualTo("nouveau.access.token");
        assertThat(result.newRefreshToken()).isNotBlank();
        // Le nouveau raw refresh token doit être différent de l'ancien
        assertThat(result.newRefreshToken()).isNotEqualTo("rawTokenValide");
    }

    @Test
    void rotateRefreshToken_tokenValide_nouveauRefreshTokenPersisteDansDb() {
        // Arrange
        RefreshToken ancienToken = buildRefreshToken(false, LocalDateTime.now().plusDays(7));
        when(refreshTokenRepository.findByTokenHash(anyString()))
                .thenReturn(Optional.of(ancienToken));
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(jwtUtil.generateToken(any(Superviseur.class), anyString()))
                .thenReturn("nouveau.access.token");

        // Act
        authService.rotateRefreshToken("rawTokenValide", "Mozilla");

        // Assert : save appelé 2 fois — une pour révoquer l'ancien, une pour persister le nouveau
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }

    // ── revokeRefreshToken ────────────────────────────────────────────────────

    @Test
    void revokeRefreshToken_tokenExistant_marqueRevoque() {
        // Arrange
        RefreshToken token = buildRefreshToken(false, LocalDateTime.now().plusDays(7));
        when(refreshTokenRepository.findByTokenHash(anyString()))
                .thenReturn(Optional.of(token));
        when(refreshTokenRepository.save(any())).thenReturn(token);

        // Act
        authService.revokeRefreshToken("rawToken");

        // Assert
        assertThat(token.isRevoque()).isTrue();
    }

    @Test
    void revokeRefreshToken_tokenIntrouvable_aucuneExceptionLevee() {
        // Arrange : token absent en DB → comportement silencieux
        when(refreshTokenRepository.findByTokenHash(anyString()))
                .thenReturn(Optional.empty());

        // Act / Assert : pas d'exception
        authService.revokeRefreshToken("rawTokenAbsent");
        verify(refreshTokenRepository, never()).save(any());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private RefreshToken buildRefreshToken(boolean revoque, LocalDateTime dateExpiration) {
        RefreshToken t = new RefreshToken();
        t.setSuperviseur(superviseurActif);
        t.setTokenHash("hash-fictif");
        t.setRevoque(revoque);
        t.setDateExpiration(dateExpiration);
        t.setUserAgent("Mozilla");
        return t;
    }
}
