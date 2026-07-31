import { Loader2 } from 'lucide-react';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { usePointMesures } from '@/hooks/useSeuils';
import { type PointMesure, type Metrique } from '@/api/alerting/seuils';

interface PointMesureMetriqueSelectorProps {
  selectedPointMesure: PointMesure | null;
  selectedMetrique: Metrique | null;
  onPointMesureChange: (point: PointMesure | null) => void;
  onMetriqueChange: (metrique: Metrique | null) => void;
  variant?: 'card' | 'inline';
}

export function PointMesureMetriqueSelector({
  selectedPointMesure,
  selectedMetrique,
  onPointMesureChange,
  onMetriqueChange,
  variant = 'card',
}: PointMesureMetriqueSelectorProps) {
  const { data: pointMesures, loading: loadingPoints } = usePointMesures();

  // Filter metrics based on selected point type (ETUVE only has TEMPERATURE)
  const availableMetriques: Metrique[] = selectedPointMesure?.typeEmplacement === 'ETUVE'
    ? ['TEMPERATURE']
    : ['TEMPERATURE', 'HUMIDITE'];

  if (variant === 'inline') {
    return (
      <div className="neu-inset flex gap-1 rounded-2xl p-1 items-center h-9">
        {/* Point de mesure selector */}
        <Select
          value={selectedPointMesure?.id.toString() ?? ''}
          onValueChange={(value) => {
            const point = pointMesures.find((p) => p.id.toString() === value);
            onPointMesureChange(point ?? null);
            if (point?.typeEmplacement === 'ETUVE' && selectedMetrique === 'HUMIDITE') {
              onMetriqueChange('TEMPERATURE');
            }
          }}
          disabled={loadingPoints}
        >
          <SelectTrigger className="flex items-center gap-1.5 rounded-xl px-3 py-1.5 h-full text-xs font-semibold border-0 shadow-none bg-transparent hover:text-foreground text-muted-foreground focus:ring-0 focus:ring-offset-0 pointer-events-auto cursor-pointer">
            {loadingPoints ? (
              <div className="flex items-center gap-1">
                <Loader2 className="h-3 w-3 animate-spin" />
                <span>Chargement...</span>
              </div>
            ) : (
              <span className="max-w-[120px] truncate">
                {selectedPointMesure ? selectedPointMesure.nom : 'Sélectionner un point'}
              </span>
            )}
          </SelectTrigger>
          <SelectContent>
            {pointMesures.map((point) => (
              <SelectItem key={point.id} value={point.id.toString()}>
                {point.nom}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>

        <span className="self-center text-muted-foreground/30 text-xs select-none">|</span>

        {/* Métrique selector */}
        <Select
          value={selectedMetrique ?? ''}
          onValueChange={(value) => onMetriqueChange(value as Metrique)}
          disabled={!selectedPointMesure}
        >
          <SelectTrigger className="flex items-center gap-1.5 rounded-xl px-3 py-1.5 h-full text-xs font-semibold border-0 shadow-none bg-transparent hover:text-foreground text-muted-foreground focus:ring-0 focus:ring-offset-0 pointer-events-auto cursor-pointer">
            <span className="max-w-[100px] truncate">
              {selectedMetrique
                ? selectedMetrique === 'TEMPERATURE'
                  ? 'Température'
                  : 'Humidité'
                : 'Métrique'}
            </span>
          </SelectTrigger>
          <SelectContent>
            {availableMetriques.map((metrique) => (
              <SelectItem key={metrique} value={metrique}>
                {metrique === 'TEMPERATURE' ? 'Température' : 'Humidité'}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>
    );
  }

  return (
    <div className="neu-card p-6">
      <div className="flex flex-wrap items-center gap-4 sm:gap-6">
        {/* Point de mesure selector */}
        <div className="flex-1 min-w-[200px]">
          <label className="block text-xs font-semibold uppercase tracking-wider text-muted-foreground mb-1.5">
            Point de mesure
          </label>
          <Select
            value={selectedPointMesure?.id.toString() ?? ''}
            onValueChange={(value) => {
              const point = pointMesures.find((p) => p.id.toString() === value);
              onPointMesureChange(point ?? null);
              if (point?.typeEmplacement === 'ETUVE' && selectedMetrique === 'HUMIDITE') {
                onMetriqueChange('TEMPERATURE');
              }
            }}
            disabled={loadingPoints}
          >
            <SelectTrigger className="w-full bg-[color:var(--surface)] shadow-[var(--shadow-neu-inset)]">
              {loadingPoints ? (
                <div className="flex items-center gap-2">
                  <Loader2 className="h-4 w-4 animate-spin" />
                  <span>Chargement...</span>
                </div>
              ) : (
                <SelectValue placeholder="Sélectionner un point" />
              )}
            </SelectTrigger>
            <SelectContent>
              {pointMesures.map((point) => (
                <SelectItem key={point.id} value={point.id.toString()}>
                  {point.nom}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        {/* Métrique selector */}
        <div className="flex-1 min-w-[150px]">
          <label className="block text-xs font-semibold uppercase tracking-wider text-muted-foreground mb-1.5">
            Métrique
          </label>
          <Select
            value={selectedMetrique ?? ''}
            onValueChange={(value) => onMetriqueChange(value as Metrique)}
            disabled={!selectedPointMesure}
          >
            <SelectTrigger className="w-full bg-[color:var(--surface)] shadow-[var(--shadow-neu-inset)]">
              <SelectValue placeholder="Sélectionner" />
            </SelectTrigger>
            <SelectContent>
              {availableMetriques.map((metrique) => (
                <SelectItem key={metrique} value={metrique}>
                  {metrique === 'TEMPERATURE' ? 'Température' : 'Humidité'}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      </div>

      {/* Empty state hint */}
      {!selectedPointMesure || !selectedMetrique ? (
        <div className="mt-4 text-center text-sm text-muted-foreground">
          Sélectionnez un point de mesure et une métrique pour afficher les seuils
        </div>
      ) : null}
    </div>
  );
}
