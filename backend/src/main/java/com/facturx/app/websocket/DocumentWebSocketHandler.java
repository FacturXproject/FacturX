package com.facturx.app.websocket;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import com.facturx.app.auth.AppUserPrincipal;
import com.facturx.app.permission.Permission;
import com.facturx.app.permission.PermissionService;

import tools.jackson.databind.json.JsonMapper;

@Component
public class DocumentWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log =
            LoggerFactory.getLogger(DocumentWebSocketHandler.class);

    private final PermissionService permissionService;
    private final JsonMapper objectMapper;

    private final Map<String, WebSocketSession> sessions =
            new ConcurrentHashMap<>();

    private final Map<String, Long> userIds =
            new ConcurrentHashMap<>();

    public DocumentWebSocketHandler(
            PermissionService permissionService,
            JsonMapper objectMapper) {

        this.permissionService = permissionService;
        this.objectMapper = objectMapper;
    }

    private boolean canReceive(
            Long userId,
            DocumentStatusChangedEvent event) {

        boolean canViewAll = permissionService.hasPermission(
                userId,
                event.organizationId(),
                Permission.VIEW_ALL_DOCUMENTS
        );

        boolean canViewOwn = permissionService.hasPermission(
                userId,
                event.organizationId(),
                Permission.VIEW_OWN_DOCUMENTS
        );

        boolean isOwner = userId.equals(event.ownerId());

        return canViewAll || (canViewOwn && isOwner);
    }

    public void broadcast(DocumentStatusChangedEvent event) throws Exception {
        TextMessage message =
                new TextMessage(objectMapper.writeValueAsString(event));

        for (WebSocketSession session : sessions.values()) {
            try {
                Long userId = userIds.get(session.getId());

                if (userId == null || !canReceive(userId, event)) {
                    continue;
                }

                synchronized (session) {
                    if (session.isOpen()) {
                        session.sendMessage(message);
                    }
                }
            } catch (Exception e) {
                log.warn(
                        "WebSocket: failed to notify session {}",
                        session.getId(),
                        e
                );

                sessions.remove(session.getId());
                userIds.remove(session.getId());

                try {
                    session.close(CloseStatus.SERVER_ERROR);
                } catch (Exception closeException) {
                    log.debug(
                            "WebSocket: failed to close session {}",
                            session.getId(),
                            closeException
                    );
                }
            }
        }
    }

    @Override
    public void afterConnectionEstablished(
            WebSocketSession session) throws Exception {

        if (!(session.getPrincipal() instanceof Authentication auth)
                || !auth.isAuthenticated()
                || !(auth.getPrincipal() instanceof AppUserPrincipal user)) {

            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        Long userId = user.getUser().getId();

        userIds.put(session.getId(), userId);
        sessions.put(session.getId(), session);
    }

    @Override
    public void afterConnectionClosed(
            WebSocketSession session,
            CloseStatus status) {

        sessions.remove(session.getId());
        userIds.remove(session.getId());
    }

    public void closeForHttpSession(String httpSessionId) {
        if (httpSessionId == null) {
            return;
        }

        for (WebSocketSession session : sessions.values()) {
            Object id = session.getAttributes().get(
                    HttpSessionHandshakeInterceptor.HTTP_SESSION_ID_ATTR_NAME
            );

            if (!httpSessionId.equals(id)) {
                continue;
            }

            sessions.remove(session.getId());
            userIds.remove(session.getId());

            try {
                synchronized (session) {
                    session.close(CloseStatus.POLICY_VIOLATION);
                }
            } catch (Exception e) {
                log.debug(
                        "WebSocket: failed to close session {}",
                        session.getId(),
                        e
                );
            }
        }
    }

    public void closeAllForUser(Long userId) {
        for (Map.Entry<String, Long> entry : userIds.entrySet()) {
            if (!userId.equals(entry.getValue())) {
                continue;
            }

            WebSocketSession session = sessions.remove(entry.getKey());
            userIds.remove(entry.getKey());

            if (session == null) {
                continue;
            }

            try {
                synchronized (session) {
                    session.close(CloseStatus.POLICY_VIOLATION);
                }
            } catch (Exception e) {
                log.debug(
                        "WebSocket: failed to close session {}",
                        session.getId(),
                        e
                );
            }
        }
    }
}