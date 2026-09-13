import { useRef, useMemo } from "react";
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from "recharts";
import { Download, RefreshCw, AlertTriangle, CheckCircle2, BarChart2, Layers } from "lucide-react";
import type { FileAnalysisResponseDTO, Granularite } from "@/api/fileanalysis";
import { formatAxeGraphique } from "@/lib/utils";

interface FileAnalysisChartSectionProps {
  data: FileAnalysisResponseDTO;
  onReset: () => void;
}

const GRANULARITE_LABELS: Record<Granularite, string> = {
  TRENTE_MIN: "Agrégation par 30 minutes",
  HORAIRE: "Agrégation horaire",
  JOURNALIERE: "Agrégation journalière",
  MENSUELLE: "Agrégation mensuelle",
};

export function FileAnalysisChartSection({ data, onReset }: FileAnalysisChartSectionProps) {
  const chartContainerRef = useRef<HTMLDivElement>(null);

  // Transformation des points pour Recharts
  const chartData = useMemo(() => {
    return data.points.map((pt) => ({
      rawDate: pt.horodatage,
      dateFormatted: formatAxeGraphique(pt.horodatage, data.granularite),
      valeur: Number(pt.valeur),
    }));
  }, [data]);

  // Domaine Y automatique avec marge de 5%
  const yDomain = useMemo(() => {
    if (!chartData || chartData.length === 0) return ["auto", "auto"];
    const values = chartData.map((d) => d.valeur);
    const min = Math.min(...values);
    const max = Math.max(...values);
    if (min === max) {
      return [Math.floor(min * 0.9), Math.ceil(max * 1.1)];
    }
    const margin = (max - min) * 0.05;
    return [
      Math.round((min - margin) * 100) / 100,
      Math.round((max + margin) * 100) / 100,
    ];
  }, [chartData]);

  // Export du graphe en image PNG via Canvas HTML5 (sans dépendances tierces)
  const handleExportPng = () => {
    if (!chartContainerRef.current) return;
    const svgElement = chartContainerRef.current.querySelector("svg");
    if (!svgElement) return;

    // Duplication de l'élément SVG pour configuration d'export
    const clonedSvg = svgElement.cloneNode(true) as SVGElement;
    const rectBounds = svgElement.getBoundingClientRect();
    const width = rectBounds.width || 900;
    const height = rectBounds.height || 400;

    clonedSvg.setAttribute("width", `${width}`);
    clonedSvg.setAttribute("height", `${height}`);
    clonedSvg.setAttribute("xmlns", "http://www.w3.org/2000/svg");

    // Fond blanc propre pour l'image exportée
    const bgRect = document.createElementNS("http://www.w3.org/2000/svg", "rect");
    bgRect.setAttribute("width", "100%");
    bgRect.setAttribute("height", "100%");
    bgRect.setAttribute("fill", "#ffffff");
    clonedSvg.insertBefore(bgRect, clonedSvg.firstChild);

    const svgString = new XMLSerializer().serializeToString(clonedSvg);
    const svgBlob = new Blob([svgString], { type: "image/svg+xml;charset=utf-8" });
    const blobUrl = URL.createObjectURL(svgBlob);

    const img = new Image();
    img.onload = () => {
      const canvas = document.createElement("canvas");
      const scale = 2; // Haute résolution HD
      canvas.width = width * scale;
      canvas.height = height * scale;
      const ctx = canvas.getContext("2d");
      if (ctx) {
        ctx.scale(scale, scale);
        ctx.drawImage(img, 0, 0);
        const pngUrl = canvas.toDataURL("image/png");

        const link = document.createElement("a");
        link.href = pngUrl;
        const cleanMetriqueName = (data.labelMetrique || "metrique")
          .toLowerCase()
          .replace(/[^a-z0-9]/gi, "_");
        link.download = `analyse_graphe_${cleanMetriqueName}.png`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
      }
      URL.revokeObjectURL(blobUrl);
    };
    img.src = blobUrl;
  };

  // Tooltip personnalisé neumorphisme
  const CustomTooltip = ({ active, payload }: any) => {
    if (!active || !payload || !payload.length) return null;
    const pointData = payload[0].payload;
    return (
      <div className="neu-card-sm rounded-2xl border border-border/60 bg-[color:var(--surface-raised)] px-4 py-3 text-xs shadow-lg">
        <p className="font-bold text-foreground">
          {new Date(pointData.rawDate).toLocaleString("fr-FR", {
            day: "2-digit",
            month: "2-digit",
            year: "numeric",
            hour: "2-digit",
            minute: "2-digit",
            second: "2-digit",
          })}
        </p>
        <div className="mt-1.5 flex items-center gap-2 text-sm">
          <span className="h-2.5 w-2.5 rounded-full bg-primary" />
          <span className="text-muted-foreground">{data.labelMetrique} :</span>
          <span className="font-bold text-foreground">{pointData.valeur}</span>
        </div>
      </div>
    );
  };

  return (
    <div className="flex flex-col gap-6 w-full max-w-5xl mx-auto animate-in fade-in duration-300">
      {/* Barre de résumé au-dessus du graphe */}
      <div className="neu-card p-6 flex flex-wrap items-center justify-between gap-4 border border-border/80">
        <div>
          <div className="flex items-center gap-2">
            <BarChart2 className="h-5 w-5 text-primary" />
            <h2 className="text-xl font-bold tracking-tight text-foreground">
              Graphe de tendance - {data.labelMetrique}
            </h2>
          </div>
          <p className="mt-1 text-xs text-muted-foreground">
            Résultat de l'analyse temporalisée en mémoire
          </p>
        </div>

        {/* Boutons d'action */}
        <div className="flex flex-wrap items-center gap-3">
          <button
            type="button"
            onClick={handleExportPng}
            disabled={chartData.length === 0}
            className="neu-pressable px-4 py-2 rounded-2xl text-xs font-bold text-foreground bg-surface-raised hover:bg-primary hover:text-primary-foreground transition-all flex items-center gap-2 shadow-sm disabled:opacity-50 disabled:cursor-not-allowed"
            title="Télécharger l'image PNG du graphe"
          >
            <Download className="h-4 w-4" />
            Télécharger le graphe
          </button>

          <button
            type="button"
            onClick={onReset}
            className="neu-pressable px-4 py-2 rounded-2xl text-xs font-bold text-muted-foreground hover:text-foreground bg-surface hover:bg-secondary transition-all flex items-center gap-2"
          >
            <RefreshCw className="h-4 w-4" />
            Importer un autre fichier
          </button>
        </div>
      </div>

      {/* Cartes d'indicateurs de traitement */}
      <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-4">
        {/* Points traités */}
        <div className="neu-inset p-4 rounded-2xl flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary/20 text-amber-700 dark:text-amber-400">
            <Layers className="h-5 w-5" />
          </div>
          <div>
            <p className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">
              Points agrégés
            </p>
            <p className="text-lg font-bold text-foreground">
              {data.points.length} points
            </p>
          </div>
        </div>

        {/* Granularité */}
        <div className="neu-inset p-4 rounded-2xl flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-blue-500/20 text-blue-600">
            <BarChart2 className="h-5 w-5" />
          </div>
          <div>
            <p className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">
              Echelle temporelle
            </p>
            <p className="text-xs font-bold text-foreground">
              {GRANULARITE_LABELS[data.granularite] || data.granularite}
            </p>
          </div>
        </div>

        {/* Lignes valides */}
        <div className="neu-inset p-4 rounded-2xl flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-emerald-500/20 text-emerald-600">
            <CheckCircle2 className="h-5 w-5" />
          </div>
          <div>
            <p className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">
              Lignes valides
            </p>
            <p className="text-lg font-bold text-emerald-600">
              {data.lignesValides}
            </p>
          </div>
        </div>

        {/* Lignes ignorées (si > 0) */}
        <div className="neu-inset p-4 rounded-2xl flex items-center gap-3">
          <div className={
            "flex h-10 w-10 items-center justify-center rounded-xl " +
            (data.lignesIgnorees > 0 ? "bg-amber-500/20 text-amber-600" : "bg-muted text-muted-foreground")
          }>
            <AlertTriangle className="h-5 w-5" />
          </div>
          <div>
            <p className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">
              Lignes ignorées
            </p>
            <p className={
              "text-xs font-bold " +
              (data.lignesIgnorees > 0 ? "text-amber-600" : "text-muted-foreground")
            }>
              {data.lignesIgnorees > 0
                ? `${data.lignesIgnorees} lignes (format invalide)`
                : "0 (fichier 100% conforme)"}
            </p>
          </div>
        </div>
      </div>

      {/* Section Graphe Recharts */}
      <div className="neu-card p-6 border border-border/80">
        <div ref={chartContainerRef} className="h-[380px] w-full relative">
          <ResponsiveContainer width="100%" height={380}>
            <AreaChart data={chartData} margin={{ top: 15, right: 20, left: 10, bottom: 10 }}>
              <defs>
                <linearGradient id="fileAnalysisGrad" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor="var(--chart-1)" stopOpacity={0.6} />
                  <stop offset="100%" stopColor="var(--chart-1)" stopOpacity={0.05} />
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" vertical={false} />
              <XAxis
                dataKey="dateFormatted"
                stroke="var(--muted-foreground)"
                fontSize={11}
                tickLine={{ stroke: "var(--border)" }}
                axisLine={{ stroke: "var(--border)", strokeWidth: 1.5 }}
              />
              <YAxis
                domain={yDomain}
                stroke="var(--muted-foreground)"
                fontSize={11}
                tickLine={{ stroke: "var(--border)" }}
                axisLine={{ stroke: "var(--border)", strokeWidth: 1.5 }}
                tickFormatter={(val) => val.toFixed(1)}
              />
              <Tooltip content={<CustomTooltip />} />
              <Area
                type="monotone"
                dataKey="valeur"
                stroke="var(--chart-1)"
                strokeWidth={2.5}
                fill="url(#fileAnalysisGrad)"
                name={data.labelMetrique}
              />
            </AreaChart>
          </ResponsiveContainer>
        </div>
      </div>
    </div>
  );
}
