import { evaluatePassword, isPasswordValid } from '../password-rules';

describe('evaluatePassword', () => {
  it('mot de passe vide : toutes les règles invalides', () => {
    const rules = evaluatePassword('');
    const lengthRule = rules.find((r) => r.id === 'length');
    const categoriesRule = rules.find((r) => r.id === 'categories');
    const noTripleRule = rules.find((r) => r.id === 'noTriple');

    expect(lengthRule?.valid).toBe(false);
    expect(categoriesRule?.valid).toBe(false);
    expect(noTripleRule?.valid).toBe(false);

    const subRules = categoriesRule?.subRules ?? [];
    subRules.forEach((sub) => expect(sub.valid).toBe(false));
  });

  it('mot de passe de 7 caractères : règle length invalide', () => {
    const rules = evaluatePassword('Abc123!');
    const lengthRule = rules.find((r) => r.id === 'length');
    expect(lengthRule?.valid).toBe(false);
  });

  it('mot de passe de 8 caractères exactement : règle length valide', () => {
    const rules = evaluatePassword('Abc1234!');
    const lengthRule = rules.find((r) => r.id === 'length');
    expect(lengthRule?.valid).toBe(true);
  });

  it('mot de passe avec seulement minuscules : categories invalide (1/4)', () => {
    const rules = evaluatePassword('abcdefgh');
    const categoriesRule = rules.find((r) => r.id === 'categories');
    expect(categoriesRule?.valid).toBe(false);

    const subRules = categoriesRule?.subRules ?? [];
    const lowercaseSub = subRules.find((s) => s.id === 'lowercase');
    const uppercaseSub = subRules.find((s) => s.id === 'uppercase');
    const digitSub = subRules.find((s) => s.id === 'digit');
    const specialSub = subRules.find((s) => s.id === 'special');

    expect(lowercaseSub?.valid).toBe(true);
    expect(uppercaseSub?.valid).toBe(false);
    expect(digitSub?.valid).toBe(false);
    expect(specialSub?.valid).toBe(false);
  });

  it('mot de passe avec exactement 3 catégories sur 4 : categories valide', () => {
    const rules = evaluatePassword('Abcdefg1');
    const categoriesRule = rules.find((r) => r.id === 'categories');
    expect(categoriesRule?.valid).toBe(true);

    const subRules = categoriesRule?.subRules ?? [];
    expect(subRules.find((s) => s.id === 'lowercase')?.valid).toBe(true);
    expect(subRules.find((s) => s.id === 'uppercase')?.valid).toBe(true);
    expect(subRules.find((s) => s.id === 'digit')?.valid).toBe(true);
    expect(subRules.find((s) => s.id === 'special')?.valid).toBe(false);
  });

  it('mot de passe avec les 4 catégories : categories valide, les 4 subRules à true', () => {
    const rules = evaluatePassword('Abcdefg1!');
    const categoriesRule = rules.find((r) => r.id === 'categories');
    expect(categoriesRule?.valid).toBe(true);

    const subRules = categoriesRule?.subRules ?? [];
    subRules.forEach((sub) => expect(sub.valid).toBe(true));
  });

  it('mot de passe avec triplet de caractères identiques consécutifs : règle noTriple invalide', () => {
    const rules = evaluatePassword('Abcaaa1!');
    const noTripleRule = rules.find((r) => r.id === 'noTriple');
    expect(noTripleRule?.valid).toBe(false);
  });

  it('mot de passe avec paire répétée mais pas triplet : règle noTriple valide', () => {
    const rules = evaluatePassword('Abccdd1!');
    const noTripleRule = rules.find((r) => r.id === 'noTriple');
    expect(noTripleRule?.valid).toBe(true);
  });

  it('mot de passe conforme à toutes les règles : les 3 règles top-level valides', () => {
    const rules = evaluatePassword('Abcdefg1!');
    rules.forEach((r) => expect(r.valid).toBe(true));
  });
});

describe('isPasswordValid', () => {
  it('retourne true pour un mot de passe conforme à toutes les règles', () => {
    expect(isPasswordValid('Abcdefg1!')).toBe(true);
  });

  it('retourne false si au moins une règle échoue (ex: trop court)', () => {
    expect(isPasswordValid('Abc1!')).toBe(false);
  });

  it('retourne false pour une chaîne vide', () => {
    expect(isPasswordValid('')).toBe(false);
  });
});
