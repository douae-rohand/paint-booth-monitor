import { createFileRoute } from "@tanstack/react-router";
import { useState } from "react";
import { HistoryTabContent } from "@/components/measures/HistoryTabContent";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/_authenticated/history")({
  head: () => ({
    meta: [
      { title: "Historique des mesures — Supervision" },
      { name: "description", content: "Historique détaillé des mesures des cabines de peinture et étuves avec filtres et alertes de seuil." },
    ],
  }),
  component: HistoryPage,
});

function HistoryPage() {
  const [activeTab, setActiveTab] = useState<"CABINE" | "ETUVE">("CABINE");

  return (
    <div className="h-full flex flex-col">
      <div className="neu-card p-6 flex flex-col h-full">
        {/* Header */}
        <div className="flex items-center justify-between mb-5 shrink-0">
          <div>
            <h2 className="text-lg font-bold">Historique des mesures</h2>
            <p className="text-sm text-muted-foreground">
              Consultez l'historique des mesures de la cabine de peinture et de l'étuve
            </p>
          </div>
        </div>

        {/* Tabs */}
        <div className="neu-inset flex gap-1 rounded-2xl p-1 mb-5 shrink-0">
          <button
            onClick={() => setActiveTab("CABINE")}
            className={cn(
              "flex-1 rounded-xl px-4 py-2.5 text-sm font-semibold transition-all duration-200",
              activeTab === "CABINE"
                ? "bg-primary text-primary-foreground shadow-sm"
                : "text-muted-foreground hover:text-foreground hover:bg-muted/40",
            )}
          >
            Cabine
          </button>
          <button
            onClick={() => setActiveTab("ETUVE")}
            className={cn(
              "flex-1 rounded-xl px-4 py-2.5 text-sm font-semibold transition-all duration-200",
              activeTab === "ETUVE"
                ? "bg-primary text-primary-foreground shadow-sm"
                : "text-muted-foreground hover:text-foreground hover:bg-muted/40",
            )}
          >
            Étuve
          </button>
        </div>

        {/* Tab Content */}
        <div className="flex-1 min-h-0 overflow-y-auto">
          <div className="h-full animate-in fade-in slide-in-from-bottom-2 duration-300">
            {activeTab === "CABINE" ? (
              <HistoryTabContent typePoint="CABINE" showZoneFilter={false} />
            ) : (
              <HistoryTabContent typePoint="ETUVE" showZoneFilter={true} />
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
