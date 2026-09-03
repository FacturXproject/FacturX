import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import OrganizationsPage from '../OrganizationsPage';
import api from '../../services/api';

vi.mock('../../services/api');

const mockOrganizations = [
  { id: 1, organizationId: 80, userId: 4, role: 'ADMIN', joinedAt: '2026-01-01' },
  { id: 2, organizationId: 81, userId: 4, role: 'CLIENT', joinedAt: '2026-01-02' },
];

function renderPage() {
  return render(
    <MemoryRouter>
      <OrganizationsPage />
    </MemoryRouter>
  );
}

describe('OrganizationsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('affiche le loader pendant le chargement', () => {
    api.get.mockReturnValue(new Promise(() => {}));
    renderPage();
    expect(screen.getByText(/Chargement des organisations/i)).toBeInTheDocument();
  });

  it('affiche la liste des organisations une fois chargees', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/organizations') return Promise.resolve({ data: mockOrganizations });
      if (url.startsWith('/organizations/')) return Promise.resolve({ data: { name: 'Cabinet Test' } });
      return Promise.resolve({ data: [] });
    });

    renderPage();

    await waitFor(() => {
      expect(screen.getAllByText('Cabinet Test').length).toBeGreaterThan(0);
    });
  });

  it('affiche un message quand il n\'y a aucune organisation', async () => {
    api.get.mockResolvedValue({ data: [] });

    renderPage();

    await waitFor(() => {
      expect(screen.getByText(/Aucune organisation à afficher/i)).toBeInTheDocument();
    });
  });

  it('affiche une erreur si l\'appel API echoue', async () => {
    api.get.mockRejectedValue(new Error('Erreur réseau'));

    renderPage();

    await waitFor(() => {
      expect(screen.getByText(/Erreur/i)).toBeInTheDocument();
    });
  });

  it('ouvre le modal de creation au clic sur le bouton', async () => {
    api.get.mockResolvedValue({ data: [] });

    renderPage();

    await waitFor(() => {
      expect(screen.getByText(/Aucune organisation/i)).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText(/Créer une organisation/i));

    expect(screen.getByPlaceholderText('Cabinet Dupont')).toBeInTheDocument();
  });

  it('filtre les organisations par role au clic sur un onglet', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/organizations') return Promise.resolve({ data: mockOrganizations });
      return Promise.resolve({ data: { name: 'Cabinet Test' } });
    });

    renderPage();

    await waitFor(() => {
      expect(screen.getAllByText('Cabinet Test').length).toBe(2);
    });

   fireEvent.click(screen.getByRole('button', { name: 'Administrateur' }));

    await waitFor(() => {
      expect(screen.getAllByText('Cabinet Test').length).toBe(1);
    });
  });
});