package com.facturx.app.document;

import com.facturx.app.permission.AccessDeniedException;
import com.facturx.app.permission.Permission;
import com.facturx.app.permission.PermissionService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * F16 - advanced document search. Validates the raw request parameters, applies
 * organisation + role scoping, then runs one paginated query.
 *
 * Scoping: the organisation is mandatory and the caller must be a member of it.
 * A member with VIEW_ALL_DOCUMENTS (admin, accountant) searches every document of
 * the organisation and may filter by uploader; a member with only VIEW_OWN_DOCUMENTS
 * (client) is always restricted to their own uploads.
 *
 * Pagination is offset-based on a total order: the requested sort column, then the
 * id as tie-breaker, so two documents with the same date/status/name never swap
 * places between two page requests.
 */
@Service
public class DocumentSearchService {

    static final int DEFAULT_PAGE_SIZE = 20;
    static final int MAX_PAGE_SIZE = 100;
    static final int MAX_TEXT_LENGTH = 255;

    /** Dates in filters are calendar days as seen by our (French) users. */
    static final ZoneId ZONE = ZoneId.of("Europe/Paris");

    /** Public type filter value -> content types stored by the upload (F06). */
    static final Map<String, Set<String>> TYPES = Map.of(
            "PDF", Set.of("application/pdf"),
            "XML", Set.of("application/xml", "text/xml"));

    /** Public sort key -> entity property. */
    static final Map<String, String> SORT_FIELDS = Map.of(
            "uploadedAt", "uploadedAt",
            "filename", "filename",
            "status", "status",
            "type", "type",
            "size", "size");

    private final DocumentRepository documentRepository;
    private final PermissionService permissionService;

    public DocumentSearchService(DocumentRepository documentRepository,
                                 PermissionService permissionService) {
        this.documentRepository = documentRepository;
        this.permissionService = permissionService;
    }

    /** Raw request parameters, exactly as received (all optional but organizationId). */
    public record SearchParams(
            Long organizationId,
            String q,
            List<String> status,
            List<String> type,
            String ownerId,
            String from,
            String to,
            String error,
            String sort,
            String page,
            String size
    ) {}

    @Transactional(readOnly = true)
    public DocumentSearchResponse search(SearchParams params, Long currentUserId) {
        DocumentSearchCriteria criteria = parse(params);
        criteria = scopeToCaller(criteria, currentUserId);

        Sort sort = parseSort(params.sort());
        int page = parseInt("page", params.page(), 0, 0, Integer.MAX_VALUE);
        int size = parseInt("size", params.size(), DEFAULT_PAGE_SIZE, 1, MAX_PAGE_SIZE);

        Page<Document> result = documentRepository.findAll(
                DocumentSpecifications.matching(criteria),
                PageRequest.of(page, size, sort));

        return DocumentSearchResponse.from(result, sortLabel(sort));
    }

    private DocumentSearchCriteria scopeToCaller(DocumentSearchCriteria criteria, Long currentUserId) {
        Long organizationId = criteria.organizationId();

        if (permissionService.hasPermission(currentUserId, organizationId, Permission.VIEW_ALL_DOCUMENTS)) {
            return criteria;
        }
        if (permissionService.hasPermission(currentUserId, organizationId, Permission.VIEW_OWN_DOCUMENTS)) {
            if (criteria.ownerId() != null && !criteria.ownerId().equals(currentUserId)) {
                throw new AccessDeniedException();
            }
            return new DocumentSearchCriteria(organizationId, criteria.query(), criteria.statuses(),
                    criteria.mimeTypes(), currentUserId, criteria.uploadedFrom(), criteria.uploadedTo(),
                    criteria.error());
        }
        // Not a member (or the organisation doesn't exist): same answer, no leak.
        throw new AccessDeniedException();
    }

    // --- parameter validation ---------------------------------------------------------

    static DocumentSearchCriteria parse(SearchParams params) {
        if (params.organizationId() == null) {
            throw new InvalidSearchFilterException("organizationId", "L'organisation est obligatoire.");
        }

        LocalDate from = parseDate("from", params.from());
        LocalDate to = parseDate("to", params.to());
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidSearchFilterException("from",
                    "La date de début doit être antérieure ou égale à la date de fin.");
        }

        return new DocumentSearchCriteria(
                params.organizationId(),
                parseText("q", params.q()),
                parseStatuses(params.status()),
                parseTypes(params.type()),
                parseId("ownerId", params.ownerId()),
                from == null ? null : from.atStartOfDay(ZONE).toInstant(),
                // "to" is inclusive for the user: everything before the next midnight
                to == null ? null : to.plusDays(1).atStartOfDay(ZONE).toInstant(),
                parseText("error", params.error()));
    }

    private static String parseText(String field, String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.strip();
        if (value.length() > MAX_TEXT_LENGTH) {
            throw new InvalidSearchFilterException(field,
                    "La recherche ne peut pas dépasser " + MAX_TEXT_LENGTH + " caractères.");
        }
        return value;
    }

    private static Set<DocumentStatus> parseStatuses(List<String> raw) {
        Set<DocumentStatus> statuses = EnumSet.noneOf(DocumentStatus.class);
        for (String value : nonBlank(raw)) {
            try {
                statuses.add(DocumentStatus.valueOf(value.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                throw new InvalidSearchFilterException("status", "Statut inconnu : " + value
                        + ". Valeurs possibles : " + List.of(DocumentStatus.values()) + ".");
            }
        }
        return statuses;
    }

    private static Set<String> parseTypes(List<String> raw) {
        Set<String> mimeTypes = new LinkedHashSet<>();
        for (String value : nonBlank(raw)) {
            Set<String> expanded = TYPES.get(value.toUpperCase(Locale.ROOT));
            if (expanded == null) {
                throw new InvalidSearchFilterException("type",
                        "Type inconnu : " + value + ". Valeurs possibles : PDF, XML.");
            }
            mimeTypes.addAll(expanded);
        }
        return mimeTypes;
    }

    private static List<String> nonBlank(List<String> raw) {
        if (raw == null) {
            return List.of();
        }
        return raw.stream().map(String::strip).filter(s -> !s.isEmpty()).toList();
    }

    private static Long parseId(String field, String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            long id = Long.parseLong(raw.strip());
            if (id > 0) {
                return id;
            }
        } catch (NumberFormatException ignored) {
            // falls through to the error below
        }
        throw new InvalidSearchFilterException(field, "Identifiant invalide : " + raw + ".");
    }

    private static LocalDate parseDate(String field, String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(raw.strip());
        } catch (DateTimeParseException e) {
            throw new InvalidSearchFilterException(field,
                    "Date invalide : " + raw + ". Format attendu : AAAA-MM-JJ.");
        }
    }

    private static int parseInt(String field, String raw, int defaultValue, int min, int max) {
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        try {
            int value = Integer.parseInt(raw.strip());
            if (value >= min && value <= max) {
                return value;
            }
        } catch (NumberFormatException ignored) {
            // falls through to the error below
        }
        throw new InvalidSearchFilterException(field,
                "Valeur invalide pour " + field + " : " + raw + " (entre " + min + " et " + max + ").");
    }

    /** "field" or "field,asc|desc"; defaults to newest first. Always ends with id. */
    static Sort parseSort(String raw) {
        String key = "uploadedAt";
        Sort.Direction direction = Sort.Direction.DESC;

        if (raw != null && !raw.isBlank()) {
            String[] parts = raw.strip().split(",", -1);
            key = parts[0].strip();
            if (parts.length > 2 || !SORT_FIELDS.containsKey(key)) {
                throw new InvalidSearchFilterException("sort", "Tri invalide : " + raw
                        + ". Champs possibles : " + SORT_FIELDS.keySet() + ", suivis de ,asc ou ,desc.");
            }
            if (parts.length == 2) {
                direction = Sort.Direction.fromOptionalString(parts[1].strip())
                        .orElseThrow(() -> new InvalidSearchFilterException("sort",
                                "Direction de tri invalide : " + parts[1] + " (asc ou desc)."));
            } else {
                direction = Sort.Direction.ASC;
            }
        }

        return Sort.by(direction, SORT_FIELDS.get(key)).and(Sort.by(direction, "id"));
    }

    private static String sortLabel(Sort sort) {
        Sort.Order first = sort.iterator().next();
        return first.getProperty() + "," + first.getDirection().name().toLowerCase(Locale.ROOT);
    }
}
