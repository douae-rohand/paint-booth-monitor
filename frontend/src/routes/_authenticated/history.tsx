import { createFileRoute } from "@tanstack/react-router";
import { useMemo, useState } from "react";
import {
  AlertTriangle,
  Calendar as CalendarIcon,
  ChevronLeft,
  ChevronRight,
  Search,
} from "lucide-react";
import { Calendar } from "@/components/ui/calendar";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Switch } from "@/components/ui/switch";
import {
  METRIC_LABELS,
  METRIC_UNIT,
  THRESHOLD,
  CABINS,
  generateHistory,
  type HistoryRow,
} from "@/lib/mock-data";
import { cn } from "@/lib/utils";
import { format } from "date-fns";
import { fr } from "date-fns/locale";
import type { DateRange } from "react-day-picker";

export const Route = createFileRoute("/_authenticated/history")({
  head: () => ({
    meta: [
      { title: "Historique des métriques — Supervision" },
      { name: "description", content: "Historique détaillé des métriques des cabines de peinture avec filtres et alertes de seuil." },
    ],
  }),
  component: HistoryPage,
});

function HistoryPage() {
  const allRows = useMemo(() => generateHistory(180), []);

  const [searchQuery, setSearchQuery] = useState("");
  const [year, setYear] = useState<string>(String(new Date().getFullYear()));
  const [mode, setMode] = useState<"exact" | "range">("range");
  const [exactDate, setExactDate] = useState<Date | undefined>();
  const [range, setRange] = useState<DateRange | undefined>();
  const [onlyExceed, setOnlyExceed] = useState(false);
  const [cabin, setCabin] = useState<string>("all");
  const [pageSize, setPageSize] = useState(10);
  const [page, setPage] = useState(1);

  const years = useMemo(() => {
    const set = new Set(allRows.map((r) => r.date.getFullYear()));
    return Array.from(set).sort((a, b) => b - a);
  }, [allRows]);

  const filtered = useMemo(() => {
    return allRows.filter((r) => {
      if (year !== "all" && r.date.getFullYear() !== Number(year)) return false;
      if (mode === "exact" && exactDate) {
        if (
          r.date.getFullYear() !== exactDate.getFullYear() ||
          r.date.getMonth() !== exactDate.getMonth() ||
          r.date.getDate() !== exactDate.getDate()
        )
          return false;
      }
      if (mode === "range" && range?.from) {
        const start = new Date(range.from);
        start.setHours(0, 0, 0, 0);
        const end = new Date(range.to ?? range.from);
        end.setHours(23, 59, 59, 999);
        if (r.date < start || r.date > end) return false;
      }
      if (cabin !== "all" && r.cabin !== cabin) return false;
      if (onlyExceed) {
        if (r.m1 <= THRESHOLD && r.m2 <= THRESHOLD) return false;
      }
      if (searchQuery.trim() !== "") {
        if (!r.caisseId.toLowerCase().includes(searchQuery.toLowerCase().trim()))
          return false;
      }
      return true;
    });
  }, [allRows, year, mode, exactDate, range, cabin, onlyExceed, searchQuery]);

  const totalPages = Math.max(1, Math.ceil(filtered.length / pageSize));
  const currentPage = Math.min(page, totalPages);
  const pageRows = filtered.slice((currentPage - 1) * pageSize, currentPage * pageSize);

  return (
    <div className="space-y-6">
      <div className="neu-card p-6">
        <div className="flex items-center justify-between gap-4">
          <div>
            <h2 className="text-lg font-bold">Historique des métriques</h2>
            <p className="text-sm text-muted-foreground">
              {filtered.length} enregistrements
            </p>
          </div>
        </div>

        {/* Filters and search section */}
        <div className="mt-5 space-y-4">
          {/* Row 1: Search + Only Exceed Toggle */}
          <div className="flex flex-wrap items-center justify-between gap-4">
            {/* Caisse ID Search */}
            <div className="neu-inset flex flex-1 max-w-md items-center gap-2 px-4 h-10 rounded-2xl">
              <Search className="h-4 w-4 text-muted-foreground" />
              <input
                type="text"
                placeholder="Rechercher une caisse par son ID (ex: CAT-0003)..."
                value={searchQuery}
                onChange={(e) => {
                  setSearchQuery(e.target.value);
                  setPage(1);
                }}
                className="w-full border-0 bg-transparent p-0 text-sm outline-none placeholder:text-muted-foreground focus:ring-0"
              />
            </div>

            {/* Only exceed */}
            <div className="neu-pressable flex items-center gap-3 rounded-2xl px-4 h-10">
              <AlertTriangle className="h-4 w-4 text-[color:var(--danger)]" />
              <span className="text-xs font-semibold">Seulement les dépassements</span>
              <Switch checked={onlyExceed} onCheckedChange={(v) => { setOnlyExceed(v); setPage(1); }} />
            </div>
          </div>

          {/* Row 2: Standard Selectors and Date Filters */}
          <div className="flex flex-wrap items-center gap-3">
            {/* Year */}
            <div className="neu-inset rounded-2xl px-1">
              <Select value={year} onValueChange={(v) => { setYear(v); setPage(1); }}>
                <SelectTrigger className="h-10 w-[130px] border-0 bg-transparent shadow-none focus:ring-0">
                  <SelectValue placeholder="Année" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">Toutes années</SelectItem>
                  {years.map((y) => (
                    <SelectItem key={y} value={String(y)}>
                      {y}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            {/* Mode toggle */}
            <div className="neu-inset flex gap-1 rounded-2xl p-1 h-10 items-center">
              <button
                onClick={() => setMode("exact")}
                className={cn(
                  "rounded-xl px-3 py-1.5 text-xs font-semibold transition-all h-8 flex items-center",
                  mode === "exact"
                    ? "bg-primary text-primary-foreground"
                    : "text-muted-foreground hover:text-foreground",
                )}
              >
                Date exacte
              </button>
              <button
                onClick={() => setMode("range")}
                className={cn(
                  "rounded-xl px-3 py-1.5 text-xs font-semibold transition-all h-8 flex items-center",
                  mode === "range"
                    ? "bg-primary text-primary-foreground"
                    : "text-muted-foreground hover:text-foreground",
                )}
              >
                Plage
              </button>
            </div>

            {/* Date picker */}
            <Popover>
              <PopoverTrigger asChild>
                <button className="neu-pressable flex h-10 items-center gap-2 rounded-2xl px-4 text-sm">
                  <CalendarIcon className="h-4 w-4 text-primary" />
                  {mode === "exact"
                    ? exactDate
                      ? format(exactDate, "d MMM yyyy", { locale: fr })
                      : "Choisir une date"
                    : range?.from
                      ? range.to
                        ? `${format(range.from, "d MMM", { locale: fr })} — ${format(range.to, "d MMM yyyy", { locale: fr })}`
                        : format(range.from, "d MMM yyyy", { locale: fr })
                      : "Choisir une plage"}
                </button>
              </PopoverTrigger>
              <PopoverContent className="w-auto p-0" align="start">
                {mode === "exact" ? (
                  <Calendar
                    mode="single"
                    selected={exactDate}
                    onSelect={(d) => { setExactDate(d); setPage(1); }}
                    locale={fr}
                    className={cn("p-3 pointer-events-auto")}
                  />
                ) : (
                  <Calendar
                    mode="range"
                    selected={range}
                    onSelect={(r) => { setRange(r); setPage(1); }}
                    locale={fr}
                    numberOfMonths={2}
                    className={cn("p-3 pointer-events-auto")}
                  />
                )}
              </PopoverContent>
            </Popover>

            {(exactDate || range) && (
              <button
                onClick={() => { setExactDate(undefined); setRange(undefined); setPage(1); }}
                className="text-xs font-semibold text-muted-foreground underline-offset-4 hover:underline"
              >
                Réinitialiser
              </button>
            )}

            {/* Cabin filter */}
            <div className="neu-inset rounded-2xl px-1">
              <Select value={cabin} onValueChange={(v) => { setCabin(v); setPage(1); }}>
                <SelectTrigger className="h-10 w-[140px] border-0 bg-transparent shadow-none focus:ring-0">
                  <SelectValue placeholder="Cabine" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">Toutes cabines</SelectItem>
                  {CABINS.map((c) => (
                    <SelectItem key={c} value={c}>Cabine {c}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>
        </div>

        {/* Table */}
        <div className="mt-6 overflow-hidden rounded-2xl border border-border">
          <div className="max-h-[560px] overflow-auto">
            <table className="w-full border-collapse text-sm">
              <thead className="sticky top-0 z-10 bg-[color:var(--surface-raised)] backdrop-blur">
                <tr className="text-left">
                  <Th>Caisse ID</Th>
                  <Th>Cabine</Th>
                  <Th>Date</Th>
                  <Th>Heure</Th>
                  <Th>{METRIC_LABELS.m1}</Th>
                  <Th>{METRIC_LABELS.m2}</Th>
                </tr>
              </thead>
              <tbody>
                {pageRows.length === 0 && (
                  <tr>
                    <td
                      colSpan={6}
                      className="px-6 py-12 text-center text-sm text-muted-foreground"
                    >
                      Aucun enregistrement pour ces filtres.
                    </td>
                  </tr>
                )}
                {pageRows.map((row, i) => (
                  <Row row={row} striped={i % 2 === 1} key={row.id} />
                ))}
              </tbody>
            </table>
          </div>
        </div>

        {/* Pagination */}
        <div className="mt-5 flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-2 text-sm text-muted-foreground">
            <span>Lignes par page</span>
            <div className="neu-inset rounded-xl">
              <Select
                value={String(pageSize)}
                onValueChange={(v) => { setPageSize(Number(v)); setPage(1); }}
              >
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
            <span className="ml-3">
              Page {currentPage} sur {totalPages}
            </span>
          </div>

          <div className="neu-inset flex items-center gap-1 rounded-2xl p-1">
            <PageBtn onClick={() => setPage((p) => Math.max(1, p - 1))} disabled={currentPage === 1}>
              <ChevronLeft className="h-4 w-4" />
            </PageBtn>
            {buildPages(currentPage, totalPages).map((p, i) =>
              p === "…" ? (
                <span key={i} className="px-2 text-xs text-muted-foreground">…</span>
              ) : (
                <button
                  key={i}
                  onClick={() => setPage(p as number)}
                  className={cn(
                    "h-8 min-w-8 rounded-xl px-2 text-xs font-semibold transition-all",
                    currentPage === p
                      ? "bg-primary text-primary-foreground"
                      : "text-muted-foreground hover:text-foreground",
                  )}
                >
                  {p}
                </button>
              ),
            )}
            <PageBtn
              onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
              disabled={currentPage === totalPages}
            >
              <ChevronRight className="h-4 w-4" />
            </PageBtn>
          </div>
        </div>
      </div>
    </div>
  );
}

function Th({ children }: { children: React.ReactNode }) {
  return (
    <th className="border-b border-border/60 px-5 py-3 text-left text-xs font-bold uppercase tracking-wider text-muted-foreground">
      {children}
    </th>
  );
}

function Row({ row, striped }: { row: HistoryRow; striped: boolean }) {
  return (
    <tr
      className={cn(
        "transition-colors hover:bg-primary/5",
        striped ? "bg-[color:oklch(0.98_0.005_90)]" : "bg-transparent",
      )}
    >
      <td className="px-5 py-3 font-mono text-xs text-muted-foreground">{row.caisseId}</td>
      <td className="px-5 py-3">
        <span className="inline-flex items-center gap-1 rounded-lg bg-primary/10 px-2.5 py-1 text-xs font-bold text-primary">
          Cabine {row.cabin}
        </span>
      </td>
      <td className="px-5 py-3 font-medium">
        {row.date.toLocaleDateString("fr-FR", { day: "2-digit", month: "short", year: "numeric" })}
      </td>
      <td className="px-5 py-3 text-muted-foreground">
        {row.date.toLocaleTimeString("fr-FR", { hour: "2-digit", minute: "2-digit" })}
      </td>
      <Cell value={row.m1} unit={METRIC_UNIT.m1} />
      <Cell value={row.m2} unit={METRIC_UNIT.m2} />
    </tr>
  );
}

function Cell({ value, unit }: { value: number; unit: string }) {
  const alert = value > THRESHOLD;
  return (
    <td className="px-5 py-3">
      <span
        className={cn(
          "inline-flex items-center gap-1.5 rounded-xl px-2.5 py-1 font-semibold tabular-nums",
          alert
            ? "bg-[color:var(--danger-soft)] text-[color:oklch(0.38_0.18_25)]"
            : "text-foreground",
        )}
      >
        {alert && <AlertTriangle className="h-3.5 w-3.5" />}
        {value} <span className="text-xs font-normal opacity-70">{unit}</span>
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
