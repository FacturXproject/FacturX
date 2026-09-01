package com.facturx.app.validation;

import org.mustangproject.validator.ZUGFeRDValidator;
import org.springframework.stereotype.Component;

/**
 * Thin wrapper around Mustangproject's {@link ZUGFeRDValidator}. Keeps the third-party
 * type out of the rest of the codebase so only this class needs to change if the
 * underlying library is ever swapped.
 */
@Component
public class MustangValidationClient {

    public MustangReport validate(byte[] file, String filename) {
        ZUGFeRDValidator validator = new ZUGFeRDValidator();
        String rawReport = validator.validate(file, filename);
        return new MustangReport(rawReport, validator.wasCompletelyValid());
    }

    public record MustangReport(String rawXmlReport, boolean completelyValid) {
    }
}
