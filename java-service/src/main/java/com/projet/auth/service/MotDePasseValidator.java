package com.projet.auth.service;

import com.projet.auth.exception.MotDePasseTropFaibleException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Validateur centralisé de robustesse des mots de passe.
 *
 * Règles appliquées (identiques aux 3 flows : activation, réinitialisation, changement de mot de passe) :
 * <ul>
 *   <li>Au moins 8 caractères</li>
 *   <li>Au moins 3 des 4 catégories : minuscules, majuscules, chiffres, caractères spéciaux</li>
 *   <li>Pas plus de 2 caractères identiques consécutifs (ex: "aaa" refusé)</li>
 * </ul>
 *
 * Utilisation : injecter {@code MotDePasseValidator} dans le service et appeler
 * {@link #valider(String)} avant toute mise à jour du hash.
 * Lance {@link MotDePasseTropFaibleException} avec la liste précise des violations.
 */
@Component
public class MotDePasseValidator {

    /**
     * Valide le mot de passe selon les règles de robustesse.
     *
     * @param motDePasse le mot de passe en clair à valider
     * @throws MotDePasseTropFaibleException si au moins une règle est violée,
     *         avec la liste détaillée des violations dans le message.
     */
    public void valider(String motDePasse) {
        List<String> violations = new ArrayList<>();

        if (motDePasse == null || motDePasse.length() < 8) {
            violations.add("Le mot de passe doit contenir au moins 8 caractères.");
        }

        if (motDePasse != null) {
            int categories = 0;
            if (motDePasse.matches(".*[a-z].*")) categories++;
            if (motDePasse.matches(".*[A-Z].*")) categories++;
            if (motDePasse.matches(".*[0-9].*")) categories++;
            if (motDePasse.matches(".*[^a-zA-Z0-9].*")) categories++;

            if (categories < 3) {
                violations.add(
                    "Le mot de passe doit contenir au moins 3 des 4 catégories suivantes : " +
                    "minuscules (a-z), majuscules (A-Z), chiffres (0-9), caractères spéciaux (!@#$%^&*…)."
                );
            }

            // Vérifier les répétitions de plus de 2 caractères consécutifs identiques
            for (int i = 0; i < motDePasse.length() - 2; i++) {
                if (motDePasse.charAt(i) == motDePasse.charAt(i + 1)
                        && motDePasse.charAt(i) == motDePasse.charAt(i + 2)) {
                    violations.add("Le mot de passe ne doit pas contenir plus de 2 caractères identiques consécutifs (ex : \"aaa\" est refusé).");
                    break;
                }
            }
        }

        if (!violations.isEmpty()) {
            throw new MotDePasseTropFaibleException(violations);
        }
    }
}
