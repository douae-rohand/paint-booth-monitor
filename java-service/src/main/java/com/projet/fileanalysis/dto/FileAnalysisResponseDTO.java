package com.projet.fileanalysis.dto;

import com.projet.config.model.enums.Granularite;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO de réponse pour l'analyse et l'agrégation d'un fichier CSV externe.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileAnalysisResponseDTO {

    /** Label de la métrique extrait de l'en-tête CSV (ex. "Humidité", "Température"). */
    private String labelMetrique;

    /** Liste des points de mesures agrégés par la granularité calculée. */
    private List<FileAnalysisPointDTO> points;

    /** Granularité appliquée automatiquement selon l'étendue des dates du fichier. */
    private Granularite granularite;

    /** Nombre de lignes CSV valides traitées. */
    private int lignesValides;

    /** Nombre de lignes CSV malformées ignorées. */
    private int lignesIgnorees;
}
