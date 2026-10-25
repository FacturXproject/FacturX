import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import ExportImportPage from '../ExportImportPage';
import api from '../../services/api';
import { downloadExport } from '../../utils/download';

vi.mock('../../services/api');
vi.mock('../../utils/download', () => ({
  downloadExport: vi.fn(() => Promise.resolve()),
  saveBlob: vi.fn(),
}));

function mockGets(role) {
  api.get.mockImplementation((url) => {
    if (url === '/organizations') {
      return Promise.resolve({ data: role ? [{ id: 1, organizationId: 80, role }] : [] });
    }
    if (url === '/organizations/80') return Promise.resolve({ data: { name: 'Cabinet Test' } });
    return Promise.resolve({ data: [] });
  });
}

function renderPage() {
  return render(
    <MemoryRouter initialEntries={['/organisations/80/export-import']}>
      <Routes>
        <Route path="/organisations/:id/export-import" element={<ExportImportPage />} />
      </Routes>
    </MemoryRouter>
  );
}

function chooseCsv(content = 'email,role\na@x.fr,CLIENT\n') {
  const file = new File([content], 'invitations.csv', { type: 'text/csv' });
  fireEvent.change(screen.getByLabelText("Fichier CSV d'invitations"), { target: { files: [file] } });
  return file;
}

const validRows = [
  { lineNumber: 2, email: 'a@x.fr', role: 'CLIENT', valid: true, error: null },
  { lineNumber: 3, email: 'b@x.fr', role: 'ACCOUNTANT', valid: true, error: null },
];

describe('ExportImportPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("l'admin voit l'export des documents, des membres et l'import", async () => {
    mockGets('ADMIN');
    renderPage();

    expect(await screen.findByText('Exporter les documents')).toBeInTheDocument();
    expect(screen.getByText('Exporter les membres')).toBeInTheDocument();
    expect(screen.getByText('Importer des invitations')).toBeInTheDocument();
    expect(screen.getByText('Cabinet Test')).toBeInTheDocument();
  });

  it('exporte les documents dans le format choisi', async () => {
    mockGets('ADMIN');
    renderPage();

    fireEvent.click(await screen.findByRole('button', { name: 'Exporter les documents en PDF' }));
    await waitFor(() => {
      expect(downloadExport).toHaveBeenCalledWith('80', 'documents', 'pdf');
    });

    fireEvent.click(screen.getByRole('button', { name: 'Exporter les membres en CSV' }));
    await waitFor(() => {
      expect(downloadExport).toHaveBeenCalledWith('80', 'members', 'csv');
    });
  });

  it("le comptable n'exporte que les documents", async () => {
    mockGets('ACCOUNTANT');
    renderPage();

    expect(await screen.findByText('Exporter les documents')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Exporter les documents en CSV' })).toBeInTheDocument();
    expect(screen.queryByText('Exporter les membres')).not.toBeInTheDocument();
    expect(screen.queryByText('Importer des invitations')).not.toBeInTheDocument();
  });

  it("le client n'a pas accès", async () => {
    mockGets('CLIENT');
    renderPage();

    expect(await screen.findByText(/réservés aux administrateurs et aux comptables/i)).toBeInTheDocument();
    expect(screen.queryByText('Exporter les documents')).not.toBeInTheDocument();
  });

  it("affiche le message d'erreur d'un export refusé", async () => {
    mockGets('ADMIN');
    const body = new Blob([JSON.stringify({ error: 'ACCESS_DENIED', message: 'Accès refusé.' })]);
    downloadExport.mockRejectedValueOnce({ response: { status: 403, data: body } });
    renderPage();

    fireEvent.click(await screen.findByRole('button', { name: 'Exporter les documents en JSON' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Accès refusé.');
  });

  it("vérifie le fichier à blanc puis envoie les invitations", async () => {
    mockGets('ADMIN');
    api.post
      .mockResolvedValueOnce({ data: validRows })
      .mockResolvedValueOnce({ data: validRows });
    renderPage();
    await screen.findByText('Importer des invitations');

    chooseCsv();

    expect(await screen.findByTestId('import-summary')).toHaveTextContent('2 lignes prêtes, 0 en erreur');
    expect(screen.getByText('b@x.fr')).toBeInTheDocument();
    expect(screen.getByText('Comptable')).toBeInTheDocument();
    expect(api.post).toHaveBeenNthCalledWith(1, '/organizations/80/import/invitations',
      expect.any(FormData), { params: { dryRun: true } });

    fireEvent.click(screen.getByRole('button', { name: 'Envoyer 2 invitations' }));

    expect(await screen.findByRole('status')).toHaveTextContent('2 invitations envoyées.');
    expect(api.post).toHaveBeenNthCalledWith(2, '/organizations/80/import/invitations',
      expect.any(FormData), { params: { dryRun: false } });
    expect(screen.queryByTestId('import-summary')).not.toBeInTheDocument();
  });

  it("bloque l'envoi tant qu'une ligne est en erreur", async () => {
    mockGets('ADMIN');
    api.post.mockResolvedValueOnce({
      data: [
        validRows[0],
        { lineNumber: 3, email: 'pas-un-email', role: 'CLIENT', valid: false, error: 'Email vide ou invalide' },
      ],
    });
    renderPage();
    await screen.findByText('Importer des invitations');

    chooseCsv();

    expect(await screen.findByTestId('import-summary')).toHaveTextContent('1 ligne prête, 1 en erreur');
    expect(screen.getByText('Email vide ou invalide')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Envoyer 1 invitation' })).toBeDisabled();
    expect(api.post).toHaveBeenCalledTimes(1);
  });

  it('affiche le motif quand le fichier est refusé', async () => {
    mockGets('ADMIN');
    api.post.mockRejectedValueOnce({
      response: { status: 400, data: { error: 'INVALID_IMPORT_FILE', message: 'Colonnes obligatoires : email et role.' } },
    });
    renderPage();
    await screen.findByText('Importer des invitations');

    chooseCsv('mail\na@x.fr\n');

    expect(await screen.findByRole('alert')).toHaveTextContent('Colonnes obligatoires : email et role.');
    expect(screen.queryByTestId('import-summary')).not.toBeInTheDocument();
  });
});
