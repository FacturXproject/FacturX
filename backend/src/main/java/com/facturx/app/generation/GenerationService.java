package com.facturx.app.generation;

import com.facturx.app.document.Document;
import com.facturx.app.document.DocumentService;
import com.facturx.app.extraction.DraftCorrection;
import com.facturx.app.extraction.ExtractionService;
import com.facturx.app.extraction.ValidatedDraft;
import com.facturx.app.validation.FacturXValidationService;
import com.facturx.app.validation.ValidationReport;
import com.facturx.app.validation.ValidationReportService;
import com.facturx.app.validation.ValidationResult;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * F13 : brouillon valide (F12) -> fichier Factur-X genere par le service Python ->
 * validation independante par le moteur Java (Mustang, F08) -> rapport lisible (F09).
 *
 * <p>Le generateur ne juge jamais sa propre production : le fichier est enregistre comme
 * un document de l'organisation puis valide exactement comme un fichier depose.
 */
@Service
public class GenerationService {

    private static final Logger log = LoggerFactory.getLogger(GenerationService.class);

    public static final Set<String> PROFILES = Set.of("EN16931", "BASIC");
    public static final String DEFAULT_PROFILE = "EN16931";

    private final ExtractionService extractionService;
    private final DocumentService documentService;
    private final GenerationClient generationClient;
    private final FacturXValidationService validationService;
    private final ValidationReportService validationReportService;
    private final GeneratedInvoiceRepository generatedInvoiceRepository;

    public GenerationService(ExtractionService extractionService,
                             DocumentService documentService,
                             GenerationClient generationClient,
                             FacturXValidationService validationService,
                             ValidationReportService validationReportService,
                             GeneratedInvoiceRepository generatedInvoiceRepository) {
        this.extractionService = extractionService;
        this.documentService = documentService;
        this.generationClient = generationClient;
        this.validationService = validationService;
        this.validationReportService = validationReportService;
        this.generatedInvoiceRepository = generatedInvoiceRepository;
    }

    public GenerationResponse generate(Long sourceDocumentId, String requestedProfile, Long userId) {
        String profile = requestedProfile == null || requestedProfile.isBlank() ? DEFAULT_PROFILE : requestedProfile;
        if (!PROFILES.contains(profile)) {
            throw new GenerationPreconditionException("GENERATION_PROFILE_UNSUPPORTED",
                    List.of("Profil non supporté : " + profile + ". Profils disponibles : EN16931, BASIC."));
        }

        Document source = documentService.getDocument(sourceDocumentId);
        ValidatedDraft draft = extractionService.validatedDraftForGeneration(sourceDocumentId);
        if (!draft.humanChecked()) {
            throw new GenerationPreconditionException("GENERATION_DRAFT_NOT_CHECKED", List.of(
                    "Vérifiez et enregistrez d'abord le brouillon dans le formulaire de correction : "
                            + "une facture n'est jamais générée à partir de l'extraction seule."));
        }
        checkFacturXRequirements(draft.data());

        GeneratedInvoice generation = new GeneratedInvoice();
        generation.setSourceDocumentId(sourceDocumentId);
        generation.setDraftInvoiceId(draft.draftId());
        generation.setOrganizationId(source.getOrganization().getId());
        generation.setProfile(profile);
        generation.setCreatedBy(userId);

        // 1. Generation (service Python)
        byte[] pdf;
        try {
            pdf = generationClient.generate(toPayload(draft.data(), profile));
        } catch (GenerationFailedException e) {
            return fail(generation, e.getMessage(), null);
        }

        // 2. Enregistrement comme document de l'organisation
        String filename = filename(draft.data().fields().get("invoiceNumber"));
        Document generated = documentService.storeGenerated(pdf, filename, source.getOrganization(), userId);
        generation.setGeneratedDocumentId(generated.getId());

        // 3. Validation independante (Mustang : XML, regles EN 16931, PDF/A-3)
        ValidationResult result;
        try {
            result = validationService.validate(pdf, filename, generated.getId());
        } catch (RuntimeException e) {
            log.error("Validation of generated Factur-X failed", e);
            return fail(generation, "Le fichier a été généré mais sa validation a échoué (erreur technique).",
                    generated);
        }

        generation.setValidationRunId(result.runId());
        generation.setStatus(result.valid() ? GenerationStatus.VALID : GenerationStatus.INVALID);
        generatedInvoiceRepository.save(generation);
        return toResponse(generation, generated);
    }

    public List<GenerationResponse> history(Long sourceDocumentId) {
        return generatedInvoiceRepository.findBySourceDocumentIdOrderByCreatedAtDesc(sourceDocumentId)
                .stream()
                .map(generation -> toResponse(generation, generation.getGeneratedDocumentId() == null
                        ? null : documentService.getDocument(generation.getGeneratedDocumentId())))
                .toList();
    }

    /**
     * Regles EN 16931 que le formulaire F12 n'impose pas (il accepte un brouillon
     * incomplet) mais sans lesquelles le fichier genere serait forcement refuse.
     */
    private void checkFacturXRequirements(DraftCorrection.Result data) {
        List<String> errors = new ArrayList<>();
        boolean standardRate = data.taxes().stream().anyMatch(tax -> tax.rate().signum() > 0);
        if (standardRate && data.fields().get("sellerVat") == null) {
            // BR-S-02 : TVA facturee => numero de TVA intracommunautaire du vendeur obligatoire.
            errors.add("Le numéro de TVA intracommunautaire du vendeur est obligatoire pour une facture avec TVA.");
        }
        if (!errors.isEmpty()) {
            throw new GenerationPreconditionException("GENERATION_DRAFT_INCOMPLETE", errors);
        }
    }

    private GenerationResponse fail(GeneratedInvoice generation, String message, Document generated) {
        generation.setStatus(GenerationStatus.FAILED);
        generation.setErrorMessage(message);
        generatedInvoiceRepository.save(generation);
        return toResponse(generation, generated);
    }

    private GenerationResponse toResponse(GeneratedInvoice generation, Document generated) {
        long errorCount = 0;
        long warningCount = 0;
        if (generation.getValidationRunId() != null) {
            ValidationReport report = validationReportService.getReport(generation.getValidationRunId());
            errorCount = report.errorCount();
            warningCount = report.warningCount();
        }
        return new GenerationResponse(
                generation.getId(),
                generation.getSourceDocumentId(),
                generation.getGeneratedDocumentId(),
                generated != null ? generated.getFilename() : null,
                generation.getProfile(),
                generation.getStatus(),
                generation.getValidationRunId(),
                errorCount,
                warningCount,
                generation.getErrorMessage(),
                generation.getCreatedAt());
    }

    static GenerationPayload toPayload(DraftCorrection.Result data, String profile) {
        Map<String, String> f = data.fields();
        BigDecimal defaultRate = DraftCorrection.parseRate(f.get("vatRate"));

        List<GenerationPayload.Line> lines = data.lines().stream()
                .map(line -> new GenerationPayload.Line(
                        line.description(),
                        line.quantity().toPlainString(),
                        line.unitPrice().toPlainString(),
                        DraftCorrection.plain(line.vatRate() != null ? line.vatRate() : defaultRate),
                        line.total().toPlainString()))
                .toList();
        List<GenerationPayload.Tax> taxes = data.taxes().stream()
                .map(tax -> new GenerationPayload.Tax(
                        DraftCorrection.plain(tax.rate()),
                        tax.taxableAmount().toPlainString(),
                        tax.taxAmount().toPlainString()))
                .toList();

        return new GenerationPayload(profile, new GenerationPayload.Invoice(
                f.get("invoiceNumber"),
                isoDate(f.get("invoiceDate")),
                isoDate(f.get("dueDate")),
                "EUR",
                new GenerationPayload.Party(f.get("sellerName"), f.get("sellerSiren"), f.get("sellerVat"),
                        f.get("sellerAddress")),
                new GenerationPayload.Party(f.get("buyerName"), f.get("buyerSiren"), null, f.get("buyerAddress")),
                lines,
                taxes,
                data.totalHt().toPlainString(),
                data.totalVat().toPlainString(),
                data.totalTtc().toPlainString()));
    }

    private static String isoDate(String value) {
        if (value == null) {
            return null;
        }
        LocalDate date = DraftCorrection.parseDate(value);
        return date != null ? date.toString() : null;
    }

    // "FACT 2026/001" -> "Factur-X_FACT_2026_001.pdf"
    static String filename(String invoiceNumber) {
        String safe = invoiceNumber == null ? "facture" : invoiceNumber.replaceAll("[^A-Za-z0-9._-]+", "_");
        return "Factur-X_" + safe + ".pdf";
    }
}
