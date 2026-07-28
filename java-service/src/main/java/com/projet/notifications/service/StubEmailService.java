package com.projet.notifications.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class StubEmailService implements EmailService {
    private static final Logger log = LoggerFactory.getLogger(StubEmailService.class);

    @Override
    public void envoyerLienActivation(String email, String lienActivation) {
        log.info("[STUB EMAIL] Envoi du lien d'activation à {}", email);
        log.info("[STUB EMAIL] Lien d'activation: {}", lienActivation);
        // TODO: Brancher un vrai provider SMTP plus tard sans modifier l'interface
    }
}
