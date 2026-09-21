package com.projet.alerting.service;

import com.projet.alerting.dto.SeuilAbsoluCreateDTO;
import com.projet.alerting.dto.SeuilAbsoluResponseDTO;
import com.projet.alerting.model.SeuilAbsolu;
import com.projet.alerting.model.enums.Metrique;
import com.projet.alerting.repository.SeuilAbsoluRepository;
import com.projet.auth.model.Admin;
import com.projet.auth.repository.AdminRepository;
import com.projet.config.BusinessException;
import com.projet.measures.model.PointMesure;
import com.projet.measures.repository.PointMesureRepository;
import com.projet.notifications.service.NotificationDispatchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeuilAbsoluServiceTest {

    @Mock
    private SeuilAbsoluRepository seuilAbsoluRepository;

    @Mock
    private PointMesureRepository pointMesureRepository;

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private NotificationDispatchService notificationDispatchService;

    @InjectMocks
    private SeuilAbsoluService seuilAbsoluService;

    // ── creer ──────────────────────────────────────────────────────────────────

    @Test
    void creer_pointMesureInexistantOuInactif_lanceBusinessException() {
        // Arrange
        SeuilAbsoluCreateDTO dto = new SeuilAbsoluCreateDTO(
                1L,
                Metrique.TEMPERATURE,
                new BigDecimal("15.0"),
                new BigDecimal("25.0")
        );
        UUID idAdmin = UUID.randomUUID();

        when(pointMesureRepository.findByIdAndActifTrueAndDeletedAtIsNull(1L))
                .thenReturn(Optional.empty());

        // Act / Assert
        BusinessException ex = assertThrows(BusinessException.class, () ->
                seuilAbsoluService.creer(dto, idAdmin)
        );

        assertEquals("POINT_MESURE_INACTIF", ex.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());

        // save ne doit jamais être appelé
        verify(seuilAbsoluRepository, never()).save(any());
    }

    @Test
    void creer_pointMesureValide_appelleSaveEtRetourneDTO() {
        // Arrange
        Long pmId = 2L;
        UUID idAdmin = UUID.randomUUID();
        Metrique metrique = Metrique.TEMPERATURE;
        BigDecimal valeurMin = new BigDecimal("10.0");
        BigDecimal valeurMax = new BigDecimal("30.0");

        SeuilAbsoluCreateDTO dto = new SeuilAbsoluCreateDTO(pmId, metrique, valeurMin, valeurMax);

        PointMesure pm = mock(PointMesure.class);
        when(pm.getId()).thenReturn(pmId);
        when(pm.getNom()).thenReturn("Cabine Peinture");

        Admin admin = new Admin();

        when(pointMesureRepository.findByIdAndActifTrueAndDeletedAtIsNull(pmId))
                .thenReturn(Optional.of(pm));
        when(adminRepository.findById(idAdmin))
                .thenReturn(Optional.of(admin));
        // Aucun ancien seuil actif pour ce point/métrique
        when(seuilAbsoluRepository.findByPointMesureIdAndMetriqueAndActifTrue(pmId, metrique))
                .thenReturn(Optional.empty());

        UUID savedId = UUID.randomUUID();
        when(seuilAbsoluRepository.save(any(SeuilAbsolu.class)))
                .thenAnswer(invocation -> {
                    SeuilAbsolu saved = invocation.getArgument(0);
                    saved.setIdSeuilAbsolu(savedId);
                    return saved;
                });

        // Act
        SeuilAbsoluResponseDTO result = seuilAbsoluService.creer(dto, idAdmin);

        // Assert
        assertNotNull(result);
        assertEquals(savedId, result.getId());
        assertEquals(pmId, result.getIdPointMesure());
        assertEquals("Cabine Peinture", result.getNomPointMesure());
        assertEquals(metrique, result.getMetrique());
        assertEquals(valeurMin, result.getValeurMin());
        assertEquals(valeurMax, result.getValeurMax());
        assertTrue(result.isActif());

        verify(seuilAbsoluRepository, times(1)).save(any(SeuilAbsolu.class));
    }
}
