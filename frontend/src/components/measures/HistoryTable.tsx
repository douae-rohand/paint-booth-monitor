import { AlertTriangle, ChevronLeft, ChevronRight } from "lucide-react";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { cn } from "@/lib/utils";
import type { HistoryRow, Metrique, Zone } from "@/lib/mock-data";
import { METRIC_LABELS, METRIC_UNIT, SEUILS_ABSOLUS } from "@/lib/mock-data";

export interface HistoryTableProps {
  data: HistoryRow[];
  typePoint: "CABINE" | "ETUVE";
  pageSize: number;
  currentPage: number;
  onPageChange: (page: number) => void;
  onPageSizeChange: (size: number) => void;
}

export function HistoryTable({
  data,
  typePoint,
  pageSize,
  currentPage,
  onPageChange,
  onPageSizeChange,
}: HistoryTableProps) {
  const totalPages = Math.max(1, Math.ceil(data.length / pageSize));
  const current = Math.min(currentPage, totalPages);
  const pageRows = data.slice((current - 1) * pageSize, current * pageSize);

  const isExceeding = (value: number, metrique: Metrique) => {
    if (typePoint === "CABINE") {
      const seuils = SEUILS_ABSOLUS.CABINE[metrique];
      if (!seuils) return false;
      return value < seuils.min || value > seuils.max;
    } else {
      // ETUVE n'a que TEMPERATURE
      if (metrique === "HUMIDITE") return false;
      const seuils = SEUILS_ABSOLUS.ETUVE.TEMPERATURE;
      if (!seuils) return false;
      return value < seuils.min || value > seuils.max;
    }
  };

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
              {pageRows.length === 0 && (
                <tr>
                  <td
                    colSpan={typePoint === "CABINE" ? 5 : 5}
                    className="px-6 py-16 text-center text-sm text-muted-foreground"
                  >
                    <div className="flex flex-col items-center gap-2">
                      <span className="text-4xl opacity-20">📋</span>
                      <span>Aucun enregistrement pour ces filtres</span>
                    </div>
                  </td>
                </tr>
              )}
              {pageRows.map((row, i) => (
                <Row row={row} typePoint={typePoint} striped={i % 2 === 1} key={row.id} isExceeding={isExceeding} />
              ))}
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
            Page {current} <span className="text-muted-foreground/60">sur</span> {totalPages}
          </span>
        </div>

        <div className="neu-inset flex items-center gap-1 rounded-2xl p-1.5">
          <PageBtn onClick={() => onPageChange(Math.max(1, current - 1))} disabled={current === 1}>
            <ChevronLeft className="h-4 w-4" />
          </PageBtn>
          {buildPages(current, totalPages).map((p, i) =>
            p === "…" ? (
              <span key={i} className="px-2 text-xs text-muted-foreground/60">…</span>
            ) : (
              <button
                key={i}
                onClick={() => onPageChange(p as number)}
                className={cn(
                  "h-8 min-w-8 rounded-xl px-2 text-xs font-semibold transition-all",
                  current === p
                    ? "bg-primary text-primary-foreground shadow-sm"
                    : "text-muted-foreground hover:bg-primary/10 hover:text-foreground",
                )}
              >
                {p}
              </button>
            ),
          )}
          <PageBtn onClick={() => onPageChange(Math.min(totalPages, current + 1))} disabled={current === totalPages}>
            <ChevronRight className="h-4 w-4" />
          </PageBtn>
        </div>
      </div>
    </div>
  );
}

function Th({ children }: { children: React.ReactNode }) {
  return (
    <th className="border-b border-border/60 px-5 py-3.5 text-left text-xs font-bold uppercase tracking-wider text-muted-foreground">
      {children}
    </th>
  );
}

function Row({ row, typePoint, striped, isExceeding }: { row: HistoryRow; typePoint: "CABINE" | "ETUVE"; striped: boolean; isExceeding: (value: number, metrique: Metrique) => boolean }) {
  return (
    <tr
      className={cn(
        "transition-colors hover:bg-primary/5",
        striped ? "bg-[color:var(--surface-muted)]/30" : "bg-transparent",
      )}
    >
      <td className="px-5 py-3.5 font-medium text-foreground">
        {row.date.toLocaleDateString("fr-FR", { day: "2-digit", month: "short", year: "numeric" })}
      </td>
      <td className="px-5 py-3.5 text-muted-foreground">
        {row.date.toLocaleTimeString("fr-FR", { hour: "2-digit", minute: "2-digit" })}
      </td>
      <td className="px-5 py-3.5 font-mono text-xs text-muted-foreground/80">{row.caisseId}</td>
      {typePoint === "ETUVE" && (
        <td className="px-5 py-3.5">
          <span className="inline-flex items-center gap-1.5 rounded-lg bg-primary/10 px-2.5 py-1 text-xs font-bold text-primary">
            {row.zone}
          </span>
        </td>
      )}
      {row.temperature !== undefined && (
        <Cell value={row.temperature} unit={METRIC_UNIT.TEMPERATURE} metrique="TEMPERATURE" isExceeding={isExceeding} />
      )}
      {typePoint === "CABINE" && row.humidite !== undefined && (
        <Cell value={row.humidite} unit={METRIC_UNIT.HUMIDITE} metrique="HUMIDITE" isExceeding={isExceeding} />
      )}
    </tr>
  );
}

function Cell({ value, unit, metrique, isExceeding }: { value: number; unit: string; metrique: Metrique; isExceeding: (value: number, metrique: Metrique) => boolean }) {
  const alert = isExceeding(value, metrique);
  return (
    <td className="px-5 py-3.5">
      <span
        className={cn(
          "inline-flex items-center gap-2 rounded-xl px-3 py-1.5 font-semibold tabular-nums text-sm transition-all",
          alert
            ? "bg-[color:var(--danger-soft)] text-[color:oklch(0.38_0.18_25)] shadow-sm"
            : "bg-[color:var(--surface-muted)]/50 text-foreground",
        )}
      >
        {alert && <AlertTriangle className="h-4 w-4" />}
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
