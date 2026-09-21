package com.projet.alerting.service;

import com.projet.alerting.dto.DetailJourAlertesDTO;
import com.projet.alerting.dto.HeatmapJourDTO;
import com.projet.alerting.dto.TopAlerteDTO;
import com.projet.alerting.model.Alerte;
import com.projet.alerting.model.SeuilAbsolu;
import com.projet.alerting.model.enums.Metrique;
import com.projet.alerting.model.enums.TypeAlerte;
import com.projet.alerting.repository.AlerteRepository;
import com.projet.alerting.repository.SeuilAbsoluRepository;
import com.projet.config.BusinessException;
import com.projet.measures.model.Mesure;
import com.projet.measures.model.PointMesure;
import com.projet.measures.repository.MesureRepository;
import com.projet.measures.repository.PointMesureRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlerteStatsServiceTest {

    @Mock
    private AlerteRepository alerteRepository;

    @Mock
    private PointMesureRepository pointMesureRepository;

    @Mock
    private MesureRepository mesureRepository;

    @Mock
    private SeuilAbsoluRepository seuilAbsoluRepository;

    @InjectMocks
    private AlerteStatsService alerteStatsService;

    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;

    @BeforeEach
    void setUp() {
        dateDebut = LocalDateTime.now().minusDays(7);
        dateFin = LocalDateTime.now();
    }

    // =========================================================================
    // getTopAlertes — tests existants (renommés, corps inchangés)
    // =========================================================================

    @Test
    @DisplayName("getTopAlertes - Doit lever une exception si les dates sont invalides")
    void getTopAlertes_datesInvalides_lanceException() {
        assertThrows(BusinessException.class, () ->
            alerteStatsService.getTopAlertes(dateFin, dateDebut, null, 10)
        );
    }

    @Test
    @DisplayName("getTopAlertes - Doit retourner une liste vide si aucune alerte")
    void getTopAlertes_aucuneAlerte_retourneListeVide() {
        when(alerteRepository.findByCreatedAtBetween(dateDebut, dateFin)).thenReturn(Collections.emptyList());

        List<TopAlerteDTO> result = alerteStatsService.getTopAlertes(dateDebut, dateFin, null, 10);

        assertTrue(result.isEmpty());
        verify(mesureRepository, never()).findAllById(any());
        verify(mesureRepository, never()).findById(any());
    }

    @Test
    @DisplayName("getTopAlertes - Doit grouper par point de mesure et métrique sans problème N+1")
    void getTopAlertes_plusieursAlertes_grouperParPointEtMetriqueSansN1() {
        UUID idMesure1 = UUID.randomUUID();
        UUID idMesure2 = UUID.randomUUID();
        Long idPoint1 = 1L;
        Long idPoint2 = 2L;

        Alerte alerte1 = new Alerte();
        ReflectionTestUtils.setField(alerte1, "idMesure", idMesure1);
        ReflectionTestUtils.setField(alerte1, "metrique", Metrique.TEMPERATURE);

        Alerte alerte2 = new Alerte();
        ReflectionTestUtils.setField(alerte2, "idMesure", idMesure1);
        ReflectionTestUtils.setField(alerte2, "metrique", Metrique.TEMPERATURE);

        Alerte alerte3 = new Alerte();
        ReflectionTestUtils.setField(alerte3, "idMesure", idMesure2);
        ReflectionTestUtils.setField(alerte3, "metrique", Metrique.HUMIDITE);

        when(alerteRepository.findByCreatedAtBetween(dateDebut, dateFin))
                .thenReturn(List.of(alerte1, alerte2, alerte3));

        PointMesure point1 = new PointMesure();
        ReflectionTestUtils.setField(point1, "id", idPoint1);
        ReflectionTestUtils.setField(point1, "nom", "Zone Peinture 1");

        PointMesure point2 = new PointMesure();
        ReflectionTestUtils.setField(point2, "id", idPoint2);
        ReflectionTestUtils.setField(point2, "nom", "Zone Séchage");

        Mesure mesure1 = new Mesure();
        ReflectionTestUtils.setField(mesure1, "idMesure", idMesure1);
        ReflectionTestUtils.setField(mesure1, "pointMesure", point1);

        Mesure mesure2 = new Mesure();
        ReflectionTestUtils.setField(mesure2, "idMesure", idMesure2);
        ReflectionTestUtils.setField(mesure2, "pointMesure", point2);

        when(mesureRepository.findAllById(any())).thenReturn(List.of(mesure1, mesure2));
        when(pointMesureRepository.findById(idPoint1)).thenReturn(Optional.of(point1));
        when(pointMesureRepository.findById(idPoint2)).thenReturn(Optional.of(point2));

        List<TopAlerteDTO> result = alerteStatsService.getTopAlertes(dateDebut, dateFin, null, 10);

        assertEquals(2, result.size());
        assertEquals("Zone Peinture 1", result.get(0).getNomPointMesure());
        assertEquals(2L, result.get(0).getNombreDepassements());
        assertEquals(Metrique.TEMPERATURE, result.get(0).getMetrique());

        assertEquals("Zone Séchage", result.get(1).getNomPointMesure());
        assertEquals(1L, result.get(1).getNombreDepassements());
        assertEquals(Metrique.HUMIDITE, result.get(1).getMetrique());

        verify(mesureRepository, times(1)).findAllById(any());
        verify(mesureRepository, never()).findById(any());
    }

    // =========================================================================
    // getHeatmapMois — test existant (renommé, corps inchangé)
    // =========================================================================

    @Test
    @DisplayName("getHeatmapMois - Doit grouper les alertes SEUIL_ABSOLU par jour sans N+1")
    void getHeatmapMois_alerteSurUnJour_retourneHeatmapCorrecte() {
        UUID idMesure1 = UUID.randomUUID();
        Long idPoint1 = 1L;
        LocalDateTime timestamp = LocalDateTime.of(2026, 9, 15, 10, 30);

        Alerte alerte1 = new Alerte();
        ReflectionTestUtils.setField(alerte1, "idMesure", idMesure1);
        ReflectionTestUtils.setField(alerte1, "metrique", Metrique.TEMPERATURE);
        ReflectionTestUtils.setField(alerte1, "createdAt", timestamp);

        when(alerteRepository.findByTypeAlerteAndCreatedAtBetween(eq(TypeAlerte.SEUIL_ABSOLU), any(), any()))
                .thenReturn(List.of(alerte1));

        PointMesure point1 = new PointMesure();
        ReflectionTestUtils.setField(point1, "id", idPoint1);

        Mesure mesure1 = new Mesure();
        ReflectionTestUtils.setField(mesure1, "idMesure", idMesure1);
        ReflectionTestUtils.setField(mesure1, "pointMesure", point1);

        when(mesureRepository.findAllById(any())).thenReturn(List.of(mesure1));

        List<HeatmapJourDTO> heatmap = alerteStatsService.getHeatmapMois(2026, 9, null, null);

        assertEquals(30, heatmap.size());
        assertEquals(1L, heatmap.get(14).getNombreAlertesCritiques()); // Jour 15
        verify(mesureRepository, times(1)).findAllById(any());
        verify(mesureRepository, never()).findById(any());
    }

    // =========================================================================
    // getDetailJour — test existant (renommé, corps inchangé)
    // =========================================================================

    @Test
    @DisplayName("getDetailJour - Doit utiliser le seuil actif AU MOMENT DE L'ALERTE et éviter N+1")
    void getDetailJour_seuilHistoriqueAuMomentAlerte_utiliseSeuilCorrect() {
        LocalDate date = LocalDate.of(2026, 9, 15);
        LocalDateTime timestampAlerte = LocalDateTime.of(2026, 9, 15, 14, 20);

        UUID idMesure = UUID.randomUUID();
        Long idPoint = 1L;

        Alerte alerte = new Alerte();
        ReflectionTestUtils.setField(alerte, "idMesure", idMesure);
        ReflectionTestUtils.setField(alerte, "metrique", Metrique.TEMPERATURE);
        ReflectionTestUtils.setField(alerte, "createdAt", timestampAlerte);

        when(alerteRepository.findByTypeAlerteAndCreatedAtBetween(eq(TypeAlerte.SEUIL_ABSOLU), any(), any()))
                .thenReturn(List.of(alerte));

        PointMesure point = new PointMesure();
        ReflectionTestUtils.setField(point, "id", idPoint);
        ReflectionTestUtils.setField(point, "nom", "Cabine Peinture");

        Mesure mesure = new Mesure();
        ReflectionTestUtils.setField(mesure, "idMesure", idMesure);
        ReflectionTestUtils.setField(mesure, "pointMesure", point);
        ReflectionTestUtils.setField(mesure, "valeur", new BigDecimal("45.50"));

        when(mesureRepository.findAllById(any())).thenReturn(List.of(mesure));

        // Seuil historique actif au moment de l'alerte
        SeuilAbsolu seuilHistorique = new SeuilAbsolu();
        seuilHistorique.setValeurMin(new BigDecimal("18.00"));
        seuilHistorique.setValeurMax(new BigDecimal("25.00"));

        when(seuilAbsoluRepository.findSeuilAtTimestamp(eq(idPoint), eq(Metrique.TEMPERATURE), eq(timestampAlerte)))
                .thenReturn(List.of(seuilHistorique));

        DetailJourAlertesDTO detail = alerteStatsService.getDetailJour(date, null, null);

        assertNotNull(detail);
        assertEquals(1L, detail.getNombreTotalDepassements());
        assertEquals(1, detail.getDetails().size());

        DetailJourAlertesDTO.DetailAlerteDTO detailAlerte = detail.getDetails().get(0);
        assertEquals(new BigDecimal("45.50"), detailAlerte.getValeurMaxAtteinte());
        assertNotNull(detailAlerte.getSeuilConfigure());
        assertEquals(new BigDecimal("18.00"), detailAlerte.getSeuilConfigure().getValeurMin());
        assertEquals(new BigDecimal("25.00"), detailAlerte.getSeuilConfigure().getValeurMax());

        // Vérification qu'on recherche bien par horodatage d'alerte et pas par seuil actif présent
        verify(seuilAbsoluRepository, times(1)).findSeuilAtTimestamp(eq(idPoint), eq(Metrique.TEMPERATURE), eq(timestampAlerte));
        verify(mesureRepository, times(1)).findAllById(any());
        verify(mesureRepository, never()).findById(any());
    }

    // =========================================================================
    // getTopAlertes — nouveaux tests
    // =========================================================================

    @Test
    @DisplayName("getTopAlertes - Filtre par idPointMesure : ne retourne que les alertes de ce point")
    void getTopAlertes_filtreParPointMesure_neRetourneQueCePoint() {
        UUID idMesure1 = UUID.randomUUID();
        UUID idMesure2 = UUID.randomUUID();
        Long idPointCible = 1L;
        Long idPointAutre = 2L;

        // Alerte appartenant au point cible
        Alerte alertePoint1 = new Alerte();
        ReflectionTestUtils.setField(alertePoint1, "idMesure", idMesure1);
        ReflectionTestUtils.setField(alertePoint1, "metrique", Metrique.TEMPERATURE);

        // Alerte appartenant à un autre point — doit être filtrée
        Alerte alertePoint2 = new Alerte();
        ReflectionTestUtils.setField(alertePoint2, "idMesure", idMesure2);
        ReflectionTestUtils.setField(alertePoint2, "metrique", Metrique.HUMIDITE);

        when(alerteRepository.findByCreatedAtBetween(dateDebut, dateFin))
                .thenReturn(List.of(alertePoint1, alertePoint2));

        PointMesure pointCible = new PointMesure();
        ReflectionTestUtils.setField(pointCible, "id", idPointCible);
        ReflectionTestUtils.setField(pointCible, "nom", "Cabine Peinture");

        PointMesure pointAutre = new PointMesure();
        ReflectionTestUtils.setField(pointAutre, "id", idPointAutre);
        ReflectionTestUtils.setField(pointAutre, "nom", "Étuve Zone 1");

        Mesure mesure1 = new Mesure();
        ReflectionTestUtils.setField(mesure1, "idMesure", idMesure1);
        ReflectionTestUtils.setField(mesure1, "pointMesure", pointCible);

        Mesure mesure2 = new Mesure();
        ReflectionTestUtils.setField(mesure2, "idMesure", idMesure2);
        ReflectionTestUtils.setField(mesure2, "pointMesure", pointAutre);

        when(mesureRepository.findAllById(any())).thenReturn(List.of(mesure1, mesure2));
        when(pointMesureRepository.findById(idPointCible)).thenReturn(Optional.of(pointCible));

        List<TopAlerteDTO> result = alerteStatsService.getTopAlertes(dateDebut, dateFin, idPointCible, 10);

        assertEquals(1, result.size());
        assertEquals(idPointCible, result.get(0).getIdPointMesure());
        assertEquals(Metrique.TEMPERATURE, result.get(0).getMetrique());
        // findAllById appelé une seule fois (pas de N+1)
        verify(mesureRepository, times(1)).findAllById(any());
        verify(mesureRepository, never()).findById(any());
    }

    @Test
    @DisplayName("getTopAlertes - limit < nombre de groupes : tronque et trie par count décroissant")
    void getTopAlertes_limitInferieurAuNombreDeGroupes_tronqueEtTrieParOrdreDecroissant() {
        UUID idM1 = UUID.randomUUID();
        UUID idM2 = UUID.randomUUID();
        UUID idM3 = UUID.randomUUID();
        Long idP1 = 1L;
        Long idP2 = 2L;
        Long idP3 = 3L;

        // point1 TEMPERATURE : 3 alertes
        Alerte a1 = new Alerte();
        ReflectionTestUtils.setField(a1, "idMesure", idM1);
        ReflectionTestUtils.setField(a1, "metrique", Metrique.TEMPERATURE);
        Alerte a2 = new Alerte();
        ReflectionTestUtils.setField(a2, "idMesure", idM1);
        ReflectionTestUtils.setField(a2, "metrique", Metrique.TEMPERATURE);
        Alerte a3 = new Alerte();
        ReflectionTestUtils.setField(a3, "idMesure", idM1);
        ReflectionTestUtils.setField(a3, "metrique", Metrique.TEMPERATURE);

        // point2 HUMIDITE : 2 alertes
        Alerte a4 = new Alerte();
        ReflectionTestUtils.setField(a4, "idMesure", idM2);
        ReflectionTestUtils.setField(a4, "metrique", Metrique.HUMIDITE);
        Alerte a5 = new Alerte();
        ReflectionTestUtils.setField(a5, "idMesure", idM2);
        ReflectionTestUtils.setField(a5, "metrique", Metrique.HUMIDITE);

        // point3 TEMPERATURE : 1 alerte
        Alerte a6 = new Alerte();
        ReflectionTestUtils.setField(a6, "idMesure", idM3);
        ReflectionTestUtils.setField(a6, "metrique", Metrique.TEMPERATURE);

        when(alerteRepository.findByCreatedAtBetween(dateDebut, dateFin))
                .thenReturn(List.of(a1, a2, a3, a4, a5, a6));

        PointMesure p1 = new PointMesure();
        ReflectionTestUtils.setField(p1, "id", idP1);
        ReflectionTestUtils.setField(p1, "nom", "Point A");

        PointMesure p2 = new PointMesure();
        ReflectionTestUtils.setField(p2, "id", idP2);
        ReflectionTestUtils.setField(p2, "nom", "Point B");

        PointMesure p3 = new PointMesure();
        ReflectionTestUtils.setField(p3, "id", idP3);
        ReflectionTestUtils.setField(p3, "nom", "Point C");

        Mesure m1 = new Mesure();
        ReflectionTestUtils.setField(m1, "idMesure", idM1);
        ReflectionTestUtils.setField(m1, "pointMesure", p1);

        Mesure m2 = new Mesure();
        ReflectionTestUtils.setField(m2, "idMesure", idM2);
        ReflectionTestUtils.setField(m2, "pointMesure", p2);

        Mesure m3 = new Mesure();
        ReflectionTestUtils.setField(m3, "idMesure", idM3);
        ReflectionTestUtils.setField(m3, "pointMesure", p3);

        when(mesureRepository.findAllById(any())).thenReturn(List.of(m1, m2, m3));
        when(pointMesureRepository.findById(idP1)).thenReturn(Optional.of(p1));
        when(pointMesureRepository.findById(idP2)).thenReturn(Optional.of(p2));

        // limit = 2 : doit retourner seulement les 2 premiers (Point A avec 3, Point B avec 2)
        List<TopAlerteDTO> result = alerteStatsService.getTopAlertes(dateDebut, dateFin, null, 2);

        assertEquals(2, result.size());
        assertEquals(3L, result.get(0).getNombreDepassements());
        assertEquals(2L, result.get(1).getNombreDepassements());
        // Point C (1 alerte) est exclu
        assertTrue(result.stream().noneMatch(r -> r.getNombreDepassements() == 1L));
    }

    // =========================================================================
    // getHeatmapMois — nouveaux tests
    // =========================================================================

    @Test
    @DisplayName("getHeatmapMois - Aucune alerte : toutes les cases à zéro, taille = jours du mois")
    void getHeatmapMois_aucuneAlerte_toutesLesCasesAZero() {
        when(alerteRepository.findByTypeAlerteAndCreatedAtBetween(eq(TypeAlerte.SEUIL_ABSOLU), any(), any()))
                .thenReturn(Collections.emptyList());

        List<HeatmapJourDTO> heatmap = alerteStatsService.getHeatmapMois(2026, 9, null, null);

        assertEquals(30, heatmap.size());
        assertTrue(heatmap.stream().allMatch(j -> j.getNombreAlertesCritiques() == 0L));
        // Aucune mesure à charger quand il n'y a pas d'alertes
        verify(mesureRepository, never()).findAllById(any());
    }

    @Test
    @DisplayName("getHeatmapMois - Février année bissextile (2024) : génère exactement 29 jours")
    void getHeatmapMois_moisDeFevrierAnneeBissextile_genere29Jours() {
        when(alerteRepository.findByTypeAlerteAndCreatedAtBetween(eq(TypeAlerte.SEUIL_ABSOLU), any(), any()))
                .thenReturn(Collections.emptyList());

        List<HeatmapJourDTO> heatmap = alerteStatsService.getHeatmapMois(2024, 2, null, null);

        assertEquals(29, heatmap.size());
        assertEquals(LocalDate.of(2024, 2, 1), heatmap.get(0).getDate());
        assertEquals(LocalDate.of(2024, 2, 29), heatmap.get(28).getDate());
    }

    @Test
    @DisplayName("getHeatmapMois - Février année non bissextile (2025) : génère exactement 28 jours")
    void getHeatmapMois_moisDeFevrierAnneeNonBissextile_genere28Jours() {
        when(alerteRepository.findByTypeAlerteAndCreatedAtBetween(eq(TypeAlerte.SEUIL_ABSOLU), any(), any()))
                .thenReturn(Collections.emptyList());

        List<HeatmapJourDTO> heatmap = alerteStatsService.getHeatmapMois(2025, 2, null, null);

        assertEquals(28, heatmap.size());
        assertEquals(LocalDate.of(2025, 2, 1), heatmap.get(0).getDate());
        assertEquals(LocalDate.of(2025, 2, 28), heatmap.get(27).getDate());
    }

    @Test
    @DisplayName("getHeatmapMois - Filtre par point et métrique : ne compte que les alertes correspondantes")
    void getHeatmapMois_filtreParPointMesureEtMetrique_neCompteQueLesAlertesCorrespondantes() {
        UUID idMesureCible = UUID.randomUUID();
        UUID idMesureAutre = UUID.randomUUID();
        Long idPointCible = 10L;
        Long idPointAutre = 20L;
        LocalDateTime jour5 = LocalDateTime.of(2026, 9, 5, 8, 0);

        // Alerte cible : point 10, TEMPERATURE, jour 5
        Alerte alerteCible = new Alerte();
        ReflectionTestUtils.setField(alerteCible, "idMesure", idMesureCible);
        ReflectionTestUtils.setField(alerteCible, "metrique", Metrique.TEMPERATURE);
        ReflectionTestUtils.setField(alerteCible, "createdAt", jour5);

        // Alerte autre point, même jour — doit être filtrée
        Alerte alerteAutrePoint = new Alerte();
        ReflectionTestUtils.setField(alerteAutrePoint, "idMesure", idMesureAutre);
        ReflectionTestUtils.setField(alerteAutrePoint, "metrique", Metrique.TEMPERATURE);
        ReflectionTestUtils.setField(alerteAutrePoint, "createdAt", jour5);

        // Alerte même point, mauvaise métrique — doit être filtrée
        Alerte alerteAutreMetrique = new Alerte();
        ReflectionTestUtils.setField(alerteAutreMetrique, "idMesure", idMesureCible);
        ReflectionTestUtils.setField(alerteAutreMetrique, "metrique", Metrique.HUMIDITE);
        ReflectionTestUtils.setField(alerteAutreMetrique, "createdAt", jour5);

        when(alerteRepository.findByTypeAlerteAndCreatedAtBetween(eq(TypeAlerte.SEUIL_ABSOLU), any(), any()))
                .thenReturn(List.of(alerteCible, alerteAutrePoint, alerteAutreMetrique));

        PointMesure pointCible = new PointMesure();
        ReflectionTestUtils.setField(pointCible, "id", idPointCible);

        PointMesure pointAutre = new PointMesure();
        ReflectionTestUtils.setField(pointAutre, "id", idPointAutre);

        Mesure mesureCible = new Mesure();
        ReflectionTestUtils.setField(mesureCible, "idMesure", idMesureCible);
        ReflectionTestUtils.setField(mesureCible, "pointMesure", pointCible);

        Mesure mesureAutre = new Mesure();
        ReflectionTestUtils.setField(mesureAutre, "idMesure", idMesureAutre);
        ReflectionTestUtils.setField(mesureAutre, "pointMesure", pointAutre);

        when(mesureRepository.findAllById(any())).thenReturn(List.of(mesureCible, mesureAutre));

        List<HeatmapJourDTO> heatmap = alerteStatsService.getHeatmapMois(2026, 9, idPointCible, Metrique.TEMPERATURE);

        assertEquals(30, heatmap.size());
        // Seule l'alerte cible doit être comptée sur le jour 5 (index 4)
        assertEquals(1L, heatmap.get(4).getNombreAlertesCritiques());
        // Tous les autres jours sont à zéro
        for (int i = 0; i < 30; i++) {
            if (i != 4) {
                assertEquals(0L, heatmap.get(i).getNombreAlertesCritiques(),
                        "Jour " + (i + 1) + " devrait être à 0");
            }
        }
        verify(mesureRepository, times(1)).findAllById(any());
        verify(mesureRepository, never()).findById(any());
    }

    // =========================================================================
    // getDetailJour — nouveaux tests
    // =========================================================================

    @Test
    @DisplayName("getDetailJour - Aucune alerte : retourne liste vide et total à zéro")
    void getDetailJour_aucuneAlerte_retourneListeVideEtTotalZero() {
        LocalDate date = LocalDate.of(2026, 9, 10);

        when(alerteRepository.findByTypeAlerteAndCreatedAtBetween(eq(TypeAlerte.SEUIL_ABSOLU), any(), any()))
                .thenReturn(Collections.emptyList());

        DetailJourAlertesDTO detail = alerteStatsService.getDetailJour(date, null, null);

        assertNotNull(detail);
        assertEquals(0L, detail.getNombreTotalDepassements());
        assertTrue(detail.getDetails().isEmpty());
        // Aucune mesure à charger
        verify(mesureRepository, never()).findAllById(any());
        verify(seuilAbsoluRepository, never()).findSeuilAtTimestamp(any(), any(), any());
    }

    @Test
    @DisplayName("getDetailJour - findSeuilAtTimestamp retourne liste vide : seuilConfigure est null sans exception")
    void getDetailJour_findSeuilAtTimestampRetourneListeVide_seuilConfigureEstNullSansException() {
        LocalDate date = LocalDate.of(2026, 9, 12);
        LocalDateTime timestampAlerte = LocalDateTime.of(2026, 9, 12, 9, 0);
        UUID idMesure = UUID.randomUUID();
        Long idPoint = 5L;

        Alerte alerte = new Alerte();
        ReflectionTestUtils.setField(alerte, "idMesure", idMesure);
        ReflectionTestUtils.setField(alerte, "metrique", Metrique.HUMIDITE);
        ReflectionTestUtils.setField(alerte, "createdAt", timestampAlerte);

        when(alerteRepository.findByTypeAlerteAndCreatedAtBetween(eq(TypeAlerte.SEUIL_ABSOLU), any(), any()))
                .thenReturn(List.of(alerte));

        PointMesure point = new PointMesure();
        ReflectionTestUtils.setField(point, "id", idPoint);
        ReflectionTestUtils.setField(point, "nom", "Étuve Zone 3");

        Mesure mesure = new Mesure();
        ReflectionTestUtils.setField(mesure, "idMesure", idMesure);
        ReflectionTestUtils.setField(mesure, "pointMesure", point);
        ReflectionTestUtils.setField(mesure, "valeur", new BigDecimal("72.00"));

        when(mesureRepository.findAllById(any())).thenReturn(List.of(mesure));

        // findSeuilAtTimestamp ne trouve rien → repli sur findByPointMesureIdAndMetriqueAndActifTrue
        when(seuilAbsoluRepository.findSeuilAtTimestamp(eq(idPoint), eq(Metrique.HUMIDITE), eq(timestampAlerte)))
                .thenReturn(Collections.emptyList());
        // Le seuil actif courant n'existe pas non plus
        when(seuilAbsoluRepository.findByPointMesureIdAndMetriqueAndActifTrue(eq(idPoint), eq(Metrique.HUMIDITE)))
                .thenReturn(Optional.empty());

        // Ne doit pas lever d'exception
        DetailJourAlertesDTO detail = assertDoesNotThrow(() ->
                alerteStatsService.getDetailJour(date, null, null));

        assertNotNull(detail);
        assertEquals(1L, detail.getNombreTotalDepassements());
        assertEquals(1, detail.getDetails().size());
        // seuilConfigure doit être null (aucun seuil trouvé)
        assertNull(detail.getDetails().get(0).getSeuilConfigure());
    }

    @Test
    @DisplayName("getDetailJour - Plusieurs alertes, même jour, points et métriques différents : regroupe correctement sans N+1")
    void getDetailJour_plusieursAlertesMemeJourPointsEtMetriquesDifferents_regroupeCorrectementSansN1() {
        LocalDate date = LocalDate.of(2026, 9, 20);
        LocalDateTime ts1 = LocalDateTime.of(2026, 9, 20, 8, 0);
        LocalDateTime ts2 = LocalDateTime.of(2026, 9, 20, 10, 0);
        LocalDateTime ts3 = LocalDateTime.of(2026, 9, 20, 12, 0);

        UUID idMesure1a = UUID.randomUUID(); // point 1, TEMPERATURE, valeur 42
        UUID idMesure1b = UUID.randomUUID(); // point 1, TEMPERATURE, valeur 50 (max)
        UUID idMesure2  = UUID.randomUUID(); // point 2, HUMIDITE, valeur 80
        Long idPoint1 = 1L;
        Long idPoint2 = 2L;

        // 2 alertes TEMPERATURE sur point1
        Alerte a1 = new Alerte();
        ReflectionTestUtils.setField(a1, "idMesure", idMesure1a);
        ReflectionTestUtils.setField(a1, "metrique", Metrique.TEMPERATURE);
        ReflectionTestUtils.setField(a1, "createdAt", ts1);

        Alerte a2 = new Alerte();
        ReflectionTestUtils.setField(a2, "idMesure", idMesure1b);
        ReflectionTestUtils.setField(a2, "metrique", Metrique.TEMPERATURE);
        ReflectionTestUtils.setField(a2, "createdAt", ts2);

        // 1 alerte HUMIDITE sur point2
        Alerte a3 = new Alerte();
        ReflectionTestUtils.setField(a3, "idMesure", idMesure2);
        ReflectionTestUtils.setField(a3, "metrique", Metrique.HUMIDITE);
        ReflectionTestUtils.setField(a3, "createdAt", ts3);

        when(alerteRepository.findByTypeAlerteAndCreatedAtBetween(eq(TypeAlerte.SEUIL_ABSOLU), any(), any()))
                .thenReturn(List.of(a1, a2, a3));

        PointMesure point1 = new PointMesure();
        ReflectionTestUtils.setField(point1, "id", idPoint1);
        ReflectionTestUtils.setField(point1, "nom", "Cabine Peinture");

        PointMesure point2 = new PointMesure();
        ReflectionTestUtils.setField(point2, "id", idPoint2);
        ReflectionTestUtils.setField(point2, "nom", "Étuve Zone 1");

        Mesure m1a = new Mesure();
        ReflectionTestUtils.setField(m1a, "idMesure", idMesure1a);
        ReflectionTestUtils.setField(m1a, "pointMesure", point1);
        ReflectionTestUtils.setField(m1a, "valeur", new BigDecimal("42.00"));
        ReflectionTestUtils.setField(m1a, "createdAt", ts1);

        Mesure m1b = new Mesure();
        ReflectionTestUtils.setField(m1b, "idMesure", idMesure1b);
        ReflectionTestUtils.setField(m1b, "pointMesure", point1);
        ReflectionTestUtils.setField(m1b, "valeur", new BigDecimal("50.00"));
        ReflectionTestUtils.setField(m1b, "createdAt", ts2);

        Mesure m2 = new Mesure();
        ReflectionTestUtils.setField(m2, "idMesure", idMesure2);
        ReflectionTestUtils.setField(m2, "pointMesure", point2);
        ReflectionTestUtils.setField(m2, "valeur", new BigDecimal("80.00"));
        ReflectionTestUtils.setField(m2, "createdAt", ts3);

        when(mesureRepository.findAllById(any())).thenReturn(List.of(m1a, m1b, m2));

        SeuilAbsolu seuilTemp = new SeuilAbsolu();
        seuilTemp.setValeurMin(new BigDecimal("15.00"));
        seuilTemp.setValeurMax(new BigDecimal("30.00"));

        SeuilAbsolu seuilHum = new SeuilAbsolu();
        seuilHum.setValeurMin(new BigDecimal("40.00"));
        seuilHum.setValeurMax(new BigDecimal("70.00"));

        when(seuilAbsoluRepository.findSeuilAtTimestamp(eq(idPoint1), eq(Metrique.TEMPERATURE), eq(ts1)))
                .thenReturn(List.of(seuilTemp));
        when(seuilAbsoluRepository.findSeuilAtTimestamp(eq(idPoint2), eq(Metrique.HUMIDITE), eq(ts3)))
                .thenReturn(List.of(seuilHum));

        DetailJourAlertesDTO detail = alerteStatsService.getDetailJour(date, null, null);

        assertNotNull(detail);
        // 3 alertes en tout (2 TEMP + 1 HUM)
        assertEquals(3L, detail.getNombreTotalDepassements());
        // 2 groupes : (point1, TEMP) et (point2, HUM)
        assertEquals(2, detail.getDetails().size());

        // Retrouver le groupe TEMPERATURE/point1
        DetailJourAlertesDTO.DetailAlerteDTO groupeTemp = detail.getDetails().stream()
                .filter(d -> d.getMetrique() == Metrique.TEMPERATURE)
                .findFirst()
                .orElseThrow();
        assertEquals(2L, groupeTemp.getNombreDepassements());
        // La valeur max des deux alertes de ce groupe est 50.00
        assertEquals(new BigDecimal("50.00"), groupeTemp.getValeurMaxAtteinte());
        assertNotNull(groupeTemp.getSeuilConfigure());
        assertEquals(new BigDecimal("30.00"), groupeTemp.getSeuilConfigure().getValeurMax());

        // Retrouver le groupe HUMIDITE/point2
        DetailJourAlertesDTO.DetailAlerteDTO groupeHum = detail.getDetails().stream()
                .filter(d -> d.getMetrique() == Metrique.HUMIDITE)
                .findFirst()
                .orElseThrow();
        assertEquals(1L, groupeHum.getNombreDepassements());
        assertEquals(new BigDecimal("80.00"), groupeHum.getValeurMaxAtteinte());
        assertNotNull(groupeHum.getSeuilConfigure());
        assertEquals(new BigDecimal("70.00"), groupeHum.getSeuilConfigure().getValeurMax());

        // Une seule requête batch de mesures — pas de N+1
        verify(mesureRepository, times(1)).findAllById(any());
        verify(mesureRepository, never()).findById(any());
        // findSeuilAtTimestamp appelé exactement une fois par groupe (2 groupes distincts)
        verify(seuilAbsoluRepository, times(1))
                .findSeuilAtTimestamp(eq(idPoint1), eq(Metrique.TEMPERATURE), eq(ts1));
        verify(seuilAbsoluRepository, times(1))
                .findSeuilAtTimestamp(eq(idPoint2), eq(Metrique.HUMIDITE), eq(ts3));
    }

    // =========================================================================
    // getDetailJour — seuils historiques chevauchants
    // =========================================================================

    /**
     * Ce test vérifie uniquement que le service retient seuils.get(0). L'ordre
     * de tri réel (dateActivation DESC NULLS LAST, createdAt DESC) est garanti
     * par la requête JPQL de SeuilAbsoluRepository.findSeuilAtTimestamp et n'est
     * pas vérifiable par un test unitaire — à couvrir par un test d'intégration
     * si les tests Testcontainers sont repris.
     */
    @Test
    @DisplayName("getDetailJour - Deux seuils chevauchants : retient le premier élément de la liste (seuils.get(0))")
    void getDetailJour_seuilsHistoriquesChevauchants_retientLePremierDeLaListe() {
        LocalDate date = LocalDate.of(2026, 9, 18);
        LocalDateTime timestampAlerte = LocalDateTime.of(2026, 9, 18, 11, 0);
        UUID idMesure = UUID.randomUUID();
        Long idPoint = 7L;

        Alerte alerte = new Alerte();
        ReflectionTestUtils.setField(alerte, "idMesure", idMesure);
        ReflectionTestUtils.setField(alerte, "metrique", Metrique.TEMPERATURE);
        ReflectionTestUtils.setField(alerte, "createdAt", timestampAlerte);

        when(alerteRepository.findByTypeAlerteAndCreatedAtBetween(eq(TypeAlerte.SEUIL_ABSOLU), any(), any()))
                .thenReturn(List.of(alerte));

        PointMesure point = new PointMesure();
        ReflectionTestUtils.setField(point, "id", idPoint);
        ReflectionTestUtils.setField(point, "nom", "Cabine Nord");

        Mesure mesure = new Mesure();
        ReflectionTestUtils.setField(mesure, "idMesure", idMesure);
        ReflectionTestUtils.setField(mesure, "pointMesure", point);
        ReflectionTestUtils.setField(mesure, "valeur", new BigDecimal("38.00"));

        when(mesureRepository.findAllById(any())).thenReturn(List.of(mesure));

        // Premier seuil : le plus récent selon ORDER BY dateActivation DESC NULLS LAST, createdAt DESC
        // bornes volontairement très distinctes du second pour une assertion non ambiguë
        SeuilAbsolu seuilPlusRecent = new SeuilAbsolu();
        seuilPlusRecent.setValeurMin(new BigDecimal("10.00"));
        seuilPlusRecent.setValeurMax(new BigDecimal("28.00"));

        // Second seuil : plus ancien, ses bornes ne doivent PAS apparaître dans le résultat
        SeuilAbsolu seuilPlusAncien = new SeuilAbsolu();
        seuilPlusAncien.setValeurMin(new BigDecimal("5.00"));
        seuilPlusAncien.setValeurMax(new BigDecimal("35.00"));

        // findSeuilAtTimestamp retourne les deux seuils dans l'ordre que produirait la requête JPQL
        when(seuilAbsoluRepository.findSeuilAtTimestamp(eq(idPoint), eq(Metrique.TEMPERATURE), eq(timestampAlerte)))
                .thenReturn(List.of(seuilPlusRecent, seuilPlusAncien));

        DetailJourAlertesDTO detail = alerteStatsService.getDetailJour(date, null, null);

        assertNotNull(detail);
        assertEquals(1, detail.getDetails().size());

        DetailJourAlertesDTO.DetailAlerteDTO detailAlerte = detail.getDetails().get(0);
        assertNotNull(detailAlerte.getSeuilConfigure());

        // Le service retient seuils.get(0) → bornes du seuilPlusRecent uniquement
        assertEquals(new BigDecimal("10.00"), detailAlerte.getSeuilConfigure().getValeurMin());
        assertEquals(new BigDecimal("28.00"), detailAlerte.getSeuilConfigure().getValeurMax());

        // Confirmer que les bornes du second seuil ne sont pas retenues
        assertNotEquals(new BigDecimal("5.00"), detailAlerte.getSeuilConfigure().getValeurMin());
        assertNotEquals(new BigDecimal("35.00"), detailAlerte.getSeuilConfigure().getValeurMax());
    }
}
