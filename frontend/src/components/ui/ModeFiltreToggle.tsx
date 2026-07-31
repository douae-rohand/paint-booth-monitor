import { Settings } from 'lucide-react';

interface ModeFiltreToggleProps {
  mode: 'global' | 'independant';
  onModeChange: (mode: 'global' | 'independant') => void;
}

export function ModeFiltreToggle({ mode, onModeChange }: ModeFiltreToggleProps) {
  return (
    <div className="flex items-center gap-3">
      <Settings className="h-4 w-4 text-muted-foreground" />
      <div className="neu-inset flex gap-1 rounded-2xl p-1">
        <button
          onClick={() => onModeChange('independant')}
          className={
            'rounded-xl px-3 py-1.5 text-xs font-semibold transition-all ' +
            (mode === 'independant'
              ? 'bg-primary text-primary-foreground'
              : 'text-muted-foreground hover:text-foreground')
          }
        >
          Par section
        </button>
        <button
          onClick={() => onModeChange('global')}
          className={
            'rounded-xl px-3 py-1.5 text-xs font-semibold transition-all ' +
            (mode === 'global'
              ? 'bg-primary text-primary-foreground'
              : 'text-muted-foreground hover:text-foreground')
          }
        >
          Global
        </button>
      </div>
    </div>
  );
}
