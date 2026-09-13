import { useState, useRef, type DragEvent, type ChangeEvent } from "react";
import { UploadCloud, FileSpreadsheet, AlertCircle, Info, Loader2, FileText } from "lucide-react";
import { extractErrorMessage } from "@/lib/errors";

interface FileAnalysisDropzoneProps {
  onFileSelect: (file: File) => Promise<void>;
  loading: boolean;
  error: string | null;
  onErrorClear: () => void;
}

const MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10 Mo

export function FileAnalysisDropzone({
  onFileSelect,
  loading,
  error,
  onErrorClear,
}: FileAnalysisDropzoneProps) {
  const [isDragging, setIsDragging] = useState(false);
  const [clientError, setClientError] = useState<string | null>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  const validateAndProcessFile = async (file: File) => {
    setClientError(null);
    onErrorClear();

    // Validation extension .csv
    if (!file.name.toLowerCase().endsWith(".csv")) {
      setClientError("Format invalide : Seuls les fichiers au format .csv sont acceptés.");
      return;
    }

    // Validation taille max 10 Mo
    if (file.size > MAX_FILE_SIZE_BYTES) {
      const sizeMo = (file.size / (1024 * 1024)).toFixed(2);
      setClientError(`Fichier trop volumineux (${sizeMo} Mo) : La taille maximale autorisée est de 10 Mo.`);
      return;
    }

    // Fichier valide -> envoi au parent
    try {
      await onFileSelect(file);
    } catch {
      // L'erreur API est capturée et passée via la prop `error`
    }
  };

  const handleDragOver = (e: DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    e.stopPropagation();
    if (!loading) {
      setIsDragging(true);
    }
  };

  const handleDragLeave = (e: DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);
  };

  const handleDrop = async (e: DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);

    if (loading) return;

    const files = e.dataTransfer.files;
    if (files && files.length > 0) {
      await validateAndProcessFile(files[0]);
    }
  };

  const handleInputChange = async (e: ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (files && files.length > 0) {
      await validateAndProcessFile(files[0]);
    }
    // Réinitialiser la valeur de l'input pour réautoriser l'import du même fichier si besoin
    if (inputRef.current) {
      inputRef.current.value = "";
    }
  };

  const activeError = error || clientError;

  return (
    <div className="flex flex-col gap-6 w-full max-w-4xl mx-auto animate-in fade-in duration-300">
      {/* Explication claire du format attendu */}
      <div className="neu-card p-6 border border-border/80">
        <div className="flex items-start gap-4">
          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl bg-primary/20 text-primary-foreground">
            <Info className="h-5 w-5 text-amber-700 dark:text-amber-400" />
          </div>
          <div className="flex-1">
            <h2 className="text-base font-bold text-foreground">
              Format de fichier CSV attendu
            </h2>
            <p className="mt-1 text-sm text-muted-foreground">
              Pour garantir l'analyse automatique et le calcul de la tendance temporalisée, votre fichier CSV doit respecter la structure suivante :
            </p>

            <div className="mt-4 grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="neu-inset p-4 rounded-xl text-xs space-y-1.5">
                <p className="font-semibold text-foreground flex items-center gap-1.5">
                  <FileText className="h-4 w-4 text-primary" /> Spécifications du format :
                </p>
                <ul className="list-disc list-inside text-muted-foreground space-y-1">
                  <li><strong>Séparateur :</strong> Point-virgule (<code className="bg-secondary px-1 py-0.5 rounded font-mono text-foreground">;</code>)</li>
                  <li><strong>Encodage :</strong> UTF-8 (avec ou sans BOM)</li>
                  <li><strong>En-tête :</strong> 3 colonnes exactes (<code className="bg-secondary px-1 py-0.5 rounded font-mono text-foreground">Date;Heure;NomMetrique</code>)</li>
                  <li><strong>Taille maximale :</strong> 10 Mo</li>
                </ul>
              </div>

              <div className="neu-inset p-4 rounded-xl text-xs space-y-1.5">
                <p className="font-semibold text-foreground flex items-center gap-1.5">
                  <FileSpreadsheet className="h-4 w-4 text-emerald-600" /> Exemple concret de fichier :
                </p>
                <div className="bg-background/90 p-2.5 rounded-lg border border-border/60 font-mono text-[11px] text-foreground leading-relaxed overflow-x-auto shadow-inner">
                  <p className="text-muted-foreground select-none">Date;Heure;Humidité</p>
                  <p className="text-emerald-700 font-semibold">06/09/2026;21:09:37.240;42.67</p>
                  <p className="text-foreground/80">06/09/2026;21:10:37.240;43.10</p>
                  <p className="text-foreground/80">06/09/2026;21:11:37.240;42.85</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Message d'erreur persistent (client ou API) */}
      {activeError && (
        <div className="neu-card p-4 border border-destructive/40 bg-destructive/5 text-destructive rounded-2xl flex items-start gap-3 animate-in slide-in-from-top-2 duration-200">
          <AlertCircle className="h-5 w-5 shrink-0 mt-0.5" />
          <div className="flex-1 text-sm font-medium">
            <p className="font-bold text-destructive">Échec de l'importation</p>
            <p className="mt-0.5 text-destructive/90">{activeError}</p>
          </div>
        </div>
      )}

      {/* Dropzone d'importation */}
      <div
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
        onClick={() => {
          if (!loading) {
            inputRef.current?.click();
          }
        }}
        className={
          "neu-card p-10 flex flex-col items-center justify-center text-center cursor-pointer transition-all duration-200 border-2 border-dashed relative overflow-hidden group " +
          (isDragging
            ? "border-primary bg-primary/10 scale-[1.01]"
            : loading
            ? "border-border/40 opacity-70 cursor-wait"
            : "border-border/80 hover:border-primary/60 hover:bg-surface-raised/80")
        }
      >
        <input
          ref={inputRef}
          type="file"
          accept=".csv"
          onChange={handleInputChange}
          disabled={loading}
          className="hidden"
          id="csv-file-input"
        />

        {loading ? (
          <div className="flex flex-col items-center gap-3 py-6">
            <Loader2 className="h-12 w-12 text-primary animate-spin" />
            <p className="text-base font-bold text-foreground">
              Analyse du fichier en cours...
            </p>
            <p className="text-xs text-muted-foreground">
              Lecture des mesures, validation et agrégation temporalisée par le backend.
            </p>
          </div>
        ) : (
          <div className="flex flex-col items-center gap-4 py-4">
            <div className="flex h-16 w-16 items-center justify-center rounded-3xl neu-inset text-primary transition-transform group-hover:scale-110">
              <UploadCloud className="h-8 w-8" />
            </div>

            <div>
              <p className="text-lg font-bold text-foreground">
                Importez un fichier CSV pour visualiser vos mesures
              </p>
              <p className="mt-1 text-xs text-muted-foreground">
                Glissez-déposez votre fichier ici ou <span className="text-primary font-semibold underline underline-offset-2">parcourez vos fichiers</span>
              </p>
            </div>

            <button
              type="button"
              disabled={loading}
              className="mt-2 neu-pressable px-6 py-2.5 rounded-2xl text-xs font-bold text-foreground bg-surface-raised hover:bg-primary hover:text-primary-foreground transition-all shadow-sm flex items-center gap-2"
            >
              <FileSpreadsheet className="h-4 w-4" />
              Sélectionner un fichier CSV
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
