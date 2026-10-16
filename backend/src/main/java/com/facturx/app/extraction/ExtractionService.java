package com.facturx.app.extraction;

import com.facturx.app.document.Document;
import com.facturx.app.document.DocumentService;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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
            draftLineRepository.save(draftLine);
        }

        return toResponse(draft);
    }
	








	@Transactional
	public DraftInvoiceResponse updateDraft(
			Long documentId,
			DraftInvoiceUpdateRequest request) {

		if (request == null || request.fields() == null || request.lines() == null) {
			throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST,
					"Invoice fields and lines are required.");
		}

		DraftInvoice draft = draftInvoiceRepository
				.findFirstByDocumentIdOrderByCreatedAtDesc(documentId)
				.orElseThrow(DraftNotFoundException::new);

		Map<String, String> values = new HashMap<>(request.fields());

		// Required fields for the correction form.
		requireText(values, "invoiceNumber");
		requireText(values, "invoiceDate");
		requireText(values, "sellerName");
		requireText(values, "buyerName");

		List<ExtractedField> existingFields =
				extractedFieldRepository.findByDraftInvoiceId(draft.getId());

		Map<String, ExtractedField> fieldsByName = new HashMap<>();
		for (ExtractedField field : existingFields) {
			fieldsByName.put(field.getFieldName(), field);
		}

		// Calculate each line and the subtotal on the server.
		BigDecimal totalHt = BigDecimal.ZERO;
		List<BigDecimal> calculatedLineTotals = new ArrayList<>();

		for (DraftInvoiceUpdateRequest.LineUpdateRequest line : request.lines()) {
			if (line == null || line.description() == null
					|| line.description().isBlank()) {
				throw new ResponseStatusException(
						HttpStatus.BAD_REQUEST,
						"Every invoice line must have a description.");
			}

			BigDecimal quantity = parseDecimal(line.quantity(), "quantity");
			BigDecimal unitPrice = parseDecimal(line.unitPrice(), "unitPrice");

			if (quantity.signum() <= 0 || unitPrice.signum() < 0) {
				throw new ResponseStatusException(
						HttpStatus.BAD_REQUEST,
						"Quantity must be positive and unit price cannot be negative.");
			}

			BigDecimal lineTotal = quantity
					.multiply(unitPrice)
					.setScale(2, RoundingMode.HALF_UP);

			calculatedLineTotals.add(lineTotal);
			totalHt = totalHt.add(lineTotal);
		}

		totalHt = totalHt.setScale(2, RoundingMode.HALF_UP);

		// VAT is calculated from the submitted rate; default to 0 when absent.
		BigDecimal vatRate = values.get("vatRate") == null
				|| values.get("vatRate").isBlank()
				? BigDecimal.ZERO
				: parseDecimal(values.get("vatRate"), "vatRate");

		if (vatRate.signum() < 0 || vatRate.compareTo(new BigDecimal("100")) > 0) {
			throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST,
					"VAT rate must be between 0 and 100.");
		}

		BigDecimal totalVat = totalHt
				.multiply(vatRate)
				.divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

		BigDecimal totalTtc = totalHt.add(totalVat)
				.setScale(2, RoundingMode.HALF_UP);

		// The server-calculated amounts override the values sent by the frontend.
		values.put("totalHt", totalHt.toPlainString());
		values.put("totalVat", totalVat.toPlainString());
		values.put("totalTtc", totalTtc.toPlainString());
		values.put("vatRate", vatRate.stripTrailingZeros().toPlainString());

		// Update existing extracted fields without losing their confidence scores.
		// Fields added manually are marked with source "manual".
		for (Map.Entry<String, String> entry : values.entrySet()) {
			String name = entry.getKey();

			if (name == null || name.isBlank() || name.length() > 50) {
				throw new ResponseStatusException(
						HttpStatus.BAD_REQUEST, "Invalid invoice field name.");
			}

			String value = entry.getValue();
			if (value != null && value.length() > 500) {
				throw new ResponseStatusException(
						HttpStatus.BAD_REQUEST,
						"Field value is too long: " + name);
			}

			ExtractedField field = fieldsByName.get(name);

			if (field == null) {
				field = new ExtractedField();
				field.setDraftInvoice(draft);
				field.setFieldName(name);
				field.setSource("manual");
				field.setConfidence(null);
			}

			field.setValue(value);
			extractedFieldRepository.save(field);
		}

		// Replace the draft's lines with the corrected lines.
		List<DraftLine> oldLines =
				draftLineRepository.findByDraftInvoiceIdOrderByPositionAsc(draft.getId());

		draftLineRepository.deleteAll(oldLines);

		for (int i = 0; i < request.lines().size(); i++) {
			DraftInvoiceUpdateRequest.LineUpdateRequest input = request.lines().get(i);

			DraftLine line = new DraftLine();
			line.setDraftInvoice(draft);
			line.setPosition(i);
			line.setDescription(input.description().trim());
			line.setQuantity(parseDecimal(input.quantity(), "quantity").toPlainString());
			line.setUnitPrice(parseDecimal(input.unitPrice(), "unitPrice").toPlainString());
			line.setTotal(calculatedLineTotals.get(i).toPlainString());

			// A corrected line is not automatically considered high-confidence.
			line.setConfidence(null);

			draftLineRepository.save(line);
		}
		draft.touchUpdatedAt();
		draftInvoiceRepository.save(draft);

		return toResponse(draft);
	}

	private void requireText(Map<String, String> fields, String name) {
		if (fields.get(name) == null || fields.get(name).isBlank()) {
			throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST,
					"Required field is missing: " + name);
		}
	}

	private BigDecimal parseDecimal(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST,
					"Missing numeric value: " + fieldName);
		}

		// Accept decimal commas and spaces used as thousands separators.
		String normalized = value.trim().replaceAll("\\s+", "");

		if (normalized.contains(",") && normalized.contains(".")) {
			if (normalized.lastIndexOf(',') > normalized.lastIndexOf('.')) {
				normalized = normalized.replace(".", "").replace(",", ".");
			} else {
				normalized = normalized.replace(",", "");
			}
		} else if (normalized.contains(",")) {
			normalized = normalized.replace(",", ".");
		}

		try {
			return new BigDecimal(normalized);
		} catch (NumberFormatException exception) {
			throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST,
					"Invalid numeric value for " + fieldName + ": " + value);
		}
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
