import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import MyData from '../MyData';
import api from '../../services/api';

const { navigateMock, setUserMock } = vi.hoisted(() => ({
  navigateMock: vi.fn(),
  setUserMock: vi.fn(),
}));

vi.mock('../../services/api');

vi.mock('../../context/AuthContext', () => ({
  useAuth: () => ({
    setUser: setUserMock,
  }),
}));

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');

  return {
    ...actual,
    useNavigate: () => navigateMock,
  };
});

function renderPage() {
  return render(
    <MemoryRouter>
      <MyData />
    </MemoryRouter>
  );
}

describe('MyData', () => {
  let createObjectURLMock;
  let revokeObjectURLMock;
  let linkClickMock;

  beforeEach(() => {
    vi.clearAllMocks();

    createObjectURLMock = vi.fn(() => 'blob:my-data');
    revokeObjectURLMock = vi.fn();
    linkClickMock = vi
      .spyOn(HTMLAnchorElement.prototype, 'click')
      .mockImplementation(() => {});

    vi.stubGlobal('URL', {
      ...URL,
      createObjectURL: createObjectURLMock,
      revokeObjectURL: revokeObjectURLMock,
    });
  });

  afterEach(() => {
    linkClickMock.mockRestore();
    vi.unstubAllGlobals();
  });

  it('affiche les actions RGPD', () => {
    renderPage();

    expect(
      screen.getByRole('heading', { name: 'Mes données' })
    ).toBeInTheDocument();

    expect(
      screen.getByRole('button', { name: 'Télécharger mes données' })
    ).toBeInTheDocument();

    expect(
      screen.getByRole('button', { name: 'Supprimer mon compte' })
    ).toBeInTheDocument();
  });

  it('exporte les donnees personnelles en JSON', async () => {
    api.get.mockResolvedValue({
      data: {
        user: {
          id: 1,
          email: 'jean@example.com',
          firstName: 'Jean',
          lastName: 'Dupont',
          status: 'active',
        },
        organizations: [],
      },
    });

    renderPage();

    fireEvent.click(
      screen.getByRole('button', { name: 'Télécharger mes données' })
    );

    await waitFor(() => {
      expect(api.get).toHaveBeenCalledWith('/gdpr/export');
    });

    expect(createObjectURLMock).toHaveBeenCalledTimes(1);
    expect(linkClickMock).toHaveBeenCalledTimes(1);
    expect(revokeObjectURLMock).toHaveBeenCalledWith('blob:my-data');
  });

  it('affiche une erreur si export impossible', async () => {
    api.get.mockRejectedValue(new Error('export failed'));

    renderPage();

    fireEvent.click(
      screen.getByRole('button', { name: 'Télécharger mes données' })
    );

    expect(
      await screen.findByText('Impossible de télécharger vos données.')
    ).toBeInTheDocument();
  });

  it('affiche puis annule la confirmation de suppression', () => {
    renderPage();

    fireEvent.click(
      screen.getByRole('button', { name: 'Supprimer mon compte' })
    );

    expect(
      screen.getByText(
        'Êtes-vous sûr de vouloir supprimer votre compte ?'
      )
    ).toBeInTheDocument();

    fireEvent.click(
      screen.getByRole('button', { name: 'Annuler' })
    );

    expect(
      screen.queryByText(
        'Êtes-vous sûr de vouloir supprimer votre compte ?'
      )
    ).not.toBeInTheDocument();

    expect(api.post).not.toHaveBeenCalled();
  });

  it('supprime le compte avec la confirmation DELETE puis deconnecte', async () => {
    api.post.mockResolvedValue({});

    renderPage();

    fireEvent.click(
      screen.getByRole('button', { name: 'Supprimer mon compte' })
    );

    fireEvent.click(
      screen.getByRole('button', { name: 'Confirmer la suppression' })
    );

    await waitFor(() => {
      expect(api.post).toHaveBeenCalledWith('/gdpr/delete', {
        confirmation: 'DELETE',
      });
    });

    expect(setUserMock).toHaveBeenCalledWith(null);
    expect(navigateMock).toHaveBeenCalledWith('/login');
  });

  it('affiche une erreur specifique si utilisateur dernier administrateur', async () => {
    api.post.mockRejectedValue({
      response: {
        data: {
          error: 'User is the last admin of an organization',
        },
      },
    });

    renderPage();

    fireEvent.click(
      screen.getByRole('button', { name: 'Supprimer mon compte' })
    );

    fireEvent.click(
      screen.getByRole('button', { name: 'Confirmer la suppression' })
    );

    expect(
      await screen.findByText(
        /Vous êtes le dernier administrateur d’une organisation/i
      )
    ).toBeInTheDocument();

    expect(setUserMock).not.toHaveBeenCalled();
    expect(navigateMock).not.toHaveBeenCalled();
  });

  it('affiche une erreur generique si suppression impossible', async () => {
    api.post.mockRejectedValue({
      response: {
        data: {
          error: 'UNKNOWN_ERROR',
        },
      },
    });

    renderPage();

    fireEvent.click(
      screen.getByRole('button', { name: 'Supprimer mon compte' })
    );

    fireEvent.click(
      screen.getByRole('button', { name: 'Confirmer la suppression' })
    );

    expect(
      await screen.findByText('Impossible de supprimer votre compte.')
    ).toBeInTheDocument();

    expect(setUserMock).not.toHaveBeenCalled();
    expect(navigateMock).not.toHaveBeenCalled();
  });

  it('ne supprime jamais le compte avant confirmation', () => {
    renderPage();

    fireEvent.click(
      screen.getByRole('button', { name: 'Supprimer mon compte' })
    );

    expect(api.post).not.toHaveBeenCalled();
    expect(setUserMock).not.toHaveBeenCalled();
    expect(navigateMock).not.toHaveBeenCalled();
  });
});
