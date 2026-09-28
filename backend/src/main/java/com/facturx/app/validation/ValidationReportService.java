package com.facturx.app.validation;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Turns a persisted {@link ValidationRun} + its {@link ValidationErrorEntity} rows into
 * a {@link ValidationReport} a non-expert can understand, joining against
 * {@link RuleCatalog} for the French explanation of each rule.
 */
@Service
public class ValidationReportService {

    // Shown instead of a bare rule code when the catalog has no curated row for it yet -
    // "never a bare rule code with no explanation" (F09 mini-task). descriptionFr falls
    // back to Mustang's own message, which is still more useful than nothing.
    private static final String UNCATALOGUED_TITLE_FR = "Erreur de validation non répertoriée";

    private final ValidationRunRepository validationRunRepository;
    private final ValidationErrorRepository validationErrorRepository;
    private final RuleCatalogRepository ruleCatalogRepository;

    public ValidationReportService(ValidationRunRepository validationRunRepository,
                                    ValidationErrorRepository validationErrorRepository,
                                    RuleCatalogRepository ruleCatalogRepository) {
        this.validationRunRepository = validationRunRepository;
        this.validationErrorRepository = validationErrorRepository;
        this.ruleCatalogRepository = ruleCatalogRepository;
    }

    public ValidationReport getReport(Long runId) {
        ValidationRun run = validationRunRepository.findById(runId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucun rapport de validation pour cet identifiant."));

        return buildReport(run);
    }

    /**
     * F08+F07 integration point: a document (real identity since F07) can now be
     * validated more than once, so the report shown on its detail page is always the
     * one for its most recent {@link ValidationRun}.
     */
    public ValidationReport getReportForDocument(Long documentId) {
        ValidationRun run = validationRunRepository.findTopByDocumentIdOrderByFinishedAtDesc(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune validation n'a encore été effectuée pour ce document."));

        return buildReport(run);
    }

    private ValidationReport buildReport(ValidationRun run) {
        List<ValidationErrorEntity> errorEntities = validationErrorRepository.findByRunId(run.getId());

        Map<String, RuleCatalog> catalogByCode = ruleCatalogRepository
                .findAllById(errorEntities.stream().map(ValidationErrorEntity::getRuleCode).distinct().toList())
                .stream()
                .collect(Collectors.toMap(RuleCatalog::getCode, Function.identity()));

        List<ReadableValidationError> readableErrors = errorEntities.stream()
                .map(entity -> enrich(entity, catalogByCode.get(entity.getRuleCode())))
                .toList();

        return new ValidationReport(
                run.getId(),
                run.getFilename(),
                run.isValid(),
                run.getLayerReached(),
                run.getStartedAt(),
                run.getFinishedAt(),
                countBySeverity(readableErrors, ValidationSeverity.ERROR),
                countBySeverity(readableErrors, ValidationSeverity.WARNING),
                countBySeverity(readableErrors, ValidationSeverity.INFO),
                readableErrors
        );
    }

    private ReadableValidationError enrich(ValidationErrorEntity entity, RuleCatalog rule) {
        boolean hasCatalogEntry = rule != null && rule.getTitleFr() != null;
        return new ReadableValidationError(
                entity.getLayer(),
                entity.getSeverity(),
                entity.getRuleCode(),
                entity.getMessage(),
                entity.getField(),
                entity.getActualValue(),
                entity.getExpectedValue(),
                hasCatalogEntry ? rule.getTitleFr() : UNCATALOGUED_TITLE_FR,
                hasCatalogEntry ? rule.getDescriptionFr() : entity.getMessage(),
                hasCatalogEntry ? rule.getCorrectionHintFr() : null
        );
    }

    private long countBySeverity(List<ReadableValidationError> errors, ValidationSeverity severity) {
        return errors.stream().filter(e -> e.severity() == severity).count();
    }
}
