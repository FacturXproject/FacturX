import { render, screen, act, cleanup } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import Dashboard from '../Dashboard';
import api from '../../services/api';
import { useDocumentStatusEvent } from '../../context/DocumentWebSocketContext';

vi.mock('../../services/api');

vi.mock('../../context/DocumentWebSocketContext', () => ({
  useDocumentStatusEvent: vi.fn(),
}));

let onEvent;
let documents;

beforeEach(() => {
  vi.clearAllMocks();

  useDocumentStatusEvent.mockImplementation((handler) => {
    onEvent = handler;
  });

  documents = [{
    id: 42,
    organizationId: 1,
    filename: 'facture.pdf',
    type: 'application/pdf',
    status: 'PROCESSING',
    uploadedAt: '2026-10-23T09:00:00Z',
  }];

  api.get.mockImplementation(async (url) => {
    if (url === '/organizations') {
      return { data: [{ id: 1 }] };
    }

    if (url === '/organizations/1') {
      return { data: { name: 'Organisation test' } };
    }

    if (url === '/documents?organizationId=1&sort=uploadedAt,desc&size=5') {
      return { data: { content: documents } };
    }

    throw new Error(`Unexpected URL: ${url}`);
  });
});

afterEach(() => {
  cleanup();
});

async function renderDashboard() {
  render(
    <MemoryRouter>
      <Dashboard />
    </MemoryRouter>
  );

  await screen.findByText('facture.pdf');
}

describe('Dashboard - WebSocket', () => {
  it('updates the status when a matching event arrives', async () => {
    await renderDashboard();

    expect(screen.getByText('PROCESSING')).toBeInTheDocument();
    const callsBefore = api.get.mock.calls.length;

    act(() => {
      onEvent({
        type: 'document.status.changed',
        documentId: 42,
        organizationId: 1,
        status: 'VALID',
      });
    });

    expect(screen.getByText('VALID')).toBeInTheDocument();
    expect(screen.queryByText('PROCESSING')).not.toBeInTheDocument();
    expect(api.get).toHaveBeenCalledTimes(callsBefore);
  });

  it('ignores events for another document or organization', async () => {
    await renderDashboard();

    act(() => {
      onEvent({
        type: 'document.status.changed',
        documentId: 42,
        organizationId: 2,
        status: 'INVALID',
      });

      onEvent({
        type: 'document.status.changed',
        documentId: 99,
        organizationId: 1,
        status: 'VALID',
      });
    });

    expect(screen.getByText('PROCESSING')).toBeInTheDocument();
    expect(screen.queryByText('VALID')).not.toBeInTheDocument();
    expect(screen.queryByText('INVALID')).not.toBeInTheDocument();
  });

  it('reloads current document statuses after reconnection', async () => {
    await renderDashboard();

    documents = [{ ...documents[0], status: 'VALID' }];
    api.get.mockClear();

    await act(async () => {
      onEvent({ type: 'websocket.reconnected' });
    });

    expect(await screen.findByText('VALID')).toBeInTheDocument();
    expect(api.get).toHaveBeenCalledWith('/organizations');
    expect(api.get).toHaveBeenCalledWith(
      '/documents?organizationId=1&sort=uploadedAt,desc&size=5'
    );
  });
});