import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import Documents from '../Documents';
import api from '../../services/api';

vi.mock('../../services/api');

const page = (content, extra = {}) => ({
  content,
  page: 0,
  size: 20,
  totalElements: content.length,
  totalPages: 1,
  sort: 'uploadedAt,desc',
  ...extra,
});

const doc = (id, filename, status = 'VALID') => ({
  id,
  filename,
  status,
  type: 'application/pdf',
  size: 2048,
  ownerName: 'Jean Dupont',
  uploadedAt: '2026-03-10T10:00:00Z',
});

function mockApi({ role = 'ADMIN', search = () => page([doc(1, 'facture-001.pdf')]) } = {}) {
  api.get.mockImplementation((url, config) => {
    if (url === '/organizations') return Promise.resolve({ data: [{ organizationId: 5, role }] });
    if (url === '/organizations/5') return Promise.resolve({ data: { name: 'Cabinet A' } });
    if (url === '/organizations/5/members') {
      return Promise.resolve({ data: [{ userId: 9, firstName: 'Marie', lastName: 'Curie' }] });
    }
    if (url === '/documents/search') return Promise.resolve({ data: search(config.params) });
    return Promise.resolve({ data: {} });
  });
}

const searchCalls = () => api.get.mock.calls.filter(([url]) => url === '/documents/search').map(([, c]) => c.params);

function renderAt(path = '/documents') {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Documents />
    </MemoryRouter>
  );
}

describe('Documents - recherche avancee (F16)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('cherche dans la premiere organisation et affiche les resultats', async () => {
    mockApi();
    renderAt();

    expect(await screen.findByText('facture-001.pdf')).toBeInTheDocument();
    expect(searchCalls()[0]).toMatchObject({ organizationId: '5', sort: 'uploadedAt,desc', page: 0, size: 20 });
    expect(screen.getByText('1 document')).toBeInTheDocument();
  });

  it('envoie la recherche par nom apres la saisie (debounce)', async () => {
    mockApi();
    renderAt();
    await screen.findByText('facture-001.pdf');

    fireEvent.change(screen.getByLabelText('Rechercher par nom de fichier'), { target: { value: 'acme' } });

    await waitFor(() => expect(searchCalls().at(-1)).toMatchObject({ q: 'acme' }));
  });

  it('combine plusieurs statuts en une liste separee par des virgules', async () => {
    mockApi();
    renderAt();
    await screen.findByText('facture-001.pdf');

    fireEvent.click(screen.getByRole('button', { name: 'INVALID' }));
    await waitFor(() => expect(searchCalls().at(-1)).toMatchObject({ status: 'INVALID' }));

    fireEvent.click(screen.getByRole('button', { name: 'FAILED' }));
    await waitFor(() => expect(searchCalls().at(-1)).toMatchObject({ status: 'INVALID,FAILED' }));
  });

  it('reprend les filtres depuis l URL', async () => {
    mockApi();
    renderAt('/documents?org=5&error=BR-02&type=XML&page=2');

    await waitFor(() =>
      expect(searchCalls()[0]).toMatchObject({ organizationId: '5', error: 'BR-02', type: 'XML', page: 2 })
    );
  });

  it('pagine et revient en page 1 quand un filtre change', async () => {
    mockApi({ search: (p) => page([doc(p.page + 1, `doc-page-${p.page}.pdf`)], { page: p.page, totalPages: 3, totalElements: 50 }) });
    renderAt();
    await screen.findByText('doc-page-0.pdf');

    fireEvent.click(screen.getByLabelText('Page suivante'));
    expect(await screen.findByText('doc-page-1.pdf')).toBeInTheDocument();
    expect(screen.getByText('Page 2 / 3')).toBeInTheDocument();

    fireEvent.change(screen.getByDisplayValue('Tous'), { target: { value: 'PDF' } });
    await waitFor(() => expect(searchCalls().at(-1)).toMatchObject({ type: 'PDF', page: 0 }));
  });

  it('propose le filtre utilisateur a un admin mais pas a un client', async () => {
    mockApi({ role: 'ADMIN' });
    const { unmount } = renderAt();
    expect(await screen.findByText('Marie Curie')).toBeInTheDocument();
    unmount();

    vi.clearAllMocks();
    mockApi({ role: 'CLIENT' });
    renderAt();
    await screen.findByText('facture-001.pdf');
    expect(screen.queryByText('Déposé par', { selector: 'label' })).not.toBeInTheDocument();
    expect(api.get).not.toHaveBeenCalledWith('/organizations/5/members');
  });

  it('affiche le message du backend pour un filtre invalide', async () => {
    mockApi();
    api.get.mockImplementation((url) => {
      if (url === '/organizations') return Promise.resolve({ data: [{ organizationId: 5, role: 'ADMIN' }] });
      if (url === '/documents/search') {
        return Promise.reject({ response: { status: 400, data: { message: 'Date invalide : 2026-13-01.' } } });
      }
      return Promise.resolve({ data: {} });
    });
    renderAt('/documents?from=2026-13-01');

    expect(await screen.findByRole('alert')).toHaveTextContent('Date invalide');
  });
});
