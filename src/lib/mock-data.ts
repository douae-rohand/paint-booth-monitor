// Mock data generators for painting-booth supervision.
export type Metric = "m1" | "m2";

export const METRIC_LABELS: Record<Metric, string> = {
  m1: "Température (°C)",
  m2: "Humidité (%)",
};

export const METRIC_SHORT: Record<Metric, string> = {
  m1: "Température",
  m2: "Humidité",
};

export const METRIC_UNIT: Record<Metric, string> = {
  m1: "°C",
  m2: "%",
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

export const CABINS = ["A", "B", "C", "D", "E"] as const;
export type Cabin = (typeof CABINS)[number];

export interface HistoryRow {
  id: string;
  caisseId: string;
  cabin: Cabin;
  date: Date;
  m1: number;
  m2: number;
}

export function generateHistory(days = 180): HistoryRow[] {
  const rand = seeded(42);
  const rows: HistoryRow[] = [];
  const now = new Date();
  let counter = 1;
  for (let i = 0; i < days; i++) {
    for (let h = 0; h < 3; h++) {
      const d = new Date(now);
      d.setDate(d.getDate() - i);
      d.setHours(6 + h * 6, Math.floor(rand() * 60), 0, 0);
      const spike1 = rand() > 0.82 ? 25 : 0;
      const spike2 = rand() > 0.88 ? 20 : 0;
      const cabin = CABINS[Math.floor(rand() * CABINS.length)];
      rows.push({
        id: `${i}-${h}`,
        caisseId: `CAI-${String(counter++).padStart(4, "0")}`,
        cabin,
        date: d,
        m1: Math.round(55 + rand() * 30 + spike1),
        m2: Math.round(40 + rand() * 40 + spike2),
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
    const spike2 = rand() > 0.9 ? 10 : 0;
    points.push({
      date: d.toLocaleDateString("fr-FR", { day: "2-digit", month: "short" }),
      timestamp: d.getTime(),
      m1: Math.round(58 + rand() * 24 + spike1),
      m2: Math.round(45 + rand() * 35 + spike2),
    });
  }
  return points;
}

export function generateHourlyTrend(targetDate?: Date): TrendPoint[] {
  const baseDate = targetDate ? new Date(targetDate) : new Date();
  const rand = seeded(24 + baseDate.getDate());
  const points: TrendPoint[] = [];
  
  if (targetDate) {
    // Generate 24 hours of that specific day: 00:00 to 23:00
    for (let h = 0; h < 24; h++) {
      const d = new Date(baseDate);
      d.setHours(h, 0, 0, 0);
      const spike1 = rand() > 0.85 ? 20 : 0;
      const spike2 = rand() > 0.9 ? 10 : 0;
      points.push({
        date: d.toLocaleTimeString("fr-FR", { hour: "2-digit", minute: "2-digit" }),
        timestamp: d.getTime(),
        m1: Math.round(58 + rand() * 24 + spike1),
        m2: Math.round(45 + rand() * 35 + spike2),
      });
    }
  } else {
    // Last 24 hours relative to now
    for (let i = 23; i >= 0; i--) {
      const d = new Date(baseDate);
      d.setHours(d.getHours() - i, 0, 0, 0);
      const spike1 = rand() > 0.85 ? 20 : 0;
      const spike2 = rand() > 0.9 ? 10 : 0;
      points.push({
        date: d.toLocaleTimeString("fr-FR", { hour: "2-digit", minute: "2-digit" }),
        timestamp: d.getTime(),
        m1: Math.round(58 + rand() * 24 + spike1),
        m2: Math.round(45 + rand() * 35 + spike2),
      });
    }
  }
  return points.sort((a, b) => a.timestamp - b.timestamp);
}

export function generateRangeTrend(from: Date, to: Date): TrendPoint[] {
  const utc1 = Date.UTC(from.getFullYear(), from.getMonth(), from.getDate());
  const utc2 = Date.UTC(to.getFullYear(), to.getMonth(), to.getDate());
  const days = Math.floor((utc2 - utc1) / (1000 * 60 * 60 * 24)) + 1;
  const rand = seeded(7 + days);
  const points: TrendPoint[] = [];
  
  for (let i = 0; i < days; i++) {
    const d = new Date(from);
    d.setDate(d.getDate() + i);
    d.setHours(12, 0, 0, 0);
    const spike1 = rand() > 0.85 ? 20 : 0;
    const spike2 = rand() > 0.9 ? 10 : 0;
    points.push({
      date: d.toLocaleDateString("fr-FR", { day: "2-digit", month: "short" }),
      timestamp: d.getTime(),
      m1: Math.round(58 + rand() * 24 + spike1),
      m2: Math.round(45 + rand() * 35 + spike2),
    });
  }
  return points;
}

export function aggregateTrend(
  points: TrendPoint[],
  unit: "day" | "month" | "year",
): TrendPoint[] {
  if (unit === "day") return points;
  const buckets = new Map<string, { m1: number[]; m2: number[]; ts: number }>();
  for (const p of points) {
    const d = new Date(p.timestamp);
    const key =
      unit === "month"
        ? `${d.getFullYear()}-${d.getMonth()}`
        : `${d.getFullYear()}`;
    if (!buckets.has(key))
      buckets.set(key, { m1: [], m2: [], ts: d.getTime() });
    const b = buckets.get(key)!;
    b.m1.push(p.m1);
    b.m2.push(p.m2);
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

export interface HeatmapDetail {
  id: string;
  cabin: string;
  metric: "Température" | "Pression";
  exceedancesCount: number;
  maxValue: number;
  threshold: number;
  unit: string;
}

export interface DetailedHeatmapDay {
  date: Date;
  details: HeatmapDetail[];
  totalCount: number;
}

export function generateDetailedHeatmap(year: number, month: number): DetailedHeatmapDay[] {
  const rand = seeded(year + month * 31);
  const first = new Date(year, month, 1);
  const daysInMonth = new Date(year, month + 1, 0).getDate();
  const days: DetailedHeatmapDay[] = [];
  
  const cabins = ["Cabine 1", "Cabine 2", "Cabine 3"];
  const metrics = [
    { name: "Température", threshold: 80, unit: "°C", minVal: 81, maxVal: 98 },
    { name: "Pression", threshold: 4.0, unit: " bar", minVal: 4.1, maxVal: 5.2 }
  ];
  
  for (let i = 0; i < daysInMonth; i++) {
    const d = new Date(first);
    d.setDate(first.getDate() + i);
    
    const details: HeatmapDetail[] = [];
    if (rand() > 0.4) {
      const numDetails = Math.floor(rand() * 3) + 1;
      const usedPairs = new Set<string>();
      
      for (let k = 0; k < numDetails; k++) {
        const cabin = cabins[Math.floor(rand() * cabins.length)];
        const metricObj = metrics[Math.floor(rand() * metrics.length)];
        const pairKey = `${cabin}-${metricObj.name}`;
        
        if (!usedPairs.has(pairKey)) {
          usedPairs.add(pairKey);
          const exceedancesCount = Math.floor(rand() * 3) + 1;
          const val = Math.round((metricObj.minVal + rand() * (metricObj.maxVal - metricObj.minVal)) * 10) / 10;
          details.push({
            id: `${i}-${cabin}-${metricObj.name}`,
            cabin,
            metric: metricObj.name as "Température" | "Pression",
            exceedancesCount,
            maxValue: val,
            threshold: metricObj.threshold,
            unit: metricObj.unit
          });
        }
      }
    }
    
    details.sort((a, b) => {
      if (a.cabin !== b.cabin) return a.cabin.localeCompare(b.cabin);
      return a.metric.localeCompare(b.metric);
    });

    const totalCount = details.reduce((sum, item) => sum + item.exceedancesCount, 0);
    days.push({
      date: d,
      details,
      totalCount
    });
  }
  
  return days;
}
