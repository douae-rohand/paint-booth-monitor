import { Alert, AlertDescription } from '@/components/ui/alert';
import { RefreshCw } from 'lucide-react';
import { Button } from '@/components/ui/button';

interface NewDataBannerProps {
  count: number;
  onRefresh: () => void;
}

/**
 * Bandeau de notification pour les nouvelles mesures disponibles.
 * Affiche un compteur et un bouton de rafraîchissement.
 */
export const NewDataBanner = ({ count, onRefresh }: NewDataBannerProps) => {
  return (
    <Alert className="mb-4 border-primary/50 bg-primary/5">
      <div className="flex items-center justify-between">
        <AlertDescription className="flex items-center gap-2">
          <span className="font-medium">
            {count} nouvelle{count > 1 ? 's' : ''} mesure{count > 1 ? 's' : ''} disponible{count > 1 ? 's' : ''}
          </span>
          <span className="text-muted-foreground">- Cliquez pour rafraîchir</span>
        </AlertDescription>
        <Button
          size="sm"
          variant="ghost"
          onClick={onRefresh}
          className="h-8 gap-2"
        >
          <RefreshCw className="h-4 w-4" />
          Rafraîchir
        </Button>
      </div>
    </Alert>
  );
};

export default NewDataBanner;
