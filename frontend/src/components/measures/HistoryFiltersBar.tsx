import { AlertTriangle, Calendar as CalendarIcon, Search } from "lucide-react";
import { Calendar } from "@/components/ui/calendar";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Switch } from "@/components/ui/switch";
import { cn } from "@/lib/utils";
import { format } from "date-fns";
import { fr } from "date-fns/locale";
import type { DateRange } from "react-day-picker";
import type { Zone } from "@/lib/mock-data";

export interface HistoryFiltersProps {
  searchQuery: string;
  onSearchChange: (value: string) => void;
  dateMode: "exact" | "range";
  onDateModeChange: (mode: "exact" | "range") => void;
  exactDate: Date | undefined;
  onExactDateChange: (date: Date | undefined) => void;
  dateRange: DateRange | undefined;
  onDateRangeChange: (range: DateRange | undefined) => void;
  onDateReset: () => void;
  onlyExceed: boolean;
  onOnlyExceedChange: (value: boolean) => void;
  selectedZone?: Zone | "all";
  onZoneChange?: (zone: Zone | "all") => void;
  showZoneFilter?: boolean;
}

export function HistoryFiltersBar({
  searchQuery,
  onSearchChange,
  dateMode,
  onDateModeChange,
  exactDate,
  onExactDateChange,
  dateRange,
  onDateRangeChange,
  onDateReset,
  onlyExceed,
  onOnlyExceedChange,
  selectedZone = "all",
  onZoneChange,
  showZoneFilter = false,
}: HistoryFiltersProps) {
  return (
    <div className="space-y-4">
      {/* Row 1: Search + Only Exceed Toggle */}
      <div className="flex flex-wrap items-center justify-between gap-4">
        {/* Caisse ID Search */}
        <div className="neu-inset flex flex-1 max-w-md items-center gap-2 px-4 h-10 rounded-2xl">
          <Search className="h-4 w-4 text-muted-foreground" />
          <input
            type="text"
            placeholder="Rechercher une caisse par son ID (ex: CAT-0003)..."
            value={searchQuery}
            onChange={(e) => onSearchChange(e.target.value)}
            className="w-full border-0 bg-transparent p-0 text-sm outline-none placeholder:text-muted-foreground focus:ring-0"
          />
        </div>

        {/* Only exceed */}
        <div className="neu-pressable flex items-center gap-3 rounded-2xl px-4 h-10">
          <AlertTriangle className="h-4 w-4 text-[color:var(--danger)]" />
          <span className="text-xs font-semibold">Seulement les dépassements</span>
          <Switch checked={onlyExceed} onCheckedChange={onOnlyExceedChange} />
        </div>
      </div>

      {/* Row 2: Date Filters + Zone Filter */}
      <div className="flex flex-wrap items-center gap-3">
        {/* Mode toggle */}
        <div className="neu-inset flex gap-1 rounded-2xl p-1 h-10 items-center">
          <button
            onClick={() => onDateModeChange("exact")}
            className={cn(
              "rounded-xl px-3 py-1.5 text-xs font-semibold transition-all h-8 flex items-center",
              dateMode === "exact"
                ? "bg-primary text-primary-foreground"
                : "text-muted-foreground hover:text-foreground",
            )}
          >
            Date exacte
          </button>
          <button
            onClick={() => onDateModeChange("range")}
            className={cn(
              "rounded-xl px-3 py-1.5 text-xs font-semibold transition-all h-8 flex items-center",
              dateMode === "range"
                ? "bg-primary text-primary-foreground"
                : "text-muted-foreground hover:text-foreground",
            )}
          >
            Plage
          </button>
        </div>

        {/* Date picker */}
        <Popover>
          <PopoverTrigger asChild>
            <button className="neu-pressable flex h-10 items-center gap-2 rounded-2xl px-4 text-sm">
              <CalendarIcon className="h-4 w-4 text-primary" />
              {dateMode === "exact"
                ? exactDate
                  ? format(exactDate, "d MMM yyyy", { locale: fr })
                  : "Choisir une date"
                : dateRange?.from
                  ? dateRange.to
                    ? `${format(dateRange.from, "d MMM", { locale: fr })} — ${format(dateRange.to, "d MMM yyyy", { locale: fr })}`
                    : format(dateRange.from, "d MMM yyyy", { locale: fr })
                  : "Choisir une plage"}
            </button>
          </PopoverTrigger>
          <PopoverContent className="w-auto p-0" align="start">
            {dateMode === "exact" ? (
              <Calendar
                mode="single"
                selected={exactDate}
                onSelect={onExactDateChange}
                locale={fr}
                className={cn("p-3 pointer-events-auto")}
              />
            ) : (
              <Calendar
                mode="range"
                selected={dateRange}
                onSelect={onDateRangeChange}
                locale={fr}
                numberOfMonths={2}
                className={cn("p-3 pointer-events-auto")}
              />
            )}
          </PopoverContent>
        </Popover>

        {(exactDate || dateRange) && (
          <button
            onClick={onDateReset}
            className="text-xs font-semibold text-muted-foreground underline-offset-4 hover:underline"
          >
            Réinitialiser
          </button>
        )}

        {/* Zone filter (only for Étuve) */}
        {showZoneFilter && onZoneChange && (
          <div className="neu-inset rounded-2xl px-1">
            <Select value={selectedZone} onValueChange={onZoneChange}>
              <SelectTrigger className="h-10 w-[140px] border-0 bg-transparent shadow-none focus:ring-0">
                <SelectValue placeholder="Zone" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">Toutes zones</SelectItem>
                <SelectItem value="Zone 1">Zone 1</SelectItem>
                <SelectItem value="Zone 2">Zone 2</SelectItem>
                <SelectItem value="Zone 3">Zone 3</SelectItem>
                <SelectItem value="Zone 4">Zone 4</SelectItem>
                <SelectItem value="Zone 5">Zone 5</SelectItem>
              </SelectContent>
            </Select>
          </div>
        )}
      </div>
    </div>
  );
}
