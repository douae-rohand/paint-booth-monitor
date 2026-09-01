import { createFileRoute } from "@tanstack/react-router";
import { useState } from "react";
import { GenerationRapportForm, HistoriqueRapportsListe } from "@/components/reports";

export const Route = createFileRoute("/_authenticated/rapports")({
  head: () => ({
    meta: [
      { title: "Rapports PDF - Supervision Cabine" },
      { name: "description", content: "Génération et consultation des rapports d'activité PDF." },
    ],
  }),
  component: RapportsPage,
});

function RapportsPage() {
  const [refreshTrigger, setRefreshTrigger] = useState(0);

  const handleRapportGenere = () => {
    setRefreshTrigger((prev) => prev + 1);
  };

  return (
    <div className="h-full flex flex-col gap-6 animate-in fade-in duration-300">
      <div className="grid grid-cols-1 xl:grid-cols-12 gap-6 items-start">
        {/* Formulaire de génération (5 colonnes) */}
        <div className="xl:col-span-5">
          <GenerationRapportForm onRapportGenere={handleRapportGenere} />
        </div>

        {/* Historique des rapports générés (7 colonnes) */}
        <div className="xl:col-span-7">
          <HistoriqueRapportsListe refreshTrigger={refreshTrigger} />
        </div>
      </div>
    </div>
  );
}
