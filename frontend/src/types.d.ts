// Types for our API and data structures

export interface User {
  id: string;
  email: string;
  role: 'Superviseur' | 'Admin';
}

export interface Measure {
  id: string;
  temperature: number;
  humidity: number;
  timestamp: string;
  boxId?: string;
}

export interface Alert {
  id: string;
  type: 'seuil_absolu' | 'seuil_dynamique' | 'ia';
  severity: 'faible' | 'moyenne' | 'critique';
  metric: 'temperature' | 'humidity';
  value: number;
  threshold: number;
  timestamp: string;
  acknowledged: boolean;
}

export interface KPI {
  conformityRate: number;
  meanTimeBetweenIncidents: number;
  meanTimeToRecover: number;
}

