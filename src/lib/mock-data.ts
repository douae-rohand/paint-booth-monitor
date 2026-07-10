// Mock data generators for painting-booth supervision.
export type Metric = "m1" | "m2" | "m3";

export const METRIC_LABELS: Record<Metric, string> = {
  m1: "Température (°C)",
  m2: "COV (ppm)",
  m3: "Débit d'air (m³/h)",
};

export const METRIC_SHORT: Record<Metric, string> = {
  m1: "Température",
  m2: "COV",
  m3: "Débit d'air",
};

export const METRIC_UNIT: Record<Metric, string> = {
  m1: "°C",
  m2: "ppm",
  m3: "m³/h",
};

export const THRESHOLD = 80;

// Deterministic pseudo-random generator so mock data is stable across renders.
function seeded(seed: number) {
  let s = seed;
  return () => {
    s = (s * 9301 + 49297) % 233280;
    return s / 233280;
  };
}

export interface HistoryRow {
  id: string;
  date: Date;
  m1: number;
  m2: number;
  m3: number;
}

export function generateHistory(days = 180): HistoryRow[] {
  const rand = seeded(42);
  const rows: HistoryRow[] = [];
  const now = new Date();
  for (let i = 0; i < days; i++) {
    for (let h = 0; h < 3; h++) {
      const d = new Date(now);
      d.setDate(d.getDate() - i);
      d.setHours(6 + h * 6, Math.floor(rand() * 60), 0, 0);
      const spike1 = rand() > 0.82 ? 25 : 0;
      const spike2 = rand() > 0.88 ? 30 : 0;
      const spike3 = rand() > 0.9 ? 20 : 0;
      rows.push({
        id: `${i}-${h}`,
        date: d,
        m1: Math.round(55 + rand() * 30 + spike1),
        m2: Math.round(45 + rand() * 40 + spike2),
        m3: Math.round(60 + rand() * 25 + spike3),
      });
    }
  }
  return rows.sort((a, b) => b.date.getTime() - a.date.getTime());
}

export interface TrendPoint {
  date: string;
  timestamp: number;
  m1: number;
  m2: number;
  m3: number;
}

export function generateTrend(days: number): TrendPoint[] {
  const rand = seeded(7 + days);
  const points: TrendPoint[] = [];
  const now = new Date();
  for (let i = days - 1; i >= 0; i--) {
    const d = new Date(now);
    d.setDate(d.getDate() - i);
    d.setHours(12, 0, 0, 0);
    const spike1 = rand() > 0.85 ? 20 : 0;
    const spike2 = rand() > 0.9 ? 25 : 0;
    const spike3 = rand() > 0.88 ? 18 : 0;
    points.push({
      date: d.toLocaleDateString("fr-FR", { day: "2-digit", month: "short" }),
      timestamp: d.getTime(),
      m1: Math.round(58 + rand() * 24 + spike1),
      m2: Math.round(50 + rand() * 30 + spike2),
      m3: Math.round(62 + rand() * 22 + spike3),
    });
  }
  return points;
}

export function aggregateTrend(
  points: TrendPoint[],
  unit: "day" | "month" | "year",
): TrendPoint[] {
  if (unit === "day") return points;
  const buckets = new Map<string, { m1: number[]; m2: number[]; m3: number[]; ts: number }>();
  for (const p of points) {
    const d = new Date(p.timestamp);
    const key =
      unit === "month"
        ? `${d.getFullYear()}-${d.getMonth()}`
        : `${d.getFullYear()}`;
    if (!buckets.has(key))
      buckets.set(key, { m1: [], m2: [], m3: [], ts: d.getTime() });
    const b = buckets.get(key)!;
    b.m1.push(p.m1);
    b.m2.push(p.m2);
    b.m3.push(p.m3);
  }
  const avg = (a: number[]) => Math.round(a.reduce((s, v) => s + v, 0) / a.length);
  return Array.from(buckets.entries())
    .map(([key, b]) => {
      const d = new Date(b.ts);
      return {
        timestamp: b.ts,
        date:
          unit === "month"
            ? d.toLocaleDateString("fr-FR", { month: "short", year: "2-digit" })
            : `${d.getFullYear()}`,
        m1: avg(b.m1),
        m2: avg(b.m2),
        m3: avg(b.m3),
      };
    })
    .sort((a, b) => a.timestamp - b.timestamp);
}

export interface HeatmapDay {
  date: Date;
  count: number;
}

export function generateHeatmap(): HeatmapDay[] {
  const rand = seeded(99);
  const now = new Date();
  const first = new Date(now.getFullYear(), now.getMonth(), 1);
  const days: HeatmapDay[] = [];
  const daysInMonth = new Date(now.getFullYear(), now.getMonth() + 1, 0).getDate();
  for (let i = 0; i < daysInMonth; i++) {
    const d = new Date(first);
    d.setDate(first.getDate() + i);
    days.push({
      date: d,
      count: rand() > 0.55 ? Math.floor(rand() * 8) : 0,
    });
  }
  return days;
}
