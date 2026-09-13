import { createFileRoute } from "@tanstack/react-router";
import { useState } from "react";
import { analyserFichier, type FileAnalysisResponseDTO } from "@/api/fileanalysis";
import { FileAnalysisDropzone, FileAnalysisChartSection } from "@/components/file-analysis";
import { extractErrorMessage } from "@/lib/errors";

export const Route = createFileRoute("/_authenticated/analyse-fichier")({
  head: () => ({
    meta: [
      { title: "Analyse de Fichier CSV - Supervision Cabine" },
      { name: "description", content: "Import et visualisation des données temporalisées d'un fichier CSV externe." },
    ],
  }),
  component: AnalyseFichierPage,
});

function AnalyseFichierPage() {
  // État React en mémoire uniquement (pas de persistance localStorage/sessionStorage)
  const [result, setResult] = useState<FileAnalysisResponseDTO | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleFileSelect = async (file: File) => {
    setLoading(true);
    setError(null);
    try {
      const data = await analyserFichier(file);
      if (!data.points || data.points.length === 0) {
        setError("Aucune donnée valide n'a pu être extraite de ce fichier CSV.");
        setResult(null);
      } else {
        setResult(data);
      }
    } catch (err: unknown) {
      const msg = extractErrorMessage(
        err,
        "Erreur lors de l'analyse du fichier CSV. Vérifiez que le fichier respecte le format et le séparateur point-virgule (;)."
      );
      setError(msg);
      setResult(null);
    } finally {
      setLoading(false);
    }
  };

  const handleReset = () => {
    setResult(null);
    setError(null);
    setLoading(false);
  };

  return (
    <div className="h-full flex flex-col gap-6 animate-in fade-in duration-300">
      <div className="flex flex-col gap-1">
        <h1 className="text-2xl font-bold tracking-tight text-foreground">
          Analyse de Fichier CSV
        </h1>
        <p className="text-sm text-muted-foreground">
          Importez ponctuellement des mesures externes pour visualiser la courbe de tendance agrégée.
        </p>
      </div>

      {result ? (
        <FileAnalysisChartSection data={result} onReset={handleReset} />
      ) : (
        <FileAnalysisDropzone
          onFileSelect={handleFileSelect}
          loading={loading}
          error={error}
          onErrorClear={() => setError(null)}
        />
      )}
    </div>
  );
}
