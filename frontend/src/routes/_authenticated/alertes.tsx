import { createFileRoute } from "@tanstack/react-router";
import { useState, useEffect } from "react";
import { AlertTriangle } from "lucide-react";
import { ActiveAlertsBand } from "@/components/alerting/ActiveAlertsBand";
import { AlertesHistoryTable } from "@/components/alerting/AlertesHistoryTable";
import { useDashboardWebSocket } from "@/hooks/useDashboardWebSocket";
import { getAlertesActives, type AlerteDTO } from "@/api/alerting";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/_authenticated/alertes")({
  head: () => ({
    meta: [
      { title: "Alertes - Supervision" },
      { name: "description", content: "Consultation des alertes actives et historique des alertes avec filtres avancés." },
    ],
  }),
  component: AlertesPage,
});

function AlertesPage() {
  const [activeTab, setActiveTab] = useState<"ACTIVE" | "HISTORY">("ACTIVE");
  const [activeAlerts, setActiveAlerts] = useState<AlerteDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const { subscribeToAlertes } = useDashboardWebSocket();

  const fetchAlertesActives = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await getAlertesActives();
      setActiveAlerts(data);
    } catch (e) {
      console.error("Erreur fetch alertes actives:", e);
      setError("Impossible de charger les alertes actives");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAlertesActives();

    // Abonnement WebSocket pour les mises à jour temps réel
    const unsubscribe = subscribeToAlertes(() => {
      fetchAlertesActives();
    });

    return unsubscribe;
  }, [subscribeToAlertes]);

  return (
    <div className="h-full flex flex-col">
      <div className="neu-card p-6 flex flex-col h-full">
        {/* Header */}
        <div className="flex items-center justify-between mb-5 shrink-0">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary/15">
              <AlertTriangle className="h-5 w-5 text-primary" />
            </div>
            <div>
              <h2 className="text-lg font-bold">Alertes</h2>
              <p className="text-sm text-muted-foreground">
                Consultez les alertes actives et l'historique des alertes
              </p>
            </div>
          </div>
        </div>

        {/* Tabs */}
        <div className="neu-inset flex gap-1 rounded-2xl p-1 mb-5 shrink-0">
          <button
            onClick={() => setActiveTab("ACTIVE")}
            className={cn(
              "flex-1 rounded-xl px-4 py-2.5 text-sm font-semibold transition-all duration-200",
              activeTab === "ACTIVE"
                ? "bg-primary text-primary-foreground shadow-sm"
                : "text-muted-foreground hover:text-foreground hover:bg-muted/40",
            )}
          >
            Alertes actives ({activeAlerts.length})
          </button>
          <button
            onClick={() => setActiveTab("HISTORY")}
            className={cn(
              "flex-1 rounded-xl px-4 py-2.5 text-sm font-semibold transition-all duration-200",
              activeTab === "HISTORY"
                ? "bg-primary text-primary-foreground shadow-sm"
                : "text-muted-foreground hover:text-foreground hover:bg-muted/40",
            )}
          >
            Historique
          </button>
        </div>

        {/* Content */}
        <div className="flex-1 min-h-0 overflow-y-auto p-1.5">
          <div className="h-full animate-in fade-in slide-in-from-bottom-2 duration-300">
            {activeTab === "ACTIVE" ? (
              <ActiveAlertsBand
                alerts={activeAlerts}
                loading={loading}
                error={error}
              />
            ) : (
              <AlertesHistoryTable />
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
