package com.facturx.app.extraction;

import com.facturx.app.document.Document;
import com.facturx.app.document.DocumentService;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExtractionService {

    static final String SOURCE_MANUAL = "manual";
    static final String SOURCE_CALCULATED = "calculated";

    private final DocumentService documentService;
    private final ExtractionClient extractionClient;
    private final DraftInvoiceRepository draftInvoiceRepository;
    private final ExtractedFieldRepository extractedFieldRepository;
    private final DraftLineRepository draftLineRepository;
    private final DraftTaxRepository draftTaxRepository;

    public ExtractionService(
            DocumentService documentService,
            ExtractionClient extractionClient,
            DraftInvoiceRepository draftInvoiceRepository,
            ExtractedFieldRepository extractedFieldRepository,
            DraftLineRepository draftLineRepository,
            DraftTaxRepository draftTaxRepository) {

        this.documentService = documentService;
        this.extractionClient = extractionClient;
        this.draftInvoiceRepository = draftInvoiceRepository;
        this.extractedFieldRepository = extractedFieldRepository;
        this.draftLineRepository = draftLineRepository;
        this.draftTaxRepository = draftTaxRepository;
    }

    // @Transactional : evite qu'un brouillon reste persiste avec des champs/lignes
    // partiels si une sauvegarde echoue en cours de route (ex: valeur extraite plus
    // longue que prevu) - soit tout est sauvegarde, soit rien ne l'est.
    @Transactional
    public DraftInvoiceResponse extract(Long documentId) {
        Document document = documentService.getDocument(documentId);

        if (!"application/pdf".equals(document.getType())) {
            throw new UnsupportedDocumentTypeException();
        }

        byte[] bytes = documentService.readFileBytes(document);

        ExtractionResponse response = extractionClient.extract(bytes, document.getFilename());
        if (response == null || response.fields() == null || response.lines() == null) {
            throw new ExtractionFailedException("Réponse invalide du service d'extraction.");
        }

        DraftInvoice draft = new DraftInvoice();
        draft.setDocumentId(documentId);
        draft.setOrganizationId(document.getOrganization().getId());
        draft.setSource(response.source());
        draft = draftInvoiceRepository.save(draft);

        for (Map.Entry<String, ExtractionResponse.FieldValue> entry : response.fields().entrySet()) {
            ExtractedField field = new ExtractedField();
            field.setDraftInvoice(draft);
            field.setFieldName(entry.getKey());
            field.setValue(entry.getValue() != null ? entry.getValue().value() : null);
            field.setConfidence(clamp(entry.getValue() != null ? entry.getValue().confidence() : null));
            field.setSource(response.source());
            extractedFieldRepository.save(field);
        }

        int position = 0;
        for (ExtractionResponse.LineValue line : response.lines()) {
            DraftLine draftLine = new DraftLine();
            draftLine.setDraftInvoice(draft);
            draftLine.setPosition(position++);
            draftLine.setDescription(line.description());
            draftLine.setQuantity(line.quantity());
            draftLine.setUnitPrice(line.unitPrice());
            draftLine.setTotal(line.total());
            draftLine.setConfidence(clamp(line.confidence()));
            draftLine.setSource(response.source());
            draftLineRepository.save(draftLine);
        }

        return toResponse(draft);
    }

    /**
     * F12 : enregistre les corrections de l'utilisateur sur le dernier brouillon du
     * document. Tout est valide et recalcule ici (DraftCorrection), independamment du
     * frontend ; les totaux envoyes par le client sont ignores.
     */
    @Transactional
    public DraftInvoiceResponse updateDraft(Long documentId, DraftInvoiceUpdateRequest request) {
        DraftCorrection.Result corrected = DraftCorrection.validate(request);

        DraftInvoice draft = draftInvoiceRepository
                .findTopByDocumentIdOrderByCreatedAtDesc(documentId)
                .orElseThrow(DraftNotFoundException::new);

        if (request.updatedAt() != null && !request.updatedAt().isEqual(draft.getUpdatedAt())) {
            throw new DraftConflictException();
        }

        Map<String, ExtractedField> fieldsByName = new HashMap<>();
        for (ExtractedField field : extractedFieldRepository.findByDraftInvoiceId(draft.getId())) {
            fieldsByName.put(field.getFieldName(), field);
        }

        // Un champ modifie perd son score de confiance : ce n'est plus l'extracteur
        // qui en repond mais l'utilisateur (source "manual"). Un champ inchange garde
        // sa confiance et sa source d'origine.
        for (Map.Entry<String, String> entry : corrected.fields().entrySet()) {
            String name = entry.getKey();
            String value = entry.getValue();
            ExtractedField field = fieldsByName.get(name);

            if (field == null) {
                if (value == null) {
                    continue;
                }
                field = new ExtractedField();
                field.setDraftInvoice(draft);
                field.setFieldName(name);
            } else if (sameFieldValue(name, field.getValue(), value)) {
                continue;
            }

            field.setValue(value);
            field.setConfidence(null);
            field.setSource(SOURCE_MANUAL);
            extractedFieldRepository.save(field);
        }

        saveCalculatedField(draft, fieldsByName, "totalHt", corrected.totalHt());
        saveCalculatedField(draft, fieldsByName, "totalVat", corrected.totalVat());
        saveCalculatedField(draft, fieldsByName, "totalTtc", corrected.totalTtc());

        // Les lignes sont remplacees ; une ligne identique a celle de meme position
        // conserve sa confiance et sa source, les autres passent en "manual".
        List<DraftLine> oldLines = draftLineRepository.findByDraftInvoiceIdOrderByPositionAsc(draft.getId());
        draftLineRepository.deleteAll(oldLines);

        for (int i = 0; i < corrected.lines().size(); i++) {
            DraftCorrection.Line input = corrected.lines().get(i);
            DraftLine old = i < oldLines.size() ? oldLines.get(i) : null;
            boolean unchanged = old != null && sameLine(old, input);

            DraftLine line = new DraftLine();
            line.setDraftInvoice(draft);
            line.setPosition(i);
            line.setDescription(input.description());
            line.setQuantity(input.quantity().toPlainString());
            line.setUnitPrice(input.unitPrice().toPlainString());
            line.setVatRate(input.vatRate() != null ? DraftCorrection.plain(input.vatRate()) : null);
            line.setTotal(input.total().toPlainString());
            line.setConfidence(unchanged ? old.getConfidence() : null);
            line.setSource(unchanged ? old.getSource() : SOURCE_MANUAL);
            draftLineRepository.save(line);
        }

        draftTaxRepository.deleteAll(draftTaxRepository.findByDraftInvoiceIdOrderByPositionAsc(draft.getId()));
        for (int i = 0; i < corrected.taxes().size(); i++) {
            DraftCorrection.Tax input = corrected.taxes().get(i);
            DraftTax tax = new DraftTax();
            tax.setDraftInvoice(draft);
            tax.setPosition(i);
            tax.setRate(input.rate());
            tax.setTaxableAmount(input.taxableAmount());
            tax.setTaxAmount(input.taxAmount());
            draftTaxRepository.save(tax);
        }

        draft.markUpdated();
        draftInvoiceRepository.save(draft);

        return toResponse(draft);
    }

    private void saveCalculatedField(DraftInvoice draft, Map<String, ExtractedField> fieldsByName,
                                     String name, BigDecimal amount) {
        ExtractedField field = fieldsByName.get(name);
        if (field == null) {
            field = new ExtractedField();
            field.setDraftInvoice(draft);
            field.setFieldName(name);
        }
        field.setValue(amount.toPlainString());
        field.setConfidence(null);
        field.setSource(SOURCE_CALCULATED);
        extractedFieldRepository.save(field);
    }

    // Le taux est compare numeriquement : "20,0" extrait et "20" renvoye par le
    // formulaire ne sont pas une correction.
    private boolean sameFieldValue(String name, String oldValue, String newValue) {
        String old = DraftCorrection.normalizeText(oldValue);
        if ("vatRate".equals(name) && old != null && newValue != null) {
            BigDecimal oldRate = DraftCorrection.parseRate(old);
            BigDecimal newRate = DraftCorrection.parseRate(newValue);
            return oldRate != null && newRate != null && oldRate.compareTo(newRate) == 0;
        }
        return Objects.equals(old, newValue);
    }

    private boolean sameLine(DraftLine old, DraftCorrection.Line input) {
        return Objects.equals(DraftCorrection.normalizeText(old.getDescription()), input.description())
                && sameNumber(DraftCorrection.parseDecimal(old.getQuantity()), input.quantity())
                && sameNumber(DraftCorrection.parseDecimal(old.getUnitPrice()), input.unitPrice())
                && sameNumber(DraftCorrection.parseRate(old.getVatRate()), input.vatRate());
    }

    private boolean sameNumber(BigDecimal a, BigDecimal b) {
        return a == null ? b == null : b != null && a.compareTo(b) == 0;
    }

    /**
     * F13 : dernier brouillon du document, revalide avec les regles F12 a partir de ce
     * qui est en base (et non de ce que le client pourrait envoyer). Leve
     * {@link DraftValidationException} si le brouillon n'est pas (ou plus) valide.
     */
    public ValidatedDraft validatedDraftForGeneration(Long documentId) {
        DraftInvoice draft = draftInvoiceRepository
                .findFirstByDocumentIdOrderByCreatedAtDesc(documentId)
                .orElseThrow(DraftNotFoundException::new);

        Map<String, String> fields = new HashMap<>();
        for (ExtractedField field : extractedFieldRepository.findByDraftInvoiceId(draft.getId())) {
            if (DraftCorrection.EDITABLE_FIELDS.contains(field.getFieldName())) {
                fields.put(field.getFieldName(), field.getValue());
            }
        }
        List<DraftInvoiceUpdateRequest.LineUpdateRequest> lines = draftLineRepository
                .findByDraftInvoiceIdOrderByPositionAsc(draft.getId())
                .stream()
                .map(line -> new DraftInvoiceUpdateRequest.LineUpdateRequest(
                        line.getDescription(), line.getQuantity(), line.getUnitPrice(), line.getVatRate()))
                .toList();

        DraftCorrection.Result result = DraftCorrection.validate(new DraftInvoiceUpdateRequest(fields, lines, null));

        // Un brouillon jamais enregistre depuis le formulaire F12 n'a pas ete verifie
        // par un humain (updated_at = created_at, ou null pour les brouillons anterieurs a F12).
        boolean humanChecked = draft.getUpdatedAt().isAfter(draft.getCreatedAt());
        return new ValidatedDraft(draft.getId(), draft.getOrganizationId(), humanChecked, result);
    }

    public DraftInvoiceResponse getLatestDraft(Long documentId) {
        DraftInvoice draft = draftInvoiceRepository
                .findFirstByDocumentIdOrderByCreatedAtDesc(documentId)
                .orElseThrow(DraftNotFoundException::new);

        return toResponse(draft);
    }

    private DraftInvoiceResponse toResponse(DraftInvoice draft) {
        return DraftInvoiceResponse.from(
                draft,
                extractedFieldRepository.findByDraftInvoiceId(draft.getId()),
                draftLineRepository.findByDraftInvoiceIdOrderByPositionAsc(draft.getId()),
                draftTaxRepository.findByDraftInvoiceIdOrderByPositionAsc(draft.getId()));
    }

    private Double clamp(Double confidence) {
        if (confidence == null) {
            return null;
        }
        return Math.max(0.0, Math.min(1.0, confidence));
    }
}
