// Mock data generators for painting-booth supervision history.
export type Metrique = "TEMPERATURE" | "HUMIDITE";
export type TypePoint = "CABINE" | "ETUVE";
export type Zone = "Zone 1" | "Zone 2" | "Zone 3" | "Zone 4" | "Zone 5";

export const METRIC_LABELS: Record<Metrique, string> = {
  TEMPERATURE: "Température",
  HUMIDITE: "Humidité",
};

export const METRIC_UNIT: Record<Metrique, string> = {
  TEMPERATURE: "°C",
  HUMIDITE: "%",
};

// Seuils absolus pour la simulation
export const SEUILS_ABSOLUS = {
  CABINE: {
    TEMPERATURE: { min: 50, max: 95 },
    HUMIDITE: { min: 40, max: 70 },
  },
  ETUVE: {
    TEMPERATURE: { min: 120, max: 160 },
  },
};

// Deterministic pseudo-random generator so mock data is stable across renders.
function seeded(seed: number) {
  let s = seed;
  return () => {
    s = (s * 9301 + 49297) % 233280;
    return s / 233280;
  };
}

export const ZONES: Zone[] = ["Zone 1", "Zone 2", "Zone 3", "Zone 4", "Zone 5"];

export interface HistoryRow {
  id: string;
  caisseId: string;
  typePoint: TypePoint;
  zone?: Zone;
  date: Date;
  temperature?: number;
  humidite?: number;
}

export function generateCabineHistory(days = 180): HistoryRow[] {
  const rand = seeded(42);
  const rows: HistoryRow[] = [];
  const now = new Date();
  let counter = 1;
  
  for (let i = 0; i < days; i++) {
    for (let h = 0; h < 3; h++) {
      const d = new Date(now);
      d.setDate(d.getDate() - i);
      d.setHours(6 + h * 6, Math.floor(rand() * 60), 0, 0);
      
      // Simulation de valeurs avec occasionnels dépassements de seuils
      const spikeTemp = rand() > 0.92 ? 15 : 0;
      const spikeHumid = rand() > 0.88 ? 12 : 0;
      
      rows.push({
        id: `cabine-${i}-${h}`,
        caisseId: `CAI-${String(counter++).padStart(4, "0")}`,
        typePoint: "CABINE",
        date: d,
        temperature: Math.round(72.5 + rand() * 15 - 7.5 + spikeTemp),
        humidite: Math.round(55 + rand() * 10 - 5 + spikeHumid),
      });
    }
  }
  return rows.sort((a, b) => b.date.getTime() - a.date.getTime());
}

export function generateEtuveHistory(days = 180): HistoryRow[] {
  const rand = seeded(123);
  const rows: HistoryRow[] = [];
  const now = new Date();
  let counter = 1;
  
  for (let i = 0; i < days; i++) {
    for (let h = 0; h < 3; h++) {
      const d = new Date(now);
      d.setDate(d.getDate() - i);
      d.setHours(6 + h * 6, Math.floor(rand() * 60), 0, 0);
      
      // Simulation de valeurs avec occasionnels dépassements de seuils
      const spikeTemp = rand() > 0.92 ? 18 : 0;
      
      ZONES.forEach((zone) => {
        rows.push({
          id: `etuve-${i}-${h}-${zone}`,
          caisseId: `CAI-${String(counter++).padStart(4, "0")}`,
          typePoint: "ETUVE",
          zone,
          date: d,
          temperature: Math.round(140 + rand() * 15 - 7.5 + spikeTemp),
        });
      });
    }
  }
  return rows.sort((a, b) => b.date.getTime() - a.date.getTime());
}
