package com.projet.config;

import com.projet.auth.service.JwtFilter;
import com.projet.auth.service.MustChangePasswordFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Désactive l'auto-enregistrement Servlet de chacun des filtres @Component
 * utilisés dans la chaîne Spring Security.
 *
 * Problème : Spring Boot enregistre automatiquement tout bean @Component qui
 * étend OncePerRequestFilter dans la chaîne Servlet globale (via un
 * FilterRegistrationBean implicite), EN PLUS de leur ajout explicite dans
 * SecurityConfig via addFilterBefore/addFilterAfter.
 * Résultat : chaque filtre s'exécutait deux fois — la première exécution
 * (hors chaîne Security, sans évaluation des permitAll()) bloquait toutes
 * les requêtes avec un 403 avant que Spring Security ne les voie.
 *
 * Solution : déclarer un FilterRegistrationBean<X> avec setEnabled(false)
 * pour chaque filtre concerné. Cela neutralise l'enregistrement automatique
 * Servlet tout en conservant intacte leur utilisation dans SecurityConfig.
 */
@Configuration
public class FilterRegistrationConfig {

    @Bean
    public FilterRegistrationBean<CsrfOriginFilter> csrfOriginFilterRegistration(CsrfOriginFilter filter) {
        FilterRegistrationBean<CsrfOriginFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<JwtFilter> jwtFilterRegistration(JwtFilter filter) {
        FilterRegistrationBean<JwtFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<MustChangePasswordFilter> mustChangePasswordFilterRegistration(MustChangePasswordFilter filter) {
        FilterRegistrationBean<MustChangePasswordFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
