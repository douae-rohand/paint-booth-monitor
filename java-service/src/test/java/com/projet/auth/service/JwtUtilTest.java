package com.projet.auth.service;

import com.projet.auth.model.Superviseur;
import com.projet.auth.service.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests unitaires de JwtUtil.
 *
 * Pas de @Mock ici : JwtUtil n'a aucune dépendance injectée au sens Mockito.
 * Ses deux champs @Value (secret, expiration) sont injectés via ReflectionTestUtils.
 *
 * Le secret de test est un Base64 valide de 64 octets (512 bits), conforme
 * à ce qu'attend getSigningKey() (Base64.getDecoder().decode(...)).
 */
@ExtendWith(MockitoExtension.class)
class JwtUtilTest {

    // Secret test : 64 octets aléatoires encodés en Base64 standard
    // (correspond exactement à ce que fait getSigningKey() : Base64.getDecoder().decode(secret))
    private static final String SECRET_TEST =
            Base64.getEncoder().encodeToString(new byte[64]);

    // Durée de vie normale : 15 minutes en ms
    private static final long EXPIRATION_NORMALE = 900_000L;

    // Durée de vie volontairement dépassée pour tester les tokens expirés
    private static final long EXPIRATION_PASSEE = -1_000L;

    private JwtUtil jwtUtil;

    private Superviseur superviseurTest;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET_TEST);
        ReflectionTestUtils.setField(jwtUtil, "expiration", EXPIRATION_NORMALE);

        superviseurTest = new Superviseur();
        superviseurTest.setIdSuperviseur(UUID.randomUUID());
        superviseurTest.setEmail("test@exemple.com");
        superviseurTest.setNom("Dupont");
        superviseurTest.setPrenom("Alice");
        superviseurTest.setActif(true);
        superviseurTest.setCompteActive(true);
    }

    // ── generateToken / structure ──────────────────────────────────────────────

    @Test
    void generateToken_superviseurValide_retourneTokenNonVide() {
        // Arrange : superviseur avec rôle SUPERVISEUR

        // Act
        String token = jwtUtil.generateToken(superviseurTest, "ROLE_SUPERVISEUR");

        // Assert
        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    void generateToken_superviseurValide_tokenContientTroisSegments() {
        // Arrange : un JWT compact est toujours header.payload.signature

        // Act
        String token = jwtUtil.generateToken(superviseurTest, "ROLE_SUPERVISEUR");

        // Assert
        assertThat(token.split("\\.")).hasSize(3);
    }

    // ── extractSubject ─────────────────────────────────────────────────────────

    @Test
    void extractSubject_tokenValide_retourneUuidDuSuperviseur() {
        // Arrange
        String token = jwtUtil.generateToken(superviseurTest, "ROLE_SUPERVISEUR");

        // Act
        String subject = jwtUtil.extractSubject(token);

        // Assert
        assertThat(subject).isEqualTo(superviseurTest.getIdSuperviseur().toString());
    }

    @Test
    void extractSubject_tokenValide_subjectNEstPasEmail() {
        // Arrange : le payload doit contenir l'UUID, jamais l'email (sécurité)
        String token = jwtUtil.generateToken(superviseurTest, "ROLE_SUPERVISEUR");

        // Act
        String subject = jwtUtil.extractSubject(token);

        // Assert
        assertThat(subject).doesNotContain("@");
        assertThat(subject).isNotEqualTo(superviseurTest.getEmail());
    }

    // ── extractRole ────────────────────────────────────────────────────────────

    @Test
    void extractRole_tokenAvecRoleSuperviseur_retourneRoleCorrect() {
        // Arrange
        String token = jwtUtil.generateToken(superviseurTest, "ROLE_SUPERVISEUR");

        // Act
        String role = jwtUtil.extractRole(token);

        // Assert
        assertThat(role).isEqualTo("ROLE_SUPERVISEUR");
    }

    @Test
    void extractRole_tokenAvecRoleAdmin_retourneRoleAdmin() {
        // Arrange
        String token = jwtUtil.generateToken(superviseurTest, "ROLE_ADMIN");

        // Act
        String role = jwtUtil.extractRole(token);

        // Assert
        assertThat(role).isEqualTo("ROLE_ADMIN");
    }

    // ── isTokenValid ───────────────────────────────────────────────────────────

    @Test
    void isTokenValid_tokenValideEtSuperviseurCorrespondant_retourneTrue() {
        // Arrange
        String token = jwtUtil.generateToken(superviseurTest, "ROLE_SUPERVISEUR");

        // Act
        boolean valid = jwtUtil.isTokenValid(token, superviseurTest);

        // Assert
        assertThat(valid).isTrue();
    }

    @Test
    void isTokenValid_tokenDunAutreSuperviseur_retourneFalse() {
        // Arrange : token généré pour un autre superviseur
        Superviseur autresSuperviseur = new Superviseur();
        autresSuperviseur.setIdSuperviseur(UUID.randomUUID());
        autresSuperviseur.setEmail("autre@exemple.com");

        String token = jwtUtil.generateToken(autresSuperviseur, "ROLE_SUPERVISEUR");

        // Act
        boolean valid = jwtUtil.isTokenValid(token, superviseurTest);

        // Assert : le subject du token ne correspond pas à superviseurTest
        assertThat(valid).isFalse();
    }

    // ── token expiré ───────────────────────────────────────────────────────────

    @Test
    void extractSubject_tokenExpire_leveExpiredJwtException() {
        // Arrange : token généré avec expiration dans le passé (-1 seconde)
        JwtUtil jwtUtilExpire = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtilExpire, "secret", SECRET_TEST);
        ReflectionTestUtils.setField(jwtUtilExpire, "expiration", EXPIRATION_PASSEE);

        String tokenExpire = jwtUtilExpire.generateToken(superviseurTest, "ROLE_SUPERVISEUR");

        // Act / Assert
        assertThatThrownBy(() -> jwtUtil.extractSubject(tokenExpire))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void isTokenValid_tokenExpire_leveExpiredJwtException() {
        // Arrange
        JwtUtil jwtUtilExpire = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtilExpire, "secret", SECRET_TEST);
        ReflectionTestUtils.setField(jwtUtilExpire, "expiration", EXPIRATION_PASSEE);

        String tokenExpire = jwtUtilExpire.generateToken(superviseurTest, "ROLE_SUPERVISEUR");

        // Act / Assert : isTokenValid appelle extractClaim qui parse le token → ExpiredJwtException
        assertThatThrownBy(() -> jwtUtil.isTokenValid(tokenExpire, superviseurTest))
                .isInstanceOf(ExpiredJwtException.class);
    }

    // ── signature invalide ─────────────────────────────────────────────────────

    @Test
    void extractSubject_tokenSignatureAltere_leveSignatureException() {
        // Arrange : token valide dont on altère le dernier octet de la signature
        String token = jwtUtil.generateToken(superviseurTest, "ROLE_SUPERVISEUR");
        String tokenAltere = token.substring(0, token.length() - 4) + "XXXX";

        // Act / Assert
        assertThatThrownBy(() -> jwtUtil.extractSubject(tokenAltere))
                .isInstanceOf(SignatureException.class);
    }

    @Test
    void extractSubject_tokenMalForme_leveMalformedJwtException() {
        // Arrange : chaîne qui n'est pas un JWT valide
        String tokenInvalide = "pas.un.token.valide.du.tout";

        // Act / Assert
        assertThatThrownBy(() -> jwtUtil.extractSubject(tokenInvalide))
                .isInstanceOf(MalformedJwtException.class);
    }
}
