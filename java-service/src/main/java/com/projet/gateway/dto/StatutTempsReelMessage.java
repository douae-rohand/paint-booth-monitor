package com.projet.gateway.dto;

import com.projet.measures.dto.PointMesureStatutDTO;

import java.util.List;

/**
 * Message WebSocket pour le statut temps réel d'un point de mesure.
 * Publié sur /topic/statut-temps-reel lors de l'insertion d'une mesure.
 * Hérite de PointMesureStatutDTO (idPointMesure, nomPointMesure, typeEmplacement, mesures).
 */
public class StatutTempsReelMessage extends PointMesureStatutDTO {

    public StatutTempsReelMessage() {
        super();
    }

    public StatutTempsReelMessage(Long idPointMesure, String nomPointMesure, String typeEmplacement, List<MesureStatutDTO> mesures) {
        super(idPointMesure, nomPointMesure, typeEmplacement, mesures);
    }
}
