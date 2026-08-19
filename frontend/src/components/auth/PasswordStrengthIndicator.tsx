/**
 * PasswordStrengthIndicator
 * Composant réutilisable affichant en temps réel l'état de chaque règle
 * de robustesse du mot de passe. Réutilisé dans les 3 flows :
 *   - /change-password (première connexion Admin)
 *   - /activation (Superviseur)
 *   - /reinitialiser-mot-de-passe (reset)
 */
import React from 'react';
import { CheckCircle2, Circle, ChevronRight } from 'lucide-react';
import { evaluatePassword, type PasswordRuleResult } from '../../lib/password-rules';

interface PasswordStrengthIndicatorProps {
  password: string;
  /** Afficher uniquement si le champ a été touché (focus+blur ou premier caractère saisi) */
  show?: boolean;
}

function RuleItem({ rule, indent = false }: { rule: PasswordRuleResult; indent?: boolean }) {
  return (
    <li
      className={`flex items-center gap-2 text-xs transition-colors duration-200 ${indent ? 'ml-4' : ''}`}
      style={{ color: rule.valid ? 'var(--success, #22c55e)' : 'var(--muted-foreground)' }}
    >
      {rule.valid ? (
        <CheckCircle2 className="w-3.5 h-3.5 shrink-0" style={{ color: 'var(--success, #22c55e)' }} />
      ) : (
        <Circle className="w-3.5 h-3.5 shrink-0 opacity-40" />
      )}
      <span className={rule.valid ? 'font-medium' : ''}>{rule.label}</span>
    </li>
  );
}

const PasswordStrengthIndicator: React.FC<PasswordStrengthIndicatorProps> = ({
  password,
  show = true,
}) => {
  if (!show) return null;

  const rules = evaluatePassword(password);

  return (
    <div
      className="rounded-xl p-4 space-y-2 transition-all duration-300"
      style={{ background: 'color-mix(in srgb, var(--muted) 30%, transparent)' }}
      aria-live="polite"
      aria-label="Indicateur de robustesse du mot de passe"
    >
      <p className="text-xs font-semibold text-muted-foreground mb-2 tracking-wide uppercase">
        Robustesse du mot de passe
      </p>
      <ul className="space-y-1.5">
        {rules.map((rule) => (
          <React.Fragment key={rule.id}>
            <RuleItem rule={rule} />
            {rule.subRules && (
              <ul className="space-y-1 mt-1">
                {rule.subRules.map((sub) => (
                  <li
                    key={sub.id}
                    className="flex items-center gap-2 text-xs ml-5 transition-colors duration-200"
                    style={{ color: sub.valid ? 'var(--success, #22c55e)' : 'var(--muted-foreground)' }}
                  >
                    <ChevronRight className="w-3 h-3 shrink-0 opacity-50" />
                    {sub.valid ? (
                      <CheckCircle2 className="w-3 h-3 shrink-0" style={{ color: 'var(--success, #22c55e)' }} />
                    ) : (
                      <Circle className="w-3 h-3 shrink-0 opacity-30" />
                    )}
                    <span className={sub.valid ? 'font-medium' : ''}>{sub.label}</span>
                  </li>
                ))}
              </ul>
            )}
          </React.Fragment>
        ))}
      </ul>
    </div>
  );
};

export default PasswordStrengthIndicator;
