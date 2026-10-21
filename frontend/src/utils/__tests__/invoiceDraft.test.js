import { describe, it, expect } from 'vitest';
import {
  buildDraftPayload,
  calculateTotals,
  formatCents,
  formatRate,
  lineTotalCents,
  parseDate,
  parseDecimal,
  parseRate,
  validateDraft,
} from '../invoiceDraft';

const line = (qty, pu, extra = {}) => ({ description: 'Prestation', qty, pu, tva: '', ...extra });

const validFields = () => ({
  numeroFacture: { value: 'F-1' },
  dateFacture: { value: '15/03/2026' },
  dateEcheance: { value: '' },
  vendeurNom: { value: 'ACME' },
  acheteurNom: { value: 'Client' },
});

describe('invoiceDraft - règles du formulaire F12 (identiques au serveur)', () => {
  it('lit les formats de nombres français et anglais, refuse la notation scientifique', () => {
    expect(parseDecimal('1 080,50')).toBe('1080.50');
    expect(parseDecimal('1 080,50')).toBe('1080.50');
    expect(parseDecimal('1.080,50')).toBe('1080.50');
    expect(parseDecimal('1,080.50')).toBe('1080.50');
    expect(parseDecimal('1e3')).toBeNull();
    expect(parseDecimal('1E+100000000')).toBeNull();
    expect(parseDecimal('abc')).toBeNull();
    expect(parseDecimal('')).toBeNull();
    expect(parseDecimal('1.1234567')).toBeNull();
    expect(parseRate('5,5 %')).toBe('5.5');
    expect(parseRate('100')).toBe('100');
    expect(parseRate('100.01')).toBeNull();
    expect(parseRate('-1')).toBeNull();
  });

  it('valide les dates comme le serveur', () => {
    expect(parseDate('15/03/2026')).not.toBeNull();
    expect(parseDate('15.03.2026')).not.toBeNull();
    expect(parseDate('2026-03-15')).not.toBeNull();
    expect(parseDate('31/02/2026')).toBeNull();
    expect(parseDate('demain')).toBeNull();
  });

  it('calcule au centime près comme le serveur (arrondi par ligne)', () => {
    expect(lineTotalCents(line('3', '33.335'))).toBe(10001n);
    // 3 x 0,005 : chaque ligne arrondie à 0,01, sous-total 0,03 (et non 0,02).
    const totals = calculateTotals([line('1', '0.005'), line('1', '0.005'), line('1', '0.005')], '20');
    expect(totals.totalHT).toBe(3n);
  });

  it('ventile la TVA par taux, le taux de ligne remplaçant le taux par défaut', () => {
    const totals = calculateTotals([
      line('5', '750.00'),
      line('2', '10,00', { tva: '5,5' }),
    ], '20');

    expect(totals.totalHT).toBe(377000n);
    expect(totals.taxes.map(t => [t.rate, t.baseCents, t.taxCents])).toEqual([
      ['20', 375000n, 75000n],
      ['5.5', 2000n, 110n],
    ]);
    expect(totals.totalVAT).toBe(75110n);
    expect(totals.totalTTC).toBe(452110n);
    expect(formatCents(totals.totalTTC)).toBe('4 521,10 €');
    expect(formatRate('5.5')).toBe('5,5');
    expect(formatRate('20.00')).toBe('20');
  });

  it('accepte un formulaire valide', () => {
    expect(validateDraft(validFields(), '20', [line('1', '10')])).toEqual([]);
  });

  it('signale tous les champs obligatoires manquants', () => {
    const errors = validateDraft({}, '', []);
    expect(errors.map(e => e.message)).toEqual([
      'Le numéro de facture est obligatoire.',
      'La date de facture est obligatoire.',
      'Le nom du vendeur est obligatoire.',
      "Le nom de l'acheteur est obligatoire.",
      'Le taux de TVA est obligatoire.',
      'La facture doit contenir au moins une ligne.',
    ]);
  });

  it('signale les valeurs de ligne invalides avec la clé du champ', () => {
    const errors = validateDraft(validFields(), '20', [
      { description: ' ', qty: '0', pu: '-1', tva: '150' },
    ]);
    expect(errors.map(e => e.key)).toEqual(['line:0:description', 'line:0:qty', 'line:0:pu', 'line:0:tva']);
  });

  it("refuse une date invalide et une échéance avant la date de facture", () => {
    const fields = validFields();
    fields.dateFacture.value = '31/02/2026';
    expect(validateDraft(fields, '20', [line('1', '1')]).map(e => e.key)).toEqual(['dateFacture']);

    const fields2 = validFields();
    fields2.dateEcheance.value = '14/03/2026';
    expect(validateDraft(fields2, '20', [line('1', '1')])[0].message)
      .toBe("La date d'échéance ne peut pas précéder la date de facture.");
  });

  it("construit la requête sans totaux, avec la version chargée", () => {
    const payload = buildDraftPayload(
      { ...validFields(), vendeurNom: { value: '  ACME \n SAS ' } },
      '20',
      [line('2', '10,5', { tva: '5,5' })],
      '2026-10-16T10:00:00Z',
    );
    expect(payload.updatedAt).toBe('2026-10-16T10:00:00Z');
    expect(payload.fields.sellerName).toBe('ACME SAS');
    expect(payload.fields.vatRate).toBe('20');
    expect(payload.fields).not.toHaveProperty('totalHt');
    expect(payload.lines).toEqual([
      { description: 'Prestation', quantity: '2', unitPrice: '10,5', vatRate: '5,5' },
    ]);
  });
});
