import { useState } from "react";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import { Label } from "@/components/ui/label";
import { Calendar } from "@/components/ui/calendar";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { CalendarIcon, Download, Loader2 } from "lucide-react";
import { cn, formatHeureAvecMillisecondes } from "@/lib/utils";
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
  const [scope, setScope] = useState<"all" | "period">("all");
  const [dateRange, setDateRange] = useState<DateRange | undefined>(undefined);
  const [loading, setLoading] = useState(false);

  const handleExport = async () => {
    setLoading(true);
    try {
      const params: any = {
        format,
        seulementDepassements: false,
      };

      if (scope === "period" && dateRange?.from && dateRange?.to) {
        const endOfRange = new Date(dateRange.to);
        endOfRange.setHours(23, 59, 59, 999);
        params.dateDebut = dateRange.from.toISOString();
        params.dateFin = endOfRange.toISOString();
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
      // TODO: Afficher une notification d'erreur
    } finally {
      setLoading(false);
    }
  };

  const handleClose = () => {
    setFormat("csv");
    setScope("all");
    setDateRange(undefined);
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

            {/* Sélecteur de période */}
            {scope === "period" && (
              <div className="pt-3">
                <Popover>
                  <PopoverTrigger asChild>
                    <Button
                      variant="outline"
                      className={cn(
                        "w-full justify-start text-left font-normal",
                        !dateRange && "text-muted-foreground"
                      )}
                    >
                      <CalendarIcon className="mr-2 h-4 w-4" />
                      {dateRange?.from ? (
                        dateRange.to ? (
                          <>
                            {dateRange.from.toLocaleDateString("fr-FR")} -{" "}
                            {dateRange.to.toLocaleDateString("fr-FR")}
                          </>
                        ) : (
                          dateRange.from.toLocaleDateString("fr-FR")
                        )
                      ) : (
                        <span>Sélectionner une période</span>
                      )}
                    </Button>
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
