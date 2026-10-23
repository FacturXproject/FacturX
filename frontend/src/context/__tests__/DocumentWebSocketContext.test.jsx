import { render, act, cleanup } from '@testing-library/react';
import {
  vi,
  describe,
  it,
  expect,
  beforeEach,
  afterEach,
} from 'vitest';
import { useAuth } from '../AuthContext';
import {
  DocumentWebSocketProvider,
  useDocumentStatusEvent,
} from '../DocumentWebSocketContext';

vi.mock('../AuthContext', () => ({
  useAuth: vi.fn(),
}));

class FakeWebSocket {
  static instances = [];

  constructor(url) {
    this.url = url;
    this.close = vi.fn();
    FakeWebSocket.instances.push(this);
  }

  open() {
    this.onopen?.();
  }

  receive(event) {
    this.onmessage?.({ data: JSON.stringify(event) });
  }

  drop(code = 1006) {
    this.onclose?.({ code });
  }
}

function Probe({ onEvent }) {
  useDocumentStatusEvent(onEvent);
  return null;
}

function setup(user = { id: 1 }) {
  useAuth.mockReturnValue({ user });
  const onEvent = vi.fn();

  const view = render(
    <DocumentWebSocketProvider>
      <Probe onEvent={onEvent} />
    </DocumentWebSocketProvider>
  );

  act(() => {
    vi.advanceTimersByTime(0);
  });

  return {
    ...view,
    onEvent,
    changeUser(nextUser) {
      useAuth.mockReturnValue({ user: nextUser });

      view.rerender(
        <DocumentWebSocketProvider>
          <Probe onEvent={onEvent} />
        </DocumentWebSocketProvider>
      );

      act(() => {
        vi.advanceTimersByTime(0);
      });
    },
  };
}

beforeEach(() => {
  vi.useFakeTimers();
  FakeWebSocket.instances = [];
  vi.stubGlobal('WebSocket', FakeWebSocket);
});

afterEach(() => {
  cleanup();
  vi.clearAllTimers();
  vi.useRealTimers();
  vi.unstubAllGlobals();
  vi.clearAllMocks();
});

describe('DocumentWebSocketProvider', () => {
  it('does not connect without an authenticated user', () => {
    setup(null);

    act(() => {
      vi.advanceTimersByTime(10000);
    });

    expect(FakeWebSocket.instances).toHaveLength(0);
  });

  it('closes the socket on logout and connects again on login', () => {
    const { changeUser, onEvent } = setup();
    const first = FakeWebSocket.instances[0];
    first.open();

    const previousMessageHandler = first.onmessage;

    changeUser(null);

    expect(first.close).toHaveBeenCalledTimes(1);
    expect(first.onmessage).toBeNull();

    act(() => {
      previousMessageHandler({
        data: JSON.stringify({
          type: 'document.status.changed',
          documentId: 42,
          organizationId: 1,
          status: 'VALID',
        }),
      });

      vi.advanceTimersByTime(10000);
    });

    expect(onEvent).not.toHaveBeenCalled();
    expect(FakeWebSocket.instances).toHaveLength(1);

    changeUser({ id: 1 });

    expect(FakeWebSocket.instances).toHaveLength(2);
  });

  it('cancels a pending reconnection on logout', () => {
    const { changeUser } = setup();

    FakeWebSocket.instances[0].drop();
    changeUser(null);

    act(() => {
      vi.advanceTimersByTime(10000);
    });

    expect(FakeWebSocket.instances).toHaveLength(1);
  });

  it('reconnects after 2 seconds and emits websocket.reconnected', () => {
    const { onEvent } = setup();
    const first = FakeWebSocket.instances[0];

    first.open();
    expect(onEvent).not.toHaveBeenCalled();

    first.drop();

    act(() => {
      vi.advanceTimersByTime(1999);
    });

    expect(FakeWebSocket.instances).toHaveLength(1);

    act(() => {
      vi.advanceTimersByTime(1);
    });

    expect(FakeWebSocket.instances).toHaveLength(2);

    act(() => {
      FakeWebSocket.instances[1].open();
    });

    expect(onEvent).toHaveBeenCalledTimes(1);
    expect(onEvent).toHaveBeenCalledWith({
      type: 'websocket.reconnected',
    });
  });

  it('does not reconnect after close code 1008', () => {
    setup();

    FakeWebSocket.instances[0].drop(1008);

    act(() => {
      vi.advanceTimersByTime(10000);
    });

    expect(FakeWebSocket.instances).toHaveLength(1);
  });

  it('forwards document status events and ignores other types', () => {
    const { onEvent } = setup();

    const event = {
      type: 'document.status.changed',
      documentId: 42,
      organizationId: 1,
      status: 'VALID',
    };

    act(() => {
      FakeWebSocket.instances[0].receive(event);
      FakeWebSocket.instances[0].receive({ type: 'other' });
    });

    expect(onEvent).toHaveBeenCalledTimes(1);
    expect(onEvent).toHaveBeenCalledWith(event);
  });

  it('closes the socket and cancels retries on unmount', () => {
    const { unmount } = setup();
    const socket = FakeWebSocket.instances[0];

    socket.drop();
    unmount();

    expect(socket.close).toHaveBeenCalledTimes(1);

    act(() => {
      vi.advanceTimersByTime(10000);
    });

    expect(FakeWebSocket.instances).toHaveLength(1);
  });
});