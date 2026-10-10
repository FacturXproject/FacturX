import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import ApiKeys from '../ApiKeys';
import api from '../../services/api';

vi.mock('../../services/api');

const activeKey = {
  id: 1,
  name: 'Logiciel comptable',
  organizationId: 80,
  organizationName: 'Cabinet Test',
  keyPrefix: 'fxk_ab12cd34',
  scopes: ['documents:read', 'documents:write'],
  createdAt: '2026-10-01T10:00:00Z',
  lastUsedAt: null,
  revokedAt: null,
};

const revokedKey = {
  ...activeKey,
  id: 2,
  name: 'Ancienne clé',
  keyPrefix: 'fxk_ef56ab78',
  revokedAt: '2026-10-02T10:00:00Z',
};

function mockApi(keys) {
  api.get.mockImplementation((url) => {
    if (url === '/api-keys') return Promise.resolve({ data: keys });
    if (url === '/organizations') {
      return Promise.resolve({ data: [{ id: 1, organizationId: 80, role: 'ADMIN' }] });
    }
    if (url === '/organizations/80') return Promise.resolve({ data: { name: 'Cabinet Test' } });
    return Promise.resolve({ data: [] });
  });
}

describe('ApiKeys', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('affiche le loader pendant le chargement', () => {
    api.get.mockReturnValue(new Promise(() => {}));
    render(<ApiKeys />);
    expect(screen.getByText(/Chargement des clés API/i)).toBeInTheDocument();
  });

  it('affiche un message quand il n\'y a aucune cle', async () => {
    mockApi([]);
    render(<ApiKeys />);

    await waitFor(() => {
      expect(screen.getByText('Aucune clé API')).toBeInTheDocument();
    });
  });

  it('affiche une erreur si l\'appel API echoue', async () => {
    api.get.mockRejectedValue(new Error('Erreur réseau'));
    render(<ApiKeys />);

    await waitFor(() => {
      expect(screen.getByText(/Impossible de charger les clés API/i)).toBeInTheDocument();
    });
  });

  it('liste les cles avec leur prefixe et leur etat, sans bouton Revoquer sur une cle revoquee', async () => {
    mockApi([activeKey, revokedKey]);
    render(<ApiKeys />);

    await waitFor(() => {
      expect(screen.getByText('Logiciel comptable')).toBeInTheDocument();
    });

    expect(screen.getByText('fxk_ab12cd34…')).toBeInTheDocument();
    expect(screen.getAllByText('Lecture, Écriture').length).toBe(2);
    expect(screen.getByText('Active')).toBeInTheDocument();
    expect(screen.getByText('Révoquée')).toBeInTheDocument();
    expect(screen.getAllByRole('button', { name: /Révoquer/i }).length).toBe(1);
  });

  it('propose un lien vers la documentation de l\'API', async () => {
    mockApi([]);
    render(<ApiKeys />);

    await waitFor(() => {
      expect(screen.getByText('Aucune clé API')).toBeInTheDocument();
    });

    const link = screen.getByRole('link', { name: /Documentation de l’API/i });
    expect(link).toHaveAttribute('href', '/api/public/docs');
  });

  it('cree une cle et l\'affiche une seule fois', async () => {
    mockApi([]);
    api.post.mockResolvedValue({
      data: { key: 'fxk_ab12cd34secretsecret', apiKey: activeKey },
    });
    render(<ApiKeys />);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Nouvelle clé/i })).not.toBeDisabled();
    });
    fireEvent.click(screen.getByRole('button', { name: /Nouvelle clé/i }));

    fireEvent.change(screen.getByLabelText('Nom de la clé'), {
      target: { value: 'Logiciel comptable' },
    });
    fireEvent.click(screen.getByLabelText(/Écriture/i));
    fireEvent.click(screen.getByRole('button', { name: /Créer la clé/i }));

    await waitFor(() => {
      expect(screen.getByTestId('created-key')).toHaveTextContent('fxk_ab12cd34secretsecret');
    });

    expect(api.post).toHaveBeenCalledWith('/api-keys', {
      name: 'Logiciel comptable',
      organizationId: 80,
      scopes: ['documents:read', 'documents:write'],
    });

    // Once the dialog is closed the full key is gone; only its prefix remains in the list.
    fireEvent.click(screen.getByRole('button', { name: /J’ai copié la clé/i }));
    expect(screen.queryByTestId('created-key')).not.toBeInTheDocument();
    expect(screen.getByText('fxk_ab12cd34…')).toBeInTheDocument();
  });

  it('ne permet pas de creer une cle sans permission', async () => {
    mockApi([]);
    render(<ApiKeys />);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Nouvelle clé/i })).not.toBeDisabled();
    });
    fireEvent.click(screen.getByRole('button', { name: /Nouvelle clé/i }));

    fireEvent.change(screen.getByLabelText('Nom de la clé'), { target: { value: 'Clé' } });
    fireEvent.click(screen.getByLabelText(/Lecture/i));

    expect(screen.getByRole('button', { name: /Créer la clé/i })).toBeDisabled();
  });

  it('revoque une cle apres confirmation', async () => {
    mockApi([activeKey]);
    api.delete.mockResolvedValue({});
    render(<ApiKeys />);

    await waitFor(() => {
      expect(screen.getByText('Logiciel comptable')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /Révoquer/i }));
    expect(api.delete).not.toHaveBeenCalled();
    expect(screen.getByText(/perdront l’accès immédiatement/i)).toBeInTheDocument();

    mockApi([{ ...activeKey, revokedAt: '2026-10-03T10:00:00Z' }]);
    const buttons = screen.getAllByRole('button', { name: /^Révoquer$/i });
    fireEvent.click(buttons[buttons.length - 1]);

    await waitFor(() => {
      expect(api.delete).toHaveBeenCalledWith('/api-keys/1');
      expect(screen.getByText('Révoquée')).toBeInTheDocument();
    });
  });
});
