package com.projet;

import com.projet.config.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * Test de démarrage du contexte Spring.
 *
 * <p>Vérifie que l'application démarre correctement avec :
 * <ul>
 *   <li>Le profil "test" (valeurs factices pour les services externes)</li>
 *   <li>Un conteneur PostgreSQL Testcontainers réel (singleton partagé)</li>
 *   <li>Flyway exécuté contre ce conteneur</li>
 * </ul>
 *
 * <p>Nommé {@code *IT.java} — pris en charge par maven-failsafe-plugin
 * ({@code mvn verify}) et non par maven-surefire-plugin ({@code mvn test}).
 * Cela garantit que {@code mvn test} (CI rapide sans Docker) ne tente jamais
 * de démarrer un conteneur Testcontainers.
 */
class JavaServiceApplicationIT extends AbstractIntegrationTest {

    @Test
    void contextLoads() {
        // Vérifie que le contexte Spring démarre sans exception.
        // Flyway s'exécute contre le conteneur PostgreSQL Testcontainers.
        // Aucune assertion supplémentaire n'est nécessaire ici — le simple
        // fait que ce test passe confirme que toutes les dépendances se
        // résolvent correctement et que les migrations Flyway sont valides.
    }
}
