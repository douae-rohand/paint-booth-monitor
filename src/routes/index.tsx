import { createFileRoute } from "@tanstack/react-router";
import { useMemo, useState } from "react";
import {
  Area,
  AreaChart,
  CartesianGrid,
  Legend,
  Line,
  LineChart,
  ReferenceLine,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import {
  AlertTriangle,
  Calendar as CalendarIcon,
} from "lucide-react";
import { Calendar } from "@/components/ui/calendar";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { format, differenceInCalendarDays } from "date-fns";
import { fr } from "date-fns/locale";
import type { DateRange } from "react-day-picker";
import {
  METRIC_LABELS,
  METRIC_SHORT,
  METRIC_UNIT,
  THRESHOLD,
  aggregateTrend,
  generateHeatmap,
  generateTrend,
  type Metric,
} from "@/lib/mock-data";

export const Route = createFileRoute("/")({
  component: Dashboard,
});

const RANGES = [
  { key: "7", label: "7j", days: 7 },
  { key: "30", label: "30j", days: 30 },
  { key: "90", label: "3 mois", days: 90 },
  { key: "180", label: "6 mois", days: 180 },
  { key: "365", label: "1 an", days: 365 },
];

const COLORS: Record<Metric, string> = {
  m1: "var(--chart-1)",
  m2: "var(--chart-2)",
  m3: "var(--chart-3)",
};

function Dashboard() {
  const [range, setRange] = useState("30");
  const [customRange, setCustomRange] = useState<DateRange | undefined>();
  const [unit, setUnit] = useState<"day" | "month" | "year">("day");
  const [selected, setSelected] = useState<Metric[]>(["m1"]);

  const days = useMemo(() => {
    if (range === "custom" && customRange?.from) {
      const end = customRange.to ?? customRange.from;
      return Math.max(1, differenceInCalendarDays(end, customRange.from) + 1);
    }
    return Number(RANGES.find((r) => r.key === range)?.days ?? 30);
  }, [range, customRange]);

  const points = useMemo(() => generateTrend(days), [days]);
  const chartData = useMemo(() => aggregateTrend(points, unit), [points, unit]);
  const heatmap = useMemo(() => generateHeatmap(), []);

  const toggleMetric = (m: Metric) => {
    setSelected((cur) =>
      cur.includes(m) ? cur.filter((x) => x !== m) || cur : [...cur, m],
    );
  };
  const showAll = () => setSelected(["m1", "m2", "m3"]);

  const active = selected.length === 0 ? (["m1"] as Metric[]) : selected;

  const topAlerts = [
    { name: "Cabine A — COV", count: 14, pct: 92 },
    { name: "Cabine C — Débit d'air", count: 11, pct: 74 },
    { name: "Cabine B — Température", count: 8, pct: 55 },
    { name: "Cabine D — COV", count: 5, pct: 34 },
  ];

  const realtime = [
    { name: "Cabine A", state: "danger", label: "COV élevé", value: "93 ppm" },
    { name: "Cabine B", state: "warning", label: "T° limite", value: "78°C" },
    { name: "Cabine C", state: "success", label: "Nominal", value: "68 m³/h" },
    { name: "Cabine D", state: "success", label: "Nominal", value: "72 m³/h" },
    { name: "Cabine E", state: "warning", label: "Débit bas", value: "58 m³/h" },
  ];

  return (
    <div className="space-y-6">

      {/* Chart + right widgets */}
      <div className="grid grid-cols-1 gap-5 xl:grid-cols-3">
        <div className="neu-card p-6 xl:col-span-2">
          <div className="flex flex-wrap items-start justify-between gap-4">
            <div>
              <h2 className="text-lg font-bold">Évolution des métriques</h2>
              <p className="text-sm text-muted-foreground">
                courbes lissées
              </p>
            </div>
            <div className="flex flex-wrap items-center gap-2">
              <div className="neu-inset flex gap-1 rounded-2xl p-1">
                {RANGES.map((r) => (
                  <button
                    key={r.key}
                    onClick={() => setRange(r.key)}
                    className={
                      "rounded-xl px-3 py-1.5 text-xs font-semibold transition-all " +
                      (range === r.key
                        ? "bg-primary text-primary-foreground"
                        : "text-muted-foreground hover:text-foreground")
                    }
                  >
                    {r.label}
                  </button>
                ))}
              </div>
              <Popover>
                <PopoverTrigger asChild>
                  <button
                    onClick={() => setRange("custom")}
                    className={
                      "flex items-center gap-2 rounded-2xl px-3 py-1.5 text-xs font-semibold transition-all " +
                      (range === "custom"
                        ? "bg-primary text-primary-foreground"
                        : "neu-inset text-muted-foreground hover:text-foreground")
                    }
                  >
                    <CalendarIcon className="h-3.5 w-3.5" />
                    {range === "custom" && customRange?.from
                      ? customRange.to
                        ? `${format(customRange.from, "d MMM", { locale: fr })} — ${format(customRange.to, "d MMM", { locale: fr })}`
                        : format(customRange.from, "d MMM yyyy", { locale: fr })
                      : "Personnalisé"}
                  </button>
                </PopoverTrigger>
                <PopoverContent className="w-auto p-0" align="end">
                  <Calendar
                    mode="range"
                    selected={customRange}
                    onSelect={(r) => {
                      setCustomRange(r);
                      if (r?.from) setRange("custom");
                    }}
                    locale={fr}
                    numberOfMonths={2}
                    className="p-3 pointer-events-auto"
                  />
                </PopoverContent>
              </Popover>
            </div>

          </div>

          <div className="mt-4 flex flex-wrap items-center gap-2">
            {(Object.keys(METRIC_LABELS) as Metric[]).map((m) => {
              const on = selected.includes(m);
              return (
                <button
                  key={m}
                  onClick={() => toggleMetric(m)}
                  className={
                    "flex items-center gap-2 rounded-full px-3 py-1.5 text-xs font-semibold transition-all " +
                    (on
                      ? "neu-pressable text-foreground"
                      : "text-muted-foreground hover:text-foreground")
                  }
                >
                  <span
                    className="h-2.5 w-2.5 rounded-full"
                    style={{ background: COLORS[m] }}
                  />
                  {METRIC_SHORT[m]}
                </button>
              );
            })}
            <button
              onClick={showAll}
              className="ml-1 rounded-full bg-primary px-3 py-1.5 text-xs font-semibold text-primary-foreground"
            >
              Tout afficher
            </button>
            <div className="ml-auto neu-inset flex gap-1 rounded-2xl p-1">
              {(["day", "month", "year"] as const).map((u) => (
                <button
                  key={u}
                  onClick={() => setUnit(u)}
                  className={
                    "rounded-xl px-3 py-1.5 text-xs font-semibold capitalize transition-all " +
                    (unit === u
                      ? "bg-primary text-primary-foreground"
                      : "text-muted-foreground hover:text-foreground")
                  }
                >
                  {u === "day" ? "Jour" : u === "month" ? "Mois" : "Année"}
                </button>
              ))}
            </div>
          </div>

          <div className="mt-6 h-[340px] w-full">
            <ResponsiveContainer width="100%" height="100%">
              {active.length === 1 ? (
                <AreaChart data={chartData} margin={{ top: 10, right: 10, left: -10, bottom: 0 }}>
                  <defs>
                    <linearGradient id="grad" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="0%" stopColor={COLORS[active[0]]} stopOpacity={0.55} />
                      <stop offset="100%" stopColor={COLORS[active[0]]} stopOpacity={0.05} />
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" vertical={false} />
                  <XAxis dataKey="date" stroke="var(--muted-foreground)" fontSize={11} tickLine={false} axisLine={false} />
                  <YAxis stroke="var(--muted-foreground)" fontSize={11} tickLine={false} axisLine={false} />
                  <Tooltip content={<ChartTip />} />
                  <ReferenceLine y={THRESHOLD} stroke="var(--danger)" strokeDasharray="6 6" label={{ value: `Seuil ${THRESHOLD}`, fill: "var(--danger)", fontSize: 11, position: "insideTopRight" }} />
                  <Area
                    type="monotone"
                    dataKey={active[0]}
                    stroke={COLORS[active[0]]}
                    strokeWidth={2.5}
                    fill="url(#grad)"
                    name={METRIC_SHORT[active[0]]}
                  />
                </AreaChart>
              ) : (
                <LineChart data={chartData} margin={{ top: 10, right: 10, left: -10, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" vertical={false} />
                  <XAxis dataKey="date" stroke="var(--muted-foreground)" fontSize={11} tickLine={false} axisLine={false} />
                  <YAxis stroke="var(--muted-foreground)" fontSize={11} tickLine={false} axisLine={false} />
                  <Tooltip content={<ChartTip />} />
                  <Legend wrapperStyle={{ fontSize: 12 }} />
                  <ReferenceLine y={THRESHOLD} stroke="var(--danger)" strokeDasharray="6 6" />
                  {active.map((m) => (
                    <Line
                      key={m}
                      type="monotone"
                      dataKey={m}
                      stroke={COLORS[m]}
                      strokeWidth={2.5}
                      dot={false}
                      name={METRIC_SHORT[m]}
                    />
                  ))}
                </LineChart>
              )}
            </ResponsiveContainer>
          </div>
        </div>

        {/* Realtime status */}
        <div className="neu-card p-6">
          <div className="flex items-center justify-between">
            <h3 className="text-base font-bold">Statut temps réel</h3>
            <span className="text-xs text-muted-foreground">Live</span>
          </div>
          <ul className="mt-4 space-y-3">
            {realtime.map((r) => {
              const color =
                r.state === "danger"
                  ? "var(--danger)"
                  : r.state === "warning"
                    ? "var(--warning)"
                    : "var(--success)";
              return (
                <li
                  key={r.name}
                  className="flex items-center justify-between rounded-2xl px-3 py-2.5"
                  style={{ boxShadow: "var(--shadow-neu-inset)" }}
                >
                  <div className="flex items-center gap-3">
                    <span
                      className={
                        "h-2.5 w-2.5 rounded-full " + (r.state === "danger" ? "pulse-dot" : "")
                      }
                      style={{ background: color }}
                    />
                    <div>
                      <p className="text-sm font-semibold">{r.name}</p>
                      <p className="text-xs text-muted-foreground">{r.label}</p>
                    </div>
                  </div>
                  <span className="text-sm font-semibold" style={{ color }}>
                    {r.value}
                  </span>
                </li>
              );
            })}
          </ul>
        </div>
      </div>

      {/* Top alerts + heatmap */}
      <div className="grid grid-cols-1 gap-5 lg:grid-cols-2">
        <div className="neu-card p-6">
          <h3 className="text-base font-bold">Top métriques en alerte</h3>
          <p className="text-sm text-muted-foreground">
            Métriques ayant dépassé leur seuil récemment
          </p>
          <ul className="mt-5 space-y-4">
            {topAlerts.map((a, idx) => (
              <li key={a.name}>
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <span className="flex h-7 w-7 items-center justify-center rounded-xl bg-primary/35 text-xs font-bold text-primary">
                      {idx + 1}
                    </span>
                    <p className="text-sm font-semibold">{a.name}</p>
                  </div>
                  <span className="text-sm font-bold text-[color:var(--danger)]">
                    {a.count} dép.
                  </span>
                </div>
                <div className="neu-inset mt-2 h-2 overflow-hidden rounded-full">
                  <div
                    className="h-full rounded-full"
                    style={{
                      width: `${a.pct}%`,
                      background:
                        "linear-gradient(90deg, var(--primary), var(--danger))",
                    }}
                  />
                </div>
              </li>
            ))}
          </ul>
        </div>

        <div className="neu-card p-6">
          <div className="flex items-center justify-between">
            <h3 className="text-base font-bold">Heatmap</h3>
            <span className="text-xs text-muted-foreground">
              {new Date().toLocaleDateString("fr-FR", { month: "long", year: "numeric" })}
            </span>
          </div>
          <div className="mt-4 grid grid-cols-7 gap-2 text-center text-[10px] text-muted-foreground">
            {["L", "M", "M", "J", "V", "S", "D"].map((d, i) => (
              <div key={i} className="font-semibold">
                {d}
              </div>
            ))}
            {(() => {
              if (!heatmap.length) return null;
              const first = heatmap[0].date.getDay();
              const pad = (first + 6) % 7; // Monday first
              return Array.from({ length: pad }).map((_, i) => (
                <div key={`pad-${i}`} />
              ));
            })()}
            {heatmap.map((d) => {
              const intensity = Math.min(d.count / 7, 1);
              const bg =
                d.count === 0
                  ? "var(--surface)"
                  : `color-mix(in oklab, var(--primary) ${35 + intensity * 65}%, white)`;
              return (
                <div
                  key={d.date.toISOString()}
                  title={`${d.date.getDate()} — ${d.count} dépassements`}
                  className="aspect-square rounded-lg text-[10px] font-semibold flex items-center justify-center"
                  style={{
                    background: bg,
                    color:
                      intensity > 0.5 ? "var(--primary-foreground)" : "var(--muted-foreground)",
                    boxShadow:
                      d.count === 0
                        ? "var(--shadow-neu-inset)"
                        : "var(--shadow-neu-sm)",
                  }}
                >
                  {d.date.getDate()}
                </div>
              );
            })}
          </div>
          <div className="mt-4 flex items-center justify-between text-xs text-muted-foreground">
            <span>Moins</span>
            <div className="flex gap-1">
              {[0.3, 0.5, 0.7, 0.85, 0.98].map((v) => (
                <div
                  key={v}
                  className="h-3 w-6 rounded"
                  style={{
                    background: `color-mix(in oklab, var(--primary) ${v * 100}%, white)`,
                  }}
                />
              ))}
            </div>
            <span>Plus</span>
          </div>
        </div>
      </div>
    </div>
  );
}

function ChartTip({ active, payload, label }: any) {
  if (!active || !payload || !payload.length) return null;
  return (
    <div className="neu-card-sm rounded-2xl border border-border/60 bg-[color:var(--surface-raised)] px-3 py-2 text-xs">
      <p className="font-semibold">{label}</p>
      {payload.map((p: any) => (
        <div key={p.dataKey} className="mt-1 flex items-center gap-2">
          <span className="h-2 w-2 rounded-full" style={{ background: p.color }} />
          <span className="text-muted-foreground">{p.name}:</span>
          <span className="font-semibold">
            {p.value} {METRIC_UNIT[p.dataKey as Metric]}
          </span>
        </div>
      ))}
    </div>
  );
}
