package com.facturx.app.validation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Turns Mustangproject's raw validation report XML into our own {@link ValidationResult}.
 *
 * <p>Confirmed against two real samples (see F08 day-1/day-2 notes): a valid
 * EN16931 invoice and a PDF/A-3-non-conformant one. Report shape:
 * <pre>{@code
 * <validation>
 *   <pdf>...<messages><notice|warning|error type="N">text</...></messages><summary status="valid|invalid"/></pdf>
 *   <xml>...<messages>...same shape...</messages><summary status="valid|invalid"/></xml>   <!-- absent if PDF/A-3 failed hard -->
 *   <messages>...cross-cutting, e.g. <exception type="17">XML could not be extracted</exception>...</messages>
 *   <summary status="valid|invalid"/>
 * </validation>
 * }</pre>
 * The element's own tag name carries the severity (notice/warning/error/exception/fatal);
 * the {@code type} attribute is Mustang's internal numeric code; a Schematron rule id, when
 * there is one, is embedded in the message text as {@code [ID RULE-CODE]}.
 *
 * <p>Mustang's report does not separate XSD from Schematron failures at the tag level, so
 * everything under {@code <xml>} is currently attributed to {@link ValidationLayer#SCHEMATRON}.
 * Splitting that further (and distinguishing a true PROFILE layer) needs another sample that
 * actually fails XSD/profile checks specifically - left as a known simplification.
 */
final class MustangReportParser {

    private static final Pattern RULE_ID_PATTERN = Pattern.compile("\\[ID ([^]]+)]");

    private MustangReportParser() {
    }

    static ValidationResult parse(String rawXmlReport, boolean completelyValid) {
        Element root = parseDocument(rawXmlReport);

        Element pdfBlock = firstChildElement(root, "pdf");
        Element xmlBlock = firstChildElement(root, "xml");

        List<ValidationError> errors = new ArrayList<>();
        if (pdfBlock != null) {
            errors.addAll(readMessages(firstChildElement(pdfBlock, "messages"), ValidationLayer.PDF_A3));
        }
        if (xmlBlock != null) {
            errors.addAll(readMessages(firstChildElement(xmlBlock, "messages"), ValidationLayer.SCHEMATRON));
        }

        ValidationLayer layerReached = determineLayerReached(pdfBlock, xmlBlock);

        // Cross-cutting messages sitting directly under <validation> (e.g. "XML could not be
        // extracted" once PDF/A-3 has already failed hard) belong to whatever layer processing
        // actually reached.
        errors.addAll(readMessages(firstChildElement(root, "messages"), layerReached));

        return new ValidationResult(completelyValid, layerReached, errors);
    }

    private static ValidationLayer determineLayerReached(Element pdfBlock, Element xmlBlock) {
        if (pdfBlock == null) {
            return ValidationLayer.PDF_A3;
        }
        if ("invalid".equals(summaryStatus(pdfBlock))) {
            return ValidationLayer.PDF_A3;
        }
        if (xmlBlock == null) {
            return ValidationLayer.PDF_A3;
        }
        return ValidationLayer.SCHEMATRON;
    }

    private static String summaryStatus(Element block) {
        Element summary = firstChildElement(block, "summary");
        return summary == null ? null : summary.getAttribute("status");
    }

    private static List<ValidationError> readMessages(Element messagesBlock, ValidationLayer layer) {
        List<ValidationError> result = new ArrayList<>();
        if (messagesBlock == null) {
            return result;
        }
        NodeList children = messagesBlock.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element messageEl = (Element) node;
            String text = messageEl.getTextContent().trim();
            String mustangType = messageEl.getAttribute("type");
            String location = messageEl.getAttribute("location");

            ValidationSeverity severity = mapSeverity(messageEl.getTagName());
            String ruleCode = extractRuleCode(text)
                    .orElse("MUSTANG-" + messageEl.getTagName().toUpperCase() + "-" + mustangType);

            result.add(new ValidationError(
                    layer,
                    severity,
                    ruleCode,
                    text,
                    location.isBlank() ? null : location,
                    null,
                    null
            ));
        }
        return result;
    }

    private static ValidationSeverity mapSeverity(String tagName) {
        return switch (tagName) {
            case "notice" -> ValidationSeverity.INFO;
            case "warning" -> ValidationSeverity.WARNING;
            default -> ValidationSeverity.ERROR; // error, exception, fatal
        };
    }

    private static Optional<String> extractRuleCode(String text) {
        Matcher matcher = RULE_ID_PATTERN.matcher(text);
        return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
    }

    private static Element firstChildElement(Element parent, String tagName) {
        if (parent == null) {
            return null;
        }
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && tagName.equals(node.getNodeName())) {
                return (Element) node;
            }
        }
        return null;
    }

    private static Element parseDocument(String rawXmlReport) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(new ByteArrayInputStream(rawXmlReport.getBytes(StandardCharsets.UTF_8)))
                    .getDocumentElement();
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new IllegalStateException("Could not parse Mustang validation report", e);
        }
    }
}
