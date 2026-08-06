import * as React from "react";
import { X } from "lucide-react";
import { cn } from "@/lib/utils";

interface SidePanelProps {
  open: boolean;
  onClose: () => void;
  children: React.ReactNode;
  title?: string;
  className?: string;
  width?: string;
}

export function SidePanel({
  open,
  onClose,
  children,
  title,
  className,
  width = "500px",
}: SidePanelProps) {
  return (
    <>
      {/* Overlay */}
      <div
        className={cn(
          "fixed inset-0 z-50 bg-black/40 transition-opacity duration-300",
          open ? "opacity-100" : "opacity-0 pointer-events-none"
        )}
        onClick={onClose}
      />

      {/* Panel */}
      <div
        className={cn(
          "fixed right-0 top-0 z-50 h-full bg-background shadow-2xl transition-transform duration-300 ease-in-out",
          open ? "translate-x-0" : "translate-x-full",
          className
        )}
        style={{ width }}
      >
        <div className="flex h-full flex-col">
          {/* Header */}
          {title && (
            <div className="flex items-center justify-between border-b border-border px-6 py-4 shrink-0">
              <h2 className="text-lg font-bold">{title}</h2>
              <button
                onClick={onClose}
                className="rounded-lg p-2 text-muted-foreground hover:bg-muted transition-colors"
                aria-label="Fermer"
              >
                <X className="h-5 w-5" />
              </button>
            </div>
          )}

          {/* Content with scroll */}
          <div className="flex-1 overflow-auto">{children}</div>
        </div>
      </div>
    </>
  );
}
