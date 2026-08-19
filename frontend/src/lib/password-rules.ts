/**
 * password-rules.ts
 * Règles de robustesse de mot de passe — source unique pour le frontend.
 * Utilisé par PasswordStrengthIndicator et les 3 formulaires (changement, activation, réinitialisation).
 * Aucune dépendance externe — logique pure TypeScript.
 */

export interface PasswordRuleResult {
  /** Identifiant unique de la règle */
  id: string;
  /** Libellé affiché à l'utilisateur */
  label: string;
  /** true si la règle est respectée */
  valid: boolean;
  /** Sous-règles (optionnel, pour les 4 catégories de caractères) */
  subRules?: PasswordRuleResult[];
}

/** Nombre minimum de catégories requises parmi les 4 */
export const MIN_CATEGORIES = 3;

/** Vérifie les 4 catégories de caractères */
function evalCategories(password: string) {
  return {
    lowercase: /[a-z]/.test(password),
    uppercase: /[A-Z]/.test(password),
    digit:     /[0-9]/.test(password),
    special:   /[^a-zA-Z0-9]/.test(password),
  };
}

/** Vérifie l'absence de plus de 2 caractères identiques consécutifs */
function noTripleRepeat(password: string): boolean {
  for (let i = 0; i < password.length - 2; i++) {
    if (
      password[i] === password[i + 1] &&
      password[i] === password[i + 2]
    ) {
      return false;
    }
  }
  return true;
}

/**
 * Évalue toutes les règles de robustesse pour un mot de passe donné.
 * @returns tableau des règles avec leur état courant (valid: true/false)
 */
export function evaluatePassword(password: string): PasswordRuleResult[] {
  const cats = password.length > 0 ? evalCategories(password) : {
    lowercase: false, uppercase: false, digit: false, special: false,
  };

  const validCategoryCount = [cats.lowercase, cats.uppercase, cats.digit, cats.special]
    .filter(Boolean).length;

  return [
    {
      id: 'length',
      label: 'Au moins 8 caractères',
      valid: password.length >= 8,
    },
    {
      id: 'categories',
      label: `Au moins ${MIN_CATEGORIES} catégories sur 4`,
      valid: validCategoryCount >= MIN_CATEGORIES,
      subRules: [
        { id: 'lowercase', label: 'Minuscules (a-z)',          valid: cats.lowercase },
        { id: 'uppercase', label: 'Majuscules (A-Z)',          valid: cats.uppercase },
        { id: 'digit',     label: 'Chiffres (0-9)',            valid: cats.digit },
        { id: 'special',   label: 'Caractères spéciaux (!@#…)', valid: cats.special },
      ],
    },
    {
      id: 'noTriple',
      label: 'Pas plus de 2 caractères identiques consécutifs',
      valid: password.length === 0 ? false : noTripleRepeat(password),
    },
  ];
}

/**
 * Retourne true si toutes les règles obligatoires sont respectées
 * (utilisé pour activer/désactiver le bouton Submit).
 */
export function isPasswordValid(password: string): boolean {
  return evaluatePassword(password).every((r) => r.valid);
}
