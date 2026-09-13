package com.projet.fileanalysis.service;

import com.projet.config.BusinessException;
import com.projet.fileanalysis.dto.FileAnalysisPointDTO;
import com.projet.fileanalysis.dto.FileAnalysisResponseDTO;
import com.projet.config.model.enums.Granularite;
import com.projet.config.GranulariteUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Service pour l'analyse, le parsing et l'agrégation en mémoire de fichiers CSV externes.
 */
@Service
@Slf4j
public class FileAnalysisService {

    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10 Mo
    private static final int MAX_ROW_COUNT = 200_000;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d/M/yyyy");
    private static final DateTimeFormatter[] TIME_FORMATTERS = {
            DateTimeFormatter.ofPattern("H:m:s.SSS"),
            DateTimeFormatter.ofPattern("H:m:s.SS"),
            DateTimeFormatter.ofPattern("H:m:s.S"),
            DateTimeFormatter.ofPattern("H:m:s"),
            DateTimeFormatter.ofPattern("H:m")
    };

    /**
     * Analyse un fichier CSV de mesures externes, extrait la métrique, valide les lignes,
     * calcule la granularité automatique et agrège les points en mémoire.
     *
     * @param file Fichier CSV téléversé
     * @return FileAnalysisResponseDTO avec métriques et points agrégés
     */
    public FileAnalysisResponseDTO analyserFichier(MultipartFile file) {
        // 1. Validation de l'existence et de la taille du fichier
        if (file == null || file.isEmpty()) {
            throw new BusinessException("FICHIER_VIDE", "Le fichier importé est vide.", HttpStatus.BAD_REQUEST);
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BusinessException(
                    "FICHIER_TROP_VOLUMINEUX",
                    "Le fichier dépasse la taille maximale autorisée (10 Mo).",
                    HttpStatus.BAD_REQUEST
            );
        }

        String labelMetrique = null;
        List<RawPoint> rawPoints = new ArrayList<>();
        int lignesValides = 0;
        int lignesIgnorees = 0;
        int totalRows = 0;

        // 2. Parsing du CSV avec Apache Commons CSV
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.builder().setDelimiter(';').build())) {

            Iterator<CSVRecord> iterator = csvParser.iterator();

            // Vérification de la présence d'un en-tête
            if (!iterator.hasNext()) {
                throw new BusinessException("FICHIER_VIDE", "Le fichier importé est vide.", HttpStatus.BAD_REQUEST);
            }

            CSVRecord headerRecord = iterator.next();
            totalRows++;

            if (headerRecord.size() < 3) {
                throw new BusinessException(
                        "ENTETE_INVALIDE",
                        "L'en-tête du fichier CSV est invalide. Attendu: Date;Heure;<NomMetrique>",
                        HttpStatus.BAD_REQUEST
                );
            }

            // Nettoyage éventuel du BOM UTF-8 (\uFEFF) sur la première colonne/libellé
            labelMetrique = headerRecord.get(2).trim();
            if (labelMetrique.startsWith("\uFEFF")) {
                labelMetrique = labelMetrique.substring(1).trim();
            }

            if (labelMetrique.isEmpty()) {
                labelMetrique = "Métrique externe";
            }

            // Parcours des lignes de données
            while (iterator.hasNext()) {
                CSVRecord record = iterator.next();
                totalRows++;

                if (totalRows > MAX_ROW_COUNT + 1) { // +1 pour l'en-tête
                    throw new BusinessException(
                            "FICHIER_TROP_VOLUMINEUX",
                            "Le fichier dépasse le nombre maximal de lignes autorisé (200 000 lignes).",
                            HttpStatus.BAD_REQUEST
                    );
                }

                // Validation de la ligne
                if (record.size() < 3) {
                    lignesIgnorees++;
                    continue;
                }

                String dateStr = record.get(0).trim();
                String heureStr = record.get(1).trim();
                String valeurStr = record.get(2).trim();

                // Nettoyage du BOM UTF-8 si présent sur la date de la première ligne
                if (dateStr.startsWith("\uFEFF")) {
                    dateStr = dateStr.substring(1).trim();
                }

                try {
                    LocalDate date = LocalDate.parse(dateStr, DATE_FORMATTER);
                    LocalTime heure = parseLocalTime(heureStr);
                    LocalDateTime timestamp = LocalDateTime.of(date, heure);

                    // Conversion de la valeur (gestion point ou virgule)
                    String cleanValeur = valeurStr.replace(',', '.');
                    BigDecimal valeur = new BigDecimal(cleanValeur);

                    rawPoints.add(new RawPoint(timestamp, valeur));
                    lignesValides++;

                } catch (Exception e) {
                    log.debug("Ligne {} ignorée (erreur de parsing: date='{}', heure='{}', valeur='{}')",
                            totalRows, dateStr, heureStr, valeurStr);
                    lignesIgnorees++;
                }
            }

        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            log.error("Erreur lors de la lecture du fichier CSV", e);
            throw new BusinessException("ERREUR_LECTURE_FICHIER", "Erreur lors de la lecture du fichier CSV.", HttpStatus.BAD_REQUEST);
        }

        // 3. Post-verification des données valides
        if (lignesValides == 0) {
            throw new BusinessException(
                    "AUCUNE_DONNEE_VALIDE",
                    "Aucune donnée valide n'a pu être extraite du fichier.",
                    HttpStatus.BAD_REQUEST
            );
        }

        // 4. Tri chronologique des points bruts
        rawPoints.sort(Comparator.comparing(RawPoint::timestamp));

        LocalDateTime dateDebut = rawPoints.get(0).timestamp();
        LocalDateTime dateFin = rawPoints.get(rawPoints.size() - 1).timestamp();

        // 5. Détermination de la granularité automatique selon l'écart
        Granularite granularite = GranulariteUtils.determinerDepuisEcart(dateDebut, dateFin);

        // 6. Agrégation des points par bucket temporel
        List<FileAnalysisPointDTO> pointsAgreges = agregerPoints(rawPoints, granularite);

        return new FileAnalysisResponseDTO(
                labelMetrique,
                pointsAgreges,
                granularite,
                lignesValides,
                lignesIgnorees
        );
    }

    /**
     * Parsing défensif de l'heure.
     */
    private LocalTime parseLocalTime(String heureStr) {
        for (DateTimeFormatter formatter : TIME_FORMATTERS) {
            try {
                return LocalTime.parse(heureStr, formatter);
            } catch (Exception ignored) {
            }
        }
        return LocalTime.parse(heureStr);
    }

    /**
     * Regroupe les points bruts par bucket de granularité et calcule la moyenne.
     */
    private List<FileAnalysisPointDTO> agregerPoints(List<RawPoint> rawPoints, Granularite granularite) {
        Map<LocalDateTime, List<BigDecimal>> buckets = new LinkedHashMap<>();

        for (RawPoint point : rawPoints) {
            LocalDateTime bucket = calculerBucket(point.timestamp(), granularite);
            buckets.computeIfAbsent(bucket, k -> new ArrayList<>()).add(point.valeur());
        }

        List<FileAnalysisPointDTO> result = new ArrayList<>();
        for (Map.Entry<LocalDateTime, List<BigDecimal>> entry : buckets.entrySet()) {
            LocalDateTime bucketTime = entry.getKey();
            List<BigDecimal> valeurs = entry.getValue();

            BigDecimal sum = BigDecimal.ZERO;
            for (BigDecimal val : valeurs) {
                sum = sum.add(val);
            }
            BigDecimal moyenne = sum.divide(BigDecimal.valueOf(valeurs.size()), 2, RoundingMode.HALF_UP);

            result.add(new FileAnalysisPointDTO(bucketTime, moyenne));
        }

        return result;
    }

    /**
     * Calcule le début du bucket temporel selon la granularité.
     */
    private LocalDateTime calculerBucket(LocalDateTime timestamp, Granularite granularite) {
        return switch (granularite) {
            case TRENTE_MIN -> {
                int min = (timestamp.getMinute() / 30) * 30;
                yield timestamp.withMinute(min).withSecond(0).withNano(0);
            }
            case HORAIRE -> timestamp.withMinute(0).withSecond(0).withNano(0);
            case JOURNALIERE -> timestamp.withHour(0).withMinute(0).withSecond(0).withNano(0);
            case MENSUELLE -> timestamp.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        };
    }

    /** Record interne temporaire pour le stockage pré-agrégation. */
    private record RawPoint(LocalDateTime timestamp, BigDecimal valeur) {}
}
