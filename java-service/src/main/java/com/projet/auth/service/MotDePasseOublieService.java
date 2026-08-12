package com.projet.auth.service;

import com.projet.auth.dto.DemandeReinitialisationDTO;
import com.projet.auth.dto.ReinitialisationMotDePasseDTO;
import com.projet.auth.exception.MotsDePasseNeCorrespondentPasException;
import com.projet.auth.exception.TokenInvalideOuExpireException;
import com.projet.auth.model.Superviseur;
import com.projet.auth.model.TokenReinitialisation;
import com.projet.auth.repository.SuperviseurRepository;
import com.projet.auth.repository.TokenReinitialisationRepository;
import com.projet.notifications.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class MotDePasseOublieService {

    private static final Logger logger = LoggerFactory.getLogger(MotDePasseOublieService.class);

    private final SuperviseurRepository superviseurRepository;
    private final TokenReinitialisationRepository tokenReinitialisationRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final MotDePasseValidator motDePasseValidator;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.auth.reset-token-expiration-minutes}")
    private int resetTokenExpirationMinutes;

    public MotDePasseOublieService(
            SuperviseurRepository superviseurRepository,
            TokenReinitialisationRepository tokenReinitialisationRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            MotDePasseValidator motDePasseValidator) {
        this.superviseurRepository = superviseurRepository;
        this.tokenReinitialisationRepository = tokenReinitialisationRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.motDePasseValidator = motDePasseValidator;
    }

    @Transactional
    public void demanderReinitialisation(DemandeReinitialisationDTO dto) {
        String email = dto.getEmail();
        Optional<Superviseur> superviseurOpt = superviseurRepository.findByEmail(email);

        if (superviseurOpt.isPresent()) {
            Superviseur superviseur = superviseurOpt.get();

            // Invalider les tokens de réinitialisation précédents valides/non expirés
            List<TokenReinitialisation> anciensTokens = tokenReinitialisationRepository.findBySuperviseur(superviseur);
            for (TokenReinitialisation t : anciensTokens) {
                if (!t.isUtilise()) {
                    t.setUtilise(true);
                }
            }
            tokenReinitialisationRepository.saveAll(anciensTokens);

            // Générer un token unique aléatoire de 64 caractères hexadécimaux
            String rawToken = UUID.randomUUID().toString().replace("-", "") +
                              UUID.randomUUID().toString().replace("-", "");
            String tokenHash = hashToken(rawToken);

            TokenReinitialisation tokenReinit = new TokenReinitialisation();
            tokenReinit.setSuperviseur(superviseur);
            tokenReinit.setTokenHash(tokenHash);
            tokenReinit.setUtilise(false);
            tokenReinit.setDateExpiration(LocalDateTime.now().plusMinutes(resetTokenExpirationMinutes));
            tokenReinit.setCreatedAt(LocalDateTime.now());
            tokenReinitialisationRepository.save(tokenReinit);

            // Construire le lien
            String lienReset = frontendUrl + "/reinitialiser-mot-de-passe?token=" + rawToken;

            // Envoyer l'email
            EmailService.EmailResult result = emailService.envoyerLienReinitialisation(email, lienReset);
            if (!result.isSucces()) {
                logger.error("[MOT DE PASSE OUBLIE] Échec de l'envoi de l'email de réinitialisation pour {} — statut={} — erreur={}",
                        email, result.statut(), result.erreur());
            } else {
                logger.info("[MOT DE PASSE OUBLIE] Email de réinitialisation envoyé avec succès pour {}", email);
            }
        } else {
            // Loguer silencieusement côté serveur pour l'audit
            logger.warn("[MOT DE PASSE OUBLIE] Demande de réinitialisation pour un email non enregistré : {}", email);
        }
    }

    @Transactional
    public void reinitialiserMotDePasse(ReinitialisationMotDePasseDTO dto) {
        // Valider la correspondance des mots de passe
        if (!dto.getNouveauMotDePasse().equals(dto.getConfirmationMotDePasse())) {
            throw new MotsDePasseNeCorrespondentPasException("Les mots de passe ne correspondent pas");
        }

        // Valider la robustesse du mot de passe (règles centralisées)
        motDePasseValidator.valider(dto.getNouveauMotDePasse());

        String hashOfToken = hashToken(dto.getToken());
        TokenReinitialisation tokenEntity = tokenReinitialisationRepository
                .findByTokenHashAndUtiliseFalseAndDateExpirationAfter(hashOfToken, LocalDateTime.now())
                .orElseThrow(() -> new TokenInvalideOuExpireException("Token de réinitialisation invalide ou expiré"));

        Superviseur superviseur = tokenEntity.getSuperviseur();
        superviseur.setMotDePasseHash(passwordEncoder.encode(dto.getNouveauMotDePasse()));
        superviseur.setUpdatedAt(LocalDateTime.now());
        superviseurRepository.save(superviseur);

        tokenEntity.setUtilise(true);
        tokenReinitialisationRepository.save(tokenEntity);

        logger.info("[MOT DE PASSE OUBLIE] Mot de passe réinitialisé pour le superviseur id={}", superviseur.getIdSuperviseur());
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error hashing token", e);
        }
    }
}
