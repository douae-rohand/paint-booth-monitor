/**
 * AUDIT API — Consultation des logs d'audit
 * Endpoint : GET /api/admin/audit
 * Réservé ADMIN — protégé côté backend par @PreAuthorize("hasRole('ADMIN')")
 */
import apiClient from '../../lib/axios';

// ── Types (miroir exact de LogAuditResponseDTO.java + ActionAudit.java) ───────

export type ActionAudit =
  | 'CONNEXION'
  | 'DECONNEXION'
  | 'TENTATIVE_CONNEXION_ECHOUEE'
  | 'CREATION_SUPERVISEUR'
  | 'COMPTE_ACTIVE_SUPERVISEUR'
  | 'ACTIVATION_SUPERVISEUR'
  | 'MODIFICATION_SUPERVISEUR'
  | 'DESACTIVATION_SUPERVISEUR'
  | 'EXPORT_MESURES'
  | 'GENERER_RAPPORT'
  | 'TELECHARGEMENT_RAPPORT'
  | 'MODIFICATION_CONFIGURATION_PLC'
  | 'MODIFICATION_CONFIGURATION_SEUILS';

export interface LogAuditDTO {
  idLog: string;
  idSuperviseur: string | null;
  nomSuperviseur: string | null;
  prenomSuperviseur: string | null;
  emailSuperviseur: string | null;
  action: ActionAudit;
  dateAction: string; // LocalDateTime → ISO string
}

export interface AuditPageResponse {
  content: LogAuditDTO[];
  page: {
    totalElements: number;
    totalPages: number;
    size: number;
    number: number;
  };
}

export interface AuditParams {
  idSuperviseur?: string;
  actions?: ActionAudit[];
  dateDebut?: string;
  dateFin?: string;
  page?: number;
  size?: number;
}

// ── Fonctions API ──────────────────────────────────────────────────────────────

/**
 * GET /api/admin/audit
 * Retourne une page de logs d'audit avec filtres optionnels.
 * Le paramètre actions est sérialisé en multi-valeur par Axios : ?actions=X&actions=Y
 */
export const getLogs = async (params?: AuditParams): Promise<AuditPageResponse> => {
  const response = await apiClient.get<AuditPageResponse>('/api/admin/audit', {
    params,
    // Axios sérialise les tableaux en ?actions=X&actions=Y par défaut
    paramsSerializer: { indexes: null },
  });
  return response.data;
};
