import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import ApiKeysPage, { API_DOCS_URL } from '../ApiKeysPage';
import api from '../../services/api';

vi.mock('../../services/api');

const activeKey = {
  id: 1,
  name: 'ERP Sage',
  prefix: 'fxk_ab12cd34',
  organizationId: 80,
  organizationName: 'Cabinet Test',
  scopes: ['DOCUMENTS_READ', 'DOCUMENTS_WRITE'],
  createdAt: '2026-10-01T10:00:00Z',
  lastUsedAt: null,
  revokedAt: null,
  active: true,
};

function mockGets({ keys = [activeKey], memberships = [{ id: 1, organizationId: 80, role: 'ADMIN' }] } = {}) {
  api.get.mockImplementation((url) => {
    if (url === '/api-keys') return Promise.resolve({ data: keys });
    if (url === '/organizations') return Promise.resolve({ data: memberships });
    if (url.startsWith('/organizations/')) return Promise.resolve({ data: { name: 'Cabinet Test' } });
    return Promise.resolve({ data: [] });
  });
}

function renderPage() {
  return render(
    <MemoryRouter>
      <ApiKeysPage />
    </MemoryRouter>
  );
}

describe('ApiKeysPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('affiche les cles sans jamais le secret, avec leurs scopes et leur statut', async () => {
    mockGets();
    renderPage();

    await waitFor(() => {
      expect(screen.getByText('ERP Sage')).toBeInTheDocument();
    });
    expect(screen.getByText('fxk_ab12cd34…')).toBeInTheDocument();
    expect(screen.getByText('Lecture')).toBeInTheDocument();
    expect(screen.getByText('Dépôt')).toBeInTheDocument();
    expect(screen.getByText('Active')).toBeInTheDocument();
  });

  it('affiche un message quand il n\'y a aucune cle', async () => {
    mockGets({ keys: [] });
    renderPage();

    await waitFor(() => {
      expect(screen.getByText(/Aucune clé API/i)).toBeInTheDocument();
    });
  });

  it('propose un lien vers la documentation de l\'API', async () => {
    mockGets();
    renderPage();

    const link = await screen.findByRole('link', { name: /Documentation de l'API/i });
    expect(link).toHaveAttribute('href', API_DOCS_URL);
  });

  it('cree une cle et affiche le secret une seule fois', async () => {
    mockGets({ keys: [] });
    api.post.mockResolvedValue({
      data: {
        apiKey: { ...activeKey, id: 2, name: 'Nouvelle', scopes: ['DOCUMENTS_READ'] },
        key: 'fxk_secret-value',
      },
    });

    renderPage();
    await waitFor(() => {
      expect(screen.getByText(/Aucune clé API/i)).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /Créer une clé/i }));
    fireEvent.change(screen.getByLabelText('Nom de la clé'), { target: { value: 'Nouvelle' } });
    fireEvent.click(screen.getByRole('button', { name: /Créer la clé/i }));

    await waitFor(() => {
      expect(screen.getByTestId('api-key-secret')).toHaveTextContent('fxk_secret-value');
    });
    expect(api.post).toHaveBeenCalledWith('/api-keys', {
      name: 'Nouvelle',
      organizationId: 80,
      scopes: ['DOCUMENTS_READ'],
    });

    fireEvent.click(screen.getByRole('button', { name: /J'ai copié la clé/i }));
    expect(screen.queryByTestId('api-key-secret')).not.toBeInTheDocument();
    expect(screen.getByText('Nouvelle')).toBeInTheDocument();
    expect(screen.queryByText('fxk_secret-value')).not.toBeInTheDocument();
  });

  it('revoque une cle apres confirmation', async () => {
    mockGets();
    api.delete.mockResolvedValue({
      data: { ...activeKey, active: false, revokedAt: '2026-10-02T10:00:00Z' },
    });

    renderPage();
    await waitFor(() => {
      expect(screen.getByText('ERP Sage')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: 'Révoquer' }));
    fireEvent.click(screen.getAllByRole('button', { name: 'Révoquer' }).at(-1));

    await waitFor(() => {
      expect(screen.getByText('Révoquée')).toBeInTheDocument();
    });
    expect(api.delete).toHaveBeenCalledWith('/api-keys/1');
  });

  it('desactive la creation sans organisation', async () => {
    mockGets({ keys: [], memberships: [] });
    renderPage();

    await waitFor(() => {
      expect(screen.getByText(/Aucune clé API/i)).toBeInTheDocument();
    });
    expect(screen.getByRole('button', { name: /Créer une clé/i })).toBeDisabled();
  });
});
