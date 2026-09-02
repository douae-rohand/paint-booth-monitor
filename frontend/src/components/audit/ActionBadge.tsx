import { Badge } from '@/components/ui/badge';
import { AUDIT_ACTION_META, AUDIT_CATEGORIE_COLORS } from '@/constants/auditLabels';
import type { ActionAudit } from '@/api/audit';
import { cn } from '@/lib/utils';

interface ActionBadgeProps {
  action: ActionAudit;
  className?: string;
}

export function ActionBadge({ action, className }: ActionBadgeProps) {
  const meta = AUDIT_ACTION_META[action];
  if (!meta) {
    return (
      <Badge className={cn('bg-slate-50 text-slate-700 border-slate-200 shadow-none font-semibold', className)}>
        {action}
      </Badge>
    );
  }

  const colorClass = AUDIT_CATEGORIE_COLORS[meta.categorie];

  return (
    <Badge className={cn('shadow-none font-semibold border', colorClass, className)}>
      {meta.label}
    </Badge>
  );
}
