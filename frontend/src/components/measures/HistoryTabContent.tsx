import { useMemo, useState } from "react";
import { HistoryFiltersBar, type HistoryFiltersProps } from "./HistoryFiltersBar";
import { HistoryTable } from "./HistoryTable";
import type { HistoryRow, Zone } from "@/lib/mock-data";
import { SEUILS_ABSOLUS } from "@/lib/mock-data";
import type { DateRange } from "react-day-picker";
import { cn } from "@/lib/utils";

export interface HistoryTabContentProps {
  data: HistoryRow[];
  typePoint: "CABINE" | "ETUVE";
  showZoneFilter?: boolean;
}

export function HistoryTabContent({ data, typePoint, showZoneFilter = false }: HistoryTabContentProps) {
  const [searchQuery, setSearchQuery] = useState("");
  const [dateMode, setDateMode] = useState<"exact" | "range">("range");
  const [exactDate, setExactDate] = useState<Date | undefined>();
  const [dateRange, setDateRange] = useState<DateRange | undefined>();
  const [onlyExceed, setOnlyExceed] = useState(false);
  const [selectedZone, setSelectedZone] = useState<Zone | "all">("all");
  const [pageSize, setPageSize] = useState(10);
  const [page, setPage] = useState(1);

  const filteredData = useMemo(() => {
    return data.filter((row) => {
      // Recherche par caisse ID
      if (searchQuery.trim() !== "") {
        if (!row.caisseId.toLowerCase().includes(searchQuery.toLowerCase().trim())) return false;
      }

      // Filtre date exacte
      if (dateMode === "exact" && exactDate) {
        if (
          row.date.getFullYear() !== exactDate.getFullYear() ||
          row.date.getMonth() !== exactDate.getMonth() ||
          row.date.getDate() !== exactDate.getDate()
        )
          return false;
      }

      // Filtre plage de dates
      if (dateMode === "range" && dateRange?.from) {
        const start = new Date(dateRange.from);
        start.setHours(0, 0, 0, 0);
        const end = new Date(dateRange.to ?? dateRange.from);
        end.setHours(23, 59, 59, 999);
        if (row.date < start || row.date > end) return false;
      }

      // Filtre zone (uniquement pour Étuve)
      if (showZoneFilter && selectedZone !== "all" && row.zone !== selectedZone) return false;

      // Filtre dépassements de seuils
      if (onlyExceed) {
        let hasExceed = false;
        if (row.temperature !== undefined) {
          const seuils = typePoint === "CABINE" ? SEUILS_ABSOLUS.CABINE.TEMPERATURE : SEUILS_ABSOLUS.ETUVE.TEMPERATURE;
          if (seuils && (row.temperature < seuils.min || row.temperature > seuils.max)) {
            hasExceed = true;
          }
        }
        if (typePoint === "CABINE" && row.humidite !== undefined) {
          const seuils = SEUILS_ABSOLUS.CABINE.HUMIDITE;
          if (seuils && (row.humidite < seuils.min || row.humidite > seuils.max)) {
            hasExceed = true;
          }
        }
        if (!hasExceed) return false;
      }

      return true;
    });
  }, [data, searchQuery, dateMode, exactDate, dateRange, showZoneFilter, selectedZone, onlyExceed, typePoint]);

  const handleDateReset = () => {
    setExactDate(undefined);
    setDateRange(undefined);
    setPage(1);
  };

  const handleFilterChange = () => {
    setPage(1);
  };

  return (
    <div className="flex flex-col h-full gap-5 animate-in fade-in duration-300">
      <div className="flex-shrink-0">
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
          selectedZone={selectedZone}
          onZoneChange={(zone) => {
            setSelectedZone(zone);
            handleFilterChange();
          }}
          showZoneFilter={showZoneFilter}
        />
      </div>

      <div className="flex-1 min-h-0 flex flex-col">
        <div className="flex-1 min-h-0 overflow-hidden rounded-2xl border border-border">
          <HistoryTable
            data={filteredData}
            typePoint={typePoint}
            pageSize={pageSize}
            currentPage={page}
            onPageChange={setPage}
            onPageSizeChange={(size) => {
              setPageSize(size);
              setPage(1);
            }}
          />
        </div>
      </div>
    </div>
  );
}
