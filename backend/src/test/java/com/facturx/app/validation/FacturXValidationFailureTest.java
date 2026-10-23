package com.facturx.app.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import com.facturx.app.document.Document;
import com.facturx.app.document.DocumentRepository;
import com.facturx.app.document.DocumentStatus;
import com.facturx.app.websocket.DocumentStatusChangedEvent;
import com.facturx.app.websocket.DocumentWebSocketHandler;

class FacturXValidationFailureTest {

    @Test
    void mustangFailureSavesFailedStatusAndNotifiesClients()
            throws Exception {

        MustangValidationClient mustang =
                mock(MustangValidationClient.class);

        ValidationRunRepository runs =
                mock(ValidationRunRepository.class);

        ValidationErrorRepository errors =
                mock(ValidationErrorRepository.class);

        DocumentRepository documents =
                mock(DocumentRepository.class);

        DocumentWebSocketHandler websocket =
                mock(DocumentWebSocketHandler.class);

        Document document = mock(Document.class, RETURNS_DEEP_STUBS);

        when(document.getId()).thenReturn(90L);
        when(document.getOrganization().getId()).thenReturn(1L);
        when(document.getOwner().getId()).thenReturn(10L);

        when(documents.findById(90L))
                .thenReturn(Optional.of(document));

        // Le mock conserve le statut attribué au document.
        doAnswer(invocation -> {
            DocumentStatus status = invocation.getArgument(0);
            when(document.getStatus()).thenReturn(status);
            return null;
        }).when(document).setStatus(any(DocumentStatus.class));

        byte[] file = new byte[] { 1, 2, 3 };

        RuntimeException failure =
                new IllegalStateException("Simulated Mustang failure");

        when(mustang.validate(file, "test.pdf"))
                .thenThrow(failure);

        FacturXValidationService service =
                new FacturXValidationService(
                        mustang,
                        runs,
                        errors,
                        documents,
                        websocket
                );

        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> service.validate(file, "test.pdf", 90L)
        );

        assertSame(failure, thrown);
        assertEquals(DocumentStatus.FAILED, document.getStatus());

        ArgumentCaptor<DocumentStatusChangedEvent> events =
                ArgumentCaptor.forClass(DocumentStatusChangedEvent.class);

        InOrder order = inOrder(document, documents, websocket);

        order.verify(document).setStatus(DocumentStatus.PROCESSING);
        order.verify(documents).save(document);
        order.verify(websocket).broadcast(events.capture());

        order.verify(document).setStatus(DocumentStatus.FAILED);
        order.verify(documents).save(document);
        order.verify(websocket).broadcast(events.capture());

        assertEquals(2, events.getAllValues().size());

        DocumentStatusChangedEvent processing =
                events.getAllValues().get(0);

        DocumentStatusChangedEvent failed =
                events.getAllValues().get(1);

        assertEquals(DocumentStatus.PROCESSING, processing.status());
        assertEquals(DocumentStatus.FAILED, failed.status());
        assertEquals("document.status.changed", failed.type());
        assertEquals(Long.valueOf(90L), failed.documentId());
        assertEquals(Long.valueOf(1L), failed.organizationId());

        verifyNoInteractions(runs, errors);
    }
}
