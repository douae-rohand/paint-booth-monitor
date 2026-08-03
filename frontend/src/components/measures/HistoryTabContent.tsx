import { useMemo, useState, useEffect, useCallback } from "react";
import { HistoryFiltersBar, type HistoryFiltersProps } from "./HistoryFiltersBar";
import { HistoryTable } from "./HistoryTable";
import { useHistoriqueCabine, useHistoriqueEtuve } from "@/hooks/useMeasures";
import { useDebounce } from "@/hooks/useDebounce";
import { useNouvellesMesuresDisponibles } from "@/hooks/useNouvellesMesuresDisponibles";
import { NewDataBanner } from "./NewDataBanner";
import { ExportDialog } from "./ExportDialog";
import type { HistoriqueCabineParams, HistoriqueEtuveParams } from "@/api/measures/index";
import type { MesureCabineDTO, MesureEtuveDTO, PointMesureResponse } from "@/api/measures/index";
import { getPointMesures } from "@/api/measures/index";
import type { DateRange } from "react-day-picker";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Download } from "lucide-react";

export interface HistoryTabContentProps {
  typePoint: "CABINE" | "ETUVE";
  showZoneFilter?: boolean;
}

export function HistoryTabContent({ typePoint, showZoneFilter = false }: HistoryTabContentProps) {
  const [searchQuery, setSearchQuery] = useState("");
  const [dateMode, setDateMode] = useState<"exact" | "range">("range");
  const [exactDate, setExactDate] = useState<Date | undefined>();
  const [dateRange, setDateRange] = useState<DateRange | undefined>();
  const [onlyExceed, setOnlyExceed] = useState(false);
  const [selectedZone, setSelectedZone] = useState<string>("all");
  const [pageSize, setPageSize] = useState(10);
  const [page, setPage] = useState(0);
  const [pointMesures, setPointMesures] = useState<PointMesureResponse[]>([]);
  const [exportDialogOpen, setExportDialogOpen] = useState(false);

  // Charger les points de mesure pour obtenir les IDs WebSocket
  useEffect(() => {
    const loadPointMesures = async () => {
      try {
        const data = await getPointMesures();
        setPointMesures(data);
      } catch (e) {
        console.error('Erreur chargement points de mesure:', e);
      }
    };
    loadPointMesures();
  }, []);

  // Extraire les IDs pour WebSocket de manière stable
  const idPointMesureCabine = useMemo(() => 
    pointMesures.find(p => p.typeEmplacement === 'CABINE')?.id,
    [pointMesures]
  );
  const idsZonesEtuve = useMemo(() => 
    pointMesures
      .filter(p => p.typeEmplacement === 'ETUVE')
      .map(p => p.id),
    [pointMesures]
  );
  const zoneNamesEtuve = useMemo(() => 
    pointMesures
      .filter(p => p.typeEmplacement === 'ETUVE')
      .map(p => p.nom),
    [pointMesures]
  );

  // Construire les paramètres API
  const cabineParams: HistoriqueCabineParams = useMemo(() => {
    const params: HistoriqueCabineParams = {
      page,
      size: pageSize,
      seulementDepassements: onlyExceed,
    };

    if (dateMode === "exact" && exactDate) {
      // Pour une date exacte, couvrir toute la journée (de 00:00:00 à 23:59:59.999)
      const startOfDay = new Date(exactDate);
      startOfDay.setHours(0, 0, 0, 0);
      const endOfDay = new Date(exactDate);
      endOfDay.setHours(23, 59, 59, 999);
      params.dateDebut = startOfDay.toISOString();
      params.dateFin = endOfDay.toISOString();
    } else if (dateMode === "range" && dateRange?.from) {
      params.dateDebut = dateRange.from.toISOString();
      if (dateRange.to) {
        const endOfRange = new Date(dateRange.to);
        endOfRange.setHours(23, 59, 59, 999);
        params.dateFin = endOfRange.toISOString();
      }
    }

    return params;
  }, [dateMode, exactDate, dateRange, onlyExceed, page, pageSize]);

  const etuveParams: HistoriqueEtuveParams = useMemo(() => {
    const params: HistoriqueEtuveParams = {
      page,
      size: pageSize,
      seulementDepassements: onlyExceed,
    };

    if (selectedZone !== "all") {
      params.zone = selectedZone;
    }

    if (dateMode === "exact" && exactDate) {
      // Pour une date exacte, couvrir toute la journée (de 00:00:00 à 23:59:59.999)
      const startOfDay = new Date(exactDate);
      startOfDay.setHours(0, 0, 0, 0);
      const endOfDay = new Date(exactDate);
      endOfDay.setHours(23, 59, 59, 999);
      params.dateDebut = startOfDay.toISOString();
      params.dateFin = endOfDay.toISOString();
    } else if (dateMode === "range" && dateRange?.from) {
      params.dateDebut = dateRange.from.toISOString();
      if (dateRange.to) {
        const endOfRange = new Date(dateRange.to);
        endOfRange.setHours(23, 59, 59, 999);
        params.dateFin = endOfRange.toISOString();
      }
    }

    return params;
  }, [dateMode, exactDate, dateRange, onlyExceed, page, pageSize, selectedZone]);

  // Appeler les hooks API selon le type de point
  const { data: cabineData, loading: cabineLoading, error: cabineError, refetch: refetchCabine } = useHistoriqueCabine(
    typePoint === "CABINE" ? cabineParams : { page: 0, size: 10 }
  );
  const { data: etuveData, loading: etuveLoading, error: etuveError, refetch: refetchEtuve } = useHistoriqueEtuve(
    typePoint === "ETUVE" ? etuveParams : { page: 0, size: 10 }
  );

  const data = typePoint === "CABINE" ? cabineData : etuveData;
  const loading = typePoint === "CABINE" ? cabineLoading : etuveLoading;
  const error = typePoint === "CABINE" ? cabineError : etuveError;
  const refetch = typePoint === "CABINE" ? refetchCabine : refetchEtuve;

  // Hook pour les notifications de nouvelles mesures via WebSocket
  const { visible: newMeasuresVisible, count: newMeasuresCount, refresh: refreshNewMeasures } = useNouvellesMesuresDisponibles({
    typePoint,
    idPointMesureCabine,
    idsZonesEtuve,
    zoneNamesEtuve,
    selectedZone,
    page,
    dateFin: dateRange?.to ? dateRange.to.toISOString() : undefined,
    onRefresh: () => {
      setPage(0);
      refetch();
    },
  });

  const handleDateReset = () => {
    setExactDate(undefined);
    setDateRange(undefined);
    setPage(0);
  };

  const handleFilterChange = useCallback(() => {
    setPage(0);
  }, []);

  const handlePageChange = useCallback((newPage: number) => {
    setPage(newPage);
  }, []);

  const handlePageSizeChange = useCallback((newSize: number) => {
    setPageSize(newSize);
    setPage(0);
  }, []);

  return (
    <div className="flex flex-col min-h-full gap-5 animate-in fade-in duration-300">
      <div className="flex items-center justify-between shrink-0">
        <h2 className="text-2xl font-semibold">
          Historique des mesures {typePoint === "CABINE" ? "Cabine" : "Étuve"}
        </h2>
        <Button 
          variant="outline" 
          size="sm" 
          onClick={() => setExportDialogOpen(true)}
          className="hover:bg-primary hover:text-primary-foreground transition-all"
        >
          <Download className="mr-2 h-4 w-4" />
          Exporter
        </Button>
      </div>

      <div className="shrink-0">
        <HistoryFiltersBar
          searchQuery={searchQuery}
          onSearchChange={(value) => {
            setSearchQuery(value);
            handleFilterChange();
          }}
          dateMode={dateMode}
          onDateModeChange={setDateMode}
          exactDate={exactDate}
          onExactDateChange={(date) => {
            setExactDate(date);
            handleFilterChange();
          }}
          dateRange={dateRange}
          onDateRangeChange={(range) => {
            setDateRange(range);
            handleFilterChange();
          }}
          onDateReset={handleDateReset}
          onlyExceed={onlyExceed}
          onOnlyExceedChange={(value) => {
            setOnlyExceed(value);
            handleFilterChange();
          }}
          selectedZone={selectedZone as any}
          onZoneChange={(zone) => {
            setSelectedZone(zone);
            handleFilterChange();
          }}
          showZoneFilter={showZoneFilter}
        />
      </div>

      {newMeasuresVisible && (
        <div className="shrink-0">
          <NewDataBanner count={newMeasuresCount} onRefresh={refreshNewMeasures} />
        </div>
      )}

      <div className="flex flex-col">
        <div className="min-h-[400px] overflow-hidden rounded-2xl border border-border">
          <HistoryTable
            data={data}
            typePoint={typePoint}
            loading={loading}
            error={error}
            pageSize={pageSize}
            currentPage={page}
            onPageChange={handlePageChange}
            onPageSizeChange={handlePageSizeChange}
          />
        </div>
      </div>

      <ExportDialog
        open={exportDialogOpen}
        onOpenChange={setExportDialogOpen}
        typePoint={typePoint}
        selectedZone={selectedZone}
      />
    </div>
  );
}
