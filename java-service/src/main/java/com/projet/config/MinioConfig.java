package com.projet.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration du client MinIO.
 *
 * Le bean MinioClient est déclaré ici comme singleton Spring pour être injecté
 * dans MinioStorageService. Les credentials sont lus depuis les variables d'environnement
 * MINIO_ENDPOINT, MINIO_ACCESS_KEY, MINIO_SECRET_KEY (via application.yml → minio.*)
 * — jamais en dur.
 *
 * SÉCURITÉ : ne jamais logger la valeur de accessKey ou secretKey, même en debug.
 */
@Configuration
public class MinioConfig {

    @Bean
    public MinioClient minioClient(
            @Value("${minio.endpoint}") String endpoint,
            @Value("${minio.access-key}") String accessKey,
            @Value("${minio.secret-key}") String secretKey) {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
