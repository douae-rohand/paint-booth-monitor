import { useState } from 'react';
import { FileText, Loader2, Calendar as CalendarIcon, AlertCircle, CheckCircle2 } from 'lucide-react';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { usePointMesures } from '@/hooks/useSeuils';
import { genererRapport, type TypeRapport, type RapportPDFResponse } from '@/api/reports';
import { cn } from '@/lib/utils';
import { format, subDays, subHours } from 'date-fns';

interface GenerationRapportFormProps {
  onRapportGenere: (rapport: RapportPDFResponse) => void;
}

type PeriodMode = 'JOURNALIER' | 'HEBDOMADAIRE' | 'MENSUEL' | 'PERSONNALISE';

export function GenerationRapportForm({ onRapportGenere }: GenerationRapportFormProps) {
  const { data: pointMesures, loading: loadingPoints } = usePointMesures();

  const [selectedPointId, setSelectedPointId] = useState<string>('');
  const [periodMode, setPeriodMode] = useState<PeriodMode>('JOURNALIER');

  // Dates personnalisées (format YYYY-MM-DDTHH:mm pour datetime-local input)
  const defaultEnd = new Date();
  const defaultStart = subDays(defaultEnd, 1);

  const formatForInput = (d: Date) => {
    const pad = (n: number) => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
  };

  const [customDateDebut, setCustomDateDebut] = useState<string>(formatForInput(defaultStart));
  const [customDateFin, setCustomDateFin] = useState<string>(formatForInput(defaultEnd));

  const [generating, setGenerating] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const calculateDatesForMode = (mode: PeriodMode): { dateDebut: Date; dateFin: Date; typeRapport: TypeRapport } => {
    const now = new Date();
    if (mode === 'JOURNALIER') {
      return { dateDebut: subHours(now, 24), dateFin: now, typeRapport: 'JOURNALIER' };
    }
    if (mode === 'HEBDOMADAIRE') {
      return { dateDebut: subDays(now, 7), dateFin: now, typeRapport: 'HEBDOMADAIRE' };
    }
    if (mode === 'MENSUEL') {
      return { dateDebut: subDays(now, 30), dateFin: now, typeRapport: 'MENSUEL' };
    }
    // Personnalisé
    return {
      dateDebut: new Date(customDateDebut),
      dateFin: new Date(customDateFin),
      typeRapport: 'PERSONNALISE',
    };
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);
    setSuccessMessage(null);

    if (!selectedPointId) {
      setErrorMessage('Veuillez sélectionner un point de mesure.');
      return;
    }

    const pointIdNum = Number(selectedPointId);
    if (isNaN(pointIdNum) || pointIdNum <= 0) {
      setErrorMessage('Point de mesure invalide.');
      return;
    }

    const { dateDebut, dateFin, typeRapport } = calculateDatesForMode(periodMode);

    if (isNaN(dateDebut.getTime()) || isNaN(dateFin.getTime())) {
      setErrorMessage('Période invalide : veuillez sélectionner des dates valides.');
      return;
    }

    if (dateDebut >= dateFin) {
      setErrorMessage('La date de début doit être antérieure à la date de fin.');
      return;
    }

    try {
      setGenerating(true);
      const requestData = {
        idPointMesure: pointIdNum,
        dateDebut: dateDebut.toISOString().substring(0, 19), // YYYY-MM-DDTHH:mm:ss
        dateFin: dateFin.toISOString().substring(0, 19),
        typeRapport,
      };

      const rapport = await genererRapport(requestData);

      if (rapport.statutGeneration === 'ECHEC') {
        setErrorMessage('La génération du rapport a échoué côté serveur.');
      } else {
        setSuccessMessage(`Rapport généré avec succès (${rapport.nomFichier ?? 'Nouveau rapport'})`);
        onRapportGenere(rapport);
      }
    } catch (err: any) {
      console.error('Erreur lors de la génération du rapport :', err);
      const apiMsg = err?.response?.data?.message || 'Erreur lors de la génération du rapport PDF.';
      setErrorMessage(apiMsg);
    } finally {
      setGenerating(false);
    }
  };

  return (
    <div className="neu-card p-6 flex flex-col gap-5">
      <div className="flex items-center gap-3 border-b border-border/40 pb-4">
        <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary/15">
          <FileText className="h-5 w-5 text-primary" />
        </div>
        <div>
          <h3 className="text-base font-bold">Générer un rapport PDF</h3>
          <p className="text-xs text-muted-foreground">
            Sélectionnez un point de mesure et une période pour générer un rapport consolidé
          </p>
        </div>
      </div>

      <form onSubmit={handleSubmit} className="flex flex-col gap-5">
        {/* Point de mesure */}
        <div className="space-y-2">
          <label className="text-xs font-bold uppercase tracking-wider text-muted-foreground">
            Point de mesure
          </label>
          <div className="neu-inset rounded-2xl p-1">
            <Select
              value={selectedPointId}
              onValueChange={setSelectedPointId}
              disabled={loadingPoints || generating}
            >
              <SelectTrigger className="h-10 w-full border-0 bg-transparent shadow-none focus:ring-0">
                {loadingPoints ? (
                  <div className="flex items-center gap-2 text-xs text-muted-foreground">
                    <Loader2 className="h-4 w-4 animate-spin text-primary" />
                    <span>Chargement des points...</span>
                  </div>
                ) : (
                  <SelectValue placeholder="Choisir un point de mesure" />
                )}
              </SelectTrigger>
              <SelectContent>
                {pointMesures.map((pm) => (
                  <SelectItem key={pm.id} value={pm.id.toString()}>
                    {pm.nom} ({pm.typeEmplacement})
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        </div>

        {/* Mode de période */}
        <div className="space-y-2">
          <label className="text-xs font-bold uppercase tracking-wider text-muted-foreground">
            Période du rapport
          </label>

          <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 neu-inset p-1.5 rounded-2xl">
            {(
              [
                { id: 'JOURNALIER', label: 'Journalier' },
                { id: 'HEBDOMADAIRE', label: 'Hebdomadaire' },
                { id: 'MENSUEL', label: 'Mensuel' },
                { id: 'PERSONNALISE', label: 'Personnalisé' },
              ] as const
            ).map((mode) => (
              <button
                key={mode.id}
                type="button"
                disabled={generating}
                onClick={() => setPeriodMode(mode.id)}
                className={cn(
                  'rounded-xl py-2 px-3 text-xs font-semibold transition-all duration-200 text-center',
                  periodMode === mode.id
                    ? 'bg-primary text-primary-foreground shadow-sm'
                    : 'text-muted-foreground hover:text-foreground hover:bg-muted/40',
                )}
              >
                {mode.label}
              </button>
            ))}
          </div>
        </div>

        {/* Période personnalisée inputs */}
        {periodMode === 'PERSONNALISE' && (
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 animate-in fade-in slide-in-from-top-1 duration-200">
            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-muted-foreground flex items-center gap-1.5">
                <CalendarIcon className="h-3.5 w-3.5 text-primary" />
                Date de début
              </label>
              <div className="neu-inset rounded-2xl px-3 py-1.5">
                <input
                  type="datetime-local"
                  value={customDateDebut}
                  onChange={(e) => setCustomDateDebut(e.target.value)}
                  disabled={generating}
                  className="w-full border-0 bg-transparent text-xs font-medium focus:outline-none focus:ring-0"
                />
              </div>
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-muted-foreground flex items-center gap-1.5">
                <CalendarIcon className="h-3.5 w-3.5 text-primary" />
                Date de fin
              </label>
              <div className="neu-inset rounded-2xl px-3 py-1.5">
                <input
                  type="datetime-local"
                  value={customDateFin}
                  onChange={(e) => setCustomDateFin(e.target.value)}
                  disabled={generating}
                  className="w-full border-0 bg-transparent text-xs font-medium focus:outline-none focus:ring-0"
                />
              </div>
            </div>
          </div>
        )}

        {/* Alerte Erreur */}
        {errorMessage && (
          <div className="flex items-center gap-2.5 rounded-2xl bg-destructive/10 border border-destructive/20 p-3.5 text-xs text-destructive animate-in fade-in duration-200">
            <AlertCircle className="h-4 w-4 shrink-0" />
            <span>{errorMessage}</span>
          </div>
        )}

        {/* Alerte Succès */}
        {successMessage && (
          <div className="flex items-center gap-2.5 rounded-2xl bg-emerald-500/10 border border-emerald-500/20 p-3.5 text-xs text-emerald-600 dark:text-emerald-400 animate-in fade-in duration-200">
            <CheckCircle2 className="h-4 w-4 shrink-0" />
            <span>{successMessage}</span>
          </div>
        )}

        {/* Bouton de soumission */}
        <button
          type="submit"
          disabled={generating || !selectedPointId}
          className={cn(
            'flex items-center justify-center gap-2 rounded-2xl py-3 px-5 text-sm font-bold transition-all duration-200',
            generating || !selectedPointId
              ? 'bg-muted text-muted-foreground cursor-not-allowed opacity-70'
              : 'bg-primary text-primary-foreground hover:opacity-90 shadow-md hover:shadow-lg',
          )}
        >
          {generating ? (
            <>
              <Loader2 className="h-4 w-4 animate-spin" />
              <span>Génération du PDF en cours...</span>
            </>
          ) : (
            <>
              <FileText className="h-4 w-4" />
              <span>Générer le rapport</span>
            </>
          )}
        </button>
      </form>
    </div>
  );
}
