package com.projet.gateway.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.projet.alerting.model.enums.Metrique;
import com.projet.alerting.model.enums.Severite;
import com.projet.alerting.model.enums.TypeAlerte;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Message WebSocket pour une alerte (création ou résolution).
 * Publié sur /topic/alertes lors de la création ou de la résolution d'une alerte.
 *
 * Le champ `evenement` permet au frontend de distinguer les deux cas sans
 * inspecter d'autres champs. AppShell et ActiveAlertsBand appellent
 * fetchAlertesActives() sur tout message reçu, quelle que soit la valeur
 * de ce champ — aucune modification frontend n'est nécessaire.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlerteMessage {

    /** "CREATION" ou "RESOLUTION" */
    private String evenement;

    private UUID idAlerte;
    private Long idPointMesure;
    private String nomPointMesure;
    private Metrique metrique;
    private TypeAlerte typeAlerte;
    private Severite severite;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dateCreation;
}
