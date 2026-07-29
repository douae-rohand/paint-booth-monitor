package com.projet.alerting.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.projet.alerting.model.enums.Metrique;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO pour le détail des alertes d'un jour donné.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DetailJourAlertesDTO {

    /**
     * Date du jour (format YYYY-MM-DD).
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    /**
     * Nombre total de dépassements ce jour.
     */
    private Long nombreTotalDepassements;

    /**
     * Liste détaillée des dépassements par point de mesure et métrique.
     */
    private List<DetailAlerteDTO> details;

    /**
     * DTO imbriqué pour le détail d'une alerte.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetailAlerteDTO {
        private Long idPointMesure;
        private String nomPointMesure;
        private Metrique metrique;
        private Long nombreDepassements;
        private BigDecimal valeurMaxAtteinte;
        private SeuilConfigureDTO seuilConfigure;
    }

    /**
     * DTO imbriqué pour le seuil configuré.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeuilConfigureDTO {
        private BigDecimal valeurMin;
        private BigDecimal valeurMax;
    }
}
