import { useState } from 'react';
import {
  CheckCircle2,
  XCircle,
  Loader2,
  AlertTriangle,
  Plus,
  History,
  Wifi,
  RefreshCw,
} from 'lucide-react';
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from '@/components/ui/alert-dialog';
import { Switch } from '@/components/ui/switch';
import {
  useSeuilAbsoluActif,
  useSeuilAbsoluHistory,
  useCreateSeuilAbsolu,
  useToggleSeuilAbsolu,
} from '@/hooks/useSeuils';
import {
  type PointMesure,
  type Metrique,
  type SeuilAbsoluResponseDTO,
  type SeuilAbsoluCreateDTO,
} from '@/api/alerting/seuils';

interface SeuilAbsoluSectionProps {
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

function StatusBadge({ actif }: { actif: boolean }) {
  return (
    <span
      className={
        'inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-semibold ' +
        (actif
          ? 'bg-[color:var(--success)]/15 text-[color:var(--success)]'
          : 'bg-muted text-muted-foreground')
      }
    >
      {actif ? (
        <CheckCircle2 className="h-3 w-3" />
      ) : (
        <XCircle className="h-3 w-3" />
      )}
      {actif ? 'Actif' : 'Inactif'}
    </span>
  );
}

function ErrorBanner({ message }: { message: string }) {
  return (
    <div className="flex items-center gap-3 rounded-2xl border border-[color:var(--danger)]/30 bg-[color:var(--danger-soft)] px-4 py-3">
      <AlertTriangle className="h-4 w-4 shrink-0 text-[color:var(--danger)]" />
      <p className="text-sm font-medium text-[color:var(--danger)]">{message}</p>
    </div>
  );
}

export function SeuilAbsoluSection({ pointMesure, metrique, onRefresh }: SeuilAbsoluSectionProps) {
  const { data: active, loading: loadingActive, error: activeError, refetch: refetchActive } = useSeuilAbsoluActif(
    pointMesure.id,
    metrique,
  );
  const { data: history, loading: loadingHistory, refetch: refetchHistory } = useSeuilAbsoluHistory(
    pointMesure.id,
    metrique,
  );
  const { create, loading: creating, error: createError } = useCreateSeuilAbsolu();
  const { activer, desactiver, loading: toggling, error: toggleError } = useToggleSeuilAbsolu();

  const [showCreateForm, setShowCreateForm] = useState(false);
  const [form, setForm] = useState({ valeurMin: '', valeurMax: '' });
  const [formError, setFormError] = useState<string | null>(null);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [actionId, setActionId] = useState<string | null>(null);
  const [refreshing, setRefreshing] = useState(false);

  const handleRefresh = async () => {
    setRefreshing(true);
    try {
      await Promise.all([refetchActive(), refetchHistory()]);
    } finally {
      setRefreshing(false);
    }
  };

  const handleCreate = async () => {
    if (!form.valeurMin || !form.valeurMax) {
      setFormError('Veuillez remplir tous les champs');
      return;
    }

    const valeurMin = Number(form.valeurMin);
    const valeurMax = Number(form.valeurMax);

    if (isNaN(valeurMin) || isNaN(valeurMax)) {
      setFormError('Les valeurs doivent être des nombres');
      return;
    }
    if (valeurMin >= valeurMax) {
      setFormError('La valeur minimale doit être inférieure à la valeur maximale');
      return;
    }

    setFormError(null);
    setConfirmOpen(true);
  };

  const handleConfirmedCreate = async () => {
    try {
      const data: SeuilAbsoluCreateDTO = {
        idPointMesure: pointMesure.id,
        metrique,
        valeurMin: Number(form.valeurMin),
        valeurMax: Number(form.valeurMax),
      };
      await create(data);
      setShowCreateForm(false);
      setForm({ valeurMin: '', valeurMax: '' });
      onRefresh();
    } catch (err) {
      setFormError(extractErrorMessage(err, 'Erreur lors de la création du seuil'));
    }
  };

  const handleToggle = async (id: string, shouldActivate: boolean) => {
    setActionId(id);
    try {
      if (shouldActivate) {
        await activer(id);
      } else {
        await desactiver(id);
      }
      onRefresh();
    } catch (err) {
      setFormError(extractErrorMessage(err, 'Erreur lors du changement de statut'));
    } finally {
      setActionId(null);
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
            <h3 className="text-base font-bold">Seuil absolu</h3>
            <p className="text-xs text-muted-foreground">
              Historisé - un seul seuil actif par combinaison point/métrique
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

      {(activeError || createError || toggleError) && (
        <ErrorBanner
          message={
            extractErrorMessage(activeError, '') ||
            extractErrorMessage(createError, '') ||
            extractErrorMessage(toggleError, '') ||
            'Erreur de chargement'
          }
        />
      )}

      {/* Active threshold display */}
      {active ? (
        <div className="rounded-2xl border-2 border-primary/40 bg-gradient-to-br from-primary/10 to-primary/5 p-6 shadow-[var(--shadow-neu-sm)]">
          <div className="flex flex-wrap items-start justify-between gap-4">
            <div className="grid grid-cols-2 gap-x-8 gap-y-3 sm:grid-cols-3">
              {[
                { label: 'Valeur min', value: active.valeurMin },
                { label: 'Valeur max', value: active.valeurMax },
                { label: 'Activé le', value: formatDate(active.dateActivation) },
              ].map(({ label, value }) => (
                <div key={label}>
                  <p className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                    {label}
                  </p>
                  <p className="mt-0.5 text-sm font-bold text-foreground">{String(value)}</p>
                </div>
              ))}
            </div>
            <StatusBadge actif={active.actif} />
          </div>
        </div>
      ) : (
        <div className="px-6 py-8 text-center text-sm text-muted-foreground">
          Aucun seuil absolu actif pour cette combinaison
        </div>
      )}

      {/* Create form */}
      {!showCreateForm ? (
        <button
          onClick={() => setShowCreateForm(true)}
          className="flex items-center gap-2 rounded-2xl border border-border px-5 py-2.5 text-sm font-semibold text-muted-foreground transition-colors hover:bg-muted"
        >
          <Plus className="h-4 w-4" />
          Créer un nouveau seuil absolu
        </button>
      ) : (
        <div className="rounded-2xl border border-border bg-muted/30 p-6">
          <div className="flex items-center justify-between mb-4">
            <h4 className="text-sm font-semibold">Nouveau seuil absolu</h4>
            <button
              onClick={() => {
                setShowCreateForm(false);
                setForm({ valeurMin: '', valeurMax: '' });
                setFormError(null);
              }}
              className="text-xs text-muted-foreground hover:text-foreground"
            >
              Annuler
            </button>
          </div>

          {formError && <ErrorBanner message={formError} />}

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-muted-foreground mb-1.5">
                Valeur min
              </label>
              <input
                type="number"
                placeholder={metrique === 'TEMPERATURE' ? 'Ex: 15' : 'Ex: 30'}
                value={form.valeurMin}
                onChange={(e) => setForm({ ...form, valeurMin: e.target.value })}
                className="w-full rounded-xl px-3 py-2 text-sm font-medium outline-none transition-shadow bg-[color:var(--surface)] shadow-[var(--shadow-neu-inset)] focus:ring-2 ring-primary"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-muted-foreground mb-1.5">
                Valeur max
              </label>
              <input
                type="number"
                placeholder={metrique === 'TEMPERATURE' ? 'Ex: 25' : 'Ex: 70'}
                value={form.valeurMax}
                onChange={(e) => setForm({ ...form, valeurMax: e.target.value })}
                className="w-full rounded-xl px-3 py-2 text-sm font-medium outline-none transition-shadow bg-[color:var(--surface)] shadow-[var(--shadow-neu-inset)] focus:ring-2 ring-primary"
              />
            </div>
          </div>

          <AlertDialog open={confirmOpen} onOpenChange={setConfirmOpen}>
            <div className="mt-4 flex items-center gap-3">
              <button
                onClick={handleCreate}
                disabled={creating}
                className="flex items-center gap-2 rounded-2xl bg-primary px-5 py-2.5 text-sm font-semibold text-primary-foreground shadow-[var(--shadow-glow)] transition-all hover:opacity-90 disabled:opacity-60"
              >
                {creating ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                  <Plus className="h-4 w-4" />
                )}
                {creating ? 'Création...' : 'Créer'}
              </button>
            </div>

            <AlertDialogContent className="rounded-2xl">
              <AlertDialogHeader>
                <AlertDialogTitle className="flex items-center gap-2">
                  <AlertTriangle className="h-5 w-5 text-[color:var(--danger)]" />
                  Confirmer la création
                </AlertDialogTitle>
                <AlertDialogDescription asChild>
                  <div className="space-y-2 text-sm text-muted-foreground">
                    <p>Vous allez créer un nouveau seuil absolu :</p>
                    <div className="rounded-xl bg-muted px-4 py-3 font-mono text-xs text-foreground space-y-1">
                      <p><span className="text-muted-foreground">Point :</span> {pointMesure.nom}</p>
                      <p><span className="text-muted-foreground">Métrique :</span> {metrique}</p>
                      <p><span className="text-muted-foreground">Min :</span> {form.valeurMin}</p>
                      <p><span className="text-muted-foreground">Max :</span> {form.valeurMax}</p>
                    </div>
                    {active && (
                      <p className="font-medium text-[color:var(--danger)]">
                        ⚠ Ceci désactivera le seuil actif actuel et créera un nouveau seuil.
                      </p>
                    )}
                  </div>
                </AlertDialogDescription>
              </AlertDialogHeader>
              <AlertDialogFooter>
                <AlertDialogCancel className="rounded-xl">Annuler</AlertDialogCancel>
                <AlertDialogAction
                  onClick={handleConfirmedCreate}
                  disabled={creating}
                  className="rounded-xl bg-[color:var(--danger)] text-white hover:opacity-90"
                >
                  {creating ? (
                    <span className="flex items-center gap-2">
                      <Loader2 className="h-4 w-4 animate-spin" /> Création...
                    </span>
                  ) : (
                    'Confirmer'
                  )}
                </AlertDialogAction>
              </AlertDialogFooter>
            </AlertDialogContent>
          </AlertDialog>
        </div>
      )}

      {/* History */}
      <div>
        <div className="flex items-center gap-2 mb-4">
          <History className="h-4 w-4 text-muted-foreground" />
          <h4 className="text-sm font-semibold">Historique</h4>
        </div>

        {!history || history.length === 0 ? (
          <div className="px-6 py-8 text-center text-sm text-muted-foreground">
            Aucun historique disponible
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border bg-muted/30">
                  {['Statut', 'Valeur min', 'Valeur max', 'Créé le'].map((h) => (
                    <th
                      key={h}
                      className="whitespace-nowrap px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-muted-foreground"
                    >
                      {h}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {history.map((seuil) => (
                  <tr
                    key={seuil.id}
                    className="transition-colors hover:bg-muted/20"
                  >
                    <td className="px-4 py-3">
                      <StatusBadge actif={seuil.actif} />
                    </td>
                    <td className="px-4 py-3">{seuil.valeurMin}</td>
                    <td className="px-4 py-3">{seuil.valeurMax}</td>
                    <td className="px-4 py-3 whitespace-nowrap text-xs text-muted-foreground">
                      {formatDate(seuil.createdAt)}
                    </td>
                    <td className="px-4 py-3">
                      {seuil.actif ? (
                        /* Deactivate with confirmation */
                        <AlertDialog>
                          <AlertDialogTrigger asChild>
                            <div className="flex items-center gap-2">
                              <Switch
                                checked
                                disabled={toggling && actionId === seuil.id}
                                aria-label={`Désactiver le seuil ${seuil.id}`}
                                className="data-[state=checked]:bg-orange-700"
                              />
                              {toggling && actionId === seuil.id && <Loader2 className="h-3 w-3 animate-spin" />}
                            </div>
                          </AlertDialogTrigger>
                          <AlertDialogContent className="rounded-2xl">
                            <AlertDialogHeader>
                              <AlertDialogTitle className="flex items-center gap-2">
                                <AlertTriangle className="h-5 w-5 text-[color:var(--danger)]" />
                                {history.filter(s => s.actif).length === 1 ? 'Aucun seuil actif' : 'Confirmer la désactivation'}
                              </AlertDialogTitle>
                              <AlertDialogDescription asChild>
                                <div className="text-sm text-muted-foreground space-y-2">
                                  {history.filter(s => s.actif).length === 1 ? (
                                    <>
                                      <p className="font-medium text-[color:var(--danger)]">
                                        Attention : Si vous le désactivez, il n'y aura plus aucun seuil actif pour{' '}
                                        <span className="font-semibold text-foreground">{seuil.nomPointMesure}</span> ({seuil.metrique}).
                                      </p>
                                      <p className="font-medium text-[color:var(--warning)]">
                                        Vous devez activer immédiatement un autre seuil pour maintenir la surveillance.
                                      </p>
                                    </>
                                  ) : (
                                    <p>
                                      Cette action désactivera le seuil absolu pour{' '}
                                      <span className="font-semibold text-foreground">{seuil.nomPointMesure}</span> ({seuil.metrique}).
                                    </p>
                                  )}
                                </div>
                              </AlertDialogDescription>
                            </AlertDialogHeader>
                            <AlertDialogFooter>
                              <AlertDialogCancel className="rounded-xl">Annuler</AlertDialogCancel>
                              <AlertDialogAction
                                onClick={() => handleToggle(seuil.id, false)}
                                className="rounded-xl bg-[color:var(--danger)] text-white hover:opacity-90"
                              >
                                Désactiver
                              </AlertDialogAction>
                            </AlertDialogFooter>
                          </AlertDialogContent>
                        </AlertDialog>
                      ) : (
                        /* Activate with confirmation */
                        <AlertDialog>
                          <AlertDialogTrigger asChild>
                            <div className="flex items-center gap-2">
                              <Switch
                                checked={false}
                                disabled={toggling && actionId === seuil.id}
                                aria-label={`Activer le seuil ${seuil.id}`}
                              />
                              {toggling && actionId === seuil.id && <Loader2 className="h-3 w-3 animate-spin" />}
                            </div>
                          </AlertDialogTrigger>
                          <AlertDialogContent className="rounded-2xl">
                            <AlertDialogHeader>
                              <AlertDialogTitle className="flex items-center gap-2">
                                <Wifi className="h-5 w-5 text-primary" />
                                Confirmer l'activation
                              </AlertDialogTitle>
                              <AlertDialogDescription asChild>
                                <div className="text-sm text-muted-foreground space-y-2">
                                  <p>
                                    Ceci activera le seuil absolu pour{' '}
                                    <span className="font-semibold text-foreground">{seuil.nomPointMesure}</span> ({seuil.metrique}).
                                  </p>
                                  {active && (
                                    <p className="font-medium text-[color:var(--warning)]">
                                      ⚠ Le seuil actif actuel sera automatiquement désactivé.
                                    </p>
                                  )}
                                </div>
                              </AlertDialogDescription>
                            </AlertDialogHeader>
                            <AlertDialogFooter>
                              <AlertDialogCancel className="rounded-xl">Annuler</AlertDialogCancel>
                              <AlertDialogAction
                                onClick={() => handleToggle(seuil.id, true)}
                                className="rounded-xl bg-primary text-primary-foreground hover:opacity-90"
                              >
                                Activer ce seuil
                              </AlertDialogAction>
                            </AlertDialogFooter>
                          </AlertDialogContent>
                        </AlertDialog>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
