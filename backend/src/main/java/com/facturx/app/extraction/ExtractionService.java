package com.facturx.app.extraction;

import com.facturx.app.document.Document;
import com.facturx.app.document.DocumentService;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ExtractionService {

    private final DocumentService documentService;
    private final ExtractionClient extractionClient;
    private final DraftInvoiceRepository draftInvoiceRepository;
    private final ExtractedFieldRepository extractedFieldRepository;
    private final DraftLineRepository draftLineRepository;

    public ExtractionService(
            DocumentService documentService,
            ExtractionClient extractionClient,
            DraftInvoiceRepository draftInvoiceRepository,
            ExtractedFieldRepository extractedFieldRepository,
            DraftLineRepository draftLineRepository) {

        this.documentService = documentService;
        this.extractionClient = extractionClient;
        this.draftInvoiceRepository = draftInvoiceRepository;
        this.extractedFieldRepository = extractedFieldRepository;
        this.draftLineRepository = draftLineRepository;
    }

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
            draftLineRepository.save(draftLine);
        }

        return toResponse(draft);
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
                draftLineRepository.findByDraftInvoiceIdOrderByPositionAsc(draft.getId()));
    }

    private Double clamp(Double confidence) {
        if (confidence == null) {
            return null;
        }
        return Math.max(0.0, Math.min(1.0, confidence));
    }
}
