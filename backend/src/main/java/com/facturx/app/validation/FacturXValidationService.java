package com.facturx.app.validation;

import java.time.OffsetDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FacturXValidationService {

    private static final Logger log = LoggerFactory.getLogger(FacturXValidationService.class);

    private final MustangValidationClient mustangValidationClient;
    private final ValidationRunRepository validationRunRepository;
    private final ValidationErrorRepository validationErrorRepository;

    public FacturXValidationService(MustangValidationClient mustangValidationClient,
                                     ValidationRunRepository validationRunRepository,
                                     ValidationErrorRepository validationErrorRepository) {
        this.mustangValidationClient = mustangValidationClient;
        this.validationRunRepository = validationRunRepository;
        this.validationErrorRepository = validationErrorRepository;
    }

    /**
     * Validates a file and persists the run and its errors. {@code documentId} is
     * nullable until F06 (document upload) exists and every call can be tied to a
     * real uploaded document.
     */
    public ValidationResult validate(byte[] file, String filename, Long documentId) {
        MustangValidationClient.MustangReport report = mustangValidationClient.validate(file, filename);
        log.debug("Mustang validation report for {}:\n{}", filename, report.rawXmlReport());

        ValidationResult result = MustangReportParser.parse(report.rawXmlReport(), report.completelyValid());

        ValidationRun run = new ValidationRun();
        run.setDocumentId(documentId);
        run.setFilename(filename);
        run.setValid(result.valid());
        run.setLayerReached(result.layerReached());
        run.setFinishedAt(OffsetDateTime.now());
        validationRunRepository.save(run);

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
