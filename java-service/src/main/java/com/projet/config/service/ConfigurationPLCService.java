package com.projet.config.service;

import com.projet.auth.model.Admin;
import com.projet.auth.repository.AdminRepository;
import com.projet.config.dto.ConfigurationPLCRequest;
import com.projet.config.dto.ConfigurationPLCResponse;
import com.projet.config.exception.ConfigurationPLCNotFoundException;
import com.projet.config.model.ConfigurationPLC;
import com.projet.config.repository.ConfigurationPLCRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ConfigurationPLCService {

    // -- Constantes metier -----------------------------------------------------

    /** Port TCP par defaut du protocole Snap7 / ISO-TSAP. */
    private static final int PORT_DEFAUT = 102;

    /** Port TCP minimum autorise. */
    private static final int PORT_MIN = 1;

    /** Port TCP maximum autorise. */
    private static final int PORT_MAX = 65535;

    /**
     * Intervalle de polling minimum en millisecondes.
     * En dessous de 100 ms le polling devient instable sur la plupart des automates S7.
     */
    private static final int POLLING_MIN_MS = 100;

    /** Regex de validation d'une adresse IPv4. */
    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^((25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)$"
    );

    // -- Dependances -----------------------------------------------------------

    private final ConfigurationPLCRepository configurationPLCRepository;
    private final AdminRepository adminRepository;

    public ConfigurationPLCService(ConfigurationPLCRepository configurationPLCRepository,
                                   AdminRepository adminRepository) {
        this.configurationPLCRepository = configurationPLCRepository;
        this.adminRepository = adminRepository;
    }

    // -- Methodes publiques ----------------------------------------------------

    /**
     * Cree une nouvelle configuration PLC et l'active immediatement.
     *
     * Logique :
     *  1. Valide tous les champs du DTO.
     *  2. Desactive la configuration actuellement active (si existante).
     *  3. Insere la nouvelle configuration avec actif=true et dateActivation=now().
     *
     * Tout s'execute dans une unique transaction pour garantir l'atomicite.
     *
     * @param request   DTO de creation envoye par le frontend.
     * @param adminId   UUID de l'Admin authentifie (extrait du SecurityContext par le controller).
     * @return          DTO de la configuration nouvellement creee.
     */
    @Transactional
    public ConfigurationPLCResponse creerConfiguration(ConfigurationPLCRequest request, UUID adminId) {
        validerRequest(request);

        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new ConfigurationPLCNotFoundException(
                        "Admin introuvable : " + adminId));

        // Desactiver la configuration active precedente
        configurationPLCRepository.findByActifTrue().ifPresent(active -> {
            active.setActif(false);
            active.setDateDesactivation(LocalDateTime.now());
            configurationPLCRepository.save(active);
        });

        // Creer et persister la nouvelle configuration
        ConfigurationPLC config = new ConfigurationPLC();
        config.setAdmin(admin);
        config.setPlcIp(request.getIp().trim());
        config.setPlcPort(request.getPort() != null ? request.getPort() : PORT_DEFAUT);
        config.setPlcRack(request.getRack());
        config.setPlcSlot(request.getSlot());
        config.setPlcPollingIntervalMs(request.getIntervallePolling());
        config.setActif(true);
        config.setDateActivation(LocalDateTime.now());

        ConfigurationPLC saved = configurationPLCRepository.save(config);
        return ConfigurationPLCResponse.from(saved);
    }

    /**
     * Active la configuration identifiee par {@code id}.
     *
     * Logique :
     *  1. Verifie que la configuration existe.
     *  2. Desactive l'eventuelle configuration actuellement active (autre que la cible).
     *  3. Active la configuration ciblee.
     *
     * Garantit qu'il n'existe jamais plus d'une configuration active apres l'operation.
     *
     * @param id    UUID de la configuration a activer.
     * @return      DTO de la configuration activee.
     */
    @Transactional
    public ConfigurationPLCResponse activerConfiguration(UUID id) {
        ConfigurationPLC cible = configurationPLCRepository.findById(id)
                .orElseThrow(() -> new ConfigurationPLCNotFoundException(
                        "Configuration PLC introuvable : " + id));

        // Desactiver l'eventuelle configuration active (differente de la cible)
        configurationPLCRepository.findByActifTrue().ifPresent(active -> {
            if (!active.getIdConfiguration().equals(id)) {
                active.setActif(false);
                active.setDateDesactivation(LocalDateTime.now());
                configurationPLCRepository.save(active);
            }
        });

        // Activer la cible si elle ne l'est pas deja
        if (!cible.isActif()) {
            cible.setActif(true);
            cible.setDateActivation(LocalDateTime.now());
            cible.setDateDesactivation(null);
            configurationPLCRepository.save(cible);
        }

        return ConfigurationPLCResponse.from(cible);
    }

    /**
     * Desactive la configuration identifiee par {@code id}.
     *
     * - Marque actif=false et date_desactivation=now().
     * - Ne touche pas a dateActivation (garde la trace de la derniere activation).
     * - Ne leve pas d'exception si aucune configuration n'est active apres l'operation.
     *
     * @param id    UUID de la configuration a desactiver.
     * @return      DTO de la configuration desactivee.
     */
    @Transactional
    public ConfigurationPLCResponse desactiverConfiguration(UUID id) {
        ConfigurationPLC config = configurationPLCRepository.findById(id)
                .orElseThrow(() -> new ConfigurationPLCNotFoundException(
                        "Configuration PLC introuvable : " + id));

        config.setActif(false);
        config.setDateDesactivation(LocalDateTime.now());
        configurationPLCRepository.save(config);

        return ConfigurationPLCResponse.from(config);
    }

    /**
     * Retourne la configuration actuellement active.
     *
     * @throws ConfigurationPLCNotFoundException si aucune configuration n'est active.
     */
    @Transactional(readOnly = true)
    public ConfigurationPLCResponse getConfigurationActive() {
        ConfigurationPLC active = configurationPLCRepository.findByActifTrue()
                .orElseThrow(() -> new ConfigurationPLCNotFoundException(
                        "Aucune configuration PLC active n'est definie"));
        return ConfigurationPLCResponse.from(active);
    }

    /**
     * Retourne l'historique complet des configurations PLC,
     * de la plus recente a la plus ancienne.
     */
    @Transactional(readOnly = true)
    public List<ConfigurationPLCResponse> getHistorique() {
        return configurationPLCRepository.findAllByOrderByDateCreationDesc()
                .stream()
                .map(ConfigurationPLCResponse::from)
                .collect(Collectors.toList());
    }

    // -- Validation ------------------------------------------------------------

    /**
     * Valide tous les champs du DTO de creation.
     * Toute violation leve une {@link IllegalArgumentException} capturee par
     * {@link com.projet.config.GlobalExceptionHandler} a†’ HTTP 400.
     */
    private void validerRequest(ConfigurationPLCRequest request) {
        if (request.getIp() == null || !IPV4_PATTERN.matcher(request.getIp().trim()).matches()) {
            throw new IllegalArgumentException(
                    "L'adresse IP de l'automate est invalide. Format attendu : IPv4 (ex: 192.168.0.1)");
        }

        if (request.getPort() != null && (request.getPort() < PORT_MIN || request.getPort() > PORT_MAX)) {
            throw new IllegalArgumentException(
                    "Le port TCP doit etre compris entre " + PORT_MIN + " et " + PORT_MAX);
        }

        if (request.getRack() == null || request.getRack() < 0) {
            throw new IllegalArgumentException("Le rack doit etre un entier positif ou nul (>= 0)");
        }

        if (request.getSlot() == null || request.getSlot() < 0) {
            throw new IllegalArgumentException("Le slot doit etre un entier positif ou nul (>= 0)");
        }

        if (request.getIntervallePolling() == null || request.getIntervallePolling() < POLLING_MIN_MS) {
            throw new IllegalArgumentException(
                    "L'intervalle de polling doit etre d'au moins " + POLLING_MIN_MS + " ms");
        }
    }
}
