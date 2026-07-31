package com.projet.measures.dto;

import com.projet.measures.model.enums.Granularite;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO de réponse pour l'historique des mesures.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MesureHistoriqueResponseDTO {

    /**
     * Liste des points de données d'historique, triés par horodatage croissant.
     */
    private List<MesureHistoriqueDTO> points;

    /**
     * Seuil absolu actif pour ce point de mesure et cette métrique.
     * Null si aucun seuil n'est configuré.
     */
    private SeuilAbsoluDTO seuilAbsolu;

    /**
     * Granularité appliquée pour l'agrégation des données.
     * Permet au frontend de connaître le format de l'axe des abscisses.
     */
    private Granularite granulariteAppliquee;

    /**
     * DTO imbriqué pour le seuil absolu.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeuilAbsoluDTO {
        private BigDecimal valeurMin;
        private BigDecimal valeurMax;
    }
}
