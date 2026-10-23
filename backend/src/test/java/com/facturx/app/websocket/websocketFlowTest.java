package com.facturx.app.websocket;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.core.Authentication;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import com.facturx.app.auth.AppUserPrincipal;
import com.facturx.app.document.DocumentStatus;
import com.facturx.app.permission.Permission;
import com.facturx.app.permission.PermissionService;

import tools.jackson.databind.json.JsonMapper;

class DocumentWebSocketHandlerTest {

    private static final Long ORG_1 = 1L;
    private static final Long ORG_2 = 2L;
    private static final Long ALICE = 10L;
    private static final Long BOB = 11L;
    private static final Long EVE = 20L;

    private PermissionService permissionService;
    private DocumentWebSocketHandler handler;

    @BeforeEach
    void setUp() {
        permissionService = mock(PermissionService.class);
        handler = new DocumentWebSocketHandler(
                permissionService,
                JsonMapper.builder().build()
        );

        when(permissionService.hasPermission(
                ALICE, ORG_1, Permission.VIEW_ALL_DOCUMENTS
        )).thenReturn(true);

        when(permissionService.hasPermission(
                BOB, ORG_1, Permission.VIEW_ALL_DOCUMENTS
        )).thenReturn(true);

        when(permissionService.hasPermission(
                EVE, ORG_2, Permission.VIEW_ALL_DOCUMENTS
        )).thenReturn(true);
    }

    private WebSocketSession connect(
            String sessionId,
            Long userId) throws Exception {

        return connect(sessionId, userId, "http-" + sessionId);
    }

    private WebSocketSession connect(
            String sessionId,
            Long userId,
            String httpSessionId) throws Exception {

        AppUserPrincipal principal =
                mock(AppUserPrincipal.class, RETURNS_DEEP_STUBS);
        when(principal.getUser().getId()).thenReturn(userId);

        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(principal);

        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn(sessionId);
        when(session.isOpen()).thenReturn(true);
        when(session.getPrincipal()).thenReturn(auth);
        when(session.getAttributes()).thenReturn(
                new HashMap<>(Map.of(
                        HttpSessionHandshakeInterceptor.HTTP_SESSION_ID_ATTR_NAME,
                        httpSessionId
                ))
        );

        handler.afterConnectionEstablished(session);
        return session;
    }

    private DocumentStatusChangedEvent eventForOrg1() {
        return new DocumentStatusChangedEvent(
                "document.status.changed",
                100L,
                ORG_1,
                DocumentStatus.VALID,
                ALICE
        );
    }

    @Test
    void twoClientsOfSameOrganisationReceiveTheUpdate() throws Exception {
        WebSocketSession alice = connect("s-alice", ALICE);
        WebSocketSession bob = connect("s-bob", BOB);

        handler.broadcast(eventForOrg1());

        ArgumentCaptor<TextMessage> captor =
                ArgumentCaptor.forClass(TextMessage.class);

        verify(alice).sendMessage(captor.capture());
        verify(bob).sendMessage(any(TextMessage.class));

        String payload = captor.getValue().getPayload();

        assertTrue(payload.contains("document.status.changed"));
        assertTrue(payload.contains("\"documentId\":100"));
        assertTrue(payload.contains("VALID"));
        assertFalse(
                payload.contains("ownerId"),
                "ownerId must never leak to clients"
        );
    }

    @Test
    void clientFromAnotherOrganisationReceivesNothing() throws Exception {
        WebSocketSession alice = connect("s-alice", ALICE);
        WebSocketSession eve = connect("s-eve", EVE);

        handler.broadcast(eventForOrg1());

        verify(alice).sendMessage(any(TextMessage.class));
        verify(eve, never()).sendMessage(any());
    }

    @Test
    void cleanDisconnectionRemovesTheSession() throws Exception {
        WebSocketSession alice = connect("s-alice", ALICE);

        handler.afterConnectionClosed(alice, CloseStatus.NORMAL);
        handler.broadcast(eventForOrg1());

        verify(alice, never()).sendMessage(any());
    }

    @Test
    void brokenClientDoesNotBlockOthers() throws Exception {
        WebSocketSession broken = connect("s-broken", ALICE);
        WebSocketSession bob = connect("s-bob", BOB);

        doThrow(new IOException("connection reset"))
                .when(broken).sendMessage(any());

        handler.broadcast(eventForOrg1());

        verify(bob).sendMessage(any(TextMessage.class));
    }

    @Test
    void closeAllForUserClosesOnlyThatUser() throws Exception {
        WebSocketSession alice1 = connect("s-alice-1", ALICE);
        WebSocketSession alice2 = connect("s-alice-2", ALICE);
        WebSocketSession bob = connect("s-bob", BOB);

        handler.closeAllForUser(ALICE);
        handler.broadcast(eventForOrg1());

        verify(alice1).close(CloseStatus.POLICY_VIOLATION);
        verify(alice2).close(CloseStatus.POLICY_VIOLATION);

        verify(alice1, never()).sendMessage(any());
        verify(alice2, never()).sendMessage(any());

        verify(bob, never()).close(any());
        verify(bob).sendMessage(any(TextMessage.class));
    }

    @Test
    void logoutOfOneHttpSessionKeepsTheOtherSocketOfTheSameUser()
            throws Exception {

        WebSocketSession tab1 = connect("ws-1", ALICE, "http-A");
        WebSocketSession tab2 = connect("ws-2", ALICE, "http-A");
        WebSocketSession incognito = connect("ws-3", ALICE, "http-B");

        handler.closeForHttpSession("http-A");
        handler.broadcast(eventForOrg1());

        verify(tab1).close(CloseStatus.POLICY_VIOLATION);
        verify(tab2).close(CloseStatus.POLICY_VIOLATION);

        verify(tab1, never()).sendMessage(any());
        verify(tab2, never()).sendMessage(any());

        verify(incognito, never()).close(any());
        verify(incognito).sendMessage(any(TextMessage.class));
    }
}