import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import DocumentDetail from '../DocumentDetail';
import api from '../../services/api';
import { downloadDocument } from '../../utils/download';

vi.mock('../../services/api');
vi.mock('../../utils/download');
vi.mock('../../context/DocumentWebSocketContext', () => ({
  useDocumentStatusEvent: vi.fn(),
}));

const sourceDocument = {
  id: 42,
  filename: 'facture.pdf',
  type: 'application/pdf',
  size: 2048,
  status: 'UPLOADED',
  organizationId: 1,
  organizationName: 'Cabinet Test',
  ownerName: 'Jean Dupont',
  uploadedAt: '2026-10-17T09:00:00Z',
};

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

function mockApi({ generations }) {
  api.get.mockImplementation((url) => {
    if (url === '/documents/42') return Promise.resolve({ data: sourceDocument });
    if (url === '/documents/42/generations') return generations;
    return Promise.reject({ response: { status: 404 } });
  });
}

function renderPage() {
  return render(
    <MemoryRouter initialEntries={['/documents/42']}>
      <Routes>
        <Route path="/documents/:id" element={<DocumentDetail />} />
      </Routes>
    </MemoryRouter>
  );
}

describe('DocumentDetail - factures Factur-X générées (F13)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('liste les factures générées avec téléchargement et lien vers le rapport', async () => {
    mockApi({
      generations: Promise.resolve({
        data: [generation(), generation({ id: 8, generatedDocumentId: 100, profile: 'BASIC', status: 'INVALID' })],
      }),
    });
    renderPage();

    expect(await screen.findByText('Factures Factur-X générées')).toBeInTheDocument();
    expect(screen.getByText('Conforme')).toBeInTheDocument();
    expect(screen.getByText('Non conforme')).toBeInTheDocument();
    const reports = screen.getAllByRole('link', { name: 'Rapport' });
    expect(reports[0]).toHaveAttribute('href', '/rapport?documentId=99');
    expect(reports[1]).toHaveAttribute('href', '/rapport?documentId=100');

    fireEvent.click(screen.getAllByRole('button', { name: 'Télécharger' })[0]);
    await waitFor(() => expect(downloadDocument).toHaveBeenCalledWith(99, 'Factur-X_FACT-001.pdf'));
  });

  it("masque la section quand l'utilisateur n'a pas accès aux générations", async () => {
    mockApi({ generations: Promise.reject({ response: { status: 403 } }) });
    renderPage();

    expect(await screen.findByText('facture.pdf')).toBeInTheDocument();
    expect(screen.queryByText('Factures Factur-X générées')).not.toBeInTheDocument();
  });
});
