package com.facturx.app.websocket;

import com.facturx.app.document.DocumentStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;

public record DocumentStatusChangedEvent(
    String type,
    Long documentId,
    Long organizationId,
    DocumentStatus status,
    @JsonIgnore Long ownerId
) {

}
