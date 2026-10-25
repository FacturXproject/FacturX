package com.facturx.app.exportimport;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

import com.facturx.app.document.DocumentStatus;

/**
 * F20 : export PDF de la liste des documents, un tableau lisible à imprimer ou
 * archiver. Les données sont celles des exports CSV / JSON (DocumentExportResponse).
 */
@Component
public class DocumentPdfExporter {

    private static final PDRectangle PAGE_SIZE =
            new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());
    private static final float MARGIN = 36;
    private static final float ROW_HEIGHT = 16;
    private static final float FONT_SIZE = 8.5f;
    private static final float CELL_PADDING = 4;

    private static final String[] HEADERS = {
        "Nom du fichier", "Type", "Taille (MB)", "Statut", "Déposé par",
        "Date de dépôt", "Conforme", "Erreurs", "Codes des règles"
    };
    // Somme = largeur utile d'une page A4 paysage (842 - 2 x 36).
    private static final float[] WIDTHS = {200, 40, 55, 70, 120, 85, 60, 45, 95};

    private static final Map<DocumentStatus, String> STATUS_LABELS = Map.of(
        DocumentStatus.UPLOADED, "Déposé",
        DocumentStatus.QUEUED, "En attente",
        DocumentStatus.PROCESSING, "En cours",
        DocumentStatus.VALID, "Valide",
        DocumentStatus.INVALID, "Invalide",
        DocumentStatus.FAILED, "Échec"
    );

    private static final DateTimeFormatter EXPORT_DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("Europe/Paris"));

    private final PDFont regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private final PDFont bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    public byte[] render(String organizationName, List<DocumentExportResponse> documents) {
        try (PDDocument pdf = new PDDocument()) {
            float top = PAGE_SIZE.getHeight() - MARGIN;
            float bottom = MARGIN + 20; // place pour le numéro de page

            PDPage page = newPage(pdf);
            PDPageContentStream content = new PDPageContentStream(pdf, page);

            text(content, bold, 14, MARGIN, top - 14, "Documents - " + organizationName);
            text(content, regular, 9, MARGIN, top - 30,
                    "Exporté le " + EXPORT_DATE.format(Instant.now()) + " - "
                            + documents.size() + " document(s)");

            float y = top - 48;
            y = headerRow(content, y);

            if (documents.isEmpty()) {
                text(content, regular, FONT_SIZE, MARGIN + CELL_PADDING, y - 11,
                        "Aucun document dans cette organisation.");
            }

            for (DocumentExportResponse doc : documents) {
                if (y - ROW_HEIGHT < bottom) {
                    content.close();
                    page = newPage(pdf);
                    content = new PDPageContentStream(pdf, page);
                    y = headerRow(content, top);
                }
                row(content, regular, y, new String[] {
                    doc.filename(),
                    doc.type(),
                    String.valueOf(doc.sizeMb()),
                    doc.status() != null ? STATUS_LABELS.get(doc.status()) : "",
                    doc.uploadedBy(),
                    doc.uploadedAt(),
                    doc.valid(),
                    doc.errorCount() != null ? doc.errorCount().toString() : "",
                    String.join(", ", doc.ruleCodes())
                });
                y -= ROW_HEIGHT;
            }
            content.close();

            int pageCount = pdf.getNumberOfPages();
            for (int i = 0; i < pageCount; i++) {
                try (PDPageContentStream footer = new PDPageContentStream(
                        pdf, pdf.getPage(i), PDPageContentStream.AppendMode.APPEND, true)) {
                    text(footer, regular, 8, PAGE_SIZE.getWidth() - MARGIN - 50, MARGIN,
                            "Page " + (i + 1) + " / " + pageCount);
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            pdf.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("PDF export failed", e);
        }
    }

    private static PDPage newPage(PDDocument pdf) {
        PDPage page = new PDPage(PAGE_SIZE);
        pdf.addPage(page);
        return page;
    }

    private float headerRow(PDPageContentStream content, float y) throws IOException {
        content.setNonStrokingColor(0.95f, 0.96f, 0.97f);
        content.addRect(MARGIN, y - ROW_HEIGHT, PAGE_SIZE.getWidth() - 2 * MARGIN, ROW_HEIGHT);
        content.fill();
        content.setNonStrokingColor(0f, 0f, 0f);
        row(content, bold, y, HEADERS);
        return y - ROW_HEIGHT;
    }

    private void row(PDPageContentStream content, PDFont font, float y, String[] cells) throws IOException {
        float x = MARGIN;
        for (int i = 0; i < cells.length; i++) {
            String cell = fit(font, cells[i], WIDTHS[i] - 2 * CELL_PADDING);
            text(content, font, FONT_SIZE, x + CELL_PADDING, y - 11, cell);
            x += WIDTHS[i];
        }
        content.setStrokingColor(0.85f, 0.86f, 0.88f);
        content.setLineWidth(0.5f);
        content.moveTo(MARGIN, y - ROW_HEIGHT);
        content.lineTo(PAGE_SIZE.getWidth() - MARGIN, y - ROW_HEIGHT);
        content.stroke();
    }

    private void text(PDPageContentStream content, PDFont font, float size, float x, float y,
                      String value) throws IOException {
        content.beginText();
        content.setFont(font, size);
        content.newLineAtOffset(x, y);
        content.showText(encodable(font, value));
        content.endText();
    }

    // Coupe le texte avec "..." pour qu'il tienne dans sa colonne.
    private String fit(PDFont font, String value, float maxWidth) throws IOException {
        String text = encodable(font, value);
        if (width(font, text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        while (!text.isEmpty() && width(font, text + ellipsis) > maxWidth) {
            text = text.substring(0, text.length() - 1);
        }
        return text + ellipsis;
    }

    private static float width(PDFont font, String text) throws IOException {
        return font.getStringWidth(text) / 1000 * FONT_SIZE;
    }

    // Les polices standard du PDF ne couvrent que WinAnsi : un caractère hors de
    // cet alphabet (cyrillique, emoji...) dans un nom de fichier devient "?".
    private static String encodable(PDFont font, String value) {
        if (value == null) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        value.codePoints().forEach(codePoint -> {
            String character = Character.isISOControl(codePoint)
                    ? " "
                    : new String(Character.toChars(codePoint));
            try {
                font.encode(character);
                result.append(character);
            } catch (IOException | IllegalArgumentException e) {
                result.append('?');
            }
        });
        return result.toString();
    }
}
