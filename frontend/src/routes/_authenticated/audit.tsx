import { createFileRoute } from '@tanstack/react-router';
import { useEffect, useState } from 'react';
import { Shield, ShieldAlert } from 'lucide-react';
import { AuditFiltersSection } from '@/components/audit/AuditFiltersSection';
import { AuditTable } from '@/components/audit/AuditTable';
import { useAuditLogs } from '@/hooks/useAuditLogs';
import { useAuth } from '@/hooks/useAuth';
import { listSuperviseurs, type SuperviseurListItemDTO } from '@/api/admin/superviseurs';

// ── Route — guard inline identique au pattern /superviseurs et /plc ───────────

export const Route = createFileRoute('/_authenticated/audit')({
  component: AuditPage,
});

// ── Page ──────────────────────────────────────────────────────────────────────

function AuditPage() {
  const { isAdmin } = useAuth();

  if (!isAdmin) {
    return (
      <div className="flex min-h-[60vh] items-center justify-center px-4">
        <div className="neu-card max-w-md p-10 text-center">
          <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-[color:var(--danger-soft)]">
            <ShieldAlert className="h-7 w-7 text-[color:var(--danger)]" />
          </div>
          <h2 className="text-xl font-bold text-foreground">Accès refusé</h2>
          <p className="mt-2 text-sm text-muted-foreground">
            Cette page est réservée aux administrateurs.
          </p>
        </div>
      </div>
    );
  }

  return <AuditContent />;
}

// ── Contenu ───────────────────────────────────────────────────────────────────

function AuditContent() {
  const {
    logs,
    loading,
    error,
    page,
    pageSize,
    totalPages,
    totalElements,
    filters,
    setFilters,
    setPage,
    setPageSize,
  } = useAuditLogs();

  const [superviseurs, setSuperviseurs] = useState<SuperviseurListItemDTO[]>([]);

  useEffect(() => {
    listSuperviseurs({ size: 200, inclureAdmin: true })
      .then((data) => setSuperviseurs(data.content ?? []))
      .catch((e) => console.error('[AuditPage] Erreur chargement superviseurs:', e));
  }, []);

  return (
    <div className="space-y-6">
      {/* En-tête */}
      <div className="flex items-center gap-3">
        <div className="neu-pressable flex h-10 w-10 items-center justify-center rounded-2xl bg-primary/10">
          <Shield className="h-5 w-5 text-primary" />
        </div>
        <div>
          <h2 className="text-xl font-bold tracking-tight">Journal d'audit</h2>
          <p className="text-sm text-muted-foreground">
            Historique complet des actions sensibles effectuées sur la plateforme.
          </p>
        </div>
      </div>

      {/* Filtres */}
      <AuditFiltersSection
        filters={filters}
        onChange={setFilters}
        totalElements={totalElements}
        superviseurs={superviseurs}
      />

      {/* Tableau paginé */}
      <AuditTable
        logs={logs}
        loading={loading}
        error={error}
        page={page}
        pageSize={pageSize}
        totalPages={totalPages}
        totalElements={totalElements}
        onPageChange={setPage}
        onPageSizeChange={setPageSize}
      />
    </div>
  );
}
