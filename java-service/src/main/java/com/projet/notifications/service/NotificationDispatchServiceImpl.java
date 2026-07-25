package com.projet.notifications.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Implémentation stub du service de dispatch des notifications d'alerte.
 * 
 * Cette implémentation logge uniquement la réception d'une alerte.
 * L'implémentation réelle (email, WhatsApp, push) sera branchée ultérieurement
 * en cohérence avec CDC section 7.
 */
@Service
public class NotificationDispatchServiceImpl implements NotificationDispatchService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationDispatchServiceImpl.class);

    @Override
    public void dispatcherAlerte(Long idAlerte) {
        logger.info("Alerte {} reçue via NOTIFY, dispatch réel à implémenter", idAlerte);
    }
}
