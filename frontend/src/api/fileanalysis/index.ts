/**
 * FILE ANALYSIS API – Import et analyse de fichiers CSV externes de mesures
 * Consumes: POST /api/analyse-fichier
 */
import apiClient from '../../lib/axios';

export type Granularite = 'TRENTE_MIN' | 'HORAIRE' | 'JOURNALIERE' | 'MENSUELLE';

export interface FileAnalysisPointDTO {
  horodatage: string;
  valeur: number;
}

export interface FileAnalysisResponseDTO {
  labelMetrique: string;
  points: FileAnalysisPointDTO[];
  granularite: Granularite;
  lignesValides: number;
  lignesIgnorees: number;
}

/**
 * Envoie un fichier CSV au backend pour analyse et agrégation temporalisée.
 * 
 * @param file Fichier CSV sélectionné par l'utilisateur
 * @returns Données de métrique, liste des points agrégés et statistiques d'importation
 */
export const analyserFichier = async (file: File): Promise<FileAnalysisResponseDTO> => {
  const formData = new FormData();
  formData.append('file', file);

  const response = await apiClient.post<FileAnalysisResponseDTO>('/api/analyse-fichier', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });

  return response.data;
};
