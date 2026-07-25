package com.projet.notifications.service;

/**
 * Service de dispatch des notifications d'alerte.
 * 
 * Ce service est responsable du dispatch multicanal des alertes (email, WhatsApp, push)
 * après réception via PostgreSQL NOTIFY.
 * 
 * L'implémentation actuelle est un stub qui logge uniquement.
 * L'implémentation réelle (cohérente avec CDC section 7) sera branchée ultérieurement.
 */
public interface NotificationDispatchService {

    /**
     * Dispatche une alerte vers les canaux configurés.
     * 
     * @param idAlerte Identifiant de l'alerte à dispatcher
     */
    void dispatcherAlerte(Long idAlerte);
}
