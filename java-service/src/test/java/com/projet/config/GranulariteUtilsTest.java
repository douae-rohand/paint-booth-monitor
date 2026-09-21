package com.projet.config;

import com.projet.config.model.enums.Granularite;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GranulariteUtilsTest {

    // ── Tests determinerDepuisEcart ──────────────────────────────────────────

    @Test
    void determinerDepuisEcart_null_retourneJournaliereParDefaut() {
        LocalDateTime now = LocalDateTime.now();
        assertEquals(Granularite.JOURNALIERE, GranulariteUtils.determinerDepuisEcart(null, now));
        assertEquals(Granularite.JOURNALIERE, GranulariteUtils.determinerDepuisEcart(now, null));
    }

    @Test
    void determinerDepuisEcart_datesIdentiques_retourneTrenteMin() {
        LocalDateTime now = LocalDateTime.now();
        assertEquals(Granularite.TRENTE_MIN, GranulariteUtils.determinerDepuisEcart(now, now));
    }

    @Test
    void determinerDepuisEcart_dateDebutApresDateFin_retourneTrenteMinSansException() {
        // Note: La validation d'ordre des dates est de la responsabilité de l'appelant
        // (MetierValidation.validerPlageDates en amont), cette classe utilitaire ne la refait pas.
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime debutApresFin = now.plusDays(5);
        assertEquals(Granularite.TRENTE_MIN, GranulariteUtils.determinerDepuisEcart(debutApresFin, now));
    }

    @Test
    void determinerDepuisEcart_ecartSousUnJour_retourneTrenteMin() {
        LocalDateTime now = LocalDateTime.now();
        assertEquals(Granularite.TRENTE_MIN, GranulariteUtils.determinerDepuisEcart(now, now.plusHours(5)));
    }

    @Test
    void determinerDepuisEcart_ecartExactementUnJour_retourneTrenteMin() {
        LocalDateTime now = LocalDateTime.now();
        assertEquals(Granularite.TRENTE_MIN, GranulariteUtils.determinerDepuisEcart(now, now.plusDays(1)));
    }

    @Test
    void determinerDepuisEcart_ecartJusteApresUnJour_retourneHoraire() {
        LocalDateTime now = LocalDateTime.now();
        assertEquals(Granularite.HORAIRE, GranulariteUtils.determinerDepuisEcart(now, now.plusDays(2)));
    }

    @Test
    void determinerDepuisEcart_ecartExactementSeptJours_retourneHoraire() {
        LocalDateTime now = LocalDateTime.now();
        assertEquals(Granularite.HORAIRE, GranulariteUtils.determinerDepuisEcart(now, now.plusDays(7)));
    }

    @Test
    void determinerDepuisEcart_ecartHuitJours_retourneJournaliere() {
        LocalDateTime now = LocalDateTime.now();
        assertEquals(Granularite.JOURNALIERE, GranulariteUtils.determinerDepuisEcart(now, now.plusDays(8)));
    }

    @Test
    void determinerDepuisEcart_ecartExactementTrenteEtUnJours_retourneJournaliere() {
        LocalDateTime now = LocalDateTime.now();
        assertEquals(Granularite.JOURNALIERE, GranulariteUtils.determinerDepuisEcart(now, now.plusDays(31)));
    }

    @Test
    void determinerDepuisEcart_ecartTrenteDeuxJours_retourneMensuelle() {
        LocalDateTime now = LocalDateTime.now();
        assertEquals(Granularite.MENSUELLE, GranulariteUtils.determinerDepuisEcart(now, now.plusDays(32)));
    }

    @Test
    void determinerDepuisEcart_ecartPlusieursMois_retourneMensuelle() {
        LocalDateTime now = LocalDateTime.now();
        assertEquals(Granularite.MENSUELLE, GranulariteUtils.determinerDepuisEcart(now, now.plusMonths(6)));
    }

    // ── Tests determinerDepuisPeriode ────────────────────────────────────────

    @Test
    void determinerDepuisPeriode_periode24h_retourneTrenteMin() {
        assertEquals(Granularite.TRENTE_MIN, GranulariteUtils.determinerDepuisPeriode("24h", null, null, null));
    }

    @Test
    void determinerDepuisPeriode_periode7jSansGranulariteDemandee_retourneHoraire() {
        assertEquals(Granularite.HORAIRE, GranulariteUtils.determinerDepuisPeriode("7j", null, null, null));
    }

    @Test
    void determinerDepuisPeriode_periode7jAvecGranulariteDemandeeHoraire_respecteLeChoix() {
        assertEquals(Granularite.HORAIRE, GranulariteUtils.determinerDepuisPeriode("7j", null, null, Granularite.HORAIRE));
    }

    @Test
    void determinerDepuisPeriode_periode7jAvecGranulariteDemandeeJournaliere_respecteLeChoix() {
        assertEquals(Granularite.JOURNALIERE, GranulariteUtils.determinerDepuisPeriode("7j", null, null, Granularite.JOURNALIERE));
    }

    @Test
    void determinerDepuisPeriode_periode7jAvecGranulariteDemandeeTrenteMin_ignoreEtRetourneHoraire() {
        assertEquals(Granularite.HORAIRE, GranulariteUtils.determinerDepuisPeriode("7j", null, null, Granularite.TRENTE_MIN));
    }

    @Test
    void determinerDepuisPeriode_periode7jAvecGranulariteDemandeeMensuelle_ignoreEtRetourneHoraire() {
        assertEquals(Granularite.HORAIRE, GranulariteUtils.determinerDepuisPeriode("7j", null, null, Granularite.MENSUELLE));
    }

    @Test
    void determinerDepuisPeriode_periode30j_retourneJournaliere() {
        assertEquals(Granularite.JOURNALIERE, GranulariteUtils.determinerDepuisPeriode("30j", null, null, null));
    }

    @Test
    void determinerDepuisPeriode_periode6mois_retourneMensuelle() {
        assertEquals(Granularite.MENSUELLE, GranulariteUtils.determinerDepuisPeriode("6mois", null, null, null));
    }

    @Test
    void determinerDepuisPeriode_periode1an_retourneMensuelle() {
        assertEquals(Granularite.MENSUELLE, GranulariteUtils.determinerDepuisPeriode("1an", null, null, null));
    }

    @Test
    void determinerDepuisPeriode_periodePersonnalise_delegueADeterminerDepuisEcart() {
        LocalDateTime now = LocalDateTime.now();
        assertEquals(Granularite.JOURNALIERE, GranulariteUtils.determinerDepuisPeriode("personnalise", now, now.plusDays(10), null));
    }

    @Test
    void determinerDepuisPeriode_periodeNull_delegueADeterminerDepuisEcart() {
        LocalDateTime now = LocalDateTime.now();
        assertEquals(Granularite.JOURNALIERE, GranulariteUtils.determinerDepuisPeriode(null, now, now.plusDays(10), null));
    }

    @Test
    void determinerDepuisPeriode_periodeInconnue_retourneJournaliereParDefaut() {
        assertEquals(Granularite.JOURNALIERE, GranulariteUtils.determinerDepuisPeriode("valeur_qui_nexiste_pas", null, null, null));
    }
}
