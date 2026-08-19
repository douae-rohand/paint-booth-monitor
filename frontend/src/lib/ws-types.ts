/**
 * Types WebSocket STOMP - miroir exact des DTOs gateway Java.
 * Source: com.projet.gateway.dto.{KpiMessage, MesureMessage, StatutTempsReelMessage, AlerteMessage}
 * Source: com.projet.gateway.WebSocketConfig (topics)
 *
 * Topics configurés (WebSocketConfig.java) :
 *  /topic/statut-temps-reel      → StatutTempsReelMessage (extends PointMesureStatutDTO)
 *  /topic/mesures/{id}/{metrique} → MesureMessage
 *  /topic/kpis                   → KpiMessage
 *  /topic/alertes                → AlerteMessage
 */

import type { Metrique } from '../api/alerting/seuils';
import type { MesureStatutDTO } from '../api/measures/index';

// ── KpiMessage (/topic/kpis) ────────────────────────────────────────────────────
/**
 * Miroir de KpiMessage.java.
 * Publié lors de la création ou résolution d'une alerte.
 * Contient uniquement les KPIs globaux instantanés (pas les KPIs scopés période).
 */
export interface KpiMessage {
  /** Nombre d'alertes actives (statut = ACTIVE). */
  alertesActives: number;
  /** Nombre de points de mesure en anomalie (≥ 1 alerte active). */
  nbPointsEnAnomalie: number;
}

// ── MesureMessage (/topic/mesures/{idPointMesure}/{metrique}) ───────────────────
/**
 * Miroir de MesureMessage.java.
 * Publié lors de l'insertion d'une mesure via PostgreSQL NOTIFY.
 * `valeur` : BigDecimal Java → number TS.
 * `timestamp` : format "yyyy-MM-dd'T'HH:mm:ss" (@JsonFormat Java).
 */
export interface MesureMessage {
  idPointMesure: number;
  nomPointMesure: string;
  metrique: Metrique;
  valeur: number;
  /** Format "yyyy-MM-dd'T'HH:mm:ss" */
  timestamp: string;
}

// ── StatutTempsReelMessage (/topic/statut-temps-reel) ─────────────────────────
/**
 * Miroir de StatutTempsReelMessage.java (extends PointMesureStatutDTO).
 * Publié après chaque nouvelle mesure pour le point concerné.
 * Structure identique à PointMesureStatutDTO (mêmes champs hérités).
 */
export interface StatutTempsReelMessage {
  idPointMesure: number;
  nomPointMesure: string;
  /** "CABINE" | "ETUVE" */
  typeEmplacement: string;
  /** Liste des statuts par métrique applicable. */
  mesures: MesureStatutDTO[];
}

// ── AlerteMessage (/topic/alertes) ─────────────────────────────────────────────
/**
 * Miroir de AlerteMessage.java.
 * Publié lors de la création ou de la résolution d'une alerte.
 * `evenement`   : "CREATION" ou "RESOLUTION" — ajouté lors de la correction résolution symétrique.
 * `idAlerte`    : UUID Java → string TS.
 * `typeAlerte`  : enum TypeAlerte Java.
 * `severite`    : enum Severite Java.
 * `dateCreation`: format "yyyy-MM-dd'T'HH:mm:ss" (@JsonFormat Java).
 */
export interface AlerteMessage {
  /** "CREATION" ou "RESOLUTION" */
  evenement: 'CREATION' | 'RESOLUTION';
  /** UUID de l'alerte */
  idAlerte: string;
  idPointMesure: number | null;
  nomPointMesure: string | null;
  metrique: Metrique;
  typeAlerte: 'SEUIL_ABSOLU' | 'SEUIL_DYNAMIQUE' | 'DERIVE_IA';
  severite: 'FAIBLE' | 'MOYENNE' | 'CRITIQUE';
  /** Format "yyyy-MM-dd'T'HH:mm:ss" */
  dateCreation: string;
}
