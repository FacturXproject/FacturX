package com.facturx.app.document;

import java.time.Instant;
import java.util.Set;

/**
 * F16: validated search filters. Every field is optional (null / empty = no filter)
 * except organizationId. Built by {@link DocumentSearchService#parse}, never directly
 * from raw request strings.
 *
 * @param query         substring of the filename, case-insensitive
 * @param statuses      document statuses (OR between them)
 * @param mimeTypes     stored content types, already expanded from PDF / XML
 * @param ownerId       uploader
 * @param uploadedFrom  inclusive lower bound
 * @param uploadedTo    exclusive upper bound
 * @param error         substring of a rule code, raw message or French title of an
 *                      error in the document's latest validation run
 */
public record DocumentSearchCriteria(
        Long organizationId,
        String query,
        Set<DocumentStatus> statuses,
        Set<String> mimeTypes,
        Long ownerId,
        Instant uploadedFrom,
        Instant uploadedTo,
        String error
) {
}
