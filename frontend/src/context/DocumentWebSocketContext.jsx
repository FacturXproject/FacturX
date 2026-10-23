import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useLayoutEffect,
  useRef,
} from 'react';
import { useAuth } from './AuthContext';

const DocumentWebSocketContext = createContext(null);

export function DocumentWebSocketProvider({ children }) {
  const { user } = useAuth();
  const userId = user?.id;
  const listeners = useRef(new Set());

  const subscribe = useCallback((listener) => {
    listeners.current.add(listener);
    return () => listeners.current.delete(listener);
  }, []);

  useEffect(() => {
    if (userId == null) return;

    let socket;
    let retryTimer;
    let stopped = false;
    let hasConnected = false;

    function emit(event) {
      for (const listener of listeners.current) {
        try {
          listener(event);
        } catch (error) {
          console.error('WebSocket listener error:', error);
        }
      }
    }

    function connect() {
      if (stopped) return;

      const protocol =
        window.location.protocol === 'https:' ? 'wss:' : 'ws:';

      socket = new WebSocket(
        `${protocol}//${window.location.host}/ws`
      );

      socket.onopen = () => {
        if (stopped) return;

        if (hasConnected) {
          emit({ type: 'websocket.reconnected' });
        }

        hasConnected = true;
      };

      socket.onmessage = (message) => {
        if (stopped) return;

        try {
          const event = JSON.parse(message.data);

          if (event?.type === 'document.status.changed') {
            emit(event);
          }
        } catch (error) {
          console.error('WebSocket invalid message:', error);
        }
      };

      socket.onerror = () => {
        if (stopped) return;
        console.error('WebSocket connection error');
      };

      socket.onclose = (event) => {
        if (stopped || event.code === 1008) return;
        retryTimer = setTimeout(connect, 2000);
      };
    }

    const timer = setTimeout(connect, 0);

    return () => {
      stopped = true;
      clearTimeout(timer);
      clearTimeout(retryTimer);

      if (socket) {
        socket.onopen = null;
        socket.onmessage = null;
        socket.onerror = null;
        socket.onclose = null;
        socket.close();
      }
    };
  }, [userId]);

  return (
    <DocumentWebSocketContext.Provider value={subscribe}>
      {children}
    </DocumentWebSocketContext.Provider>
  );
}

export function useDocumentStatusEvent(handler) {
  const subscribe = useContext(DocumentWebSocketContext);
  const handlerRef = useRef(handler);

  useLayoutEffect(() => {
    handlerRef.current = handler;
  }, [handler]);

  useEffect(() => {
    if (!subscribe) {
      throw new Error(
        'useDocumentStatusEvent requires DocumentWebSocketProvider'
      );
    }

    return subscribe((event) => handlerRef.current(event));
  }, [subscribe]);
}