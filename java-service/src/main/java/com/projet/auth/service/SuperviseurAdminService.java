package com.projet.auth.service;

import com.projet.auth.dto.*;
import com.projet.auth.exception.EmailDejaUtiliseException;
import com.projet.auth.exception.MotsDePasseNeCorrespondentPasException;
import com.projet.auth.exception.SuperviseurNonTrouveException;
import com.projet.auth.exception.TokenInvalideOuExpireException;
import com.projet.auth.model.Superviseur;
import com.projet.auth.model.TokenActivation;
import com.projet.auth.repository.RefreshTokenRepository;
import com.projet.auth.repository.SuperviseurRepository;
import com.projet.auth.repository.TokenActivationRepository;
import com.projet.notifications.service.EmailService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class SuperviseurAdminService {

    private final SuperviseurRepository superviseurRepository;
    private final TokenActivationRepository tokenActivationRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public SuperviseurAdminService(
            SuperviseurRepository superviseurRepository,
            TokenActivationRepository tokenActivationRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService) {
        this.superviseurRepository = superviseurRepository;
        this.tokenActivationRepository = tokenActivationRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Transactional
    public SuperviseurResponseDTO creer(SuperviseurCreateDTO dto) {
        // Valider email unique
        if (superviseurRepository.existsByEmail(dto.getEmail())) {
            throw new EmailDejaUtiliseException("Cet email est déjà utilisé");
        }

        // Créer le Superviseur avec mot_de_passe_hash = null (compte non activé)
        Superviseur superviseur = new Superviseur();
        superviseur.setNom(dto.getNom());
        superviseur.setPrenom(dto.getPrenom());
        superviseur.setEmail(dto.getEmail());
        superviseur.setPhone(dto.getTelephone());
        superviseur.setMotDePasseHash(null);
        superviseur.setActif(true);
        superviseur.setCompteActive(false);
        superviseur.setCreatedAt(LocalDateTime.now());
        superviseur.setMustChangePassword(false);

        superviseur = superviseurRepository.save(superviseur);

        // Générer le token d'activation
        String rawToken = UUID.randomUUID().toString().replace("-", "") + 
                          UUID.randomUUID().toString().replace("-", "");
        String tokenHash = hashToken(rawToken);

        TokenActivation tokenActivation = new TokenActivation();
        tokenActivation.setSuperviseur(superviseur);
        tokenActivation.setTokenHash(tokenHash);
        tokenActivation.setUtilise(false);
        tokenActivation.setDateExpiration(LocalDateTime.now().plusHours(48));
        tokenActivation.setCreatedAt(LocalDateTime.now());
        tokenActivationRepository.save(tokenActivation);

        // Construire le lien d'activation
        // TODO: Récupérer l'URL frontend depuis la configuration
        String lienActivation = "https://<frontend>/activation?token=" + rawToken;

        // Envoyer l'email (stub pour l'instant)
        emailService.envoyerLienActivation(dto.getEmail(), lienActivation);

        return mapToResponseDTO(superviseur);
    }

    @Transactional
    public void activerCompte(ActivationCompteDTO dto) {
        // Retrouver le token
        String tokenHash = hashToken(dto.getToken());
        TokenActivation tokenActivation = tokenActivationRepository
                .findByTokenHashAndUtiliseFalseAndDateExpirationAfter(tokenHash, LocalDateTime.now())
                .orElseThrow(() -> new TokenInvalideOuExpireException("Token invalide ou expiré"));

        // Valider que les mots de passe correspondent
        if (!dto.getNouveauMotDePasse().equals(dto.getConfirmationMotDePasse())) {
            throw new MotsDePasseNeCorrespondentPasException("Les mots de passe ne correspondent pas");
        }

        // Valider la politique de mot de passe (au moins 8 caractères)
        if (dto.getNouveauMotDePasse().length() < 8) {
            throw new IllegalArgumentException("Le mot de passe doit contenir au moins 8 caractères");
        }

        // Hasher et définir le mot de passe
        Superviseur superviseur = tokenActivation.getSuperviseur();
        superviseur.setMotDePasseHash(passwordEncoder.encode(dto.getNouveauMotDePasse()));
        superviseur.setCompteActive(true);
        superviseur.setUpdatedAt(LocalDateTime.now());
        superviseurRepository.save(superviseur);

        // Marquer le token comme utilisé
        tokenActivation.setUtilise(true);
        tokenActivationRepository.save(tokenActivation);
    }

    public Page<SuperviseurListItemDTO> lister(Boolean filtreActif, Boolean filtreCompteActive, Pageable pageable) {
        Page<Superviseur> page;
        if (filtreActif != null && filtreCompteActive != null) {
            page = superviseurRepository.findByAdminIsNullAndActifAndCompteActive(filtreActif, filtreCompteActive, pageable);
        } else if (filtreActif != null) {
            page = superviseurRepository.findByAdminIsNullAndActif(filtreActif, pageable);
        } else if (filtreCompteActive != null) {
            page = superviseurRepository.findByAdminIsNullAndCompteActive(filtreCompteActive, pageable);
        } else {
            page = superviseurRepository.findByAdminIsNull(pageable);
        }
        return page.map(this::mapToListItemDTO);
    }

    public SuperviseurResponseDTO getById(UUID id) {
        Superviseur superviseur = superviseurRepository.findById(id)
                .orElseThrow(() -> new SuperviseurNonTrouveException("Superviseur non trouvé"));

        // Vérifier que ce n'est pas un Admin
        if (superviseur.getAdmin() != null) {
            throw new SuperviseurNonTrouveException("Superviseur non trouvé");
        }

        return mapToResponseDTO(superviseur);
    }

    @Transactional
    public SuperviseurResponseDTO modifier(UUID id, SuperviseurUpdateDTO dto) {
        Superviseur superviseur = superviseurRepository.findById(id)
                .orElseThrow(() -> new SuperviseurNonTrouveException("Superviseur non trouvé"));

        // Vérifier que ce n'est pas un Admin
        if (superviseur.getAdmin() != null) {
            throw new SuperviseurNonTrouveException("Superviseur non trouvé");
        }

        // Valider unicité email si modifié
        if (dto.getEmail() != null && !dto.getEmail().equals(superviseur.getEmail())) {
            if (superviseurRepository.existsByEmailAndIdNot(dto.getEmail(), id)) {
                throw new EmailDejaUtiliseException("Cet email est déjà utilisé");
            }
        }

        // UPDATE nom/prenom/email/telephone uniquement
        if (dto.getNom() != null) {
            superviseur.setNom(dto.getNom());
        }
        if (dto.getPrenom() != null) {
            superviseur.setPrenom(dto.getPrenom());
        }
        if (dto.getEmail() != null) {
            superviseur.setEmail(dto.getEmail());
        }
        if (dto.getTelephone() != null) {
            superviseur.setPhone(dto.getTelephone());
        }

        superviseur.setUpdatedAt(LocalDateTime.now());
        superviseur = superviseurRepository.save(superviseur);

        return mapToResponseDTO(superviseur);
    }

    @Transactional
    public void desactiver(UUID id) {
        Superviseur superviseur = superviseurRepository.findById(id)
                .orElseThrow(() -> new SuperviseurNonTrouveException("Superviseur non trouvé"));

        // Vérifier que ce n'est pas un Admin
        if (superviseur.getAdmin() != null) {
            throw new SuperviseurNonTrouveException("Superviseur non trouvé");
        }

        superviseur.setActif(false);
        superviseur.setUpdatedAt(LocalDateTime.now());
        superviseurRepository.save(superviseur);

        // Invalider tous les refresh_token actifs de ce superviseur
        refreshTokenRepository.findBySuperviseur(superviseur)
                .forEach(token -> {
                    token.setRevoque(true);
                    refreshTokenRepository.save(token);
                });
    }

    @Transactional
    public void activer(UUID id) {
        Superviseur superviseur = superviseurRepository.findById(id)
                .orElseThrow(() -> new SuperviseurNonTrouveException("Superviseur non trouvé"));

        // Vérifier que ce n'est pas un Admin
        if (superviseur.getAdmin() != null) {
            throw new SuperviseurNonTrouveException("Superviseur non trouvé");
        }

        superviseur.setActif(true);
        superviseur.setUpdatedAt(LocalDateTime.now());
        superviseurRepository.save(superviseur);

        // Ne pas toucher à compteActive (statut d'activation initial indépendant)
    }

    private SuperviseurResponseDTO mapToResponseDTO(Superviseur superviseur) {
        SuperviseurResponseDTO dto = new SuperviseurResponseDTO();
        dto.setId(superviseur.getIdSuperviseur());
        dto.setNom(superviseur.getNom());
        dto.setPrenom(superviseur.getPrenom());
        dto.setEmail(superviseur.getEmail());
        dto.setTelephone(superviseur.getPhone());
        dto.setActif(superviseur.isActif());
        dto.setCompteActive(superviseur.isCompteActive());
        dto.setCreatedAt(superviseur.getCreatedAt());
        return dto;
    }

    private SuperviseurListItemDTO mapToListItemDTO(Superviseur superviseur) {
        SuperviseurListItemDTO dto = new SuperviseurListItemDTO();
        dto.setId(superviseur.getIdSuperviseur());
        dto.setNom(superviseur.getNom());
        dto.setPrenom(superviseur.getPrenom());
        dto.setEmail(superviseur.getEmail());
        dto.setActif(superviseur.isActif());
        dto.setCompteActive(superviseur.isCompteActive());
        return dto;
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
