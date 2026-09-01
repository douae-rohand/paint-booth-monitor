/**
 * REPORTS API – Génération, historique et téléchargement de rapports PDF
 * Consumes: POST /api/rapports, GET /api/rapports, GET /api/rapports/:id, GET /api/rapports/:id/telecharger
 */
import apiClient from '../../lib/axios';

export type TypeRapport = 'JOURNALIER' | 'HEBDOMADAIRE' | 'MENSUEL' | 'PERSONNALISE';
export type StatutGeneration = 'EN_COURS' | 'TERMINE' | 'ECHEC';

export interface PointMesureSummary {
  id: number;
  nom: string;
  typeEmplacement?: string;
}

export interface RapportPDFResponse {
  idRapport: string;
  pointMesure: PointMesureSummary;
  typeRapport: TypeRapport;
  periodeDebut: string;
  periodeFin: string;
  objetMinioStorageKey: string | null;
  nomFichier: string | null;
  tailleFichier: number | null;
  statutGeneration: StatutGeneration;
  generatedAt: string | null;
  dateRapport: string;
}

export interface RapportGenerationRequestDTO {
  idPointMesure: number;
  dateDebut: string;
  dateFin: string;
  typeRapport: TypeRapport;
}

export interface PaginatedRapportsResponse {
  content: RapportPDFResponse[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
}

/**
 * Génère un rapport PDF synchrone.
 */
export const genererRapport = async (
  data: RapportGenerationRequestDTO,
): Promise<RapportPDFResponse> => {
  const response = await apiClient.post<RapportPDFResponse>('/api/rapports', data);
  return response.data;
};

/**
 * Liste les rapports PDF de l'utilisateur connecté avec pagination.
 */
export const listerRapports = async (
  page: number = 0,
  size: number = 10,
): Promise<PaginatedRapportsResponse> => {
  const response = await apiClient.get<PaginatedRapportsResponse>('/api/rapports', {
    params: {
      page,
      size,
      sort: 'dateRapport,desc',
    },
  });
  return response.data;
};

/**
 * Télécharge le fichier PDF du rapport.
 */
export const telechargerRapport = async (
  idRapport: string,
  nomFichier: string = 'rapport.pdf',
): Promise<void> => {
  const response = await apiClient.get(`/api/rapports/${idRapport}/telecharger`, {
    responseType: 'blob',
  });

  const blob = new Blob([response.data], { type: 'application/pdf' });
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.setAttribute('download', nomFichier || 'rapport.pdf');
  document.body.appendChild(link);
  link.click();
  link.parentNode?.removeChild(link);
  window.URL.revokeObjectURL(url);
};
