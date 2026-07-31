import { createFileRoute } from "@tanstack/react-router";
import { KpiSection } from "@/components/kpis/KpiSection";
import { GraphiqueEvolutionSection } from "@/components/measures/GraphiqueEvolutionSection";
import { StatutTempsReelSection } from "@/components/measures/StatutTempsReelSection";
import { TopAlertesSection } from "@/components/alerting/TopAlertesSection";
import { HeatmapSection } from "@/components/alerting/HeatmapSection";

export const Route = createFileRoute("/_authenticated/dashboard")({
  component: Dashboard,
});

function Dashboard() {
  return (
    <div className="space-y-6">
      {/* KPI Section */}
      <KpiSection modeFiltre="independant" />

      {/* Chart + statut temps réel */}
      <div className="grid grid-cols-1 gap-5 xl:grid-cols-3">
        <div className="xl:col-span-2">
          <GraphiqueEvolutionSection modeFiltre="independant" />
        </div>
        <div>
          <StatutTempsReelSection />
        </div>
      </div>

      {/* Top alertes + heatmap */}
      <div className="grid grid-cols-1 gap-5 lg:grid-cols-2">
        <TopAlertesSection modeFiltre="independant" />
        <HeatmapSection modeFiltre="independant" />
      </div>
    </div>
  );
}
