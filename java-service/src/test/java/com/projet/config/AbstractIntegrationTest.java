package com.projet.config;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

/**
 * Classe de base abstraite pour tous les tests d'intégration.
 *
 * <h2>Stratégie du conteneur singleton</h2>
 * Le conteneur PostgreSQL est démarré une seule fois pour toute la JVM de test
 * (champ {@code static} + {@code start()} dans le bloc statique). Cette approche
 * évite de recréer et de relancer Flyway pour chaque classe de test, ce qui réduit
 * significativement le temps d'exécution de la suite d'intégration.
 *
 * <h2>Script d'initialisation</h2>
 * {@code docker/postgres/init.sh} est copié dans {@code /docker-entrypoint-initdb.d/}
 * du conteneur, dossier que PostgreSQL exécute automatiquement au premier démarrage.
 * Ce script est la seule source de vérité pour la création des rôles applicatifs
 * ({@code java_service}, {@code python_service}) et leurs grants — identique à ce
 * que fait Docker Compose en production.
 *
 * <p>Le script utilise cinq variables d'environnement injectées via {@code .withEnv()} :
 * {@code POSTGRES_USER} et {@code POSTGRES_DB} sont déjà positionnées par
 * {@link PostgreSQLContainer} ; les quatre autres sont injectées explicitement
 * avec des valeurs de test dédiées.
 *
 * <h2>Injection des propriétés Spring</h2>
 * {@link #injecterProprietesConteneur} est annotée {@link DynamicPropertySource} :
 * Spring injecte l'URL JDBC, le nom d'utilisateur et le mot de passe du conteneur
 * avant de démarrer le contexte applicatif, écrasant les valeurs vides de
 * {@code application-test.yml}.
 *
 * <p>La datasource Spring pointe sur {@code test_user} (superutilisateur du conteneur)
 * et non sur {@code java_service}, car {@code java_service} est créé par le script
 * d'init et ne dispose pas encore des droits nécessaires pour que Flyway crée
 * la table {@code flyway_schema_history} au premier démarrage.
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
     *
     * <p>{@code docker/postgres/init.sh} est copié dans
     * {@code /docker-entrypoint-initdb.d/} afin que PostgreSQL l'exécute
     * automatiquement au premier démarrage du conteneur. Les variables
     * d'environnement requises par le script sont injectées via {@code .withEnv()}.
     */
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("supervision_test")
                    .withUsername("test_user")
                    .withPassword("test_password")
                    // Variables d'environnement requises par init.sh.
                    // POSTGRES_USER et POSTGRES_DB sont déjà positionnées par PostgreSQLContainer.
                    // Les valeurs ci-dessous sont identiques à celles du .env Docker existant
                    // (JAVA_SERVICE_DB_USER, JAVA_SERVICE_DB_PASSWORD, etc.) pour garantir
                    // la cohérence entre l'environnement de test et l'environnement Docker Compose.
                    .withEnv("JAVA_SERVICE_DB_USER",       "java_service_test")
                    .withEnv("JAVA_SERVICE_DB_PASSWORD",   "test_pwd_only")
                    .withEnv("PYTHON_SERVICE_DB_USER",     "python_service_test")
                    .withEnv("PYTHON_SERVICE_DB_PASSWORD", "test_pwd_only")
                    // Copie du script dans docker-entrypoint-initdb.d/ avant le démarrage du conteneur.
                    // PostgreSQL exécute automatiquement tous les fichiers *.sh et *.sql de ce dossier
                    // lors du premier démarrage. Le mode 0755 garantit les droits d'exécution requis
                    // par l'image officielle postgres (scripts non exécutables = silencieusement ignorés).
                    // Chemin source résolu depuis le working directory Maven (java-service/) :
                    //   ../docker/postgres/init.sh → <racine_projet>/docker/postgres/init.sh
                    .withCopyFileToContainer(
                            MountableFile.forHostPath("../docker/postgres/init.sh", 0755),
                            "/docker-entrypoint-initdb.d/init.sh"
                    );

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
        registry.add("spring.datasource.url",      POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
