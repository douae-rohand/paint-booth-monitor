import { useState, useRef, useEffect, type FormEvent } from 'react';
import { Send, X, RotateCcw, AlertCircle, Maximize2, Minimize2 } from 'lucide-react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { useChatbot } from '@/hooks/useChatbot';
import { cn } from '@/lib/utils';

export function CoatSenseWidget() {
  const [isOpen, setIsOpen] = useState(false);
  const [isExpanded, setIsExpanded] = useState(false);
  const [inputValue, setInputValue] = useState('');
  const { history, loading, ask, retry, clearHistory } = useChatbot();
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  // Auto-scroll au bas de la conversation à chaque nouveau message ou changement d'état
  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    if (isOpen) {
      scrollToBottom();
    }
  }, [history, loading, isOpen, isExpanded]);

  // Focus sur le champ de saisie lors de l'ouverture ou du changement de taille
  useEffect(() => {
    if (isOpen) {
      setTimeout(() => inputRef.current?.focus(), 150);
    }
  }, [isOpen, isExpanded]);

  // Gestion de la touche Échap (Esc) pour réduire ou fermer
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && isOpen) {
        if (isExpanded) {
          setIsExpanded(false);
        } else {
          setIsOpen(false);
        }
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, isExpanded]);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!inputValue.trim() || loading) return;
    const text = inputValue;
    setInputValue('');
    await ask(text);
  };

  const handleSuggestionClick = async (suggestion: string) => {
    if (loading) return;
    await ask(suggestion);
  };

  const toggleExpand = () => {
    setIsExpanded((prev) => !prev);
  };

  const handleClose = () => {
    setIsOpen(false);
    setIsExpanded(false);
  };

  // Composant réutilisable pour le contenu de la fenêtre de chat
  const renderChatWindow = () => (
    <div
      className={cn(
        "flex flex-col overflow-hidden rounded-3xl border border-border/80 bg-background shadow-2xl neu-card transition-all duration-300 ease-in-out",
        isExpanded
          ? "w-[94vw] max-w-[850px] h-[85vh] max-h-[750px] animate-in zoom-in-95 duration-200"
          : "mb-4 h-[520px] w-[90vw] max-w-[400px] max-h-[80vh] animate-in slide-in-from-bottom-5 fade-in duration-200"
      )}
    >
      {/* En-tête du Chatbot */}
      <div className="flex items-center justify-between border-b border-border/60 bg-surface-raised px-4 py-3">
        <div className="flex items-center gap-3">
          <div className="relative flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-surface p-0.5 shadow-sm border border-border/50">
            <img
              src="/images/logo_ia.png"
              alt="CoatSense Logo"
              className="h-full w-full object-contain"
            />
            <span className="absolute -bottom-0.5 -right-0.5 h-3 w-3 rounded-full bg-emerald-500 ring-2 ring-background animate-pulse" />
          </div>
          <div>
            <h3 className="font-bold text-sm text-foreground tracking-tight">CoatSense</h3>
            <p className="text-[11px] text-muted-foreground">Assistant de Supervision</p>
          </div>
        </div>

        {/* Actions de l'en-tête */}
        <div className="flex items-center gap-1">
          {history.length > 0 && (
            <button
              type="button"
              onClick={clearHistory}
              title="Réinitialiser la conversation"
              className="flex h-8 w-8 items-center justify-center rounded-xl text-muted-foreground hover:bg-muted hover:text-foreground transition-colors cursor-pointer"
            >
              <RotateCcw className="h-4 w-4" />
            </button>
          )}

          {/* Bouton Agrandir / Réduire */}
          <button
            type="button"
            onClick={toggleExpand}
            title={isExpanded ? "Réduire la taille" : "Agrandir et centrer"}
            className="flex h-8 w-8 items-center justify-center rounded-xl text-muted-foreground hover:bg-muted hover:text-foreground transition-colors cursor-pointer"
          >
            {isExpanded ? <Minimize2 className="h-4 w-4" /> : <Maximize2 className="h-4 w-4" />}
          </button>

          {/* Bouton Fermer */}
          <button
            type="button"
            onClick={handleClose}
            title="Fermer"
            className="flex h-8 w-8 items-center justify-center rounded-xl text-muted-foreground hover:bg-muted hover:text-foreground transition-colors cursor-pointer"
          >
            <X className="h-4 w-4" />
          </button>
        </div>
      </div>

      {/* Zone des messages */}
      <div className="flex-1 overflow-y-auto p-4 md:p-6 space-y-4">
        {/* État vide (Aucun message) */}
        {history.length === 0 && (
          <div className="flex flex-col items-center justify-center h-full text-center p-4 space-y-4 max-w-md mx-auto">
            <div className="h-20 w-20 rounded-3xl bg-surface p-2 shadow-md border border-border/60 flex items-center justify-center neu-inset">
              <img src="/images/logo_ia.png" alt="CoatSense" className="h-full w-full object-contain" />
            </div>
            <div>
              <h4 className="font-semibold text-base text-foreground">Bonjour ! Je suis CoatSense</h4>
              <p className="mt-1 text-xs text-muted-foreground leading-relaxed">
                Votre assistant dédié à la cabine de peinture. Posez-moi vos questions sur les métriques, alertes ou seuils en temps réel.
              </p>
            </div>

            {/* Suggestions de questions rapides */}
            <div className="w-full space-y-2 pt-2">
              <p className="text-[11px] font-medium text-muted-foreground uppercase tracking-wider text-left">Suggestions :</p>
              {[
                "Quel est l'état actuel de cabine ?",
                "Y a-t-il des alertes actives ?",
                "Analyse la température récente",
              ].map((suggestion, idx) => (
                <button
                  key={idx}
                  type="button"
                  onClick={() => handleSuggestionClick(suggestion)}
                  className="w-full text-left p-3 rounded-2xl text-xs bg-surface-raised border border-border/60 hover:bg-primary/10 hover:border-primary/40 text-foreground transition-all duration-150 cursor-pointer neu-pressable"
                >
                  {suggestion}
                </button>
              ))}
            </div>
          </div>
        )}

        {/* Liste des messages de la conversation */}
        {history.map((msg) => {
          const isUser = msg.role === 'user';
          const isErr = msg.isError;

          return (
            <div
              key={msg.id || `${msg.role}-${Math.random()}`}
              className={cn(
                "flex w-full flex-col",
                isUser ? "items-end" : "items-start"
              )}
            >
              <div
                className={cn(
                  "rounded-2xl px-4 py-3 text-sm leading-relaxed shadow-sm transition-all",
                  isExpanded ? "max-w-[78%]" : "max-w-[88%]",
                  isUser
                    ? "bg-primary text-primary-foreground font-medium rounded-tr-xs"
                    : isErr
                    ? "bg-danger-soft/30 border border-danger/40 text-destructive rounded-tl-xs"
                    : "bg-surface-raised border border-border/70 text-foreground rounded-tl-xs neu-card-sm"
                )}
              >
                {isErr && (
                  <div className="flex items-center gap-1.5 font-semibold text-xs mb-1 text-destructive">
                    <AlertCircle className="h-3.5 w-3.5 shrink-0" />
                    <span>Erreur CoatSense</span>
                  </div>
                )}
                {isUser ? (
                  <p className="whitespace-pre-wrap break-words">{msg.content}</p>
                ) : (
                  <div className="chatbot-markdown break-words">
                    <ReactMarkdown
                      remarkPlugins={[remarkGfm]}
                      components={{
                        p: ({ children }) => <p className="mb-2 last:mb-0 leading-relaxed">{children}</p>,
                        h1: ({ children }) => <h1 className="text-base font-bold mb-2 mt-1">{children}</h1>,
                        h2: ({ children }) => <h2 className="text-sm font-bold mb-1.5 mt-1">{children}</h2>,
                        h3: ({ children }) => <h3 className="text-sm font-semibold mb-1 mt-1">{children}</h3>,
                        ul: ({ children }) => <ul className="list-disc list-outside pl-4 mb-2 space-y-0.5">{children}</ul>,
                        ol: ({ children }) => <ol className="list-decimal list-outside pl-4 mb-2 space-y-0.5">{children}</ol>,
                        li: ({ children }) => <li className="leading-relaxed">{children}</li>,
                        strong: ({ children }) => <strong className="font-semibold">{children}</strong>,
                        em: ({ children }) => <em className="italic">{children}</em>,
                        code: ({ children }) => (
                          <code className="bg-muted/70 text-foreground rounded px-1 py-0.5 text-[11px] font-mono">{children}</code>
                        ),
                        pre: ({ children }) => (
                          <pre className="bg-muted/50 border border-border/60 rounded-lg p-3 overflow-x-auto text-[11px] font-mono mb-2">{children}</pre>
                        ),
                        hr: () => <hr className="border-border/50 my-2" />,
                        table: ({ children }) => (
                          <div className="overflow-x-auto my-2 rounded-lg border border-border/60">
                            <table className="min-w-full text-xs border-collapse">{children}</table>
                          </div>
                        ),
                        thead: ({ children }) => (
                          <thead className="bg-muted/60">{children}</thead>
                        ),
                        tbody: ({ children }) => (
                          <tbody className="divide-y divide-border/40">{children}</tbody>
                        ),
                        tr: ({ children }) => (
                          <tr className="hover:bg-muted/30 transition-colors">{children}</tr>
                        ),
                        th: ({ children }) => (
                          <th className="px-3 py-2 text-left font-semibold text-foreground tracking-wide whitespace-nowrap">{children}</th>
                        ),
                        td: ({ children }) => (
                          <td className="px-3 py-2 text-muted-foreground">{children}</td>
                        ),
                        blockquote: ({ children }) => (
                          <blockquote className="border-l-2 border-primary/50 pl-3 text-muted-foreground italic my-2">{children}</blockquote>
                        ),
                      }}
                    >
                      {msg.content}
                    </ReactMarkdown>
                  </div>
                )}
              </div>

              {/* Bouton Retry — visible uniquement sur les erreurs temporaires */}
              {isErr && msg.isRetryable && msg.originalMessage && (
                <button
                  type="button"
                  onClick={() => retry(msg.originalMessage!)}
                  disabled={loading}
                  className="mt-1.5 flex items-center gap-1.5 rounded-xl px-3 py-1.5 text-[11px] font-medium text-destructive border border-destructive/30 bg-danger-soft/20 hover:bg-danger-soft/40 transition-colors disabled:opacity-40 cursor-pointer"
                >
                  <RotateCcw className="h-3 w-3" />
                  Relancer
                </button>
              )}
            </div>
          );
        })}

        {/* Indicateur de chargement / Réponse en cours */}
        {loading && (
          <div className="flex w-full flex-col items-start">
            <div className="flex items-center gap-2 rounded-2xl rounded-tl-xs bg-surface-raised border border-border/70 px-4 py-3 neu-card-sm">
              <div className="flex items-center gap-1">
                <span className="h-2 w-2 rounded-full bg-primary animate-bounce [animation-delay:-0.3s]" />
                <span className="h-2 w-2 rounded-full bg-primary animate-bounce [animation-delay:-0.15s]" />
                <span className="h-2 w-2 rounded-full bg-primary animate-bounce" />
              </div>
              <span className="text-xs text-muted-foreground font-medium ml-1">CoatSense réfléchit...</span>
            </div>
          </div>
        )}

        <div ref={messagesEndRef} />
      </div>

      {/* Formulaire de Saisie */}
      <form onSubmit={handleSubmit} className="flex items-center gap-2 border-t border-border/60 bg-surface-raised p-3 md:p-4">
        <input
          ref={inputRef}
          type="text"
          value={inputValue}
          onChange={(e) => setInputValue(e.target.value)}
          placeholder="Posez une question à CoatSense..."
          disabled={loading}
          className="flex-1 rounded-2xl border border-border bg-background px-4 py-3 text-sm text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary/50 disabled:opacity-50 transition-all"
        />
        <button
          type="submit"
          disabled={!inputValue.trim() || loading}
          title="Envoyer"
          className="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-primary text-primary-foreground hover:opacity-90 disabled:opacity-40 transition-all shadow-sm cursor-pointer"
        >
          <Send className="h-4 w-4" />
        </button>
      </form>
    </div>
  );

  return (
    <>
      {/* Mode Agrandit / Centré (Modal avec Overlay Flou) */}
      {isOpen && isExpanded && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-xs p-4 md:p-6 animate-in fade-in duration-200"
          onClick={(e) => {
            if (e.target === e.currentTarget) {
              setIsExpanded(false);
            }
          }}
        >
          {renderChatWindow()}
        </div>
      )}

      {/* Mode Flottant (Bas à Droite) */}
      <div className="fixed bottom-6 right-6 z-50 flex flex-col items-end">
        {isOpen && !isExpanded && renderChatWindow()}

        {/* Bouton Flottant Fermé / Réducteur */}
        <button
          type="button"
          onClick={() => {
            if (isOpen && isExpanded) {
              setIsExpanded(false);
            } else {
              setIsOpen(!isOpen);
            }
          }}
          title={isOpen ? "Fermer CoatSense" : "Ouvrir l'assistant CoatSense"}
          className={cn(
            "group relative flex h-16 w-16 items-center justify-center rounded-full border border-border/80 bg-background p-1.5 shadow-xl transition-all duration-200 hover:scale-105 active:scale-95 neu-pressable cursor-pointer overflow-hidden",
            isOpen && "ring-2 ring-primary/60"
          )}
        >
          <img
            src="/images/logo_ia.png"
            alt="CoatSense AI Assistant"
            className="h-full w-full object-contain transition-transform duration-200 group-hover:scale-110"
          />
          {/* Halo / Aura lumineuse discrète */}
          <span className="absolute inset-0 rounded-full bg-primary/10 opacity-0 group-hover:opacity-100 transition-opacity" />
        </button>
      </div>
    </>
  );
}
