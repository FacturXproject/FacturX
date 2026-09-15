package com.facturx.app.document;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.springframework.stereotype.Component;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Component
public class CiiInvoiceParser {

	public InvoiceViewResponse parse(byte[] xmlBytes) {
		try {
			org.w3c.dom.Document xml = parseXml(xmlBytes);

			XPath xpath = XPathFactory.newInstance().newXPath();

			String invoiceNumber = text(xpath, xml,
					"//*[local-name()='ExchangedDocument']/*[local-name()='ID']");

			String rawInvoiceDate = text(xpath, xml,
					"//*[local-name()='ExchangedDocument']"
					+ "/*[local-name()='IssueDateTime']"
					+ "/*[local-name()='DateTimeString']");

			String invoiceDate = normalizeDate(rawInvoiceDate);

			String currency = text(xpath, xml,
					"//*[local-name()='ApplicableHeaderTradeSettlement']"
					+ "/*[local-name()='InvoiceCurrencyCode']");

			InvoiceViewResponse.Party seller = parseParty(
					xpath,
					xml,
					"SellerTradeParty"
			);

			InvoiceViewResponse.Party buyer = parseParty(
					xpath,
					xml,
					"BuyerTradeParty"
			);

			List<InvoiceViewResponse.InvoiceLine> lines =
					parseLines(xpath, xml);

			BigDecimal subtotal = decimal(xpath, xml,
					"//*[local-name()='SpecifiedTradeSettlementHeaderMonetarySummation']"
					+ "/*[local-name()='LineTotalAmount']");

			BigDecimal vat = decimal(xpath, xml,
					"//*[local-name()='SpecifiedTradeSettlementHeaderMonetarySummation']"
					+ "/*[local-name()='TaxTotalAmount']");

			BigDecimal total = decimal(xpath, xml,
					"//*[local-name()='SpecifiedTradeSettlementHeaderMonetarySummation']"
					+ "/*[local-name()='GrandTotalAmount']");

			return new InvoiceViewResponse(
					invoiceNumber,
					invoiceDate,
					currency,
					seller,
					buyer,
					lines,
					subtotal,
					vat,
					total
			);

		}
		catch (Exception e) {
			throw new InvalidInvoiceXmlException(e);
		}
	}

	private org.w3c.dom.Document parseXml(byte[] xmlBytes) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

		factory.setNamespaceAware(true);

		factory.setFeature(
				"http://apache.org/xml/features/disallow-doctype-decl",
				true
		);

		factory.setFeature(
				"http://xml.org/sax/features/external-general-entities",
				false
		);

		factory.setFeature(
				"http://xml.org/sax/features/external-parameter-entities",
				false
		);

		factory.setAttribute(
				XMLConstants.ACCESS_EXTERNAL_DTD,
				""
		);

		factory.setAttribute(
				XMLConstants.ACCESS_EXTERNAL_SCHEMA,
				""
		);

		return factory
				.newDocumentBuilder()
				.parse(new ByteArrayInputStream(xmlBytes));
	}

	private InvoiceViewResponse.Party parseParty(
			XPath xpath,
			org.w3c.dom.Document xml,
			String partyName
	) throws Exception {

		String base =
				"//*[local-name()='" + partyName + "']";

		String name = text(
				xpath,
				xml,
				base + "/*[local-name()='Name']"
		);

		String vatId = text(
				xpath,
				xml,
				base
				+ "/*[local-name()='SpecifiedTaxRegistration']"
				+ "/*[local-name()='ID']"
		);

		String address = buildAddress(
				text(xpath, xml,
						base
						+ "/*[local-name()='PostalTradeAddress']"
						+ "/*[local-name()='LineOne']"),

				text(xpath, xml,
						base
						+ "/*[local-name()='PostalTradeAddress']"
						+ "/*[local-name()='PostcodeCode']"),

				text(xpath, xml,
						base
						+ "/*[local-name()='PostalTradeAddress']"
						+ "/*[local-name()='CityName']"),

				text(xpath, xml,
						base
						+ "/*[local-name()='PostalTradeAddress']"
						+ "/*[local-name()='CountryID']")
		);

		return new InvoiceViewResponse.Party(
				name,
				address,
				vatId
		);
	}

	private List<InvoiceViewResponse.InvoiceLine> parseLines(
			XPath xpath,
			org.w3c.dom.Document xml
	) throws Exception {

		NodeList nodes = (NodeList) xpath.evaluate(
				"//*[local-name()='IncludedSupplyChainTradeLineItem']",
				xml,
				XPathConstants.NODESET
		);

		List<InvoiceViewResponse.InvoiceLine> lines =
				new ArrayList<>();

		for (int i = 0; i < nodes.getLength(); i++) {
			Node line = nodes.item(i);

			String description = text(
					xpath,
					line,
					".//*[local-name()='SpecifiedTradeProduct']"
					+ "/*[local-name()='Name']"
			);

			BigDecimal quantity = decimal(
					xpath,
					line,
					".//*[local-name()='SpecifiedLineTradeDelivery']"
					+ "/*[local-name()='BilledQuantity']"
			);

			BigDecimal unitPrice = decimal(
					xpath,
					line,
					".//*[local-name()='NetPriceProductTradePrice']"
					+ "/*[local-name()='ChargeAmount']"
			);

			BigDecimal lineTotal = decimal(
					xpath,
					line,
					".//*[local-name()='SpecifiedTradeSettlementLineMonetarySummation']"
					+ "/*[local-name()='LineTotalAmount']"
			);

			lines.add(
					new InvoiceViewResponse.InvoiceLine(
							description,
							quantity,
							unitPrice,
							lineTotal
					)
			);
		}

		return lines;
	}

	private String text(
			XPath xpath,
			Object node,
			String expression
	) throws Exception {

		String value = xpath.evaluate(expression, node);

		if (value == null || value.isBlank()) {
			return null;
		}

		return value.trim();
	}

	private BigDecimal decimal(
			XPath xpath,
			Object node,
			String expression
	) throws Exception {

		String value = text(xpath, node, expression);

		if (value == null) {
			return null;
		}

		return new BigDecimal(value);
	}

	private String buildAddress(
			String line,
			String postcode,
			String city,
			String country
	) {
		List<String> parts = new ArrayList<>();

		if (line != null) {
			parts.add(line);
		}

		String cityPart = "";

		if (postcode != null) {
			cityPart += postcode;
		}

		if (city != null) {
			if (!cityPart.isEmpty()) {
				cityPart += " ";
			}

			cityPart += city;
		}

		if (!cityPart.isEmpty()) {
			parts.add(cityPart);
		}

		if (country != null) {
			parts.add(country);
		}

		return parts.isEmpty()
				? null
				: String.join(", ", parts);
	}

	private String normalizeDate(String rawDate) {
		if (rawDate == null || rawDate.isBlank()) {
			return null;
		}

		try {
			LocalDate date = LocalDate.parse(
					rawDate,
					DateTimeFormatter.ofPattern("yyyyMMdd")
			);

			return date.format(DateTimeFormatter.ISO_LOCAL_DATE);

		} catch (DateTimeParseException e) {
			return rawDate;
		}
	}
}
