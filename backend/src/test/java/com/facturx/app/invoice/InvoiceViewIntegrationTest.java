package com.facturx.app.invoice;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.facturx.app.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

@AutoConfigureMockMvc
@ActiveProfiles("test")
class InvoiceViewIntegrationTest extends AbstractIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	private static String uniqueEmail(String label) {
		return label + "-" + UUID.randomUUID() + "@x.fr";
	}

	private static String registerBody(String email, String password) {
		return """
				{"email":"%s","password":"%s","firstName":"Jean","lastName":"Dupont"}
				""".formatted(email, password);
	}

	private static Cookie sessionCookie(MvcResult result) {
		return result.getResponse().getCookie("EFACTURE_SESSION");
	}

	private Cookie registerAndLogin(String label) throws Exception {
		String email = uniqueEmail(label);

		MvcResult result = mockMvc.perform(
						post("/api/auth/register")
								.with(csrf())
								.contentType(MediaType.APPLICATION_JSON)
								.content(registerBody(
										email,
										"correcthorsebattery"
								))
				)
				.andExpect(status().isCreated())
				.andReturn();

		return sessionCookie(result);
	}

	private long createOrganization(Cookie session) throws Exception {
		MvcResult result = mockMvc.perform(
						post("/api/organizations?name=Cabinet Test")
								.with(csrf())
								.cookie(session)
				)
				.andExpect(status().isOk())
				.andReturn();

		return new ObjectMapper()
				.readTree(result.getResponse().getContentAsString())
				.get("id")
				.asLong();
	}

	private static byte[] validCiiXmlBytes() {
		String xml = """
				<?xml version="1.0" encoding="UTF-8"?>
				<rsm:CrossIndustryInvoice
					xmlns:rsm="urn:un:unece:uncefact:data:standard:CrossIndustryInvoice:100"
					xmlns:ram="urn:un:unece:uncefact:data:standard:ReusableAggregateBusinessInformationEntity:100"
					xmlns:udt="urn:un:unece:uncefact:data:standard:UnqualifiedDataType:100">

					<rsm:ExchangedDocument>
						<ram:ID>INV-001</ram:ID>
						<ram:IssueDateTime>
							<udt:DateTimeString format="102">20260914</udt:DateTimeString>
						</ram:IssueDateTime>
					</rsm:ExchangedDocument>

					<rsm:SupplyChainTradeTransaction>

						<ram:IncludedSupplyChainTradeLineItem>
							<ram:SpecifiedTradeProduct>
								<ram:Name>Consulting service</ram:Name>
							</ram:SpecifiedTradeProduct>

							<ram:SpecifiedLineTradeDelivery>
								<ram:BilledQuantity>2</ram:BilledQuantity>
							</ram:SpecifiedLineTradeDelivery>

							<ram:SpecifiedLineTradeAgreement>
								<ram:NetPriceProductTradePrice>
									<ram:ChargeAmount>100.00</ram:ChargeAmount>
								</ram:NetPriceProductTradePrice>
							</ram:SpecifiedLineTradeAgreement>

							<ram:SpecifiedLineTradeSettlement>
								<ram:SpecifiedTradeSettlementLineMonetarySummation>
									<ram:LineTotalAmount>200.00</ram:LineTotalAmount>
								</ram:SpecifiedTradeSettlementLineMonetarySummation>
							</ram:SpecifiedLineTradeSettlement>
						</ram:IncludedSupplyChainTradeLineItem>

						<ram:ApplicableHeaderTradeAgreement>
							<ram:SellerTradeParty>
								<ram:Name>Seller Company</ram:Name>
								<ram:PostalTradeAddress>
									<ram:LineOne>1 Seller Street</ram:LineOne>
									<ram:PostcodeCode>06000</ram:PostcodeCode>
									<ram:CityName>Nice</ram:CityName>
									<ram:CountryID>FR</ram:CountryID>
								</ram:PostalTradeAddress>
								<ram:SpecifiedTaxRegistration>
									<ram:ID>FR123456789</ram:ID>
								</ram:SpecifiedTaxRegistration>
							</ram:SellerTradeParty>

							<ram:BuyerTradeParty>
								<ram:Name>Buyer Company</ram:Name>
								<ram:PostalTradeAddress>
									<ram:LineOne>2 Buyer Street</ram:LineOne>
									<ram:PostcodeCode>75001</ram:PostcodeCode>
									<ram:CityName>Paris</ram:CityName>
									<ram:CountryID>FR</ram:CountryID>
								</ram:PostalTradeAddress>
								<ram:SpecifiedTaxRegistration>
									<ram:ID>FR987654321</ram:ID>
								</ram:SpecifiedTaxRegistration>
							</ram:BuyerTradeParty>
						</ram:ApplicableHeaderTradeAgreement>

						<ram:ApplicableHeaderTradeSettlement>
							<ram:InvoiceCurrencyCode>EUR</ram:InvoiceCurrencyCode>

							<ram:SpecifiedTradeSettlementHeaderMonetarySummation>
								<ram:LineTotalAmount>200.00</ram:LineTotalAmount>
								<ram:TaxTotalAmount>40.00</ram:TaxTotalAmount>
								<ram:GrandTotalAmount>240.00</ram:GrandTotalAmount>
							</ram:SpecifiedTradeSettlementHeaderMonetarySummation>
						</ram:ApplicableHeaderTradeSettlement>

					</rsm:SupplyChainTradeTransaction>
				</rsm:CrossIndustryInvoice>
				""";

		return xml.getBytes(StandardCharsets.UTF_8);
	}

	private long uploadFile(
			Cookie session,
			long orgId,
			String filename,
			String contentType,
			byte[] bytes
	) throws Exception {

		MockMultipartFile file = new MockMultipartFile(
				"file",
				filename,
				contentType,
				bytes
		);

		MvcResult result = mockMvc.perform(
						multipart("/api/documents?organizationId=" + orgId)
								.file(file)
								.with(csrf())
								.cookie(session)
				)
				.andExpect(status().isOk())
				.andReturn();

		return new ObjectMapper()
				.readTree(result.getResponse().getContentAsString())
				.get("id")
				.asLong();
	}

	private long uploadXml(
			Cookie session,
			long orgId,
			byte[] xmlBytes
	) throws Exception {

		return uploadFile(
				session,
				orgId,
				"facture.xml",
				"application/xml",
				xmlBytes
		);
	}

	@Test
	void uploadedCiiXmlCanBeDisplayedAsInvoiceView() throws Exception {
		Cookie session = registerAndLogin("invoice-view");
		long orgId = createOrganization(session);

		long documentId = uploadXml(
				session,
				orgId,
				validCiiXmlBytes()
		);

		mockMvc.perform(
						get("/api/documents/" + documentId + "/invoice-view")
								.cookie(session)
				)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.invoiceNumber").value("INV-001"))
				.andExpect(jsonPath("$.invoiceDate").value("2026-09-14"))
				.andExpect(jsonPath("$.currency").value("EUR"))
				.andExpect(jsonPath("$.seller.name").value("Seller Company"))
				.andExpect(jsonPath("$.buyer.name").value("Buyer Company"))
				.andExpect(jsonPath("$.lines[0].description")
						.value("Consulting service"))
				.andExpect(jsonPath("$.lines[0].quantity").value(2))
				.andExpect(jsonPath("$.lines[0].unitPrice").value(100.00))
				.andExpect(jsonPath("$.lines[0].lineTotal").value(200.00))
				.andExpect(jsonPath("$.subtotal").value(200.00))
				.andExpect(jsonPath("$.vat").value(40.00))
				.andExpect(jsonPath("$.total").value(240.00));
	}

	@Test
	void malformedXmlReturnsCleanError() throws Exception {
		Cookie session = registerAndLogin("invoice-invalid");
		long orgId = createOrganization(session);

		byte[] malformedXml = """
				<?xml version="1.0"?>
				<CrossIndustryInvoice>
					<Broken>
				</CrossIndustryInvoice>
				""".getBytes(StandardCharsets.UTF_8);

		long documentId = uploadXml(
				session,
				orgId,
				malformedXml
		);

		mockMvc.perform(
						get("/api/documents/" + documentId + "/invoice-view")
								.cookie(session)
				)
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.error")
						.value("INVALID_INVOICE_XML"));
	}

	@Test
	void unknownDocumentReturns404() throws Exception {
		Cookie session = registerAndLogin("invoice-missing");

		mockMvc.perform(
						get("/api/documents/999999/invoice-view")
								.cookie(session)
				)
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error")
						.value("DOCUMENT_NOT_FOUND"));
	}

	@Test
	void pdfDocumentCannotBeDisplayedAsInvoiceView() throws Exception {
		Cookie session = registerAndLogin("invoice-pdf");
		long orgId = createOrganization(session);

		byte[] pdfBytes =
				"%PDF-1.4 test".getBytes(StandardCharsets.UTF_8);

		long documentId = uploadFile(
				session,
				orgId,
				"facture.pdf",
				"application/pdf",
				pdfBytes
		);

		mockMvc.perform(
						get("/api/documents/" + documentId + "/invoice-view")
								.cookie(session)
				)
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.error")
						.value("INVALID_INVOICE_XML"));
	}

	@Test
	void userCannotViewDocumentFromAnotherOrganization()
			throws Exception {

		Cookie ownerSession = registerAndLogin("invoice-owner");
		long ownerOrgId = createOrganization(ownerSession);

		long documentId = uploadXml(
				ownerSession,
				ownerOrgId,
				validCiiXmlBytes()
		);

		Cookie otherUserSession =
				registerAndLogin("invoice-other-user");

		mockMvc.perform(
						get("/api/documents/" + documentId + "/invoice-view")
								.cookie(otherUserSession)
				)
				.andExpect(status().isForbidden());
	}
}
