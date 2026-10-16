import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import Conversion from '../Conversion';
import api from '../../services/api';

vi.mock('../../services/api');

const draft = (overrides = {}) => ({
  id: 1,
  documentId: 42,
  updatedAt: '2026-10-16T10:00:00Z',
  fields: {
    invoiceNumber: { value: 'FACT-001', confidence: 0.9, source: 'pdf-extraction-v1' },
    invoiceDate: { value: '15/03/2026', confidence: 0.8, source: 'pdf-extraction-v1' },
    sellerName: { value: 'ACME SAS', confidence: 0.3, source: 'pdf-extraction-v1' },
    buyerName: { value: 'Client SARL', confidence: 0.9, source: 'pdf-extraction-v1' },
    vatRate: { value: '20', confidence: 0.75, source: 'pdf-extraction-v1' },
  },
  lines: [
    { description: 'Prestation conseil', quantity: '5', unitPrice: '750.00', vatRate: null, total: '3750.00', confidence: 0.5 },
  ],
  taxes: [],
  ...overrides,
});

function renderPage() {
  return render(
    <MemoryRouter initialEntries={['/conversion?documentId=42']}>
      <Conversion />
    </MemoryRouter>
  );
}

async function loaded() {
  renderPage();
  await screen.findByDisplayValue('FACT-001');
}

const save = () => fireEvent.click(screen.getByRole('button', { name: 'Enregistrer les corrections' }));

describe('Conversion - formulaire de correction humaine (F12)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    api.get.mockResolvedValue({ data: draft() });
  });

  it('affiche le brouillon, les indicateurs de confiance et les totaux recalculés', async () => {
    await loaded();

    expect(api.get).toHaveBeenCalledWith('/documents/42/draft');
    expect(screen.getByDisplayValue('Prestation conseil')).toBeInTheDocument();
    // sellerName à 0,3 : "à vérifier".
    expect(document.getElementById('field-vendeurNom')).toHaveStyle({ background: '#fffdf0' });
    expect(screen.getAllByLabelText('À vérifier').length).toBeGreaterThan(0);
    // Testing Library ramène l'espace fine insécable (séparateur de milliers) à une espace simple.
    expect(screen.getByText('Total HT', { selector: 'span' }).nextSibling).toHaveTextContent('3 750,00 €');
    expect(screen.getByText('TVA 20 % sur 3 750,00 €')).toBeInTheDocument();
    expect(screen.getByText('Total TTC', { selector: 'span' }).nextSibling).toHaveTextContent('4 500,00 €');
  });

  it("refuse l'enregistrement si un champ obligatoire est vide, sans appeler l'API", async () => {
    await loaded();

    fireEvent.change(screen.getByDisplayValue('Client SARL'), { target: { value: '  ' } });
    save();

    expect(await screen.findByRole('alert')).toHaveTextContent("Le nom de l'acheteur est obligatoire.");
    expect(api.put).not.toHaveBeenCalled();
  });

  it('ajoute et supprime des lignes en recalculant les totaux', async () => {
    await loaded();

    fireEvent.click(screen.getByRole('button', { name: '+ Ajouter une ligne' }));
    fireEvent.change(screen.getByLabelText('Description de la ligne 2'), { target: { value: 'Livre' } });
    fireEvent.change(screen.getByLabelText('Prix unitaire de la ligne 2'), { target: { value: '10,00' } });
    fireEvent.change(screen.getByLabelText('Taux de TVA de la ligne 2'), { target: { value: '5,5' } });

    expect(screen.getByText('TVA 5,5 % sur 10,00 €')).toBeInTheDocument();
    expect(screen.getByText('Total TTC', { selector: 'span' }).nextSibling).toHaveTextContent('4 510,55 €');

    fireEvent.click(screen.getByRole('button', { name: 'Supprimer la ligne 1' }));
    expect(screen.queryByDisplayValue('Prestation conseil')).not.toBeInTheDocument();
    expect(screen.getByText('Total TTC', { selector: 'span' }).nextSibling).toHaveTextContent('10,55 €');
  });

  it('envoie les corrections sans totaux, avec la version chargée, puis affiche la réponse du serveur', async () => {
    api.put.mockResolvedValue({
      data: draft({
        updatedAt: '2026-10-16T10:05:00Z',
        fields: {
          ...draft().fields,
          invoiceNumber: { value: 'FACT-002', confidence: null, source: 'manual' },
        },
      }),
    });
    await loaded();

    fireEvent.change(screen.getByDisplayValue('FACT-001'), { target: { value: 'FACT-002' } });
    expect(screen.getByText('Modifications non enregistrées.')).toBeInTheDocument();
    save();

    expect(await screen.findByRole('status')).toHaveTextContent('Corrections enregistrées avec succès.');
    const [url, payload] = api.put.mock.calls[0];
    expect(url).toBe('/documents/42/draft');
    expect(payload.updatedAt).toBe('2026-10-16T10:00:00Z');
    expect(payload.fields.invoiceNumber).toBe('FACT-002');
    expect(payload.fields).not.toHaveProperty('totalTtc');
    expect(payload.lines).toEqual([
      { description: 'Prestation conseil', quantity: '5', unitPrice: '750.00', vatRate: '' },
    ]);
    expect(screen.getByDisplayValue('FACT-002')).toBeInTheDocument();
    expect(screen.queryByText('Modifications non enregistrées.')).not.toBeInTheDocument();

    // Sauvegarde suivante : nouvelle version renvoyée par le serveur.
    save();
    await waitFor(() => expect(api.put).toHaveBeenCalledTimes(2));
    expect(api.put.mock.calls[1][1].updatedAt).toBe('2026-10-16T10:05:00Z');
  });

  it('affiche les erreurs renvoyées par le serveur', async () => {
    api.put.mockRejectedValue({
      response: {
        status: 400,
        data: { error: 'DRAFT_INVALID', message: 'x', errors: ['Champ refusé par le serveur.'] },
      },
    });
    await loaded();

    save();

    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('Le serveur a refusé les corrections');
    expect(alert).toHaveTextContent('Champ refusé par le serveur.');
  });

  it('affiche le message de conflit si le brouillon a changé entre-temps', async () => {
    api.put.mockRejectedValue({
      response: {
        status: 409,
        data: { error: 'DRAFT_CONFLICT', message: 'Ce brouillon a été modifié entre-temps.' },
      },
    });
    await loaded();

    save();

    expect(await screen.findByRole('alert')).toHaveTextContent('Ce brouillon a été modifié entre-temps.');
  });

  it('marque un champ corrigé comme "Corrigé manuellement"', async () => {
    await loaded();
    const before = screen.queryAllByLabelText('Corrigé manuellement').length;

    fireEvent.change(screen.getByDisplayValue('ACME SAS'), { target: { value: 'ACME' } });

    expect(screen.getAllByLabelText('Corrigé manuellement').length).toBe(before + 1);
  });
});
