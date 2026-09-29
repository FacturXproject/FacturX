import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, act } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import Processing from '../Processing';

const mockNavigate = vi.fn();

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return { ...actual, useNavigate: () => mockNavigate };
});

// F09+: le flux "Verifier" (F08/F09) ne passe plus par cette page - il est
// gere directement par UploadPage (upload + validate + navigation vers
// /rapport dans un seul gestionnaire). Processing.jsx ne sert plus que la
// simulation de conversion (F13, pas encore branchee sur un service reel).
describe('Processing - simulation de conversion (F13, mockee)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('affiche la progression puis redirige vers /conversion apres le delai simule', () => {
    render(
      <MemoryRouter>
        <Processing />
      </MemoryRouter>
    );

    expect(screen.getByText(/Traitement en cours/i)).toBeInTheDocument();

    act(() => {
      vi.advanceTimersByTime(2200);
    });

    expect(mockNavigate).toHaveBeenCalledWith('/conversion');
  });
});
