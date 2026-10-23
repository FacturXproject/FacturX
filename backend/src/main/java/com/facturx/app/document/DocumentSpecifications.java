package com.facturx.app.document;

import com.facturx.app.validation.RuleCatalog;
import com.facturx.app.validation.ValidationErrorEntity;
import com.facturx.app.validation.ValidationRun;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/**
 * F16: turns validated {@link DocumentSearchCriteria} into a single JPA predicate.
 * Filters are AND-ed; values inside one filter (several statuses) are OR-ed.
 * The organisation predicate is always present - that is the tenant isolation.
 */
final class DocumentSpecifications {

    private static final char LIKE_ESCAPE = '!';

    private DocumentSpecifications() {}

    static Specification<Document> matching(DocumentSearchCriteria criteria) {
        return (root, query, cb) -> {
            // The response maps owner and organisation of every row: load them in the
            // same query instead of one SELECT per distinct owner (N+1). Spring Data
            // reuses this Specification for the count query, where a fetch is illegal.
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("owner", JoinType.LEFT);
                root.fetch("organization", JoinType.LEFT);
            }

            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("organization").get("id"), criteria.organizationId()));

            if (criteria.query() != null) {
                predicates.add(cb.like(cb.lower(root.get("filename")),
                        containsPattern(criteria.query()), LIKE_ESCAPE));
            }
            if (!criteria.statuses().isEmpty()) {
                predicates.add(root.get("status").in(criteria.statuses()));
            }
            if (!criteria.mimeTypes().isEmpty()) {
                predicates.add(root.get("type").in(criteria.mimeTypes()));
            }
            if (criteria.ownerId() != null) {
                predicates.add(cb.equal(root.get("owner").get("id"), criteria.ownerId()));
            }
            if (criteria.uploadedFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("uploadedAt"), criteria.uploadedFrom()));
            }
            if (criteria.uploadedTo() != null) {
                predicates.add(cb.lessThan(root.get("uploadedAt"), criteria.uploadedTo()));
            }
            if (criteria.error() != null) {
                predicates.add(hasMatchingError(root, query, cb, containsPattern(criteria.error())));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    /**
     * EXISTS an error, in the document's latest validation run, whose rule code, raw
     * Mustang message or French catalog title contains the pattern. Only the latest
     * run counts (that's the one the status and the F09 report reflect), and the
     * BR-DE / PEPPOL families are skipped since ValidationReportService hides them
     * from users - a search must not match on something the user can't see.
     */
    private static Predicate hasMatchingError(Root<Document> document, CriteriaQuery<?> query,
                                              CriteriaBuilder cb, String pattern) {
        Subquery<Long> latestRun = query.subquery(Long.class);
        Root<ValidationRun> run = latestRun.from(ValidationRun.class);
        latestRun.select(cb.max(run.get("id")))
                .where(cb.equal(run.get("documentId"), document.get("id")));

        Subquery<String> catalogCodes = query.subquery(String.class);
        Root<RuleCatalog> rule = catalogCodes.from(RuleCatalog.class);
        catalogCodes.select(rule.get("code"))
                .where(cb.like(cb.lower(rule.get("titleFr")), pattern, LIKE_ESCAPE));

        Subquery<Long> errors = query.subquery(Long.class);
        Root<ValidationErrorEntity> error = errors.from(ValidationErrorEntity.class);
        Expression<String> ruleCode = error.get("ruleCode");
        errors.select(error.get("id")).where(
                cb.equal(error.get("run").get("id"), latestRun),
                cb.notLike(ruleCode, "BR-DE%"),
                cb.notLike(ruleCode, "PEPPOL%"),
                cb.or(
                        cb.like(cb.lower(ruleCode), pattern, LIKE_ESCAPE),
                        cb.like(cb.lower(error.get("message")), pattern, LIKE_ESCAPE),
                        ruleCode.in(catalogCodes)
                ));

        return cb.exists(errors);
    }

    /** "%term%", lowercased, with LIKE wildcards in the user's input escaped. */
    private static String containsPattern(String term) {
        String escaped = term.toLowerCase(Locale.ROOT)
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
        return "%" + escaped + "%";
    }
}
