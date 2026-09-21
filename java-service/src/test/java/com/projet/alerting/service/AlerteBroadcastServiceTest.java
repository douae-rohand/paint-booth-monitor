package com.projet.alerting.service;

import com.projet.alerting.model.Alerte;
import com.projet.alerting.model.enums.Metrique;
import com.projet.alerting.model.enums.Severite;
import com.projet.alerting.model.enums.TypeAlerte;
import com.projet.gateway.dto.AlerteMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AlerteBroadcastServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private AlerteBroadcastService alerteBroadcastService;

    @Captor
    private ArgumentCaptor<AlerteMessage> messageCaptor;

    @Test
    void publierAlerte_evenementCreation_utiliseCreatedAtCommeTimestamp() {
        UUID idAlerte = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now().minusHours(2);
        LocalDateTime updatedAt = LocalDateTime.now();

        Alerte alerte = new Alerte();
        alerte.setIdAlerte(idAlerte);
        alerte.setMetrique(Metrique.TEMPERATURE);
        alerte.setTypeAlerte(TypeAlerte.SEUIL_ABSOLU);
        alerte.setSeverite(Severite.CRITIQUE);
        alerte.setCreatedAt(createdAt);
        alerte.setUpdatedAt(updatedAt);

        Long idPointMesure = 10L;
        String nomPointMesure = "Cabine 1";

        alerteBroadcastService.publierAlerte(alerte, "CREATION", idPointMesure, nomPointMesure);

        verify(messagingTemplate).convertAndSend(eq("/topic/alertes"), messageCaptor.capture());

        AlerteMessage msg = messageCaptor.getValue();
        assertNotNull(msg);
        assertEquals("CREATION", msg.getEvenement());
        assertEquals(idAlerte, msg.getIdAlerte());
        assertEquals(idPointMesure, msg.getIdPointMesure());
        assertEquals(nomPointMesure, msg.getNomPointMesure());
        assertEquals(Metrique.TEMPERATURE, msg.getMetrique());
        assertEquals(TypeAlerte.SEUIL_ABSOLU, msg.getTypeAlerte());
        assertEquals(Severite.CRITIQUE, msg.getSeverite());
        assertEquals(createdAt, msg.getDateCreation());
    }

    @Test
    void publierAlerte_evenementResolution_utiliseUpdatedAtCommeTimestamp() {
        UUID idAlerte = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now().minusHours(2);
        LocalDateTime updatedAt = LocalDateTime.now();

        Alerte alerte = new Alerte();
        alerte.setIdAlerte(idAlerte);
        alerte.setMetrique(Metrique.HUMIDITE);
        alerte.setTypeAlerte(TypeAlerte.SEUIL_DYNAMIQUE);
        alerte.setSeverite(Severite.MOYENNE);
        alerte.setCreatedAt(createdAt);
        alerte.setUpdatedAt(updatedAt);

        Long idPointMesure = 5L;
        String nomPointMesure = "Zone Etuve 2";

        alerteBroadcastService.publierAlerte(alerte, "RESOLUTION", idPointMesure, nomPointMesure);

        verify(messagingTemplate).convertAndSend(eq("/topic/alertes"), messageCaptor.capture());

        AlerteMessage msg = messageCaptor.getValue();
        assertNotNull(msg);
        assertEquals("RESOLUTION", msg.getEvenement());
        assertEquals(updatedAt, msg.getDateCreation());
    }

    @Test
    void publierAlerte_toujours_envoieSurTopicAlertes() {
        Alerte alerte = new Alerte();
        alerte.setIdAlerte(UUID.randomUUID());
        alerte.setMetrique(Metrique.TEMPERATURE);
        alerte.setTypeAlerte(TypeAlerte.DERIVE_IA);
        alerte.setSeverite(Severite.FAIBLE);
        alerte.setCreatedAt(LocalDateTime.now());

        alerteBroadcastService.publierAlerte(alerte, "CREATION", 1L, "Test Point");

        verify(messagingTemplate).convertAndSend(eq("/topic/alertes"), messageCaptor.capture());
        assertEquals("/topic/alertes", "/topic/alertes");
    }
}
