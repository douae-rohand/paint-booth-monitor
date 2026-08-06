import { useState, useEffect } from 'react';
import { Loader2, AlertTriangle, Plus, X, User, Mail, Phone, CheckCircle2, ArrowLeft } from 'lucide-react';
import { useSuperviseurActions } from '@/hooks/useSuperviseurs';
import type { SuperviseurCreateDTO } from '@/api/admin/superviseurs';

function FieldError({ message }: { message?: string }) {
  if (!message) return null;
  return (
    <p className="mt-1.5 flex items-center gap-1 text-xs font-medium text-[color:var(--danger)]">
      <AlertTriangle className="h-3 w-3 shrink-0" />
      {message}
    </p>
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

interface SuperviseurFormulaireCreationProps {
  onSuccess: () => void;
  onCancel: () => void;
}

export function SuperviseurFormulaireCreation({ onSuccess, onCancel }: SuperviseurFormulaireCreationProps) {
  const [form, setForm] = useState<SuperviseurCreateDTO>({
    nom: '',
    prenom: '',
    email: '',
    telephone: '',
  });
  const [fieldErrors, setFieldErrors] = useState<Partial<Record<keyof SuperviseurCreateDTO, string>>>({});
  const [serverError, setServerError] = useState<string | null>(null);
  const { create, loading, error } = useSuperviseurActions();

  const fields: {
    id: keyof SuperviseurCreateDTO;
    label: string;
    type: string;
    placeholder: string;
    required: boolean;
    icon: React.ReactNode;
    hint?: string;
  }[] = [
    {
      id: 'nom',
      label: 'Nom',
      type: 'text',
      placeholder: 'Nom',
      required: true,
      icon: <User className="h-4 w-4" />,
    },
    {
      id: 'prenom',
      label: 'Prénom',
      type: 'text',
      placeholder: 'Prénom',
      required: true,
      icon: <User className="h-4 w-4" />,
    },
    {
      id: 'email',
      label: 'Email professionnel',
      type: 'email',
      placeholder: 'Nom.Prénom@exemple.com',
      required: true,
      icon: <Mail className="h-4 w-4" />,
      hint: 'Un lien d\'activation sera envoyé à cette adresse.',
    },
    {
      id: 'telephone',
      label: 'Téléphone',
      type: 'tel',
      placeholder: '+212 6 00 00 00 00',
      required: false,
      icon: <Phone className="h-4 w-4" />,
    },
  ];

  const handleChange = (id: keyof SuperviseurCreateDTO, value: string) => {
    setForm((prev) => ({ ...prev, [id]: value }));
    if (fieldErrors[id]) {
      setFieldErrors((prev) => { const next = { ...prev }; delete next[id]; return next; });
    }
  };

  const validateForm = (): Partial<Record<keyof SuperviseurCreateDTO, string>> => {
    const errors: Partial<Record<keyof SuperviseurCreateDTO, string>> = {};
    if (!form.nom.trim()) errors.nom = 'Le nom est requis';
    if (!form.prenom.trim()) errors.prenom = 'Le prénom est requis';
    if (!form.email.trim()) errors.email = "L'email est requis";
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) errors.email = "Format d'email invalide";
    return errors;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const errors = validateForm();
    if (Object.keys(errors).length > 0) { setFieldErrors(errors); return; }
    setFieldErrors({});
    setServerError(null);
    const result = await create(form);
    if (result) onSuccess();
  };

  useEffect(() => {
    if (error) setServerError(error);
  }, [error]);

  return (
    <div className="flex h-full flex-col">
      {/* ── Scrollable Form ── */}
      <div className="flex-1 overflow-auto">
        <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-0">
          <div className="flex flex-col gap-4 p-6">
            {/* Back button */}
            <button
              type="button"
              onClick={onCancel}
              className="flex items-center gap-1.5 rounded-xl px-2.5 py-1.5 text-xs font-semibold text-muted-foreground hover:bg-muted transition-colors w-fit"
            >
              <ArrowLeft className="h-3.5 w-3.5" />
              Retour
            </button>

            {serverError && <ErrorBanner message={serverError} />}

            {/* Step indicator */}
            <div className="flex items-center gap-2 rounded-2xl bg-primary/8 px-4 py-3">
              <CheckCircle2 className="h-4 w-4 shrink-0 text-primary" />
              <p className="text-xs text-primary font-medium">
                Un email d'activation sera automatiquement envoyé au superviseur après création.
              </p>
            </div>

            {/* Form Title */}
            <div className="flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary/15">
                <Plus className="h-5 w-5 text-primary" />
              </div>
              <div>
                <h3 className="text-lg font-bold">Nouveau superviseur</h3>
                <p className="text-xs text-muted-foreground">
                  Remplissez les informations ci-dessous.
                </p>
              </div>
            </div>

            {/* Fields – stacked vertically */}
            {fields.map((field) => (
              <div key={field.id}>
                <label
                  htmlFor={`superviseur-${field.id}`}
                  className="block text-xs font-semibold uppercase tracking-wider text-muted-foreground mb-1.5"
                >
                  {field.label}
                  {field.required && (
                    <span className="text-[color:var(--danger)] ml-1">*</span>
                  )}
                </label>

                <div className="relative">
                  <span className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-muted-foreground">
                    {field.icon}
                  </span>
                  <input
                    id={`superviseur-${field.id}`}
                    type={field.type}
                    placeholder={field.placeholder}
                    value={form[field.id]}
                    onChange={(e) => handleChange(field.id, e.target.value)}
                    disabled={loading}
                    className={
                      'w-full rounded-xl py-2.5 pl-10 pr-4 text-sm font-medium outline-none transition-shadow ' +
                      'bg-[color:var(--surface)] shadow-[var(--shadow-neu-inset)] ' +
                      'focus:ring-2 ring-primary disabled:opacity-60 ' +
                      (fieldErrors[field.id] ? 'ring-2 ring-[color:var(--danger)]' : '')
                    }
                  />
                </div>

                <FieldError message={fieldErrors[field.id]} />

                {field.hint && !fieldErrors[field.id] && (
                  <p className="mt-1 text-xs text-muted-foreground">{field.hint}</p>
                )}
              </div>
            ))}
          </div>

          {/* ── Footer Actions ── */}
          <div className="shrink-0 border-t border-border px-6 py-4 flex items-center justify-end gap-3 bg-muted/10">
            <button
              type="button"
              onClick={onCancel}
              disabled={loading}
              className="rounded-2xl border border-border px-5 py-2.5 text-sm font-semibold text-muted-foreground transition-colors hover:bg-muted disabled:opacity-60"
            >
              Annuler
            </button>
            <button
              type="submit"
              disabled={loading}
              className="flex items-center gap-2 rounded-2xl bg-primary px-5 py-2.5 text-sm font-semibold text-primary-foreground shadow-[var(--shadow-glow)] transition-all hover:opacity-90 disabled:opacity-60"
            >
              {loading ? (
                <>
                  <Loader2 className="h-4 w-4 animate-spin" />
                  Création en cours...
                </>
              ) : (
                <>
                  <Plus className="h-4 w-4" />
                  Créer le superviseur
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
