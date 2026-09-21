package com.projet.alerting.service;

import com.projet.alerting.dto.SeuilDynamiqueCreateDTO;
import com.projet.alerting.dto.SeuilDynamiqueResponseDTO;
import com.projet.alerting.model.SeuilDynamique;
import com.projet.alerting.model.enums.Metrique;
import com.projet.alerting.repository.SeuilDynamiqueRepository;
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
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeuilDynamiqueServiceTest {

    @Mock
    private SeuilDynamiqueRepository seuilDynamiqueRepository;

    @Mock
    private PointMesureRepository pointMesureRepository;

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private NotificationDispatchService notificationDispatchService;

    @InjectMocks
    private SeuilDynamiqueService seuilDynamiqueService;

    @Test
    void get_seuilInexistant_lanceBusinessExceptionNonTrouve() {
        when(seuilDynamiqueRepository.findByPointMesureIdAndMetrique(1L, Metrique.TEMPERATURE))
                .thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                seuilDynamiqueService.get(1L, Metrique.TEMPERATURE)
        );

        assertEquals("SEUIL_DYNAMIQUE_NON_TROUVE", ex.getCode());
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void get_seuilExistant_retourneDTOMappe() {
        PointMesure pm = mock(PointMesure.class);
        when(pm.getId()).thenReturn(1L);
        when(pm.getNom()).thenReturn("Cabine Peinture");

        UUID seuilId = UUID.randomUUID();
        SeuilDynamique seuil = new SeuilDynamique();
        seuil.setIdSeuilDynamique(seuilId);
        seuil.setPointMesure(pm);
        seuil.setMetrique(Metrique.TEMPERATURE);
        seuil.setMargeConfiguree(new BigDecimal("2.5"));
        seuil.setValeurMinCalculee(new BigDecimal("18.0"));
        seuil.setValeurMaxCalculee(new BigDecimal("25.0"));
        seuil.setDateCalcul(LocalDateTime.now());

        when(seuilDynamiqueRepository.findByPointMesureIdAndMetrique(1L, Metrique.TEMPERATURE))
                .thenReturn(Optional.of(seuil));

        SeuilDynamiqueResponseDTO dto = seuilDynamiqueService.get(1L, Metrique.TEMPERATURE);

        assertNotNull(dto);
        assertEquals(seuilId, dto.getId());
        assertEquals(1L, dto.getIdPointMesure());
        assertEquals("Cabine Peinture", dto.getNomPointMesure());
        assertEquals(Metrique.TEMPERATURE, dto.getMetrique());
        assertEquals(new BigDecimal("2.5"), dto.getMargeConfiguree());
        assertEquals(new BigDecimal("18.0"), dto.getValeurMinCalculee());
        assertEquals(new BigDecimal("25.0"), dto.getValeurMaxCalculee());
    }

    @Test
    void creer_pointMesureInexistantOuInactif_lanceBusinessException() {
        SeuilDynamiqueCreateDTO dto = new SeuilDynamiqueCreateDTO(1L, Metrique.TEMPERATURE, new BigDecimal("2.0"));
        UUID idAdmin = UUID.randomUUID();

        when(pointMesureRepository.findByIdAndActifTrueAndDeletedAtIsNull(1L))
                .thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                seuilDynamiqueService.creer(dto, idAdmin)
        );

        assertEquals("POINT_MESURE_INACTIF", ex.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(seuilDynamiqueRepository, never()).save(any());
    }

    @Test
    void creer_pointMesureValide_appelleSaveEtRetourneDTO() {
        Long pmId = 1L;
        UUID idAdmin = UUID.randomUUID();
        Metrique metrique = Metrique.TEMPERATURE;
        BigDecimal marge = new BigDecimal("3.0");

        SeuilDynamiqueCreateDTO dto = new SeuilDynamiqueCreateDTO(pmId, metrique, marge);

        PointMesure pm = mock(PointMesure.class);
        when(pm.getId()).thenReturn(pmId);
        when(pm.getNom()).thenReturn("Point 1");

        Admin admin = new Admin();

        when(pointMesureRepository.findByIdAndActifTrueAndDeletedAtIsNull(pmId))
                .thenReturn(Optional.of(pm));
        when(seuilDynamiqueRepository.existsByPointMesureIdAndMetrique(pmId, metrique))
                .thenReturn(false);
        when(adminRepository.findById(idAdmin))
                .thenReturn(Optional.of(admin));

        UUID seuilSavedId = UUID.randomUUID();
        when(seuilDynamiqueRepository.save(any(SeuilDynamique.class)))
                .thenAnswer(invocation -> {
                    SeuilDynamique saved = invocation.getArgument(0);
                    saved.setIdSeuilDynamique(seuilSavedId);
                    return saved;
                });

        SeuilDynamiqueResponseDTO result = seuilDynamiqueService.creer(dto, idAdmin);

        assertNotNull(result);
        assertEquals(seuilSavedId, result.getId());
        assertEquals(pmId, result.getIdPointMesure());
        assertEquals("Point 1", result.getNomPointMesure());
        assertEquals(metrique, result.getMetrique());
        assertEquals(marge, result.getMargeConfiguree());

        verify(seuilDynamiqueRepository, times(1)).save(any(SeuilDynamique.class));
    }
}
