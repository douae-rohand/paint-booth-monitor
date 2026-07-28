import { useState } from 'react';
import { Loader2, AlertTriangle, Edit, Plus, RefreshCw } from 'lucide-react';
import {
  useSeuilDynamique,
  useCreateSeuilDynamique,
  useUpdateSeuilDynamique,
} from '@/hooks/useSeuils';
import {
  type PointMesure,
  type Metrique,
  type SeuilDynamiqueResponseDTO,
  type SeuilDynamiqueCreateDTO,
  type SeuilDynamiqueUpdateDTO,
} from '@/api/alerting/seuils';

interface SeuilDynamiqueSectionProps {
  pointMesure: PointMesure;
  metrique: Metrique;
  onRefresh: () => void;
}

function formatDate(iso: string | null) {
  if (!iso) return '-';
  return new Date(iso).toLocaleString('fr-FR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

function extractErrorMessage(err: unknown, fallback: string): string {
  if (err && typeof err === 'object' && 'response' in err) {
    const res = (err as { response?: { data?: { message?: string } } }).response;
    if (res?.data?.message) return res.data.message;
  }
  return fallback;
}

function ErrorBanner({ message }: { message: string }) {
  return (
    <div className="flex items-center gap-3 rounded-2xl border border-[color:var(--danger)]/30 bg-[color:var(--danger-soft)] px-4 py-3">
      <AlertTriangle className="h-4 w-4 shrink-0 text-[color:var(--danger)]" />
      <p className="text-sm font-medium text-[color:var(--danger)]">{message}</p>
    </div>
  );
}

export function SeuilDynamiqueSection({ pointMesure, metrique, onRefresh }: SeuilDynamiqueSectionProps) {
  const { data: seuil, loading, error, refetch } = useSeuilDynamique(pointMesure.id, metrique);
  const { create, loading: creating, error: createError } = useCreateSeuilDynamique();
  const { update, loading: updating, error: updateError } = useUpdateSeuilDynamique();

  const [showEditForm, setShowEditForm] = useState(false);
  const [form, setForm] = useState({ margeConfiguree: '' });
  const [formError, setFormError] = useState<string | null>(null);
  const [refreshing, setRefreshing] = useState(false);

  const handleRefresh = async () => {
    setRefreshing(true);
    try {
      await refetch();
    } finally {
      setRefreshing(false);
    }
  };

  const handleCreate = async () => {
    if (!form.margeConfiguree) {
      setFormError('Veuillez remplir le champ marge');
      return;
    }

    const marge = Number(form.margeConfiguree);
    if (isNaN(marge) || marge <= 0) {
      setFormError('La marge doit être un nombre positif');
      return;
    }

    setFormError(null);
    try {
      const data: SeuilDynamiqueCreateDTO = {
        idPointMesure: pointMesure.id,
        metrique,
        margeConfiguree: marge,
      };
      await create(data);
      setForm({ margeConfiguree: '' });
      refetch();
      onRefresh();
    } catch (err) {
      setFormError(extractErrorMessage(err, 'Erreur lors de la création du seuil dynamique'));
    }
  };

  const handleUpdate = async () => {
    if (!seuil) return;

    const marge = Number(form.margeConfiguree);
    if (isNaN(marge) || marge <= 0) {
      setFormError('La marge doit être un nombre positif');
      return;
    }

    setFormError(null);
    try {
      const data: SeuilDynamiqueUpdateDTO = {
        margeConfiguree: marge,
      };
      await update(seuil.id, data);
      setShowEditForm(false);
      refetch();
      onRefresh();
    } catch (err) {
      setFormError(extractErrorMessage(err, 'Erreur lors de la modification de la marge'));
    }
  };

  return (
    <div className="neu-card p-6 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-primary/15">
            <AlertTriangle className="h-4 w-4 text-primary" />
          </div>
          <div>
            <h3 className="text-base font-bold">Seuil dynamique</h3>
            <p className="text-xs text-muted-foreground">
              Non historisé - recalculé automatiquement toutes les heures
            </p>
          </div>
        </div>
        <button
          onClick={handleRefresh}
          className="flex h-8 w-8 items-center justify-center rounded-lg border border-border bg-muted/30 text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
          title="Actualiser"
        >
          {refreshing ? (
            <Loader2 className="h-4 w-4 animate-spin" />
          ) : (
            <RefreshCw className="h-4 w-4" />
          )}
        </button>
      </div>

      {(error || createError || updateError) && (
        <ErrorBanner
          message={
            extractErrorMessage(error, '') ||
            extractErrorMessage(createError, '') ||
            extractErrorMessage(updateError, '') ||
            'Erreur de chargement'
          }
        />
      )}

      {/* Explanatory text */}
      <div className="rounded-xl bg-muted/30 px-4 py-3 text-sm text-muted-foreground">
        <p>
          Les valeurs min/max sont recalculées automatiquement toutes les heures à partir de la
          marge configurée - non modifiables directement.
        </p>
      </div>

      {!seuil ? (
        /* Create form for new threshold */
        <div className="rounded-2xl border border-border bg-muted/30 p-6">
          <h4 className="text-sm font-semibold mb-4">Créer un seuil dynamique</h4>

          {formError && <ErrorBanner message={formError} />}

          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-muted-foreground mb-1.5">
              Marge configurée (%)
            </label>
            <input
              type="number"
              value={form.margeConfiguree}
              onChange={(e) => setForm({ ...form, margeConfiguree: e.target.value })}
              placeholder="Ex: 5"
              className="w-full rounded-xl px-3 py-2 text-sm font-medium outline-none transition-shadow bg-[color:var(--surface)] shadow-[var(--shadow-neu-inset)] focus:ring-2 ring-primary"
            />
          </div>

          <button
            onClick={handleCreate}
            disabled={creating}
            className="mt-4 flex items-center gap-2 rounded-2xl bg-primary px-5 py-2.5 text-sm font-semibold text-primary-foreground shadow-[var(--shadow-glow)] transition-all hover:opacity-90 disabled:opacity-60"
          >
            {creating ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              <Plus className="h-4 w-4" />
            )}
            {creating ? 'Création...' : 'Créer le seuil dynamique'}
          </button>
        </div>
      ) : (
        /* Display existing threshold with edit form */
        <div className="space-y-4">
          {/* Read-only calculated values */}
          <div className="rounded-2xl border border-border bg-muted/30 p-6">
            <div className="grid grid-cols-2 gap-x-8 gap-y-3 sm:grid-cols-3">
              {[
                { label: 'Valeur min calculée', value: seuil.valeurMinCalculee ?? 'Pas encore calculé' },
                { label: 'Valeur max calculée', value: seuil.valeurMaxCalculee ?? 'Pas encore calculé' },
                { label: 'Dernier calcul', value: formatDate(seuil.dateCalcul) },
              ].map(({ label, value }) => (
                <div key={label}>
                  <p className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                    {label}
                  </p>
                  <p className="mt-0.5 text-sm font-bold text-foreground">{String(value)}</p>
                </div>
              ))}
            </div>
          </div>

          {/* Edit margin form */}
          {!showEditForm ? (
            <button
              onClick={() => {
                setShowEditForm(true);
                setForm({ margeConfiguree: String(seuil.margeConfiguree) });
              }}
              className="flex items-center gap-2 rounded-2xl border border-border px-5 py-2.5 text-sm font-semibold text-muted-foreground transition-colors hover:bg-muted"
            >
              <Edit className="h-4 w-4" />
              Modifier la marge ({seuil.margeConfiguree}%)
            </button>
          ) : (
            <div className="rounded-2xl border border-border bg-muted/30 p-6">
              <div className="flex items-center justify-between mb-4">
                <h4 className="text-sm font-semibold">Modifier la marge</h4>
                <button
                  onClick={() => {
                    setShowEditForm(false);
                    setForm({ margeConfiguree: '' });
                    setFormError(null);
                  }}
                  className="text-xs text-muted-foreground hover:text-foreground"
                >
                  Annuler
                </button>
              </div>

              {formError && <ErrorBanner message={formError} />}

              <div>
                <label className="block text-xs font-semibold uppercase tracking-wider text-muted-foreground mb-1.5">
                  Marge configurée (%)
                </label>
                <input
                  type="number"
                  value={form.margeConfiguree}
                  onChange={(e) => setForm({ ...form, margeConfiguree: e.target.value })}
                  className="w-full rounded-xl px-3 py-2 text-sm font-medium outline-none transition-shadow bg-[color:var(--surface)] shadow-[var(--shadow-neu-inset)] focus:ring-2 ring-primary"
                />
              </div>

              <button
                onClick={handleUpdate}
                disabled={updating}
                className="mt-4 flex items-center gap-2 rounded-2xl bg-primary px-5 py-2.5 text-sm font-semibold text-primary-foreground shadow-[var(--shadow-glow)] transition-all hover:opacity-90 disabled:opacity-60"
              >
                {updating ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                  <Edit className="h-4 w-4" />
                )}
                {updating ? 'Modification...' : 'Enregistrer'}
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
