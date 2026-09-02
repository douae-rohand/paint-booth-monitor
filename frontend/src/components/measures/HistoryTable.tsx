import { useMemo } from "react";
import { AlertTriangle, ChevronLeft, ChevronRight, Loader2 } from "lucide-react";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { cn, formatHeureAvecMillisecondes } from "@/lib/utils";

export type Metrique = "TEMPERATURE" | "HUMIDITE";

export const METRIC_LABELS: Record<Metrique, string> = {
  TEMPERATURE: "Température",
  HUMIDITE: "Humidité",
};

export const METRIC_UNIT: Record<Metrique, string> = {
  TEMPERATURE: "°C",
  HUMIDITE: "%",
};

import type { Page, MesureCabineDTO, MesureEtuveDTO } from "@/api/measures/index";

export interface HistoryTableProps {
  data: Page<MesureCabineDTO> | Page<MesureEtuveDTO> | null;
  typePoint: "CABINE" | "ETUVE";
  loading?: boolean;
  error?: Error | null;
  pageSize: number;
  currentPage: number;
  onPageChange: (page: number) => void;
  onPageSizeChange: (size: number) => void;
}

export function HistoryTable({
  data,
  typePoint,
  loading = false,
  error = null,
  pageSize,
  currentPage,
  onPageChange,
  onPageSizeChange,
}: HistoryTableProps) {
  const totalPages = data?.page?.totalPages ?? 1;
  const current = Math.min(currentPage, totalPages - 1);
  const pageRows = data?.content ?? [];

  // État de chargement initial
  if (loading && !data) {
    return (
      <div className="flex flex-col h-full">
        <div className="flex-1 flex items-center justify-center">
          <div className="flex flex-col items-center gap-3">
            <Loader2 className="h-8 w-8 animate-spin text-primary" />
            <span className="text-sm text-muted-foreground">Chargement...</span>
          </div>
        </div>
      </div>
    );
  }

  // État d'erreur
  if (error) {
    return (
      <div className="flex flex-col h-full">
        <div className="flex-1 flex items-center justify-center">
          <div className="flex flex-col items-center gap-3">
            <AlertTriangle className="h-8 w-8 text-destructive" />
            <span className="text-sm text-muted-foreground">
              {error.message || "Erreur lors du chargement des données"}
            </span>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="flex flex-col h-full">
      {/* Table */}
      <div className="flex-1 overflow-hidden rounded-2xl border border-border/60 bg-[color:var(--surface)]">
        <div className="h-full overflow-auto">
          <table className="w-full border-collapse text-sm">
            <thead className="sticky top-0 z-10 bg-[color:var(--surface-raised)] backdrop-blur-sm border-b border-border/60">
              <tr className="text-left">
                <Th>Date</Th>
                <Th>Heure</Th>
                <Th>Caisse ID</Th>
                {typePoint === "ETUVE" && <Th>Zone</Th>}
                <Th>{METRIC_LABELS.TEMPERATURE}</Th>
                {typePoint === "CABINE" && <Th>{METRIC_LABELS.HUMIDITE}</Th>}
              </tr>
            </thead>
            <tbody className="divide-y divide-border/40">
              {loading && pageRows.length === 0 ? (
                <tr>
                  <td
                    colSpan={typePoint === "CABINE" ? 5 : 5}
                    className="px-6 py-16 text-center text-sm text-muted-foreground"
                  >
                    <div className="flex flex-col items-center gap-2">
                      <Loader2 className="h-6 w-6 animate-spin text-primary" />
                      <span>Chargement...</span>
                    </div>
                  </td>
                </tr>
              ) : pageRows.length === 0 ? (
                <tr>
                  <td
                    colSpan={typePoint === "CABINE" ? 5 : 5}
                    className="px-6 py-16 text-center text-sm text-muted-foreground"
                  >
                    <div className="flex flex-col items-center gap-2">
                      <AlertTriangle className="h-6 w-6 text-muted-foreground/40" />
                      <span>Aucun enregistrement pour ces filtres</span>
                    </div>
                  </td>
                </tr>
              ) : (
                pageRows.map((row, i) => (
                  <Row row={row} typePoint={typePoint} striped={i % 2 === 1} key={getRowKey(row, i)} />
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Pagination */}
      <div className="mt-4 flex flex-wrap items-center justify-between gap-3 px-1">
        <div className="flex items-center gap-2 text-sm text-muted-foreground">
          <span className="font-medium">Lignes par page</span>
          <div className="neu-inset rounded-xl">
            <Select value={String(pageSize)} onValueChange={(v) => onPageSizeChange(Number(v))}>
              <SelectTrigger className="h-9 w-[80px] border-0 bg-transparent shadow-none focus:ring-0">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {[10, 25, 50].map((s) => (
                  <SelectItem key={s} value={String(s)}>
                    {s}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <span className="ml-2 font-medium">
            Page {current + 1} <span className="text-muted-foreground/60">sur</span> {totalPages}
          </span>
        </div>

        <div className="neu-inset flex items-center gap-1 rounded-2xl p-1.5">
          <PageBtn onClick={() => onPageChange(Math.max(0, current - 1))} disabled={current === 0}>
            <ChevronLeft className="h-4 w-4" />
          </PageBtn>
          {buildPages(current + 1, totalPages).map((p, i) =>
            p === "…" ? (
              <span key={i} className="px-2 text-xs text-muted-foreground/60">…</span>
            ) : (
              <button
                key={i}
                onClick={() => onPageChange((p as number) - 1)}
                className={cn(
                  "h-8 min-w-8 rounded-xl px-2 text-xs font-semibold transition-all",
                  current + 1 === p
                    ? "bg-primary text-primary-foreground shadow-sm"
                    : "text-muted-foreground hover:bg-primary/10 hover:text-foreground",
                )}
              >
                {p}
              </button>
            ),
          )}
          <PageBtn onClick={() => onPageChange(Math.min(totalPages - 1, current + 1))} disabled={current === totalPages - 1}>
            <ChevronRight className="h-4 w-4" />
          </PageBtn>
        </div>
      </div>
    </div>
  );
}

function getRowKey(row: MesureCabineDTO | MesureEtuveDTO, index: number): string {
  if ('timestampCycle' in row) {
    return `cabine-${row.timestampCycle}-${index}`;
  }
  return `etuve-${row.idMesure}-${index}`;
}

function Th({ children }: { children: React.ReactNode }) {
  return (
    <th className="border-b border-border/60 px-5 py-3.5 text-left text-xs font-bold uppercase tracking-wider text-muted-foreground">
      {children}
    </th>
  );
}

function Row({ row, typePoint, striped }: { row: MesureCabineDTO | MesureEtuveDTO; typePoint: "CABINE" | "ETUVE"; striped: boolean }) {
  const dateStr = typePoint === "CABINE" 
    ? (row as MesureCabineDTO).timestampCycle 
    : (row as MesureEtuveDTO).dateMesure;
  
  const date = new Date(dateStr);
  const caisseId = typePoint === "CABINE" 
    ? (row as MesureCabineDTO).caisseId 
    : null;
  
  const zone = typePoint === "ETUVE" 
    ? (row as MesureEtuveDTO).zone 
    : null;
  
  const temperature = typePoint === "CABINE" 
    ? (row as MesureCabineDTO).temperature 
    : (row as MesureEtuveDTO).temperature;
  
  const humidite = typePoint === "CABINE" 
    ? (row as MesureCabineDTO).humidite 
    : null;
  
  const depassementTemperature = typePoint === "CABINE" 
    ? (row as MesureCabineDTO).depassementTemperature 
    : (row as MesureEtuveDTO).depassement;
  
  const depassementHumidite = typePoint === "CABINE" 
    ? (row as MesureCabineDTO).depassementHumidite 
    : false;

  return (
    <tr
      className={cn(
        "transition-colors hover:bg-primary/5",
        striped ? "bg-[color:var(--surface-muted)]/30" : "bg-transparent",
      )}
    >
      <td className="px-5 py-3.5 font-medium text-foreground">
        {date.toLocaleDateString("fr-FR", { day: "2-digit", month: "short", year: "numeric" })}
      </td>
      <td className="px-5 py-3.5 text-muted-foreground">
        {formatHeureAvecMillisecondes(date)}
      </td>
      <td className="px-5 py-3.5 font-mono text-xs text-muted-foreground/80">
        {caisseId ?? "—"}
      </td>
      {typePoint === "ETUVE" && zone && (
        <td className="px-5 py-3.5">
          <span className="inline-flex items-center gap-1.5 rounded-lg bg-primary/10 px-2.5 py-1 text-xs font-bold text-primary">
            {zone}
          </span>
        </td>
      )}
      {temperature !== null && (
        <Cell value={temperature} unit={METRIC_UNIT.TEMPERATURE} depassement={depassementTemperature} />
      )}
      {typePoint === "CABINE" && humidite !== null && (
        <Cell value={humidite} unit={METRIC_UNIT.HUMIDITE} depassement={depassementHumidite} />
      )}
    </tr>
  );
}

function Cell({ value, unit, depassement }: { value: number; unit: string; depassement: boolean }) {
  return (
    <td className="px-5 py-3.5">
      <span
        className={cn(
          "inline-flex items-center gap-2 rounded-xl px-3 py-1.5 font-semibold tabular-nums text-sm transition-all",
          depassement
            ? "bg-[color:var(--danger-soft)] text-[color:oklch(0.38_0.18_25)] shadow-sm"
            : "bg-[color:var(--surface-muted)]/50 text-foreground",
        )}
      >
        {depassement && <AlertTriangle className="h-4 w-4" />}
        {value} <span className="text-xs font-normal opacity-70 ml-0.5">{unit}</span>
      </span>
    </td>
  );
}

function PageBtn({
  children,
  onClick,
  disabled,
}: {
  children: React.ReactNode;
  onClick: () => void;
  disabled?: boolean;
}) {
  return (
    <button
      onClick={onClick}
      disabled={disabled}
      className={cn(
        "flex h-8 w-8 items-center justify-center rounded-xl transition-all",
        disabled
          ? "cursor-not-allowed text-muted-foreground/40"
          : "text-muted-foreground hover:bg-primary/10 hover:text-primary",
      )}
    >
      {children}
    </button>
  );
}

function buildPages(current: number, total: number): (number | "…")[] {
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1);
  const pages: (number | "…")[] = [1];
  const start = Math.max(2, current - 1);
  const end = Math.min(total - 1, current + 1);
  if (start > 2) pages.push("…");
  for (let i = start; i <= end; i++) pages.push(i);
  if (end < total - 1) pages.push("…");
  pages.push(total);
  return pages;
}
