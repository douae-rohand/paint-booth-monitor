import { createFileRoute } from '@tanstack/react-router';
import { useState } from 'react';
import { ShieldAlert, Users, PlusCircle } from 'lucide-react';
import { useAuth } from '@/hooks/useAuth';
import { SuperviseurListe } from '@/components/admin/SuperviseurListe';
import { SuperviseurFormulaireCreation } from '@/components/admin/SuperviseurFormulaireCreation';
import { SuperviseurDetail } from '@/components/admin/SuperviseurDetail';

export const Route = createFileRoute('/_authenticated/superviseurs')({
  component: SuperviseursPage,
});

function SuperviseursPage() {
  const { isAdmin } = useAuth();
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [showCreate, setShowCreate] = useState(false);
  const [refreshKey, setRefreshKey] = useState(0);

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

  const handleRowClick = (id: string) => {
    setSelectedId(id);
    setShowCreate(false); // close form when opening detail
  };

  const handleRefresh = () => {
    setRefreshKey((prev) => prev + 1);
  };

  const handleCreateNew = () => {
    setShowCreate(true);
    setSelectedId(null); // close detail when opening form
  };

  const handleCreateSuccess = () => {
    setShowCreate(false);
    handleRefresh();
  };

  const handleCancelCreate = () => {
    setShowCreate(false);
  };

  const handleCloseDetail = () => {
    setSelectedId(null);
  };

  const rightPanelOpen = selectedId !== null || showCreate;

  return (
    <div className="flex h-[calc(100vh-4rem)] flex-col gap-4 p-3 pb-5">
      {/* ── Page Header ── */}
      <div className="flex items-center justify-between shrink-0">
        <div className="flex items-center gap-3">
          <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-primary/15 shadow-sm">
            <Users className="h-5 w-5 text-primary" />
          </div>
          <div>
            <h1 className="text-2xl font-bold tracking-tight">Gestion des superviseurs</h1>
            <p className="text-sm text-muted-foreground">
              Créez et gérez les comptes superviseurs de l'application.
            </p>
          </div>
        </div>
      </div>

      {/* ── Content – Split View ── */}
      <div className="flex flex-1 gap-4 overflow-visible p-1.5 min-h-0">
        {/* Left Panel – List */}
        <div
          className={`flex flex-col transition-all duration-300 ${
            rightPanelOpen ? 'w-[52%]' : 'w-full'
          }`}
        >
          <SuperviseurListe
            key={refreshKey}
            refreshKey={refreshKey}
            onRowClick={handleRowClick}
            onRefresh={handleRefresh}
            onCreateNew={handleCreateNew}
            activeDetailId={selectedId}
          />
        </div>

        {/* Right Panel – Detail OR Create Form (never both) */}
        {rightPanelOpen && (
          <div className="flex w-[48%] flex-col animate-in slide-in-from-right duration-300">
            {showCreate && (
              <SuperviseurFormulaireCreation
                onSuccess={handleCreateSuccess}
                onCancel={handleCancelCreate}
              />
            )}
            {selectedId && !showCreate && (
              <SuperviseurDetail
                id={selectedId}
                onBack={handleCloseDetail}
                onRefresh={handleRefresh}
                refreshKey={refreshKey}
              />
            )}
          </div>
        )}
      </div>
    </div>
  );
}
