import { StrictMode } from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import UploadPage from '../UploadPage';
import api from '../../services/api';

vi.mock('../../services/api');

const mockNavigate = vi.fn();

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return { ...actual, useNavigate: () => mockNavigate };
});

function makeFile(name, content, type) {
  return new File([content], name, { type });
}

function renderVerifier({ strict = false } = {}) {
  const tree = (
    <MemoryRouter>
      <UploadPage mode="verifier" />
    </MemoryRouter>
  );
  return render(strict ? <StrictMode>{tree}</StrictMode> : tree);
}

describe('UploadPage - mode verifier (F06/F08 reels + choix de l\'organisation)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('propose un choix quand plusieurs organisations sont eligibles, et upload dans celle selectionnee', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/organizations') {
        return Promise.resolve({
          data: [
            { organizationId: 5, role: 'ADMIN' },
            { organizationId: 6, role: 'ACCOUNTANT' },
          ],
        });
      }
      if (url === '/organizations/5') return Promise.resolve({ data: { name: 'Cabinet A' } });
      if (url === '/organizations/6') return Promise.resolve({ data: { name: 'Cabinet B' } });
      return Promise.resolve({ data: {} });
    });
    api.post.mockImplementation((url) => {
      if (url.startsWith('/documents?organizationId=')) return Promise.resolve({ data: { id: 21 } });
      return Promise.resolve({ data: { valid: true } });
    });

    renderVerifier();

    const select = await screen.findByRole('combobox');
    expect(screen.getByText('Cabinet A')).toBeInTheDocument();
    expect(screen.getByText('Cabinet B')).toBeInTheDocument();

    fireEvent.change(select, { target: { value: '6' } });

    const file = makeFile('facture.pdf', '%PDF-1.4', 'application/pdf');
    const input = document.querySelector('input[type="file"]');
    fireEvent.change(input, { target: { files: [file] } });

    await waitFor(() => {
      expect(api.post).toHaveBeenCalledWith(
        '/documents?organizationId=6',
        expect.any(FormData),
        expect.any(Object)
      );
    });
  });

  it('affiche une organisation unique en libelle simple, sans liste deroulante', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/organizations') {
        return Promise.resolve({ data: [{ organizationId: 8, role: 'ADMIN' }] });
      }
      if (url === '/organizations/8') return Promise.resolve({ data: { name: 'Cabinet Solo' } });
      return Promise.resolve({ data: {} });
    });

    renderVerifier();

    await waitFor(() => {
      expect(screen.getByText('Cabinet Solo')).toBeInTheDocument();
    });
    expect(screen.queryByRole('combobox')).not.toBeInTheDocument();
  });

  it('n\'affiche aucune zone de depot pour un utilisateur uniquement CLIENT', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/organizations') {
        return Promise.resolve({ data: [{ organizationId: 9, role: 'CLIENT' }] });
      }
      return Promise.resolve({ data: {} });
    });

    renderVerifier();

    await waitFor(() => {
      expect(
        screen.getByText(/Vous n'avez pas le droit de vérifier des factures dans vos organisations/i)
      ).toBeInTheDocument();
    });
    expect(document.querySelector('input[type="file"]')).not.toBeInTheDocument();
    expect(api.post).not.toHaveBeenCalled();
  });

  it('un seul clic = un seul appel a /validate, meme sous React.StrictMode', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/organizations') {
        return Promise.resolve({ data: [{ organizationId: 5, role: 'ADMIN' }] });
      }
      if (url === '/organizations/5') return Promise.resolve({ data: { name: 'Cabinet X' } });
      return Promise.resolve({ data: {} });
    });
    api.post.mockImplementation((url) => {
      if (url.startsWith('/documents?organizationId=')) return Promise.resolve({ data: { id: 21 } });
      if (url === '/documents/21/validate') return Promise.resolve({ data: { valid: true } });
      return Promise.reject(new Error(`URL inattendue : ${url}`));
    });

    renderVerifier({ strict: true });

    await waitFor(() => expect(screen.getByText('Cabinet X')).toBeInTheDocument());

    const file = makeFile('facture.pdf', '%PDF-1.4', 'application/pdf');
    const input = document.querySelector('input[type="file"]');
    fireEvent.change(input, { target: { files: [file] } });

    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/rapport?documentId=21');
    });

    const uploadCalls = api.post.mock.calls.filter(([url]) => url.startsWith('/documents?organizationId='));
    const validateCalls = api.post.mock.calls.filter(([url]) => url === '/documents/21/validate');
    expect(uploadCalls).toHaveLength(1);
    expect(validateCalls).toHaveLength(1);
  });

  it('affiche une erreur cote client pour un type de fichier invalide sans appeler le backend', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/organizations') {
        return Promise.resolve({ data: [{ organizationId: 5, role: 'ADMIN' }] });
      }
      if (url === '/organizations/5') return Promise.resolve({ data: { name: 'Cabinet X' } });
      return Promise.resolve({ data: {} });
    });

    renderVerifier();
    await waitFor(() => expect(screen.getByText('Cabinet X')).toBeInTheDocument());

    const file = makeFile('image.png', 'contenu', 'image/png');
    const input = document.querySelector('input[type="file"]');
    fireEvent.change(input, { target: { files: [file] } });

    await waitFor(() => {
      expect(screen.getByText(/seuls les fichiers PDF et XML sont acceptés/i)).toBeInTheDocument();
    });
    expect(api.post).not.toHaveBeenCalled();
  });

  it('affiche une erreur si le depot echoue cote serveur', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/organizations') {
        return Promise.resolve({ data: [{ organizationId: 5, role: 'ADMIN' }] });
      }
      if (url === '/organizations/5') return Promise.resolve({ data: { name: 'Cabinet X' } });
      return Promise.resolve({ data: {} });
    });
    api.post.mockRejectedValue({
      response: { data: { message: 'Échec du dépôt du fichier.' } },
    });

    renderVerifier();
    await waitFor(() => expect(screen.getByText('Cabinet X')).toBeInTheDocument());

    const file = makeFile('facture.pdf', '%PDF-1.4', 'application/pdf');
    const input = document.querySelector('input[type="file"]');
    fireEvent.change(input, { target: { files: [file] } });

    await waitFor(() => {
      expect(screen.getByText(/Échec du dépôt du fichier/i)).toBeInTheDocument();
    });
    expect(mockNavigate).not.toHaveBeenCalled();
  });
});
