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
 * Message WebSocket pour une nouvelle alerte.
 * Publié sur /topic/alertes lors de la création d'une alerte.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlerteMessage {

    private UUID idAlerte;
    private Long idPointMesure;
    private String nomPointMesure;
    private Metrique metrique;
    private TypeAlerte typeAlerte;
    private Severite severite;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dateCreation;
}
