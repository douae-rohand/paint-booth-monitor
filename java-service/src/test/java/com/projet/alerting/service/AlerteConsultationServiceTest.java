package com.projet.alerting.service;

import com.projet.alerting.dto.AlerteDTO;
import com.projet.alerting.repository.AlerteRepository;
import com.projet.config.BusinessException;
import com.projet.measures.repository.MesureRepository;
import com.projet.measures.repository.PointMesureRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlerteConsultationServiceTest {

    @Mock
    private AlerteRepository alerteRepository;

    @Mock
    private MesureRepository mesureRepository;

    @Mock
    private PointMesureRepository pointMesureRepository;

    @InjectMocks
    private AlerteConsultationService alerteConsultationService;

    private final Pageable pageable = PageRequest.of(0, 10);

    @Test
    void getHistoriqueAlertes_statutInvalide_lanceBusinessExceptionFiltreInvalide() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                alerteConsultationService.getHistoriqueAlertes(
                        "VALEUR_INEXISTANTE", null, null, null, null, null, pageable
                )
        );

        assertEquals("FILTRE_ALERTE_INVALIDE", ex.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("statut"));
    }

    @Test
    void getHistoriqueAlertes_typeAlerteInvalide_lanceBusinessExceptionFiltreInvalide() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                alerteConsultationService.getHistoriqueAlertes(
                        null, "VALEUR_INEXISTANTE", null, null, null, null, pageable
                )
        );

        assertEquals("FILTRE_ALERTE_INVALIDE", ex.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("typeAlerte"));
    }

    @Test
    void getHistoriqueAlertes_severiteInvalide_lanceBusinessExceptionFiltreInvalide() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                alerteConsultationService.getHistoriqueAlertes(
                        null, null, "VALEUR_INEXISTANTE", null, null, null, pageable
                )
        );

        assertEquals("FILTRE_ALERTE_INVALIDE", ex.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("severite"));
    }

    @Test
    void getHistoriqueAlertes_filtresValides_appelleRepositoryAvecEnumsCorrects() {
        when(alerteRepository.findAlertesNative(eq("ACTIVE"), eq("SEUIL_ABSOLU"), eq("CRITIQUE"), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of());
        when(alerteRepository.countAlertesNative(eq("ACTIVE"), eq("SEUIL_ABSOLU"), eq("CRITIQUE"), any(), any(), any()))
                .thenReturn(0L);

        Page<AlerteDTO> page = alerteConsultationService.getHistoriqueAlertes(
                "ACTIVE", "SEUIL_ABSOLU", "CRITIQUE", null, null, null, pageable
        );

        assertNotNull(page);
        verify(alerteRepository, times(1)).findAlertesNative(
                eq("ACTIVE"), eq("SEUIL_ABSOLU"), eq("CRITIQUE"), any(), any(), any(), eq(10), eq(0)
        );
    }

    @Test
    void getHistoriqueAlertes_filtresNull_neLancePasException() {
        when(alerteRepository.findAlertesNative(isNull(), isNull(), isNull(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of());
        when(alerteRepository.countAlertesNative(isNull(), isNull(), isNull(), any(), any(), any()))
                .thenReturn(0L);

        Page<AlerteDTO> page = alerteConsultationService.getHistoriqueAlertes(
                null, null, null, null, null, null, pageable
        );

        assertNotNull(page);
        verify(alerteRepository, times(1)).findAlertesNative(
                isNull(), isNull(), isNull(), any(), any(), any(), eq(10), eq(0)
        );
    }
}
