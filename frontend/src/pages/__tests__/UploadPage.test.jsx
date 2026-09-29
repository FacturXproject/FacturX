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

function renderVerifier() {
  return render(
    <MemoryRouter>
      <UploadPage mode="verifier" />
    </MemoryRouter>
  );
}

describe('UploadPage - mode verifier (F06/F08 reels)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    api.get.mockResolvedValue({ data: [{ organizationId: 5 }] });
  });

  it('depose le fichier via /documents puis redirige vers le traitement avec le documentId', async () => {
    api.post.mockResolvedValue({ data: { id: 21, filename: 'facture.pdf' } });

    renderVerifier();

    await waitFor(() => expect(api.get).toHaveBeenCalledWith('/organizations'));

    const file = makeFile('facture.pdf', '%PDF-1.4', 'application/pdf');
    const input = document.querySelector('input[type="file"]');
    fireEvent.change(input, { target: { files: [file] } });

    await waitFor(() => {
      expect(api.post).toHaveBeenCalledWith(
        '/documents?organizationId=5',
        expect.any(FormData),
        expect.any(Object)
      );
    });

    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/traitement?action=verifier&documentId=21');
    });
  });

  it('affiche une erreur cote client pour un type de fichier invalide sans appeler le backend', async () => {
    renderVerifier();
    await waitFor(() => expect(api.get).toHaveBeenCalled());

    const file = makeFile('image.png', 'contenu', 'image/png');
    const input = document.querySelector('input[type="file"]');
    fireEvent.change(input, { target: { files: [file] } });

    await waitFor(() => {
      expect(screen.getByText(/seuls les fichiers PDF et XML sont acceptés/i)).toBeInTheDocument();
    });
    expect(api.post).not.toHaveBeenCalled();
  });

  it('affiche une erreur si le depot echoue cote serveur', async () => {
    api.post.mockRejectedValue({
      response: { data: { message: 'Échec du dépôt du fichier.' } },
    });

    renderVerifier();
    await waitFor(() => expect(api.get).toHaveBeenCalled());

    const file = makeFile('facture.pdf', '%PDF-1.4', 'application/pdf');
    const input = document.querySelector('input[type="file"]');
    fireEvent.change(input, { target: { files: [file] } });

    await waitFor(() => {
      expect(screen.getByText(/Échec du dépôt du fichier/i)).toBeInTheDocument();
    });
    expect(mockNavigate).not.toHaveBeenCalled();
  });
});
