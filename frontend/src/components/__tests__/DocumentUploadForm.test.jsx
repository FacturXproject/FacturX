import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import DocumentUploadForm from '../DocumentUploadForm';
import api from '../../services/api';

vi.mock('../../services/api');

function makeFile(name, content, type) {
  return new File([content], name, { type });
}

describe('DocumentUploadForm', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  // ── Affichage initial ──────────────────────────────────────────

  it('affiche la zone de depot avec le texte attendu', () => {
    render(<DocumentUploadForm organizationId={87} />);
    expect(screen.getByText(/Glissez vos fichiers ici/i)).toBeInTheDocument();
    expect(screen.getByText(/PDF ou XML, 10 Mo max/i)).toBeInTheDocument();
  });

  it('n\'affiche aucune liste de fichiers avant toute selection', () => {
    render(<DocumentUploadForm organizationId={87} />);
    expect(screen.queryByText(/Déposer/i)).not.toBeInTheDocument();
  });

  // ── Selection de fichiers valides ──────────────────────────────

  it('ajoute un fichier PDF valide a la liste apres selection', async () => {
    render(<DocumentUploadForm organizationId={87} />);

    const file = makeFile('facture.pdf', '%PDF-1.4 contenu', 'application/pdf');
    const input = document.querySelector('input[type="file"]');

    fireEvent.change(input, { target: { files: [file] } });

    await waitFor(() => {
      expect(screen.getByText('facture.pdf')).toBeInTheDocument();
    });
  });

  it('ajoute un fichier XML valide a la liste apres selection', async () => {
    render(<DocumentUploadForm organizationId={87} />);

    const file = makeFile('facture.xml', '<?xml version="1.0"?><facture/>', 'application/xml');
    const input = document.querySelector('input[type="file"]');

    fireEvent.change(input, { target: { files: [file] } });

    await waitFor(() => {
      expect(screen.getByText('facture.xml')).toBeInTheDocument();
    });
  });

  it('accepte la selection de plusieurs fichiers a la fois', async () => {
    render(<DocumentUploadForm organizationId={87} />);

    const file1 = makeFile('facture1.pdf', '%PDF-1.4 contenu', 'application/pdf');
    const file2 = makeFile('facture2.pdf', '%PDF-1.4 contenu', 'application/pdf');
    const input = document.querySelector('input[type="file"]');

    fireEvent.change(input, { target: { files: [file1, file2] } });

    await waitFor(() => {
      expect(screen.getByText('facture1.pdf')).toBeInTheDocument();
      expect(screen.getByText('facture2.pdf')).toBeInTheDocument();
      expect(screen.getByText(/Déposer 2 fichier/i)).toBeInTheDocument();
    });
  });

  it('affiche autant de fichiers que de selections successives', async () => {
    render(<DocumentUploadForm organizationId={87} />);

    const input = document.querySelector('input[type="file"]');
    const file1 = makeFile('un.pdf', '%PDF-1.4', 'application/pdf');
    const file2 = makeFile('deux.pdf', '%PDF-1.4', 'application/pdf');

    fireEvent.change(input, { target: { files: [file1] } });
    await waitFor(() => expect(screen.getByText('un.pdf')).toBeInTheDocument());

    fireEvent.change(input, { target: { files: [file2] } });
    await waitFor(() => {
      expect(screen.getByText('un.pdf')).toBeInTheDocument();
      expect(screen.getByText('deux.pdf')).toBeInTheDocument();
    });
  });

  // ── Validation cote client ─────────────────────────────────────

  it('affiche une erreur cote client pour un fichier trop volumineux', async () => {
    render(<DocumentUploadForm organizationId={87} />);

    const bigContent = new Array(11 * 1024 * 1024).fill('a').join('');
    const file = makeFile('trop-gros.pdf', bigContent, 'application/pdf');
    const input = document.querySelector('input[type="file"]');

    fireEvent.change(input, { target: { files: [file] } });

    await waitFor(() => {
      expect(screen.getByText(/dépasse la taille maximale/i)).toBeInTheDocument();
    });
  });

  it('accepte un fichier juste en dessous de la limite de 10 Mo', async () => {
    render(<DocumentUploadForm organizationId={87} />);

    const almostTenMo = new Array(10 * 1024 * 1024 - 1024).fill('a').join('');
    const file = makeFile('presque-limite.pdf', almostTenMo, 'application/pdf');
    const input = document.querySelector('input[type="file"]');

    fireEvent.change(input, { target: { files: [file] } });

    await waitFor(() => {
      expect(screen.getByText('presque-limite.pdf')).toBeInTheDocument();
      expect(screen.queryByText(/dépasse la taille maximale/i)).not.toBeInTheDocument();
    });
  });

  it('affiche une erreur cote client pour un type de fichier invalide', async () => {
    render(<DocumentUploadForm organizationId={87} />);

    const file = makeFile('image.png', 'contenu', 'image/png');
    const input = document.querySelector('input[type="file"]');

    fireEvent.change(input, { target: { files: [file] } });

    await waitFor(() => {
      expect(screen.getByText(/seuls les fichiers PDF et XML sont acceptés/i)).toBeInTheDocument();
    });
  });

  it('ne propose pas de bouton deposer si tous les fichiers sont invalides', async () => {
    render(<DocumentUploadForm organizationId={87} />);

    const file = makeFile('image.png', 'contenu', 'image/png');
    const input = document.querySelector('input[type="file"]');

    fireEvent.change(input, { target: { files: [file] } });

    await waitFor(() => {
      expect(screen.getByText(/seuls les fichiers PDF et XML sont acceptés/i)).toBeInTheDocument();
    });

    expect(screen.queryByText(/Déposer/i)).not.toBeInTheDocument();
  });

  it('mixe fichiers valides et invalides sans bloquer les valides', async () => {
    render(<DocumentUploadForm organizationId={87} />);

    const validFile = makeFile('facture.pdf', '%PDF-1.4', 'application/pdf');
    const invalidFile = makeFile('image.png', 'contenu', 'image/png');
    const input = document.querySelector('input[type="file"]');

    fireEvent.change(input, { target: { files: [validFile, invalidFile] } });

    await waitFor(() => {
      expect(screen.getByText('facture.pdf')).toBeInTheDocument();
      expect(screen.getByText('image.png')).toBeInTheDocument();
      expect(screen.getByText(/seuls les fichiers PDF et XML sont acceptés/i)).toBeInTheDocument();
      // un seul fichier est deposable, le bouton doit le refleter
      expect(screen.getByText(/Déposer 1 fichier/i)).toBeInTheDocument();
    });
  });

  // ── Suppression avant envoi ─────────────────────────────────────

  it('permet de retirer un fichier valide de la liste avant envoi', async () => {
    render(<DocumentUploadForm organizationId={87} />);

    const file = makeFile('facture.pdf', '%PDF-1.4 contenu', 'application/pdf');
    const input = document.querySelector('input[type="file"]');

    fireEvent.change(input, { target: { files: [file] } });

    await waitFor(() => {
      expect(screen.getByText('facture.pdf')).toBeInTheDocument();
    });

    const removeButtons = document.querySelectorAll('button');
    const removeButton = Array.from(removeButtons).find((btn) => btn.querySelector('svg'));
    fireEvent.click(removeButton);

    await waitFor(() => {
      expect(screen.queryByText('facture.pdf')).not.toBeInTheDocument();
    });
  });

  it('permet de retirer un fichier en erreur de la liste', async () => {
    render(<DocumentUploadForm organizationId={87} />);

    const file = makeFile('image.png', 'contenu', 'image/png');
    const input = document.querySelector('input[type="file"]');

    fireEvent.change(input, { target: { files: [file] } });

    await waitFor(() => {
      expect(screen.getByText('image.png')).toBeInTheDocument();
    });

    const removeButtons = document.querySelectorAll('button');
    const removeButton = Array.from(removeButtons).find((btn) => btn.querySelector('svg'));
    fireEvent.click(removeButton);

    await waitFor(() => {
      expect(screen.queryByText('image.png')).not.toBeInTheDocument();
    });
  });

  it('fait disparaitre le bouton deposer si le dernier fichier valide est retire', async () => {
    render(<DocumentUploadForm organizationId={87} />);

    const file = makeFile('facture.pdf', '%PDF-1.4', 'application/pdf');
    const input = document.querySelector('input[type="file"]');

    fireEvent.change(input, { target: { files: [file] } });

    await waitFor(() => {
      expect(screen.getByText(/Déposer 1 fichier/i)).toBeInTheDocument();
    });

    const removeButtons = document.querySelectorAll('button');
    const removeButton = Array.from(removeButtons).find((btn) => btn.querySelector('svg'));
    fireEvent.click(removeButton);

    await waitFor(() => {
      expect(screen.queryByText(/Déposer/i)).not.toBeInTheDocument();
    });
  });

  // ── Envoi reel au backend ────────────────────────────────────────

  it('envoie le fichier au backend au clic sur le bouton deposer', async () => {
    api.post.mockResolvedValue({
      data: { id: 1, filename: 'facture.pdf', status: 'UPLOADED' },
    });

    const onUploaded = vi.fn();
    render(<DocumentUploadForm organizationId={87} onUploaded={onUploaded} />);

    const file = makeFile('facture.pdf', '%PDF-1.4 contenu', 'application/pdf');
    const input = document.querySelector('input[type="file"]');

    fireEvent.change(input, { target: { files: [file] } });

    await waitFor(() => {
      expect(screen.getByText(/Déposer 1 fichier/i)).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText(/Déposer 1 fichier/i));

    await waitFor(() => {
      expect(api.post).toHaveBeenCalledWith(
        '/documents?organizationId=87',
        expect.any(FormData),
        expect.any(Object)
      );
    });

    await waitFor(() => {
      expect(onUploaded).toHaveBeenCalledWith({ id: 1, filename: 'facture.pdf', status: 'UPLOADED' });
    });
  });

  it('utilise le bon organizationId dans l\'URL d\'upload', async () => {
    api.post.mockResolvedValue({ data: { id: 1, filename: 'facture.pdf' } });

    render(<DocumentUploadForm organizationId={42} />);

    const file = makeFile('facture.pdf', '%PDF-1.4', 'application/pdf');
    const input = document.querySelector('input[type="file"]');

    fireEvent.change(input, { target: { files: [file] } });
    await waitFor(() => screen.getByText(/Déposer 1 fichier/i));
    fireEvent.click(screen.getByText(/Déposer 1 fichier/i));

    await waitFor(() => {
      expect(api.post).toHaveBeenCalledWith(
        '/documents?organizationId=42',
        expect.any(FormData),
        expect.any(Object)
      );
    });
  });

  it('envoie plusieurs fichiers valides en parallele au clic sur deposer', async () => {
    api.post.mockResolvedValue({ data: { id: 1, filename: 'ok.pdf', status: 'UPLOADED' } });

    render(<DocumentUploadForm organizationId={87} />);

    const file1 = makeFile('un.pdf', '%PDF-1.4', 'application/pdf');
    const file2 = makeFile('deux.pdf', '%PDF-1.4', 'application/pdf');
    const input = document.querySelector('input[type="file"]');

    fireEvent.change(input, { target: { files: [file1, file2] } });
    await waitFor(() => screen.getByText(/Déposer 2 fichier/i));

    fireEvent.click(screen.getByText(/Déposer 2 fichier/i));

    await waitFor(() => {
      expect(api.post).toHaveBeenCalledTimes(2);
    });
  });

  it('affiche une coche de succes apres un upload reussi', async () => {
    api.post.mockResolvedValue({ data: { id: 1, filename: 'facture.pdf', status: 'UPLOADED' } });

    const { container } = render(<DocumentUploadForm organizationId={87} />);

    const file = makeFile('facture.pdf', '%PDF-1.4', 'application/pdf');
    const input = document.querySelector('input[type="file"]');

    fireEvent.change(input, { target: { files: [file] } });
    await waitFor(() => screen.getByText(/Déposer 1 fichier/i));
    fireEvent.click(screen.getByText(/Déposer 1 fichier/i));

    await waitFor(() => {
      expect(container.querySelectorAll('svg').length).toBeGreaterThan(0);
      expect(screen.queryByText(/Déposer/i)).not.toBeInTheDocument();
    });
  });

  // ── Gestion des erreurs serveur ──────────────────────────────────

  it('affiche une erreur serveur si l\'upload echoue (type invalide detecte cote serveur)', async () => {
    api.post.mockRejectedValue({
      response: { data: { message: 'Seuls les fichiers PDF et XML sont acceptés.' } },
    });

    render(<DocumentUploadForm organizationId={87} />);

    const file = makeFile('facture.pdf', '%PDF-1.4 contenu', 'application/pdf');
    const input = document.querySelector('input[type="file"]');

    fireEvent.change(input, { target: { files: [file] } });

    await waitFor(() => {
      expect(screen.getByText(/Déposer 1 fichier/i)).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText(/Déposer 1 fichier/i));

    await waitFor(() => {
      expect(screen.getByText(/Seuls les fichiers PDF et XML sont acceptés/i)).toBeInTheDocument();
    });
  });

  it('accepte un fichier cote client meme si son contenu reel est invalide (le serveur tranche)', async () => {
    api.post.mockRejectedValue({
      response: { data: { message: 'Seuls les fichiers PDF et XML sont acceptés.' } },
    });

    render(<DocumentUploadForm organizationId={87} />);

    // fichier nomme .pdf et declare comme application/pdf, mais son contenu n'est pas un vrai PDF
    const file = makeFile('fake.pdf', 'ceci n\'est pas un vrai pdf', 'application/pdf');
    const input = document.querySelector('input[type="file"]');

    fireEvent.change(input, { target: { files: [file] } });

    await waitFor(() => {
      expect(screen.getByText('fake.pdf')).toBeInTheDocument();
      expect(screen.getByText(/Déposer 1 fichier/i)).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText(/Déposer 1 fichier/i));

    await waitFor(() => {
      expect(screen.getByText(/Seuls les fichiers PDF et XML sont acceptés/i)).toBeInTheDocument();
    });
  });

  it('affiche un message d\'erreur generique si le serveur ne renvoie pas de message', async () => {
    api.post.mockRejectedValue(new Error('Network Error'));

    render(<DocumentUploadForm organizationId={87} />);

    const file = makeFile('facture.pdf', '%PDF-1.4', 'application/pdf');
    const input = document.querySelector('input[type="file"]');

    fireEvent.change(input, { target: { files: [file] } });
    await waitFor(() => screen.getByText(/Déposer 1 fichier/i));
    fireEvent.click(screen.getByText(/Déposer 1 fichier/i));

    await waitFor(() => {
      expect(screen.getByText(/Network Error/i)).toBeInTheDocument();
    });
  });

  it('permet de reessayer apres un echec en retirant puis reselectionnant un fichier', async () => {
    api.post.mockRejectedValueOnce({
      response: { data: { message: 'Erreur serveur.' } },
    });

    render(<DocumentUploadForm organizationId={87} />);

    const file = makeFile('facture.pdf', '%PDF-1.4', 'application/pdf');
    const input = document.querySelector('input[type="file"]');

    fireEvent.change(input, { target: { files: [file] } });
    await waitFor(() => screen.getByText(/Déposer 1 fichier/i));
    fireEvent.click(screen.getByText(/Déposer 1 fichier/i));

    await waitFor(() => {
      expect(screen.getByText(/Erreur serveur/i)).toBeInTheDocument();
    });

    // le fichier en echec reste affiche, on peut le retirer et reessayer
    const removeButtons = document.querySelectorAll('button');
    const removeButton = Array.from(removeButtons).find((btn) => btn.querySelector('svg'));
    fireEvent.click(removeButton);

    await waitFor(() => {
      expect(screen.queryByText('facture.pdf')).not.toBeInTheDocument();
    });
  });
});