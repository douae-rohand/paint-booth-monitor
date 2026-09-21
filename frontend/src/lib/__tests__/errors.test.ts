import { extractErrorMessage } from '../errors';

describe('extractErrorMessage', () => {
  const FALLBACK = 'Message par défaut';

  it('retourne message quand err.response.data.message existe', () => {
    const err = { response: { data: { message: 'Erreur test' } } };
    expect(extractErrorMessage(err, FALLBACK)).toBe('Erreur test');
  });

  it('retourne fallback quand err est undefined', () => {
    expect(extractErrorMessage(undefined, FALLBACK)).toBe(FALLBACK);
  });

  it('retourne fallback quand err est null', () => {
    expect(extractErrorMessage(null, FALLBACK)).toBe(FALLBACK);
  });

  it('retourne fallback quand err n\'a pas de propriété response', () => {
    const err = new Error('erreur JS standard');
    expect(extractErrorMessage(err, FALLBACK)).toBe(FALLBACK);
  });

  it('retourne fallback quand err.response existe mais sans data', () => {
    const err = { response: {} };
    expect(extractErrorMessage(err, FALLBACK)).toBe(FALLBACK);
  });

  it('retourne fallback quand err.response.data existe mais sans message', () => {
    const err = { response: { data: {} } };
    expect(extractErrorMessage(err, FALLBACK)).toBe(FALLBACK);
  });

  it('retourne fallback quand err.response.data.message est une chaîne vide', () => {
    const err = { response: { data: { message: '' } } };
    expect(extractErrorMessage(err, FALLBACK)).toBe(FALLBACK);
  });

  it('retourne fallback quand err est une primitive', () => {
    expect(extractErrorMessage('une chaîne', FALLBACK)).toBe(FALLBACK);
    expect(extractErrorMessage(404, FALLBACK)).toBe(FALLBACK);
  });
});
