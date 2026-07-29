package com.projet.measures.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.projet.alerting.model.enums.Metrique;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO pour le statut temps réel d'un point de mesure.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PointMesureStatutDTO {

    /**
     * ID du point de mesure.
     */
    private Long idPointMesure;

    /**
     * Nom du point de mesure.
     */
    private String nomPointMesure;

    /**
     * Type d'emplacement (CABINE ou ETUVE).
     */
    private String typeEmplacement;

    /**
     * Liste des mesures avec leur statut pour chaque métrique applicable.
     */
    private List<MesureStatutDTO> mesures;

    /**
     * DTO imbriqué pour le statut d'une mesure.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MesureStatutDTO {
        private Metrique metrique;
        private BigDecimal derniereValeur;
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime dateDerniereMesure;
        private StatutMesure statut;
    }

    /**
     * Enum pour le statut d'une mesure.
     */
    public enum StatutMesure {
        CRITIQUE,
        ATTENTION,
        NOMINAL,
        INCONNU
    }
}
