package com.facturx.app.validation;

import java.time.OffsetDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.facturx.app.document.Document;
import com.facturx.app.document.DocumentRepository;
import com.facturx.app.document.DocumentStatus;

@Service
public class FacturXValidationService {

    private static final Logger log =
            LoggerFactory.getLogger(FacturXValidationService.class);

    private final MustangValidationClient mustangValidationClient;
    private final ValidationRunRepository validationRunRepository;
    private final ValidationErrorRepository validationErrorRepository;
    private final DocumentRepository documentRepository;

    public FacturXValidationService(
            MustangValidationClient mustangValidationClient,
            ValidationRunRepository validationRunRepository,
            ValidationErrorRepository validationErrorRepository,
            DocumentRepository documentRepository) {

        this.mustangValidationClient = mustangValidationClient;
        this.validationRunRepository = validationRunRepository;
        this.validationErrorRepository = validationErrorRepository;
        this.documentRepository = documentRepository;
    }

    /**
     * Validates a file and persists the run and its errors.
     * documentId can be null for validation without an uploaded document.
     */
    public ValidationResult validate(
            byte[] file,
            String filename,
            Long documentId) {

        Document document = null;

        // If validation is linked to an uploaded document
        if (documentId != null) {
            document = documentRepository.findById(documentId)
                    .orElseThrow(() ->
                            new RuntimeException("Document not found"));

            document.setStatus(DocumentStatus.PROCESSING);
            documentRepository.save(document);
        }

        MustangValidationClient.MustangReport report;

        try {
            report = mustangValidationClient.validate(file, filename);
        } catch (RuntimeException e) {

            // Update status only if we have a document
            if (document != null) {
                document.setStatus(DocumentStatus.FAILED);
                documentRepository.save(document);
            }

            throw e;
        }

        log.debug(
                "Mustang validation report for {}:\n{}",
                filename,
                report.rawXmlReport()
        );

        ValidationResult result = MustangReportParser.parse(
                report.rawXmlReport(),
                report.completelyValid()
        );

        // Update final document status
        if (document != null) {
            if (result.valid()) {
                document.setStatus(DocumentStatus.VALID);
            } else {
                document.setStatus(DocumentStatus.INVALID);
            }

            documentRepository.save(document);
        }

        // Save validation run
        ValidationRun run = new ValidationRun();
        run.setDocumentId(documentId);
        run.setFilename(filename);
        run.setValid(result.valid());
        run.setLayerReached(result.layerReached());
        run.setFinishedAt(OffsetDateTime.now());

        validationRunRepository.save(run);

        // Save validation errors
        for (ValidationError error : result.errors()) {

            ValidationErrorEntity entity = new ValidationErrorEntity();

            entity.setRun(run);
            entity.setLayer(error.layer());
            entity.setSeverity(error.severity());
            entity.setRuleCode(error.ruleCode());
            entity.setMessage(error.message());
            entity.setField(error.field());
            entity.setActualValue(error.actualValue());
            entity.setExpectedValue(error.expectedValue());

            validationErrorRepository.save(entity);
        }

        return result;
    }
}