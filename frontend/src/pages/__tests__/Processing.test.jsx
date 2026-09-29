import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import Processing from '../Processing';
import api from '../../services/api';

vi.mock('../../services/api');

const mockNavigate = vi.fn();

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return { ...actual, useNavigate: () => mockNavigate };
});

function renderAt(path) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Processing />
    </MemoryRouter>
  );
}

describe('Processing - flux Verifier (F08/F09 reel)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('lance la validation reelle du document puis redirige vers le rapport', async () => {
    api.post.mockResolvedValue({ data: { valid: true } });

    renderAt('/traitement?action=verifier&documentId=7');

    await waitFor(() => {
      expect(api.post).toHaveBeenCalledWith('/documents/7/validate');
    });

    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/rapport?documentId=7');
    });
  });

  it('affiche un message clair si aucun documentId n\'est fourni', async () => {
    renderAt('/traitement?action=verifier');

    await waitFor(() => {
      expect(screen.getByText(/Aucun document à valider/i)).toBeInTheDocument();
    });
    expect(api.post).not.toHaveBeenCalled();
  });

  it('affiche le message de refus quand un role CLIENT ne peut pas valider (403)', async () => {
    api.post.mockRejectedValue({
      response: { data: { message: 'You do not have permission to perform this action.' } },
    });

    renderAt('/traitement?action=verifier&documentId=9');

    await waitFor(() => {
      expect(screen.getByText(/You do not have permission to perform this action/i)).toBeInTheDocument();
    });
    expect(mockNavigate).not.toHaveBeenCalled();
  });

  it('affiche un message d\'echec generique si le serveur ne renvoie pas de message', async () => {
    api.post.mockRejectedValue(new Error('Network Error'));

    renderAt('/traitement?action=verifier&documentId=3');

    await waitFor(() => {
      expect(screen.getByText(/Échec de la validation du document/i)).toBeInTheDocument();
    });
  });
});
