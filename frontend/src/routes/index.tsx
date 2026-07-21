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
  ChevronDown,
  Thermometer,
  Droplets,
  CheckCircle,
  ChevronLeft,
  ChevronRight,
} from "lucide-react";
import { Calendar } from "@/components/ui/calendar";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import {
  Tooltip as RadixTooltip,
  TooltipTrigger as RadixTooltipTrigger,
  TooltipContent as RadixTooltipContent,
  TooltipProvider as RadixTooltipProvider,
} from "@/components/ui/tooltip";
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
  generateHourlyTrend,
  generateRangeTrend,
  generateTrend,
  generateDetailedHeatmap,
  type Metric,
  type DetailedHeatmapDay,
  type HeatmapDetail,
} from "@/lib/mock-data";

export const Route = createFileRoute("/")({
  component: Dashboard,
});

const RANGES = [
  { key: "1", label: "24h", days: 1 },
  { key: "7", label: "7j", days: 7 },
  { key: "30", label: "30j", days: 30 },
  { key: "90", label: "3 mois", days: 90 },
  { key: "180", label: "6 mois", days: 180 },
  { key: "365", label: "1 an", days: 365 },
];

const COLORS: Record<Metric, string> = {
  m1: "var(--chart-1)",
  m2: "var(--chart-2)",
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

  const allowedUnits = useMemo(() => {
    if (days === 1) {
      return ["hour"] as const;
    }
    if (days <= 30) {
      return ["day"] as const;
    }
    if (days >= 730) {
      return ["day", "month", "year"] as const;
    }
    return ["day", "month"] as const;
  }, [days]);

  const activeUnit = useMemo(() => {
    if ((allowedUnits as readonly string[]).includes(unit)) {
      return unit;
    }
    return allowedUnits[0];
  }, [unit, allowedUnits]);

  const points = useMemo(() => {
    if (range === "1") {
      return generateHourlyTrend();
    }
    if (range === "custom" && customRange?.from) {
      if (days === 1) {
        return generateHourlyTrend(customRange.from);
      }
      return generateRangeTrend(customRange.from, customRange.to ?? customRange.from);
    }
    return generateTrend(days);
  }, [days, range, customRange]);

  const chartData = useMemo(() => {
    if (days === 1) {
      return points;
    }
    return aggregateTrend(points, activeUnit === "hour" ? "day" : activeUnit);
  }, [points, days, activeUnit]);
  // Heatmap upgrades
  const [currentMonthDate, setCurrentMonthDate] = useState(() => new Date());
  const [metricFilter, setMetricFilter] = useState<"all" | "Température" | "Pression">("all");

  const handlePrevMonth = () => {
    setCurrentMonthDate((d) => new Date(d.getFullYear(), d.getMonth() - 1, 1));
  };
  const handleNextMonth = () => {
    setCurrentMonthDate((d) => new Date(d.getFullYear(), d.getMonth() + 1, 1));
  };

  const heatmap = useMemo(() => {
    return generateDetailedHeatmap(currentMonthDate.getFullYear(), currentMonthDate.getMonth());
  }, [currentMonthDate]);

  const toggleMetric = (m: Metric) => {
    setSelected((cur) =>
      cur.includes(m) ? cur.filter((x) => x !== m) || cur : [...cur, m],
    );
  };
  const showAll = () => setSelected(["m1", "m2"]);

  const active = selected.length === 0 ? (["m1"] as Metric[]) : selected;

  const topAlerts = [
    { name: "Cabine A - Humidité", count: 14, pct: 92 },
    { name: "Cabine C - Température", count: 11, pct: 74 },
    { name: "Cabine B - Température", count: 8, pct: 55 },
    { name: "Cabine D - Humidité", count: 5, pct: 34 },
  ];

  const realtime = [
    { name: "Cabine A", state: "danger",  label: "T° élevée",  temp: 93,  humidity: 62 },
    { name: "Cabine B", state: "warning", label: "T° limite",  temp: 78,  humidity: 55 },
    { name: "Cabine C", state: "success", label: "Nominal",     temp: 64,  humidity: 48 },
    { name: "Cabine D", state: "success", label: "Nominal",     temp: 67,  humidity: 51 },
    { name: "Cabine E", state: "warning", label: "Humidité élevée", temp: 70, humidity: 82 },
  ];

  const [expandedCabin, setExpandedCabin] = useState<string | null>(null);
  const toggleCabin = (name: string) =>
    setExpandedCabin((prev) => (prev === name ? null : name));

  // Compute live KPIs
  const activeAlerts = useMemo(() => {
    return realtime.filter((r) => r.state !== "success").length;
  }, [realtime]);

  const avgTemp = useMemo(() => {
    return Math.round(realtime.reduce((acc, r) => acc + r.temp, 0) / realtime.length);
  }, [realtime]);

  const avgHumidity = useMemo(() => {
    return Math.round(realtime.reduce((acc, r) => acc + r.humidity, 0) / realtime.length);
  }, [realtime]);

  const successCount = useMemo(() => {
    return realtime.filter((r) => r.state === "success").length;
  }, [realtime]);

  const complianceRate = useMemo(() => {
    return Math.round((successCount / realtime.length) * 100);
  }, [successCount, realtime.length]);

  return (
    <div className="space-y-6">

      {/* KPI Section */}
      <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-4">
        {/* KPI 1: Alerts */}
        <div className="neu-card p-5 flex items-center justify-between">
          <div className="space-y-1">
            <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">Alertes Actives</p>
            <h3 className="text-2xl font-bold tracking-tight text-[color:var(--danger)]">{activeAlerts}</h3>
            <p className="text-xs text-muted-foreground">cabines nécessitant attention</p>
          </div>
          <div className="neu-pressable p-3 rounded-2xl">
            <AlertTriangle className="h-5 w-5 text-[color:var(--danger)]" />
          </div>
        </div>

        {/* KPI 2: Temp moyenne */}
        <div className="neu-card p-5 flex items-center justify-between">
          <div className="space-y-1">
            <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">Température Moyenne</p>
            <h3 className="text-2xl font-bold tracking-tight text-foreground">{avgTemp} °C</h3>
            <p className="text-xs text-muted-foreground">Consigne: &lt; {THRESHOLD} °C</p>
          </div>
          <div className="neu-pressable p-3 rounded-2xl">
            <Thermometer className="h-5 w-5 text-primary" />
          </div>
        </div>

        {/* KPI 3: Humidité moyenne */}
        <div className="neu-card p-5 flex items-center justify-between">
          <div className="space-y-1">
            <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">Humidité Moyenne</p>
            <h3 className="text-2xl font-bold tracking-tight text-foreground">{avgHumidity} %</h3>
            <p className="text-xs text-muted-foreground">Consigne: &lt; 70 %</p>
          </div>
          <div className="neu-pressable p-3 rounded-2xl">
            <Droplets className="h-5 w-5 text-chart-2" />
          </div>
        </div>

        {/* KPI 4: Conformité */}
        <div className="neu-card p-5 flex items-center justify-between">
          <div className="space-y-1">
            <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">Taux de Conformité</p>
            <h3 className="text-2xl font-bold tracking-tight text-foreground">{complianceRate} %</h3>
            <p className="text-xs text-muted-foreground">{successCount} cabines nominales sur {realtime.length}</p>
          </div>
          <div className="neu-pressable p-3 rounded-2xl">
            <CheckCircle className="h-5 w-5 text-chart-3" />
          </div>
        </div>
      </div>

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
              {days === 1 ? (
                <button
                  disabled
                  className="rounded-xl px-3 py-1.5 text-xs font-semibold bg-primary text-primary-foreground opacity-90 cursor-default"
                >
                  Heure
                </button>
              ) : days <= 30 ? (
                <button
                  disabled
                  className="rounded-xl px-3 py-1.5 text-xs font-semibold bg-primary text-primary-foreground opacity-90 cursor-default"
                >
                  Jour
                </button>
              ) : (
                (days >= 730
                  ? (["day", "month", "year"] as const)
                  : (["day", "month"] as const)
                ).map((u) => (
                  <button
                    key={u}
                    onClick={() => setUnit(u)}
                    className={
                      "rounded-xl px-3 py-1.5 text-xs font-semibold capitalize transition-all " +
                      (activeUnit === u
                        ? "bg-primary text-primary-foreground"
                        : "text-muted-foreground hover:text-foreground")
                    }
                  >
                    {u === "day" ? "Jour" : u === "month" ? "Mois" : "Année"}
                  </button>
                ))
              )}
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
          <ul className="mt-4 space-y-2">
            {realtime.map((r) => {
              const color =
                r.state === "danger"
                  ? "var(--danger)"
                  : r.state === "warning"
                    ? "var(--warning)"
                    : "var(--success)";
              const isOpen = expandedCabin === r.name;
              return (
                <li key={r.name}>
                  <button
                    onClick={() => toggleCabin(r.name)}
                    className="w-full flex items-center justify-between rounded-2xl px-3 py-2.5 transition-all hover:bg-muted/40"
                    style={{ boxShadow: "var(--shadow-neu-inset)" }}
                  >
                    <div className="flex items-center gap-3">
                      <span
                        className={
                          "h-2.5 w-2.5 rounded-full flex-shrink-0 " +
                          (r.state === "danger" ? "pulse-dot" : "")
                        }
                        style={{ background: color }}
                      />
                      <div className="text-left">
                        <p className="text-sm font-semibold">{r.name}</p>
                        <p className="text-xs text-muted-foreground">{r.label}</p>
                      </div>
                    </div>
                    <ChevronDown
                      className={"h-4 w-4 text-muted-foreground transition-transform duration-200 " +
                        (isOpen ? "rotate-180" : "")}
                    />
                  </button>

                  {isOpen && (
                    <div className="mt-1 mx-1 rounded-xl px-4 py-3 space-y-2"
                      style={{ boxShadow: "var(--shadow-neu-sm)" }}>
                      <div className="flex items-center justify-between text-xs">
                        <span className="text-muted-foreground font-medium">Température</span>
                        <span
                          className="font-bold"
                          style={{ color: r.temp > 82 ? "var(--danger)" : r.temp > 74 ? "var(--warning)" : "var(--success)" }}
                        >
                          {r.temp} °C
                        </span>
                      </div>
                      <div className="h-px bg-border/50" />
                      <div className="flex items-center justify-between text-xs">
                        <span className="text-muted-foreground font-medium">Humidité</span>
                        <span
                          className="font-bold"
                          style={{ color: r.humidity > 75 ? "var(--danger)" : r.humidity > 65 ? "var(--warning)" : "var(--success)" }}
                        >
                          {r.humidity} %
                        </span>
                      </div>
                    </div>
                  )}
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
          {/* Title row */}
          <div>
            <h3 className="text-base font-bold">Heatmap des dépassements</h3>
            <p className="text-xs text-muted-foreground">Consulter et filtrer les anomalies mensuelles</p>
          </div>

          {/* Controls row */}
          <div className="flex flex-wrap items-center justify-between gap-3 mt-3">
            {/* Navigation Mois */}
            <div className="neu-inset flex items-center gap-1.5 p-1 rounded-xl">
              <button
                type="button"
                onClick={handlePrevMonth}
                className="p-1 hover:bg-primary/20 hover:text-primary rounded-lg transition-colors"
              >
                <ChevronLeft className="h-4 w-4" />
              </button>
              <span className="text-xs font-bold capitalize px-1 min-w-[100px] text-center text-foreground">
                {currentMonthDate.toLocaleDateString("fr-FR", { month: "long", year: "numeric" })}
              </span>
              <button
                type="button"
                onClick={handleNextMonth}
                className="p-1 hover:bg-primary/20 hover:text-primary rounded-lg transition-colors"
              >
                <ChevronRight className="h-4 w-4" />
              </button>
            </div>

            {/* Filtre Métrique */}
            <div className="neu-inset rounded-xl px-1">
              <Select
                value={metricFilter}
                onValueChange={(v: "all" | "Température" | "Pression") => setMetricFilter(v)}
              >
                <SelectTrigger className="h-8 w-[150px] text-xs border-0 bg-transparent shadow-none focus:ring-0">
                  <SelectValue placeholder="Toutes métriques" />
                </SelectTrigger>
                <SelectContent className="text-xs">
                  <SelectItem value="all">Toutes métriques</SelectItem>
                  <SelectItem value="Température">Température</SelectItem>
                  <SelectItem value="Pression">Pression</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>

          <div className="mt-4 grid grid-cols-7 gap-2 text-center text-[10px] text-muted-foreground">
            {["L", "M", "M", "J", "V", "S", "D"].map((d, i) => (
              <div key={i} className="font-semibold py-1">
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
              // Calculate counts filter-dependently
              const dayDetails = metricFilter === "all" ? d.details : d.details.filter(x => x.metric === metricFilter);
              const dayCount = dayDetails.reduce((sum, item) => sum + item.exceedancesCount, 0);

              const intensity = Math.min(dayCount / 8, 1);
              const bg =
                dayCount === 0
                  ? "var(--surface)"
                  : `color-mix(in oklab, var(--primary) ${35 + intensity * 65}%, white)`;
              
              return (
                <RadixTooltipProvider key={d.date.toISOString()}>
                  <RadixTooltip delayDuration={100}>
                    <RadixTooltipTrigger asChild>
                      <div
                        className="aspect-square rounded-lg text-[10px] font-semibold flex items-center justify-center cursor-help transition-all duration-150 hover:scale-105"
                        style={{
                          background: bg,
                          color:
                            dayCount > 0 && intensity > 0.5 ? "var(--primary-foreground)" : "var(--muted-foreground)",
                          boxShadow:
                            dayCount === 0
                              ? "var(--shadow-neu-inset)"
                              : "var(--shadow-neu-sm)",
                        }}
                      >
                        {d.date.getDate()}
                      </div>
                    </RadixTooltipTrigger>
                    <RadixTooltipContent className="neu-card border border-border rounded-xl p-3 shadow-xl w-72 pointer-events-none z-50 text-xs">
                      <div className="space-y-2">
                        <p className="font-bold text-foreground capitalize">
                          {format(d.date, "EEEE d MMMM yyyy", { locale: fr })}
                        </p>
                        <div className="h-px bg-border" />
                        <p className="font-semibold text-foreground">
                          {dayCount} {dayCount <= 1 ? "dépassement" : "dépassements"}{" "}
                          {metricFilter === "all" ? "au total" : `pour ${metricFilter}`}
                        </p>
                        {dayDetails.length > 0 && (
                          <div className="space-y-1.5 mt-1 text-[11px] text-muted-foreground">
                            {dayDetails.map((det) => (
                              <p key={det.id} className="leading-snug">
                                {det.cabin} - {det.metric} : {det.exceedancesCount} {det.exceedancesCount <= 1 ? "dépassement" : "dépassements"}{" "}
                                <span className="font-medium font-mono text-[10px] text-muted-foreground/70">
                                  (valeur max {det.maxValue}{det.unit}, seuil {det.threshold}{det.unit})
                                </span>
                              </p>
                            ))}
                          </div>
                        )}
                      </div>
                    </RadixTooltipContent>
                  </RadixTooltip>
                </RadixTooltipProvider>
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
