import { createFileRoute } from '@tanstack/react-router';
import { useState } from 'react';
import { ShieldAlert, AlertTriangle } from 'lucide-react';
import { useAuth } from '@/hooks/useAuth';
import { PointMesureMetriqueSelector } from '@/components/alerting/PointMesureMetriqueSelector';
import { SeuilAbsoluSection } from '@/components/alerting/SeuilAbsoluSection';
import { SeuilDynamiqueSection } from '@/components/alerting/SeuilDynamiqueSection';
import { type PointMesure, type Metrique } from '@/api/alerting/seuils';

export const Route = createFileRoute('/_authenticated/seuils')({
  component: SeuilsPage,
});

function SeuilsPage() {
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

  return <SeuilsContent />;
}

function SeuilsContent() {
  const [selectedPointMesure, setSelectedPointMesure] = useState<PointMesure | null>(null);
  const [selectedMetrique, setSelectedMetrique] = useState<Metrique | null>(null);
  const [refreshKey, setRefreshKey] = useState(0);

  const handleRefresh = () => {
    setRefreshKey((prev) => prev + 1);
  };

  const isSelectionComplete = selectedPointMesure && selectedMetrique;

  return (
    <div className="space-y-6">
      {/* ── Header ── */}
      <div className="flex items-center gap-3">
        <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary/15">
          <AlertTriangle className="h-5 w-5 text-primary" />
        </div>
        <div>
          <h1 className="text-2xl font-bold text-foreground">Gestion des seuils</h1>
          <p className="text-sm text-muted-foreground">
            Configurez les seuils absolus et dynamiques pour chaque point de mesure
          </p>
        </div>
      </div>

      {/* ── Selector ── */}
      <PointMesureMetriqueSelector
        selectedPointMesure={selectedPointMesure}
        selectedMetrique={selectedMetrique}
        onPointMesureChange={setSelectedPointMesure}
        onMetriqueChange={setSelectedMetrique}
      />

      {/* ── Sections (only shown when selection is complete) ── */}
      {isSelectionComplete ? (
        <div className="space-y-6">
          <SeuilAbsoluSection
            key={`absolu-${refreshKey}-${selectedPointMesure.id}-${selectedMetrique}`}
            pointMesure={selectedPointMesure}
            metrique={selectedMetrique}
            onRefresh={handleRefresh}
          />
          <SeuilDynamiqueSection
            key={`dynamique-${refreshKey}-${selectedPointMesure.id}-${selectedMetrique}`}
            pointMesure={selectedPointMesure}
            metrique={selectedMetrique}
            onRefresh={handleRefresh}
          />
        </div>
      ) : null}
    </div>
  );
}
