package com.projet.config;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Classe de base abstraite pour tous les tests d'intégration.
 *
 * <h2>Stratégie du conteneur singleton</h2>
 * Le conteneur PostgreSQL est démarré une seule fois pour toute la JVM de test
 * (champ {@code static} + {@code start()} dans le bloc statique). Cette approche
 * évite de recréer et de relancer Flyway pour chaque classe de test, ce qui réduit
 * significativement le temps d'exécution de la suite d'intégration.
 *
 * <h2>Injection des propriétés</h2>
 * {@link #injecterProprietesConteneur} est annotée {@link DynamicPropertySource} :
 * Spring injecte l'URL JDBC, le nom d'utilisateur et le mot de passe du conteneur
 * avant de démarrer le contexte applicatif, écrasant les valeurs vides de
 * {@code application-test.yml}.
 *
 * <h2>Flyway</h2>
 * Flyway s'exécute normalement contre le conteneur PostgreSQL à chaque démarrage
 * du contexte Spring. La base est donc dans un état propre et migré pour chaque
 * contexte (ou réutilisée si le contexte est mis en cache par Spring Test).
 *
 * <h2>Convention de nommage</h2>
 * Les tests d'intégration héritant de cette classe doivent être nommés {@code *IT.java}
 * pour être pris en charge par maven-failsafe-plugin (mvn verify) et non par
 * maven-surefire-plugin (mvn test).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
public abstract class AbstractIntegrationTest {

    /**
     * Conteneur PostgreSQL partagé entre toutes les classes de test de la JVM.
     *
     * <p>L'image {@code postgres:16-alpine} est choisie pour sa légèreté et sa
     * compatibilité avec la version PostgreSQL utilisée en production (définie dans
     * docker-compose.yml). Le conteneur reste actif jusqu'à l'arrêt de la JVM.
     */
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("supervision_test")
                    .withUsername("test_user")
                    .withPassword("test_password");

    static {
        POSTGRES.start();
    }

    /**
     * Injecte les propriétés de connexion du conteneur Testcontainers dans le
     * contexte Spring avant son démarrage, en remplacement des valeurs vides
     * définies dans {@code application-test.yml}.
     */
    @DynamicPropertySource
    static void injecterProprietesConteneur(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
