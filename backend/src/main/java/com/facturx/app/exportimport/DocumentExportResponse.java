package com.facturx.app.exportimport;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.facturx.app.document.Document;
import com.facturx.app.document.DocumentStatus;
import com.facturx.app.validation.ReadableValidationError;
import com.facturx.app.validation.ValidationReport;

public record DocumentExportResponse(
    Long id,
    String organizationName,
    String uploadedBy,
    String filename,
    String type,
    double sizeMb,
    DocumentStatus status,
    String uploadedAt,
    String email,
    String valid,
    Long errorCount,
    List<String> ruleCodes
) {
    public static DocumentExportResponse from(
            Document document,
            ValidationReport report
    ) {
        List<String> ruleCodes = new ArrayList<>();

        // Păstrează fiecare cod o singură dată.
        if (report != null) {
            for (ReadableValidationError error : report.errors()) {
                if (!ruleCodes.contains(error.ruleCode())) {
                    ruleCodes.add(error.ruleCode());
                }
            }
        }

        return new DocumentExportResponse(
            document.getId(),

            document.getOrganization() != null
                ? document.getOrganization().getName()
                : null,

            document.getOwner() != null
                ? document.getOwner().getFirstName() + " "
                    + document.getOwner().getLastName()
                : null,

            document.getFilename(),

            document.getType() != null
                ? document.getType()
                    .substring(document.getType().lastIndexOf('/') + 1)
                    .toUpperCase(Locale.ROOT)
                : null,

            Math.round(document.getSize() / 1_000_000.0 * 100) / 100.0,

            document.getStatus(),

            document.getUploadedAt() != null
                ? DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                    .withZone(ZoneId.of("Europe/Paris"))
                    .format(document.getUploadedAt())
                : null,

            document.getOwner() != null
                ? document.getOwner().getEmail()
                : null,

            report == null
                ? "Non validé"
                : (report.valid() ? "Oui" : "Non"),

            report != null ? report.errorCount() : null,

            ruleCodes
        );
    }
}