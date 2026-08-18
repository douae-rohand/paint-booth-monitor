package com.projet.notifications.service;

import com.projet.alerting.model.enums.Metrique;
import com.projet.auth.model.Superviseur;

import java.util.UUID;

/**
 * Contrat de dispatch des notifications multicanal (EMAIL + IN_APP).
 * Chaque méthode correspond à un TypeEvenement distinct.
 */
public interface NotificationDispatchService {

    /** Déclenché par NOTIFY nouvelle_alerte — crée et dispatche ALERTE_CREE. */
    void dispatcherAlerte(UUID idAlerte);

    /** Déclenché par NOTIFY alerte_resolue — crée et dispatche ALERTE_RESOLU. */
    void dispatcherAlerteResolue(UUID idAlerte);

    /** Déclenché à l'activation d'un compte superviseur. */
    void dispatcherCompteActive(Superviseur superviseur);

    /** Déclenché à la modification d'un seuil absolu ou dynamique par un Admin. */
    void dispatcherSeuilModifie(String nomPointMesure, Metrique metrique, boolean estAbsolu);

    /** Déclenché à la génération d'un rapport PDF. */
    void dispatcherRapportGenere(String nomRapport);
}
