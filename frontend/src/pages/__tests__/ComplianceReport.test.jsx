import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import ComplianceReport from '../ComplianceReport';
import api from '../../services/api';

vi.mock('../../services/api');

function renderAt(path) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <ComplianceReport />
    </MemoryRouter>
  );
}

describe('ComplianceReport - rapport reel F08/F09 (page Verifier)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('affiche un rapport conforme pour un fichier valide', async () => {
    api.get.mockResolvedValue({
      data: {
        filename: 'facture-ok.xml',
        valid: true,
        errorCount: 0,
        warningCount: 0,
        infoCount: 1,
        errors: [],
      },
    });

    renderAt('/rapport?documentId=42');

    await waitFor(() => {
      expect(api.get).toHaveBeenCalledWith('/documents/42/report');
    });

    await waitFor(() => {
      expect(screen.getAllByText('Conforme').length).toBeGreaterThan(0);
      expect(screen.getByText('facture-ok.xml')).toBeInTheDocument();
    });
  });

  it('affiche le message dédié pour un PDF sans XML Factur-X embarqué', async () => {
    api.get.mockResolvedValue({
      data: {
        filename: 'simple-pdf.pdf',
        valid: false,
        errorCount: 1,
        warningCount: 0,
        infoCount: 0,
        errors: [
          {
            titleFr: 'Ce PDF ne contient pas de facture Factur-X',
            descriptionFr:
              "Mustang n'a trouvé aucune donnée de facture structurée (XML) intégrée dans ce fichier.",
            ruleCode: 'MUSTANG-EXCEPTION-17',
            correctionHintFr: 'Utilisez la fonction « Convertir » pour générer une véritable facture Factur-X.',
          },
        ],
      },
    });

    renderAt('/rapport?documentId=13');

    await waitFor(() => {
      expect(screen.getByText('Ce PDF ne contient pas de facture Factur-X')).toBeInTheDocument();
    });
    expect(screen.getAllByText('Non conforme').length).toBeGreaterThan(0);
  });

  it("affiche une erreur si aucun documentId n'est fourni dans l'URL", async () => {
    renderAt('/rapport');

    await waitFor(() => {
      expect(screen.getByText(/Aucun document à afficher/i)).toBeInTheDocument();
    });
    expect(api.get).not.toHaveBeenCalled();
  });

  it("affiche le message de refus quand un role CLIENT n'a pas accès au rapport", async () => {
    api.get.mockRejectedValue({
      response: { data: { message: 'You do not have permission to perform this action.' } },
    });

    renderAt('/rapport?documentId=99');

    await waitFor(() => {
      expect(screen.getByText(/You do not have permission to perform this action/i)).toBeInTheDocument();
    });
  });
});
