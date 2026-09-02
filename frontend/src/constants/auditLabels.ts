/**
 * Table de correspondance ActionAudit → libellé français + catégorie + couleur badge.
 * Source unique — ne pas dupliquer dans les composants.
 */
import type { ActionAudit } from '@/api/audit';

// ── Catégories ─────────────────────────────────────────────────────────────────

export type AuditCategorie =
  | 'authentification'
  | 'superviseurs'
  | 'configuration'
  | 'donnees';

export interface AuditActionMeta {
  label: string;
  categorie: AuditCategorie;
}

// ── Libellés et catégories ─────────────────────────────────────────────────────

export const AUDIT_ACTION_META: Record<ActionAudit, AuditActionMeta> = {
  CONNEXION:                      { label: 'Connexion',                    categorie: 'authentification' },
  DECONNEXION:                    { label: 'Déconnexion',                   categorie: 'authentification' },
  TENTATIVE_CONNEXION_ECHOUEE:    { label: 'Tentative de connexion échouée', categorie: 'authentification' },
  CREATION_SUPERVISEUR:           { label: 'Création superviseur',           categorie: 'superviseurs' },
  COMPTE_ACTIVE_SUPERVISEUR:      { label: 'Superviseur a activé son compte', categorie: 'superviseurs' },
  ACTIVATION_SUPERVISEUR:         { label: 'Réactivation superviseur',        categorie: 'superviseurs' },
  MODIFICATION_SUPERVISEUR:       { label: 'Modification superviseur',        categorie: 'superviseurs' },
  DESACTIVATION_SUPERVISEUR:      { label: 'Désactivation superviseur',       categorie: 'superviseurs' },
  EXPORT_MESURES:                 { label: 'Export mesures',                  categorie: 'donnees' },
  GENERER_RAPPORT:                { label: 'Génération rapport',              categorie: 'donnees' },
  TELECHARGEMENT_RAPPORT:         { label: 'Téléchargement rapport',          categorie: 'donnees' },
  MODIFICATION_CONFIGURATION_PLC:    { label: 'Configuration PLC modifiée',   categorie: 'configuration' },
  MODIFICATION_CONFIGURATION_SEUILS: { label: 'Configuration seuils modifiée', categorie: 'configuration' },
};

// ── Groupes de catégories pour les filtres ─────────────────────────────────────

export interface AuditCategorieGroupe {
  label: string;
  actions: ActionAudit[];
}

export const AUDIT_CATEGORIE_GROUPES: AuditCategorieGroupe[] = [
  {
    label: 'Authentification',
    actions: ['CONNEXION', 'DECONNEXION', 'TENTATIVE_CONNEXION_ECHOUEE'],
  },
  {
    label: 'Gestion superviseurs',
    actions: [
      'CREATION_SUPERVISEUR',
      'COMPTE_ACTIVE_SUPERVISEUR',
      'ACTIVATION_SUPERVISEUR',
      'MODIFICATION_SUPERVISEUR',
      'DESACTIVATION_SUPERVISEUR',
    ],
  },
  {
    label: 'Configuration',
    actions: ['MODIFICATION_CONFIGURATION_PLC', 'MODIFICATION_CONFIGURATION_SEUILS'],
  },
  {
    label: 'Données',
    actions: ['EXPORT_MESURES', 'GENERER_RAPPORT', 'TELECHARGEMENT_RAPPORT'],
  },
];

// ── Couleurs par catégorie (classes Tailwind) ──────────────────────────────────
// Palette limitée à 4 couleurs cohérentes avec le design system existant.

export const AUDIT_CATEGORIE_COLORS: Record<AuditCategorie, string> = {
  authentification: 'bg-blue-50 text-blue-700 border-blue-200',
  superviseurs:     'bg-amber-50 text-amber-700 border-amber-200',
  configuration:    'bg-violet-50 text-violet-700 border-violet-200',
  donnees:          'bg-emerald-50 text-emerald-700 border-emerald-200',
};
