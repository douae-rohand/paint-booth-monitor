// Mock data generators for painting-booth supervision history.
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
