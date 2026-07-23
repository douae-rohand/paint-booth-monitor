import { createFileRoute } from '@tanstack/react-router';
import { useState } from 'react';
import {
  Cpu,
  Plus,
  CheckCircle2,
  XCircle,
  Clock,
  Wifi,
  WifiOff,
  ChevronDown,
  ChevronUp,
  Loader2,
  AlertTriangle,
  History,
  ShieldAlert,
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
  getActivePlcConfiguration,
  getPlcConfigurationHistory,
  createPlcConfiguration,
  activatePlcConfiguration,
  deactivatePlcConfiguration,
  type PlcConfiguration,
  type CreatePlcConfigurationRequest,
} from '@/api/plc/index';
import { useAuth } from '@/hooks/useAuth';

export const Route = createFileRoute('/_authenticated/plc')({
  component: PlcConfigPage,
});

// ─── helpers ──────────────────────────────────────────────────────────────────

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

/** Extract a human-readable message from an axios error ({code, message} backend format) */
function extractErrorMessage(err: unknown, fallback: string): string {
  if (
    err &&
    typeof err === 'object' &&
    'response' in err
  ) {
    const res = (err as { response?: { data?: { message?: string } } }).response;
    if (res?.data?.message) return res.data.message;
  }
  return fallback;
}

// ─── field-level validation ───────────────────────────────────────────────────

const IPV4_REGEX =
  /^(25[0-5]|2[0-4]\d|1\d{2}|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d{2}|[1-9]?\d)){3}$/;

type FormErrors = Partial<Record<keyof CreatePlcConfigurationRequest, string>>;

function validateForm(form: CreatePlcConfigurationRequest): FormErrors {
  const errors: FormErrors = {};
  if (!IPV4_REGEX.test(form.ip)) {
    errors.ip = 'Adresse IPv4 invalide (ex: 192.168.0.1)';
  }
  if (form.port < 1 || form.port > 65535) {
    errors.port = 'Le port doit être compris entre 1 et 65535';
  }
  if (form.rack < 0 || form.rack > 7) {
    errors.rack = 'Le rack doit être compris entre 0 et 7';
  }
  if (form.slot < 0 || form.slot > 31) {
    errors.slot = 'Le slot doit être compris entre 0 et 31';
  }
  if (form.intervallePolling <= 0) {
    errors.intervallePolling = "L'intervalle de polling doit être strictement positif";
  }
  return errors;
}

// ─── sub-components ───────────────────────────────────────────────────────────

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

/** Server error banner */
function ErrorBanner({ message }: { message: string }) {
  return (
    <div className="flex items-center gap-3 rounded-2xl border border-[color:var(--danger)]/30 bg-[color:var(--danger-soft)] px-4 py-3">
      <AlertTriangle className="h-4 w-4 shrink-0 text-[color:var(--danger)]" />
      <p className="text-sm font-medium text-[color:var(--danger)]">{message}</p>
    </div>
  );
}

/** Field-level error */
function FieldError({ message }: { message?: string }) {
  if (!message) return null;
  return (
    <p className="mt-1 flex items-center gap-1 text-xs font-medium text-[color:var(--danger)]">
      <AlertTriangle className="h-3 w-3 shrink-0" />
      {message}
    </p>
  );
}

// ─── Active configuration card ────────────────────────────────────────────────

function ActiveConfigCard({
  config,
  onDeactivate,
  loading,
  isOnlyConfig,
}: {
  config: PlcConfiguration;
  onDeactivate: () => void;
  loading: boolean;
  isOnlyConfig?: boolean;
}) {
  return (
    <div className="rounded-2xl border-2 border-primary/40 bg-gradient-to-br from-primary/10 to-primary/5 p-6 shadow-[var(--shadow-neu-sm)]">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <div className="mt-4 grid grid-cols-2 gap-x-8 gap-y-3 sm:grid-cols-3">
            {(
              [
                { label: 'Adresse IP', value: config.ip },
                { label: 'Port', value: config.port },
                { label: 'Rack', value: config.rack },
                { label: 'Slot', value: config.slot },
                { label: 'Intervalle de polling', value: `${config.intervallePolling} ms` },
                { label: 'Activé le', value: formatDate(config.dateActivation) },
              ] as { label: string; value: string | number }[]
            ).map(({ label, value }) => (
              <div key={label}>
                <p className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                  {label}
                </p>
                <p className="mt-0.5 text-sm font-bold text-foreground">{String(value)}</p>
              </div>
            ))}
          </div>
        </div>

        {/* Deactivate with confirmation */}
        <AlertDialog>
          <AlertDialogTrigger asChild>
            <span className="inline-flex">
              <Switch
              checked
              disabled={loading}
              aria-label="Désactiver cette configuration PLC"
              className="h-6 w-11 data-[state=checked]:bg-orange-700"
            />
            </span>
          </AlertDialogTrigger>
          <AlertDialogContent className="rounded-2xl">
            <AlertDialogHeader>
              <AlertDialogTitle className="flex items-center gap-2">
                <AlertTriangle className="h-5 w-5 text-[color:var(--danger)]" />
                {isOnlyConfig ? 'Perte de connexion au PLC' : 'Confirmer la désactivation'}
              </AlertDialogTitle>
              <AlertDialogDescription asChild>
                <div className="text-sm text-muted-foreground space-y-2">
                  {isOnlyConfig ? (
                    <>
                      <p className="font-medium text-[color:var(--danger)]">
                        Attention : Il s'agit de la seule configuration enregistrée.
                      </p>
                      <p>
                        Si vous la désactivez, vous allez perdre la connexion avec le PLC ({config.ip}:{config.port}) et la collecte de données sera interrompue.
                      </p>
                    </>
                  ) : (
                    <p>
                      Cette action désactivera la connexion au PLC{' '}
                      <span className="font-semibold text-foreground">{config.ip}:{config.port}</span>.
                      <br />
                      Le service Python ne pourra plus lire les données de l'automate jusqu'à
                      l'activation d'une nouvelle configuration.
                    </p>
                  )}
                </div>
              </AlertDialogDescription>
            </AlertDialogHeader>
            <AlertDialogFooter>
              <AlertDialogCancel className="rounded-xl">Annuler</AlertDialogCancel>
              <AlertDialogAction
                onClick={onDeactivate}
                className="rounded-xl bg-[color:var(--danger)] text-white hover:opacity-90"
              >
                Désactiver
              </AlertDialogAction>
            </AlertDialogFooter>
          </AlertDialogContent>
        </AlertDialog>
      </div>
    </div>
  );
}

// ─── Create form ──────────────────────────────────────────────────────────────

function CreateConfigForm({
  activeConfig,
  onSuccess,
  onCancel,
}: {
  activeConfig: PlcConfiguration | null;
  onSuccess: () => void;
  onCancel: () => void;
}) {
  const [form, setForm] = useState<CreatePlcConfigurationRequest>({
    ip: '',
    port: 102,
    rack: 0,
    slot: 1,
    intervallePolling: 1000,
  });
  const [fieldErrors, setFieldErrors] = useState<FormErrors>({});
  const [serverError, setServerError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [checkingDuplicate, setCheckingDuplicate] = useState(false);
  const [duplicateConfig, setDuplicateConfig] = useState<PlcConfiguration | null>(null);
  const [activatingDuplicate, setActivatingDuplicate] = useState(false);

  const fields: {
    id: keyof CreatePlcConfigurationRequest;
    label: string;
    type: string;
    placeholder: string;
    hint?: string;
  }[] = [
    { id: 'ip', label: 'Adresse IP', type: 'text', placeholder: '192.168.0.1', hint: 'Format IPv4' },
    { id: 'port', label: 'Port', type: 'number', placeholder: '102', hint: '1 – 65535' },
    { id: 'rack', label: 'Rack', type: 'number', placeholder: '0', hint: '0 – 7' },
    { id: 'slot', label: 'Slot', type: 'number', placeholder: '1', hint: '0 – 31' },
    {
      id: 'intervallePolling',
      label: 'Intervalle de polling (ms)',
      type: 'number',
      placeholder: '1000',
      hint: 'Durée en ms entre deux lectures PLC',
    },
  ];

  const handleChange = (id: keyof CreatePlcConfigurationRequest, raw: string) => {
    const value = id === 'ip' ? raw : Number(raw);
    setForm((prev) => ({ ...prev, [id]: value }));
    // Clear field error on change
    if (fieldErrors[id]) {
      setFieldErrors((prev) => { const next = { ...prev }; delete next[id]; return next; });
    }
  };

  const handleRequestSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const errors = validateForm(form);
    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors);
      return;
    }

    setFieldErrors({});
    setServerError(null);
    setCheckingDuplicate(true);
    try {
      const history = await getPlcConfigurationHistory();
      const duplicate = history.find((config) =>
        config.ip === form.ip.trim() &&
        config.port === form.port &&
        config.rack === form.rack &&
        config.slot === form.slot &&
        config.intervallePolling === form.intervallePolling,
      );

      if (duplicate) {
        setDuplicateConfig(duplicate);
        return;
      }

      setConfirmOpen(true);
    } catch (err) {
      setServerError(extractErrorMessage(err, 'Impossible de verifier les configurations existantes.'));
    } finally {
      setCheckingDuplicate(false);
    }
  };

  const handleActivateDuplicate = async () => {
    if (!duplicateConfig) return;

    setActivatingDuplicate(true);
    setServerError(null);
    try {
      await activatePlcConfiguration(duplicateConfig.id);
      setDuplicateConfig(null);
      onSuccess();
    } catch (err) {
      setServerError(extractErrorMessage(err, "Erreur lors de l'activation de la configuration."));
    } finally {
      setActivatingDuplicate(false);
    }
  };

  const handleConfirmedSubmit = async () => {
    setSubmitting(true);
    setServerError(null);
    try {
      await createPlcConfiguration(form);
      setForm({ ip: '', port: 102, rack: 0, slot: 1, intervallePolling: 1000 });
      onSuccess();
    } catch (err) {
      setServerError(
        extractErrorMessage(err, 'Erreur lors de la création de la configuration.'),
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="neu-card p-6">
      <div className="flex items-center gap-3">
        <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-primary/15">
          <Plus className="h-4 w-4 text-primary" />
        </div>
        <div>
          <h3 className="text-base font-bold">Nouvelle configuration</h3>
          <p className="text-xs text-muted-foreground">
            Renseignez les paramètres de connexion au PLC Siemens S7-1200
          </p>
        </div>
      </div>

      {serverError && (
        <div className="mt-4">
          <ErrorBanner message={serverError} />
        </div>
      )}

      <form onSubmit={handleRequestSubmit} noValidate>
        <div className="mt-5 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {fields.map((field) => (
            <div key={field.id}>
              <label
                htmlFor={`plc-${field.id}`}
                className="block text-xs font-semibold uppercase tracking-wider text-muted-foreground"
              >
                {field.label}
              </label>
              <input
                id={`plc-${field.id}`}
                type={field.type}
                placeholder={field.placeholder}
                value={String(form[field.id])}
                onChange={(e) => handleChange(field.id, e.target.value)}
                className={
                  'mt-1.5 w-full rounded-xl px-3 py-2 text-sm font-medium outline-none transition-shadow ' +
                  'bg-[color:var(--surface)] shadow-[var(--shadow-neu-inset)] ' +
                  'focus:ring-2 ring-primary ' +
                  (fieldErrors[field.id] ? 'ring-2 ring-[color:var(--danger)]' : '')
                }
              />
              {field.hint && !fieldErrors[field.id] && (
                <p className="mt-1 text-xs text-muted-foreground">{field.hint}</p>
              )}
              <FieldError message={fieldErrors[field.id]} />
            </div>
          ))}
        </div>

        {/* Confirmation dialog wraps the submit button */}
        <AlertDialog open={confirmOpen} onOpenChange={setConfirmOpen}>
          <div className="mt-6 flex items-center gap-3">
              <button
                type="submit"
                disabled={submitting || checkingDuplicate}
                className="flex items-center gap-2 rounded-2xl bg-primary px-5 py-2.5 text-sm font-semibold text-primary-foreground shadow-[var(--shadow-glow)] transition-all hover:opacity-90 disabled:opacity-60"
              >
                {submitting ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                  <Plus className="h-4 w-4" />
                )}
                {submitting ? 'Création en cours...' : 'Créer la configuration'}
              </button>
            <button
              type="button"
              onClick={onCancel}
              disabled={submitting || checkingDuplicate}
              className="rounded-2xl border border-border px-5 py-2.5 text-sm font-semibold text-muted-foreground transition-colors hover:bg-muted disabled:opacity-60"
            >
              Annuler
            </button>
          </div>

          <AlertDialogContent className="rounded-2xl">
            <AlertDialogHeader>
              <AlertDialogTitle className="flex items-center gap-2">
                <AlertTriangle className="h-5 w-5 text-[color:var(--warning)]" />
                Confirmer la création
              </AlertDialogTitle>
              <AlertDialogDescription asChild>
                <div className="space-y-2 text-sm text-muted-foreground">
                  <p>
                    Vous allez créer et activer la configuration suivante :
                  </p>
                  <div className="rounded-xl bg-muted px-4 py-3 font-mono text-xs text-foreground space-y-1">
                    <p><span className="text-muted-foreground">IP :</span> {form.ip}</p>
                    <p><span className="text-muted-foreground">Port :</span> {form.port}</p>
                    <p><span className="text-muted-foreground">Rack :</span> {form.rack} - Slot : {form.slot}</p>
                    <p><span className="text-muted-foreground">Polling :</span> {form.intervallePolling} ms</p>
                  </div>
                  {activeConfig && (
                    <p className="font-medium text-[color:var(--warning)]">
                      ⚠ Cette action désactivera la configuration actuellement active
                      ({activeConfig.ip}:{activeConfig.port}) et activera celle-ci.
                    </p>
                  )}
                </div>
              </AlertDialogDescription>
            </AlertDialogHeader>
            <AlertDialogFooter>
              <AlertDialogCancel className="rounded-xl" onClick={() => setConfirmOpen(false)}>
                Annuler
              </AlertDialogCancel>
              <AlertDialogAction
                onClick={handleConfirmedSubmit}
                disabled={submitting || checkingDuplicate}
                className="rounded-xl bg-primary text-primary-foreground hover:opacity-90 disabled:opacity-50"
              >
                {submitting ? (
                  <span className="flex items-center gap-2">
                    <Loader2 className="h-4 w-4 animate-spin" /> Création...
                  </span>
                ) : (
                  'Confirmer la création'
                )}
              </AlertDialogAction>
            </AlertDialogFooter>
          </AlertDialogContent>
        </AlertDialog>

        <AlertDialog
          open={duplicateConfig !== null}
          onOpenChange={(open) => {
            if (!open && !activatingDuplicate) setDuplicateConfig(null);
          }}
        >
          <AlertDialogContent className="rounded-2xl">
            <AlertDialogHeader>
              <AlertDialogTitle className="flex items-center gap-2">
                <AlertTriangle className="h-5 w-5 text-[color:var(--warning)]" />
                {duplicateConfig?.actif ? 'Configuration deja active' : 'Configuration deja existante'}
              </AlertDialogTitle>
              <AlertDialogDescription>
                {duplicateConfig?.actif
                  ? 'Cette configuration est deja active. Aucune action supplementaire n est necessaire.'
                  : "Cette configuration existe deja dans l'historique. Vous pouvez simplement l'activer au lieu d'en creer une nouvelle."}
              </AlertDialogDescription>
            </AlertDialogHeader>
            <AlertDialogFooter>
              <AlertDialogCancel disabled={activatingDuplicate} className="rounded-xl">
                {duplicateConfig?.actif ? 'Fermer' : 'Annuler'}
              </AlertDialogCancel>
              {!duplicateConfig?.actif && (
                <AlertDialogAction
                  onClick={(event) => {
                    event.preventDefault();
                    void handleActivateDuplicate();
                  }}
                  disabled={activatingDuplicate}
                  className="rounded-xl bg-primary text-primary-foreground hover:opacity-90"
                >
                  {activatingDuplicate ? 'Activation...' : 'Activer la configuration'}
                </AlertDialogAction>
              )}
            </AlertDialogFooter>
          </AlertDialogContent>
        </AlertDialog>
      </form>
    </div>
  );
}

// ─── History table ────────────────────────────────────────────────────────────

function HistoryTable({
  history,
  activeId,
  actionId,
  onActivate,
  onDeactivate,
  isOnlyConfig,
}: {
  history: PlcConfiguration[];
  activeId: string | null;
  actionId: string | null;
  onActivate: (cfg: PlcConfiguration) => void;
  onDeactivate: (cfg: PlcConfiguration) => void;
  isOnlyConfig?: boolean;
}) {
  if (history.length === 0) {
    return (
      <div className="px-6 py-8 text-center text-sm text-muted-foreground">
        Aucune configuration enregistrée - créez la première via le formulaire ci-dessus.
      </div>
    );
  }

  return (
    <div className="overflow-x-auto">
      <table className="w-full text-sm">
        <thead>
          <tr className="border-b border-border bg-muted/30">
            {[
              'Statut',
              'IP',
              'Port',
              'Rack',
              'Slot',
              'Polling (ms)',
              'Créé le',
              'Activé le',
              'Désactivé le',
              'Actions',
            ].map((h) => (
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
          {history.map((cfg) => {
            const isActive = cfg.id === activeId;
            const isLoading = actionId === cfg.id;
            return (
              <tr
                key={cfg.id}
                className={
                  'transition-colors hover:bg-muted/20 ' +
                  (isActive ? 'bg-[color:var(--success)]/5' : '')
                }
              >
                <td className="px-4 py-3">
                  <StatusBadge actif={cfg.actif} />
                </td>
                <td className="px-4 py-3 font-mono text-xs font-semibold">{cfg.ip}</td>
                <td className="px-4 py-3">{cfg.port}</td>
                <td className="px-4 py-3">{cfg.rack}</td>
                <td className="px-4 py-3">{cfg.slot}</td>
                <td className="px-4 py-3">{cfg.intervallePolling}</td>
                <td className="px-4 py-3 whitespace-nowrap text-xs text-muted-foreground">
                  <span className="flex items-center gap-1.5">
                    {formatDate(cfg.dateCreation)}
                  </span>
                </td>
                <td className="px-4 py-3 whitespace-nowrap text-xs text-muted-foreground">
                  {formatDate(cfg.dateActivation)}
                </td>
                <td className="px-4 py-3 whitespace-nowrap text-xs text-muted-foreground">
                  {formatDate(cfg.dateDesactivation)}
                </td>
                <td className="px-4 py-3">
                  {isActive ? (
                    /* Deactivate with confirmation */
                    <AlertDialog>
                      <AlertDialogTrigger asChild>
                        <div className="flex items-center gap-2">
                          <Switch
                            checked
                            disabled={isLoading}
                            aria-label={`Désactiver la configuration ${cfg.ip}:${cfg.port}`}
                            className="data-[state=checked]:bg-orange-700"
                          />
                          {isLoading && <Loader2 className="h-3 w-3 animate-spin" />}
                        </div>
                      </AlertDialogTrigger>
                      <AlertDialogContent className="rounded-2xl">
                        <AlertDialogHeader>
                          <AlertDialogTitle className="flex items-center gap-2">
                            <AlertTriangle className="h-5 w-5 text-[color:var(--danger)]" />
                            {isOnlyConfig ? 'Perte de connexion au PLC' : 'Confirmer la désactivation'}
                          </AlertDialogTitle>
                          <AlertDialogDescription asChild>
                            <div className="text-sm text-muted-foreground space-y-2">
                              {isOnlyConfig ? (
                                <>
                                  <p className="font-medium text-[color:var(--danger)]">
                                    Attention : Il s'agit de la seule configuration enregistrée.
                                  </p>
                                  <p>
                                    Si vous la désactivez, vous allez perdre la connexion avec le PLC ({cfg.ip}:{cfg.port}) et la collecte de données sera interrompue.
                                  </p>
                                </>
                              ) : (
                                <p>
                                  Le service Python ne pourra plus lire les données de l'automate
                                  ({cfg.ip}:{cfg.port}) jusqu'à l'activation d'une nouvelle configuration.
                                </p>
                              )}
                            </div>
                          </AlertDialogDescription>
                        </AlertDialogHeader>
                        <AlertDialogFooter>
                          <AlertDialogCancel className="rounded-xl">Annuler</AlertDialogCancel>
                          <AlertDialogAction
                            onClick={() => onDeactivate(cfg)}
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
                            disabled={isLoading}
                            aria-label={`Activer la configuration ${cfg.ip}:${cfg.port}`}
                          />
                          {isLoading && <Loader2 className="h-3 w-3 animate-spin" />}
                        </div>
                      </AlertDialogTrigger>
                      <AlertDialogContent className="rounded-2xl">
                        <AlertDialogHeader>
                          <AlertDialogTitle className="flex items-center gap-2">
                            <Wifi className="h-5 w-5 text-primary" />
                            Confirmer l'activation
                          </AlertDialogTitle>
                          <AlertDialogDescription>
                            Ceci désactivera la configuration actuellement active et remplacera
                            l'IP/port utilisés par le service de connexion par{' '}
                            <span className="font-semibold text-foreground">
                              {cfg.ip}:{cfg.port}
                            </span>
                          </AlertDialogDescription>
                        </AlertDialogHeader>
                        <AlertDialogFooter>
                          <AlertDialogCancel className="rounded-xl">Annuler</AlertDialogCancel>
                          <AlertDialogAction
                            onClick={() => onActivate(cfg)}
                            className="rounded-xl bg-primary text-primary-foreground hover:opacity-90"
                          >
                            Activer cette configuration
                          </AlertDialogAction>
                        </AlertDialogFooter>
                      </AlertDialogContent>
                    </AlertDialog>
                  )}
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}

// ─── main page ────────────────────────────────────────────────────────────────

function PlcConfigPage() {
  const { isAdmin } = useAuth();

  // ── Admin guard ──
  if (!isAdmin) {
    return (
      <div className="flex min-h-[60vh] items-center justify-center px-4">
        <div className="neu-card max-w-md p-10 text-center">
          <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-[color:var(--danger-soft)]">
            <ShieldAlert className="h-7 w-7 text-[color:var(--danger)]" />
          </div>
          <h2 className="text-xl font-bold text-foreground">Accès refusé</h2>
          <p className="mt-2 text-sm text-muted-foreground">
            Cette page est réservée aux administrateurs. Contactez votre administrateur système
            si vous pensez avoir accès à cette fonctionnalité.
          </p>
        </div>
      </div>
    );
  }

  return <PlcConfigContent />;
}

function PlcConfigContent() {
  const [active, setActive] = useState<PlcConfiguration | null | undefined>(undefined);
  const [history, setHistory] = useState<PlcConfiguration[]>([]);
  const [loadingActive, setLoadingActive] = useState(false);
  const [loadingHistory, setLoadingHistory] = useState(false);
  const [historyLoaded, setHistoryLoaded] = useState(false);
  const [historyOpen, setHistoryOpen] = useState(false);
  const [actionId, setActionId] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);

  // ── data loading ──

  const fetchActive = async () => {
    setLoadingActive(true);
    try {
      const data = await getActivePlcConfiguration();
      setActive(data);
    } catch (err) {
      setActionError(extractErrorMessage(err, 'Impossible de charger la configuration active.'));
    } finally {
      setLoadingActive(false);
    }
  };

  const fetchHistory = async () => {
    setLoadingHistory(true);
    try {
      const data = await getPlcConfigurationHistory();
      setHistory(data);
      setHistoryLoaded(true);
    } catch (err) {
      setActionError(extractErrorMessage(err, "Impossible de charger l'historique."));
    } finally {
      setLoadingHistory(false);
    }
  };

  // Lazy initial load
  if (active === undefined && !loadingActive) {
    fetchActive();
    if (!historyLoaded && !loadingHistory) {
      fetchHistory();
    }
  }

  // ── handlers ──

  const toggleHistory = async () => {
    const next = !historyOpen;
    setHistoryOpen(next);
    if (next && !historyLoaded) await fetchHistory();
  };

  const handleActivate = async (cfg: PlcConfiguration) => {
    setActionId(cfg.id);
    setActionError(null);
    try {
      await activatePlcConfiguration(cfg.id);
      await Promise.all([fetchActive(), ...(historyLoaded ? [fetchHistory()] : [])]);
    } catch (err) {
      setActionError(extractErrorMessage(err, "Erreur lors de l'activation."));
    } finally {
      setActionId(null);
    }
  };

  const handleDeactivate = async (cfg: PlcConfiguration) => {
    setActionId(cfg.id);
    setActionError(null);
    try {
      await deactivatePlcConfiguration(cfg.id);
      await Promise.all([fetchActive(), ...(historyLoaded ? [fetchHistory()] : [])]);
    } catch (err) {
      setActionError(extractErrorMessage(err, 'Erreur lors de la désactivation.'));
    } finally {
      setActionId(null);
    }
  };

  const handleFormSuccess = async () => {
    setShowForm(false);
    setHistoryLoaded(false);
    await fetchActive();
    if (historyOpen) await fetchHistory();
  };

  const activeId = active?.id ?? null;

  return (
    <div className="space-y-6">
      {/* ── Header ── */}
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div className="flex items-center gap-3">          
          <div>
            <h2 className="text-xl font-bold tracking-tight">Configuration PLC</h2>
            <p className="text-sm text-muted-foreground">
              Paramètres de connexion à l'automate Siemens S7-1200
            </p>
          </div>
        </div>
        <button
          type="button"
          onClick={() => setShowForm((v) => !v)}
          className="flex items-center gap-2 rounded-2xl bg-primary px-4 py-2.5 text-sm font-semibold text-primary-foreground shadow-[var(--shadow-glow)] transition-all hover:opacity-90"
        >
          <Plus className="h-4 w-4" />
          {showForm ? 'Fermer le formulaire' : 'Nouvelle configuration'}
        </button>
      </div>

      {/* ── Action error banner ── */}
      {actionError && <ErrorBanner message={actionError} />}

      {/* ── Create form ── */}
      {showForm && (
        <CreateConfigForm
          activeConfig={active ?? null}
          onSuccess={handleFormSuccess}
          onCancel={() => setShowForm(false)}
        />
      )}

      {/* ── Active configuration ── */}
      <div className="neu-card p-6">
        <div className="mb-5 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-[color:var(--warning)]/15">
              <Wifi className="h-4 w-4 text-[color:var(--warning)]" />
            </div>
            <h3 className="text-base font-bold">Configuration active</h3>
          </div>
          <button
            type="button"
            onClick={fetchActive}
            disabled={loadingActive}
            className="text-xs font-semibold text-muted-foreground transition-colors hover:text-primary disabled:opacity-50"
          >
            {loadingActive ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              'Actualiser'
            )}
          </button>
        </div>

        {loadingActive && active === undefined ? (
          /* Skeleton */
          <div className="space-y-3">
            {[1, 2].map((i) => (
              <div key={i} className="h-5 w-full animate-pulse rounded-xl bg-muted" />
            ))}
          </div>
        ) : active ? (
          <ActiveConfigCard
            config={active}
            onDeactivate={() => handleDeactivate(active)}
            loading={actionId === active.id}
            isOnlyConfig={history.length === 1}
          />
        ) : (
          <div className="flex items-start gap-4 rounded-2xl border border-dashed border-border py-7 px-5 text-muted-foreground">
            <WifiOff className="h-5 w-5 mt-0.5 shrink-0 text-[color:var(--danger)]" />
            <div>
              <p className="text-sm font-semibold text-foreground">
                Aucune configuration active
              </p>
              <p className="mt-0.5 text-sm">
                Le service de connexion ne peut pas se connecter au PLC. Créez et activez une
                configuration ci-dessus pour démarrer la collecte de données.
              </p>
            </div>
          </div>
        )}
      </div>

      {/* ── History ── */}
      <div className="neu-card overflow-hidden">
        <button
          type="button"
          onClick={toggleHistory}
          className="flex w-full items-center justify-between p-6 transition-colors hover:bg-muted/20"
        >
          <div className="flex items-center gap-3">
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-primary/15">
              <History className="h-4 w-4 text-primary" />
            </div>
            <div className="text-left">
              <h3 className="text-base font-bold">Historique des configurations</h3>
              <p className="text-xs text-muted-foreground">
                Triées de la plus récente à la plus ancienne
              </p>
            </div>
          </div>
          {historyOpen ? (
            <ChevronUp className="h-5 w-5 text-muted-foreground" />
          ) : (
            <ChevronDown className="h-5 w-5 text-muted-foreground" />
          )}
        </button>

        {historyOpen && (
          <div className="border-t border-border">
            {loadingHistory ? (
              <div className="flex items-center gap-3 p-6 text-sm text-muted-foreground">
                <Loader2 className="h-4 w-4 animate-spin" />
                Chargement de l'historique...
              </div>
            ) : (
              <HistoryTable
                history={history}
                activeId={activeId}
                actionId={actionId}
                onActivate={handleActivate}
                onDeactivate={handleDeactivate}
                isOnlyConfig={history.length === 1}
               />
            )}
          </div>
        )}
      </div>
    </div>
  );
}
