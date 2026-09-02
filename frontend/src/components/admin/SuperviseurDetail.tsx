import { useState } from 'react';
import {
  Loader2,
  AlertTriangle,
  Power,
  PowerOff,
  Edit2,
  ArrowLeft,
  Clock,
  Mail,
  Phone,
  Calendar,
  CheckCircle2,
  XCircle,
  Save,
  X,
  User,
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
import { useSuperviseur, useSuperviseurActions } from '@/hooks/useSuperviseurs';
import type { SuperviseurUpdateDTO } from '@/api/admin/superviseurs';

function formatDate(iso: string | null) {
  if (!iso) return '-';
  return new Date(iso).toLocaleString('fr-FR', {
    day: '2-digit',
    month: 'long',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

function SuccessBanner({ message }: { message: string }) {
  return (
    <div className="flex items-center gap-3 rounded-2xl border border-[color:var(--success)]/30 bg-[color:var(--success)]/10 px-4 py-3">
      <CheckCircle2 className="h-4 w-4 shrink-0 text-[color:var(--success)]" />
      <p className="text-sm font-medium text-[color:var(--success)]">{message}</p>
    </div>
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

function FieldError({ message }: { message?: string }) {
  if (!message) return null;
  return (
    <p className="mt-1 flex items-center gap-1 text-xs font-medium text-[color:var(--danger)]">
      <AlertTriangle className="h-3 w-3 shrink-0" />
      {message}
    </p>
  );
}

function StatusBadgeSingle({ actif, compteActive }: { actif: boolean; compteActive: boolean }) {
  if (!compteActive) {
    return (
      <span className="inline-flex items-center gap-1.5 rounded-full bg-amber-500/15 px-3 py-1 text-xs font-semibold text-amber-500">
        <Clock className="h-3.5 w-3.5" />
        En attente
      </span>
    );
  }
  if (actif) {
    return (
      <span className="inline-flex items-center gap-1.5 rounded-full bg-[color:var(--success)]/15 px-3 py-1 text-xs font-semibold text-[color:var(--success)]">
        <CheckCircle2 className="h-3.5 w-3.5" />
        Actif
      </span>
    );
  }
  return (
    <span className="inline-flex items-center gap-1.5 rounded-full bg-muted px-3 py-1 text-xs font-semibold text-muted-foreground">
      <XCircle className="h-3.5 w-3.5" />
      Inactif
    </span>
  );
}

interface SuperviseurDetailProps {
  id: string;
  onBack: () => void;
  onRefresh: () => void;
  refreshKey?: number;
}

export function SuperviseurDetail({ id, onBack, onRefresh, refreshKey }: SuperviseurDetailProps) {
  const { data: initialData, loading, error } = useSuperviseur(id, refreshKey);
  const { update, activate, deactivate, resendActivation } = useSuperviseurActions();
  const [updatedData, setUpdatedData] = useState<typeof initialData | null>(null);
  const [isEditing, setIsEditing] = useState(false);
  const [editForm, setEditForm] = useState<SuperviseurUpdateDTO>({});
  const [fieldErrors, setFieldErrors] = useState<Partial<Record<keyof SuperviseurUpdateDTO, string>>>({});
  const [serverError, setServerError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [actionLoading, setActionLoading] = useState(false);
  const [resendLoading, setResendLoading] = useState(false);
  const [actionId, setActionId] = useState<string | null>(null);

  const data = updatedData ?? initialData;

  if (loading) {
    return (
      <div className="neu-card flex h-full items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-primary/60" />
      </div>
    );
  }

  if (error || !data) {
    return (
      <div className="neu-card flex h-full flex-col items-center justify-center gap-4 px-6 py-8">
        <ErrorBanner message="Impossible de charger les détails du superviseur." />
        <button
          onClick={onBack}
          className="mt-2 flex items-center gap-2 rounded-xl px-4 py-2 text-sm font-semibold text-muted-foreground hover:bg-muted transition-colors"
        >
          <ArrowLeft className="h-4 w-4" />
          Retour à la liste
        </button>
      </div>
    );
  }

  const initials = `${data.prenom?.[0] ?? ''}${data.nom?.[0] ?? ''}`.toUpperCase();

  const handleEdit = () => {
    setEditForm({ nom: data.nom, prenom: data.prenom, email: data.email, telephone: data.telephone });
    setIsEditing(true);
    setFieldErrors({});
    setServerError(null);
  };

  const handleCancelEdit = () => {
    setIsEditing(false);
    setEditForm({});
    setFieldErrors({});
    setServerError(null);
  };

  const validateEditForm = (): Partial<Record<keyof SuperviseurUpdateDTO, string>> => {
    const errors: Partial<Record<keyof SuperviseurUpdateDTO, string>> = {};
    if (editForm.nom !== undefined && !editForm.nom.trim()) errors.nom = 'Le nom est requis';
    if (editForm.prenom !== undefined && !editForm.prenom.trim()) errors.prenom = 'Le prénom est requis';
    if (editForm.email !== undefined) {
      if (!editForm.email.trim()) errors.email = "L'email est requis";
      else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(editForm.email)) errors.email = "Format d'email invalide";
    }
    return errors;
  };

  const handleSaveEdit = async () => {
    const errors = validateEditForm();
    if (Object.keys(errors).length > 0) { setFieldErrors(errors); return; }
    setActionLoading(true);
    setServerError(null);
    const result = await update(id, editForm);
    setActionLoading(false);
    if (result) { setIsEditing(false); onRefresh(); }
  };

  const handleToggleActif = async () => {
    setActionId(id);
    setServerError(null);
    setSuccessMessage(null);
    const success = data.actif ? await deactivate(id) : await activate(id);
    if (success) onRefresh();
    setActionId(null);
  };

  const handleResendActivation = async () => {
    setResendLoading(true);
    setServerError(null);
    setSuccessMessage(null);
    const result = await resendActivation(id);
    setResendLoading(false);
    if (result) {
      setUpdatedData(result);
      setSuccessMessage("Un nouveau lien d'activation a été envoyé par email avec succès.");
      onRefresh();
    }
  };

  return (
    <div className="flex h-full flex-col">
      {/* ── Scrollable Content ── */}
      <div className="flex-1 overflow-auto">
        <div className="flex flex-col gap-4 p-6">
          {/* Back button */}
          <button
            onClick={onBack}
            className="flex items-center gap-1.5 rounded-xl px-2.5 py-1.5 text-xs font-semibold text-muted-foreground hover:bg-muted transition-colors w-fit"
          >
            <ArrowLeft className="h-3.5 w-3.5" />
            Retour
          </button>

          {serverError && <ErrorBanner message={serverError} />}
          {successMessage && <SuccessBanner message={successMessage} />}

          {/* ── Profile Header ── */}
          <div className="flex flex-col items-center gap-3 pt-2">
            {/* Avatar */}
            <div className="relative">
              <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-primary/20 text-xl font-extrabold text-primary ring-4 ring-primary/10">
                {initials || <User className="h-7 w-7" />}
              </div>
            </div>

            <div className="text-center">
              <h2 className="text-xl font-bold tracking-tight">
                {data.prenom} {data.nom}
              </h2>
              <p className="mt-0.5 text-sm text-muted-foreground">{data.email}</p>
            </div>

            {/* Status pills */}
            <div className="flex items-center gap-2">
              <StatusBadgeSingle actif={data.actif} compteActive={data.compteActive} />
            </div>
          </div>

          {/* ── Info Fields / Edit Form ── */}
          {isEditing ? (
            <div className="rounded-2xl border border-border bg-muted/20 p-4">
              <h3 className="mb-4 text-sm font-bold">Modifier le profil</h3>
              <div className="flex flex-col gap-3">
                {[
                  { key: 'nom' as keyof SuperviseurUpdateDTO, label: 'Nom', type: 'text', placeholder: 'Dupont' },
                  { key: 'prenom' as keyof SuperviseurUpdateDTO, label: 'Prénom', type: 'text', placeholder: 'Jean' },
                  { key: 'email' as keyof SuperviseurUpdateDTO, label: 'Email', type: 'email', placeholder: 'jean@example.com' },
                  { key: 'telephone' as keyof SuperviseurUpdateDTO, label: 'Téléphone', type: 'tel', placeholder: '+33 6 12 34 56 78' },
                ].map(({ key, label, type, placeholder }) => (
                  <div key={key}>
                    <label className="block text-xs font-semibold uppercase tracking-wider text-muted-foreground mb-1.5">
                      {label}
                    </label>
                    <input
                      type={type}
                      value={(editForm[key] as string) || ''}
                      onChange={(e) => setEditForm({ ...editForm, [key]: e.target.value })}
                      disabled={actionLoading}
                      placeholder={placeholder}
                      className={
                        'w-full rounded-xl px-3.5 py-2.5 text-sm font-medium outline-none transition-shadow ' +
                        'bg-[color:var(--surface)] shadow-[var(--shadow-neu-inset)] ' +
                        'focus:ring-2 ring-primary ' +
                        (fieldErrors[key] ? 'ring-2 ring-[color:var(--danger)]' : '')
                      }
                    />
                    <FieldError message={fieldErrors[key]} />
                  </div>
                ))}
              </div>

              <div className="mt-4 flex items-center justify-end gap-2">
                <button
                  onClick={handleCancelEdit}
                  disabled={actionLoading}
                  className="flex items-center gap-1.5 rounded-xl px-4 py-2 text-sm font-semibold text-muted-foreground hover:bg-muted disabled:opacity-60 transition-colors"
                >
                  <X className="h-3.5 w-3.5" />
                  Annuler
                </button>
                <button
                  onClick={handleSaveEdit}
                  disabled={actionLoading}
                  className="flex items-center gap-1.5 rounded-xl bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground hover:opacity-90 disabled:opacity-60 shadow-[var(--shadow-glow)] transition-all"
                >
                  {actionLoading ? (
                    <Loader2 className="h-3.5 w-3.5 animate-spin" />
                  ) : (
                    <Save className="h-3.5 w-3.5" />
                  )}
                  Enregistrer
                </button>
              </div>
            </div>
          ) : (
            <>
              {/* Info Cards */}
              <div className="rounded-2xl border border-border bg-muted/20 p-4">
                <div className="mb-3 flex items-center justify-between">
                  <h3 className="text-sm font-bold">Informations</h3>
                  <button
                    onClick={handleEdit}
                    className="flex items-center gap-1.5 rounded-xl px-3 py-1.5 text-xs font-semibold text-muted-foreground hover:bg-muted hover:text-foreground transition-colors"
                  >
                    <Edit2 className="h-3.5 w-3.5" />
                    Modifier
                  </button>
                </div>

                <div className="flex flex-col gap-3">
                  <InfoRow icon={<User className="h-3.5 w-3.5" />} label="Nom complet" value={`${data.prenom} ${data.nom}`} />
                  <InfoRow icon={<Mail className="h-3.5 w-3.5" />} label="Email" value={data.email} />
                  <InfoRow icon={<Phone className="h-3.5 w-3.5" />} label="Téléphone" value={data.telephone || '—'} />
                  <InfoRow icon={<Calendar className="h-3.5 w-3.5" />} label="Créé le" value={formatDate(data.createdAt)} />
                </div>
              </div>

              {/* Account control */}
              <div className="rounded-2xl border border-border bg-muted/20 p-4">
                <h3 className="mb-3 text-sm font-bold">Contrôle du compte</h3>
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm font-medium">Accès à l'application</p>
                    <p className="text-xs text-muted-foreground mt-0.5">
                      {data.actif
                        ? 'Ce superviseur peut se connecter.'
                        : 'Ce superviseur ne peut pas se connecter.'}
                    </p>
                  </div>
                  <AlertDialog>
                    <AlertDialogTrigger asChild>
                      <div className="flex items-center gap-2 cursor-pointer">
                        <Switch
                          checked={data.actif}
                          disabled={actionId === id}
                          aria-label={data.actif ? 'Désactiver' : 'Activer'}
                          className="data-[state=checked]:bg-orange-600"
                        />
                        {actionId === id && <Loader2 className="h-3.5 w-3.5 animate-spin" />}
                      </div>
                    </AlertDialogTrigger>
                    <AlertDialogContent className="rounded-2xl">
                      <AlertDialogHeader>
                        <AlertDialogTitle className="flex items-center gap-2">
                          {data.actif ? (
                            <>
                              <PowerOff className="h-5 w-5 text-[color:var(--danger)]" />
                              Désactiver le compte
                            </>
                          ) : (
                            <>
                              <Power className="h-5 w-5 text-primary" />
                              Activer le compte
                            </>
                          )}
                        </AlertDialogTitle>
                        <AlertDialogDescription>
                          {data.actif
                            ? `Êtes-vous sûr de vouloir désactiver le compte de ${data.prenom} ${data.nom} ? L'utilisateur ne pourra plus se connecter.`
                            : `Êtes-vous sûr de vouloir activer le compte de ${data.prenom} ${data.nom} ?`}
                        </AlertDialogDescription>
                      </AlertDialogHeader>
                      <AlertDialogFooter>
                        <AlertDialogCancel className="rounded-xl">Annuler</AlertDialogCancel>
                        <AlertDialogAction
                          onClick={handleToggleActif}
                          className="rounded-xl bg-primary text-primary-foreground hover:opacity-90"
                        >
                          {data.actif ? 'Désactiver' : 'Activer'}
                        </AlertDialogAction>
                      </AlertDialogFooter>
                    </AlertDialogContent>
                  </AlertDialog>
                </div>
              </div>

              {/* Section Activation du compte (si compte_active == false) */}
              {!data.compteActive && (
                <div className="rounded-2xl border border-amber-500/30 bg-amber-500/5 p-4">
                  <div className="flex items-center justify-between mb-3">
                    <div className="flex items-center gap-2">
                      <Mail className="h-4 w-4 text-amber-500" />
                      <h3 className="text-sm font-bold text-amber-600 dark:text-amber-400">
                        Activation du compte
                      </h3>
                    </div>
                    <span className="rounded-full bg-amber-500/15 px-2.5 py-0.5 text-xs font-semibold text-amber-500">
                      En attente
                    </span>
                  </div>

                  {(() => {
                    const expirationDate = data.dateExpirationActivation ? new Date(data.dateExpirationActivation) : null;
                    const isExpired = !expirationDate || expirationDate < new Date();

                    return (
                      <div className="flex flex-col gap-3">
                        <p className="text-xs text-muted-foreground leading-relaxed">
                          {isExpired ? (
                            <span className="text-[color:var(--danger)] font-medium">
                              Le lien d'activation précédent a expiré (dépassé 24h). Vous pouvez renvoyer un nouveau lien.
                            </span>
                          ) : (
                            <span>
                              Le lien d'activation actuel est encore valide jusqu'au{' '}
                              <strong className="text-foreground">{formatDate(data.dateExpirationActivation ?? null)}</strong>.
                            </span>
                          )}
                        </p>

                        <button
                          onClick={handleResendActivation}
                          disabled={!isExpired || resendLoading}
                          className="flex items-center justify-center gap-2 rounded-xl bg-primary px-4 py-2.5 text-xs font-semibold text-primary-foreground hover:opacity-90 disabled:opacity-50 disabled:cursor-not-allowed shadow-[var(--shadow-glow)] transition-all w-full sm:w-auto"
                        >
                          {resendLoading ? (
                            <Loader2 className="h-4 w-4 animate-spin" />
                          ) : (
                            <Mail className="h-4 w-4" />
                          )}
                          Renvoyer le lien d'activation
                        </button>
                      </div>
                    );
                  })()}
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
}

function InfoRow({
  icon,
  label,
  value,
}: {
  icon: React.ReactNode;
  label: string;
  value: string;
}) {
  return (
    <div className="flex items-start gap-3">
      <div className="mt-0.5 flex h-6 w-6 shrink-0 items-center justify-center rounded-lg bg-muted/60 text-muted-foreground">
        {icon}
      </div>
      <div className="min-w-0 flex-1">
        <p className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
          {label}
        </p>
        <p className="mt-0.5 truncate text-sm font-medium text-foreground">{value}</p>
      </div>
    </div>
  );
}
