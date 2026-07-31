package com.projet.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Message WebSocket pour les KPIs globaux.
 * Publié sur /topic/kpis lors de la création ou résolution d'une alerte.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KpiMessage {

    /**
     * Nombre d'alertes actives (statut = ACTIVE).
     */
    private Long alertesActives;

    /**
     * Nombre de points de mesure en anomalie (ayant au moins une alerte active).
     */
    private Long nbPointsEnAnomalie;
}
