import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import FacturXGenerationPanel from '../FacturXGenerationPanel';
import api from '../../services/api';

vi.mock('../../services/api');

const generation = (overrides = {}) => ({
  id: 7,
  sourceDocumentId: 42,
  generatedDocumentId: 99,
  filename: 'Factur-X_FACT-001.pdf',
  profile: 'EN16931',
  status: 'VALID',
  validationRunId: 5,
  errorCount: 0,
  warningCount: 0,
  errorMessage: null,
  createdAt: '2026-10-17T10:00:00Z',
  ...overrides,
});

function renderPanel(props = {}) {
  return render(
    <MemoryRouter>
      <FacturXGenerationPanel documentId="42" {...props} />
    </MemoryRouter>
  );
}

const generateButton = () => screen.getByRole('button', { name: /Générer la facture Factur-X/ });

describe('FacturXGenerationPanel - génération Factur-X (F13)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    api.get.mockResolvedValue({ data: [] });
  });

  it("bloque la génération tant que le brouillon n'est pas prêt", async () => {
    renderPanel({ blockedReason: 'Enregistrez d’abord vos corrections avant de générer.' });

    expect(generateButton()).toBeDisabled();
    expect(screen.getByText('Enregistrez d’abord vos corrections avant de générer.')).toBeInTheDocument();
    await waitFor(() => expect(api.get).toHaveBeenCalledWith('/documents/42/generations'));
  });

  it('génère avec le profil choisi et affiche un résultat conforme', async () => {
    api.post.mockResolvedValue({ data: generation({ profile: 'BASIC' }) });
    renderPanel();

    fireEvent.change(screen.getByLabelText('Profil'), { target: { value: 'BASIC' } });
    fireEvent.click(generateButton());

    expect(await screen.findByText('Facture Factur-X générée et conforme.')).toBeInTheDocument();
    expect(api.post).toHaveBeenCalledWith('/documents/42/generate', { profile: 'BASIC' });
    expect(screen.getByRole('button', { name: 'Télécharger Factur-X_FACT-001.pdf' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Voir le rapport de validation' }))
      .toHaveAttribute('href', '/rapport?documentId=99');
  });

  it('signale un fichier généré non conforme', async () => {
    api.post.mockResolvedValue({ data: generation({ status: 'INVALID', errorCount: 2 }) });
    renderPanel();

    fireEvent.click(generateButton());

    expect(await screen.findByText("Le fichier généré n'est pas conforme (2 erreurs).")).toBeInTheDocument();
  });

  it("affiche l'échec technique de la génération", async () => {
    api.post.mockResolvedValue({
      data: generation({ status: 'FAILED', generatedDocumentId: null, validationRunId: null, errorMessage: 'Service indisponible.' }),
    });
    renderPanel();

    fireEvent.click(generateButton());

    expect(await screen.findByText('Service indisponible.')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Télécharger/ })).not.toBeInTheDocument();
  });

  it('affiche les erreurs renvoyées par le serveur', async () => {
    api.post.mockRejectedValue({
      response: { status: 400, data: { error: 'GENERATION_DRAFT_INCOMPLETE', errors: ['Le numéro de TVA intracommunautaire du vendeur est obligatoire pour une facture avec TVA.'] } },
    });
    renderPanel();

    fireEvent.click(generateButton());

    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('Génération impossible');
    expect(alert).toHaveTextContent('TVA intracommunautaire du vendeur');
  });

  it("affiche l'historique et télécharge un fichier précédent", async () => {
    api.get.mockImplementation((url) => (url.endsWith('/generations')
      ? Promise.resolve({ data: [generation({ id: 3, status: 'INVALID', errorCount: 1 })] })
      : Promise.resolve({ data: new Blob(['%PDF']) })));
    URL.createObjectURL = vi.fn(() => 'blob:x');
    URL.revokeObjectURL = vi.fn();
    // jsdom ne sait pas "naviguer" vers le lien de téléchargement.
    const click = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {});
    renderPanel();

    expect(await screen.findByText('Générations précédentes')).toBeInTheDocument();
    expect(screen.getByText('Non conforme')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Télécharger' }));

    await waitFor(() => expect(api.get).toHaveBeenCalledWith('/documents/99/download', { responseType: 'blob' }));
    expect(URL.createObjectURL).toHaveBeenCalled();
    expect(click).toHaveBeenCalled();
    click.mockRestore();
  });
});
