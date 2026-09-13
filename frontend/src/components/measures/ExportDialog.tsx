import { useState } from "react";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import { Label } from "@/components/ui/label";
import { Calendar } from "@/components/ui/calendar";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { CalendarIcon, Clock, Download, Loader2 } from "lucide-react";
import { cn } from "@/lib/utils";
import type { DateRange } from "react-day-picker";
import { exportHistoriqueCabine, exportHistoriqueEtuve } from "@/api/measures/index";

interface ExportDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  typePoint: "CABINE" | "ETUVE";
  selectedZone?: string;
}

export const ExportDialog = ({ open, onOpenChange, typePoint, selectedZone }: ExportDialogProps) => {
  const [format, setFormat] = useState<"csv" | "pdf" | "xlsx">("csv");
  const [metriqueSelection, setMetriqueSelection] = useState<"ALL" | "TEMPERATURE" | "HUMIDITE">("ALL");
  const [scope, setScope] = useState<"all" | "period">("all");
  const [dateRange, setDateRange] = useState<DateRange | undefined>(undefined);
  const [heureDebut, setHeureDebut] = useState<string>("00:00");
  const [heureFin, setHeureFin] = useState<string>("23:59");
  const [loading, setLoading] = useState(false);

  const handleExport = async () => {
    setLoading(true);
    try {
      const params: any = {
        format,
        seulementDepassements: false,
      };

      if (typePoint === "CABINE" && metriqueSelection !== "ALL") {
        params.metrique = metriqueSelection;
      }

      if (scope === "period" && dateRange?.from) {
        const start = new Date(dateRange.from);
        const [hStart, mStart] = heureDebut.split(":").map(Number);
        start.setHours(isNaN(hStart) ? 0 : hStart, isNaN(mStart) ? 0 : mStart, 0, 0);
        params.dateDebut = start.toISOString();

        const endDate = dateRange.to ? new Date(dateRange.to) : new Date(dateRange.from);
        const [hEnd, mEnd] = heureFin.split(":").map(Number);
        endDate.setHours(isNaN(hEnd) ? 23 : hEnd, isNaN(mEnd) ? 59 : mEnd, 59, 999);
        params.dateFin = endDate.toISOString();
      }

      if (typePoint === "ETUVE" && selectedZone && selectedZone !== "all") {
        params.zone = selectedZone;
      }

      let blob: Blob;
      if (typePoint === "CABINE") {
        blob = await exportHistoriqueCabine(params);
      } else {
        blob = await exportHistoriqueEtuve(params);
      }

      // Créer un lien de téléchargement
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      
      // Générer le nom du fichier
      const date = new Date().toISOString().split("T")[0];
      const filename = `mesures_${typePoint.toLowerCase()}_${date}.${format}`;
      a.download = filename;
      
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);

      onOpenChange(false);
    } catch (error) {
      console.error("Erreur lors de l'export:", error);
    } finally {
      setLoading(false);
    }
  };

  const handleClose = () => {
    setFormat("csv");
    setMetriqueSelection("ALL");
    setScope("all");
    setDateRange(undefined);
    setHeureDebut("00:00");
    setHeureFin("23:59");
    onOpenChange(false);
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[500px]">
        <DialogHeader>
          <DialogTitle>Exporter les mesures</DialogTitle>
          <DialogDescription>
            {typePoint === "CABINE" ? "Cabine" : "Étuve"}
            {typePoint === "ETUVE" && selectedZone && selectedZone !== "all" && ` - ${selectedZone}`}
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-6 py-4">
          {/* Format */}
          <div className="space-y-3">
            <Label className="text-base font-medium">Format</Label>
            <RadioGroup value={format} onValueChange={(value) => setFormat(value as "csv" | "pdf" | "xlsx")}>
              <div className="flex items-center space-x-2">
                <RadioGroupItem value="csv" id="csv" />
                <Label htmlFor="csv" className="cursor-pointer">CSV</Label>
              </div>
              <div className="flex items-center space-x-2">
                <RadioGroupItem value="pdf" id="pdf" />
                <Label htmlFor="pdf" className="cursor-pointer">PDF</Label>
              </div>
              <div className="flex items-center space-x-2">
                <RadioGroupItem value="xlsx" id="xlsx" />
                <Label htmlFor="xlsx" className="cursor-pointer">Excel (.xlsx)</Label>
              </div>
            </RadioGroup>
          </div>

          {/* Choix des métriques pour Cabine */}
          {typePoint === "CABINE" && (
            <div className="space-y-3">
              <Label className="text-base font-medium">Métriques à exporter</Label>
              <RadioGroup value={metriqueSelection} onValueChange={(val) => setMetriqueSelection(val as any)}>
                <div className="flex items-center space-x-2">
                  <RadioGroupItem value="ALL" id="metrique-all" />
                  <Label htmlFor="metrique-all" className="cursor-pointer">Température et Humidité</Label>
                </div>
                <div className="flex items-center space-x-2">
                  <RadioGroupItem value="TEMPERATURE" id="metrique-temp" />
                  <Label htmlFor="metrique-temp" className="cursor-pointer">Température uniquement</Label>
                </div>
                <div className="flex items-center space-x-2">
                  <RadioGroupItem value="HUMIDITE" id="metrique-hum" />
                  <Label htmlFor="metrique-hum" className="cursor-pointer">Humidité uniquement</Label>
                </div>
              </RadioGroup>
            </div>
          )}

          {/* Périmètre */}
          <div className="space-y-3">
            <Label className="text-base font-medium">Périmètre</Label>
            <RadioGroup value={scope} onValueChange={(value) => setScope(value as "all" | "period")}>
              <div className="flex items-center space-x-2">
                <RadioGroupItem value="all" id="all" />
                <Label htmlFor="all" className="cursor-pointer">Toutes les mesures</Label>
              </div>
              <div className="flex items-center space-x-2">
                <RadioGroupItem value="period" id="period" />
                <Label htmlFor="period" className="cursor-pointer">Une période</Label>
              </div>
            </RadioGroup>

            {/* Sélecteur de période & horaire */}
            {scope === "period" && (
              <div className="space-y-3 pt-3">
                <Popover>
                  <PopoverTrigger asChild>
                    <button
                      className={cn(
                        "neu-pressable flex w-full items-center gap-2 rounded-2xl border border-border/50 px-4 py-2.5 text-sm font-semibold transition-all hover:bg-primary/10 hover:text-primary hover:border-primary/30",
                        !dateRange && "text-muted-foreground"
                      )}
                    >
                      <CalendarIcon className="h-4 w-4 shrink-0 text-primary" />
                      {dateRange?.from ? (
                        dateRange.to ? (
                          <>
                            {dateRange.from.toLocaleDateString("fr-FR")} –{" "}
                            {dateRange.to.toLocaleDateString("fr-FR")}
                          </>
                        ) : (
                          dateRange.from.toLocaleDateString("fr-FR")
                        )
                      ) : (
                        <span>Sélectionner une période</span>
                      )}
                    </button>
                  </PopoverTrigger>
                  <PopoverContent className="w-auto p-0" align="start">
                    <Calendar
                      mode="range"
                      selected={dateRange}
                      onSelect={setDateRange}
                      initialFocus
                    />
                  </PopoverContent>
                </Popover>

                {/* Sélection horaire */}
                <div className="grid grid-cols-2 gap-3 pt-1">
                  <div className="space-y-1.5">
                    <Label className="text-xs font-semibold text-muted-foreground flex items-center gap-1.5">
                      <Clock className="h-3.5 w-3.5 text-primary" />
                      Heure de début
                    </Label>
                    <div className="neu-inset rounded-2xl px-3 py-1.5 border border-border/50">
                      <input
                        type="time"
                        value={heureDebut}
                        onChange={(e) => setHeureDebut(e.target.value)}
                        className="w-full border-0 bg-transparent text-xs font-medium focus:outline-none focus:ring-0"
                      />
                    </div>
                  </div>

                  <div className="space-y-1.5">
                    <Label className="text-xs font-semibold text-muted-foreground flex items-center gap-1.5">
                      <Clock className="h-3.5 w-3.5 text-primary" />
                      Heure de fin
                    </Label>
                    <div className="neu-inset rounded-2xl px-3 py-1.5 border border-border/50">
                      <input
                        type="time"
                        value={heureFin}
                        onChange={(e) => setHeureFin(e.target.value)}
                        className="w-full border-0 bg-transparent text-xs font-medium focus:outline-none focus:ring-0"
                      />
                    </div>
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>

        <DialogFooter>
          <Button 
            variant="outline" 
            onClick={handleClose} 
            disabled={loading}
            className="hover:bg-primary hover:text-primary-foreground transition-all"
          >
            Annuler
          </Button>
          <Button 
            onClick={handleExport} 
            disabled={loading}
            className="hover:bg-primary hover:text-primary-foreground transition-all"
          >
            {loading ? (
              <>
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                Export en cours...
              </>
            ) : (
              <>
                <Download className="mr-2 h-4 w-4" />
                Exporter
              </>
            )}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};

export default ExportDialog;
