package com.projet.reports.storage;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Service pour le stockage et la récupération de fichiers sur MinIO.
 * Module: reports
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MinioStorageService {

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucketName;

    /**
     * Upload un fichier sur MinIO.
     *
     * @param objetKey Clé d'objet (chemin complet dans le bucket)
     * @param contenu Contenu du fichier en bytes
     * @throws Exception en cas d'erreur d'upload
     */
    public void upload(String objetKey, byte[] contenu) throws Exception {
        PutObjectArgs args = PutObjectArgs.builder()
                .bucket(bucketName)
                .object(objetKey)
                .stream(new java.io.ByteArrayInputStream(contenu), contenu.length, -1)
                .contentType("application/pdf")
                .build();

        minioClient.putObject(args);
        log.info("Fichier uploadé sur MinIO: bucket={}, key={}", bucketName, objetKey);
    }

    /**
     * Télécharge un fichier depuis MinIO.
     *
     * @param objetKey Clé d'objet (chemin complet dans le bucket)
     * @return InputStream du fichier
     * @throws Exception en cas d'erreur de téléchargement
     */
    public InputStream download(String objetKey) throws Exception {
        GetObjectArgs args = GetObjectArgs.builder()
                .bucket(bucketName)
                .object(objetKey)
                .build();

        return minioClient.getObject(args);
    }

    /**
     * Génère la clé d'objet MinIO pour un rapport PDF.
     * Format: rapports/{année}/{mois}/{idRapport}.pdf
     *
     * @param idRapport ID du rapport
     * @param dateGeneration Date de génération du rapport
     * @return Clé d'objet MinIO
     */
    public String generateObjetKey(UUID idRapport, java.time.LocalDateTime dateGeneration) {
        YearMonth yearMonth = YearMonth.from(dateGeneration);
        return String.format("rapports/%d/%02d/%s.pdf",
                yearMonth.getYear(),
                yearMonth.getMonthValue(),
                idRapport);
    }

    /**
     * Génère le nom de fichier lisible pour un rapport PDF.
     * Format: rapport_{nomPoint}_{dateDebut}_{dateFin}.pdf
     * Les caractères spéciaux et espaces sont normalisés.
     *
     * @param nomPointMesure Nom du point de mesure
     * @param dateDebut Date de début de la période
     * @param dateFin Date de fin de la période
     * @return Nom de fichier lisible
     */
    public String generateNomFichier(String nomPointMesure, java.time.LocalDateTime dateDebut, java.time.LocalDateTime dateFin) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm");
        String nomPointNormalise = nomPointMesure.replaceAll("[^a-zA-Z0-9_-]", "_");
        return String.format("rapport_%s_%s_%s.pdf",
                nomPointNormalise,
                dateDebut.format(formatter),
                dateFin.format(formatter));
    }
}
