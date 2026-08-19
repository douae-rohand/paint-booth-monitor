package com.projet.config;

import com.sendgrid.SendGrid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration du client SendGrid.
 *
 * Le bean SendGrid est déclaré ici comme singleton Spring pour être injecté
 * dans SendGridEmailService. La clé API est lue depuis la variable d'environnement
 * SENDGRID_API_KEY (via application.yml → sendgrid.api-key) — jamais en dur.
 *
 * SÉCURITÉ : ne jamais logger la valeur de apiKey, même en debug.
 */
@Configuration
public class SendGridConfig {

    @Bean
    public SendGrid sendGridClient(@Value("${sendgrid.api-key}") String apiKey) {
        return new SendGrid(apiKey);
    }
}
