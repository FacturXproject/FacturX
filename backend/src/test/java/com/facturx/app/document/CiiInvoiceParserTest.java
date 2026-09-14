package com.facturx.app.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class CiiInvoiceParserTest {

	private final CiiInvoiceParser parser = new CiiInvoiceParser();

	@Test
	void parsesSimpleCiiInvoice() {
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
								<ram:BilledQuantity unitCode="HUR">2</ram:BilledQuantity>
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
									<ram:PostcodeCode>06000</ram:PostcodeCode>
									<ram:LineOne>1 Seller Street</ram:LineOne>
									<ram:CityName>Nice</ram:CityName>
									<ram:CountryID>FR</ram:CountryID>
								</ram:PostalTradeAddress>
								<ram:SpecifiedTaxRegistration>
									<ram:ID schemeID="VA">FR123456789</ram:ID>
								</ram:SpecifiedTaxRegistration>
							</ram:SellerTradeParty>

							<ram:BuyerTradeParty>
								<ram:Name>Buyer Company</ram:Name>
								<ram:PostalTradeAddress>
									<ram:PostcodeCode>75001</ram:PostcodeCode>
									<ram:LineOne>2 Buyer Street</ram:LineOne>
									<ram:CityName>Paris</ram:CityName>
									<ram:CountryID>FR</ram:CountryID>
								</ram:PostalTradeAddress>
								<ram:SpecifiedTaxRegistration>
									<ram:ID schemeID="VA">FR987654321</ram:ID>
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

		InvoiceViewResponse result =
				parser.parse(xml.getBytes(StandardCharsets.UTF_8));

		assertThat(result.invoiceNumber()).isEqualTo("INV-001");
		assertThat(result.invoiceDate()).isEqualTo("2026-09-14");
		assertThat(result.currency()).isEqualTo("EUR");

		assertThat(result.seller().name()).isEqualTo("Seller Company");
		assertThat(result.seller().vatId()).isEqualTo("FR123456789");

		assertThat(result.buyer().name()).isEqualTo("Buyer Company");
		assertThat(result.buyer().vatId()).isEqualTo("FR987654321");

		assertThat(result.lines()).hasSize(1);
		assertThat(result.lines().get(0).description())
				.isEqualTo("Consulting service");

		assertThat(result.lines().get(0).quantity())
				.isEqualByComparingTo("2");

		assertThat(result.lines().get(0).unitPrice())
				.isEqualByComparingTo("100.00");

		assertThat(result.lines().get(0).lineTotal())
				.isEqualByComparingTo("200.00");

		assertThat(result.subtotal())
				.isEqualByComparingTo("200.00");

		assertThat(result.vat())
				.isEqualByComparingTo("40.00");

		assertThat(result.total())
				.isEqualByComparingTo("240.00");
	}

	@Test
	void malformedXmlThrowsCleanException() {
		String xml = """
				<CrossIndustryInvoice>
					<Broken>
				</CrossIndustryInvoice>
				""";

		assertThatThrownBy(() ->
				parser.parse(xml.getBytes(StandardCharsets.UTF_8))
		).isInstanceOf(InvalidInvoiceXmlException.class);
	}

	@Test
		void parsesMultipleInvoiceLinesAndAmounts() {
			String xml = """
					<?xml version="1.0" encoding="UTF-8"?>
					<rsm:CrossIndustryInvoice
						xmlns:rsm="urn:un:unece:uncefact:data:standard:CrossIndustryInvoice:100"
						xmlns:ram="urn:un:unece:uncefact:data:standard:ReusableAggregateBusinessInformationEntity:100"
						xmlns:udt="urn:un:unece:uncefact:data:standard:UnqualifiedDataType:100">

						<rsm:ExchangedDocument>
							<ram:ID>INV-002</ram:ID>
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
										<ram:ChargeAmount>100.50</ram:ChargeAmount>
									</ram:NetPriceProductTradePrice>
								</ram:SpecifiedLineTradeAgreement>

								<ram:SpecifiedLineTradeSettlement>
									<ram:SpecifiedTradeSettlementLineMonetarySummation>
										<ram:LineTotalAmount>201.00</ram:LineTotalAmount>
									</ram:SpecifiedTradeSettlementLineMonetarySummation>
								</ram:SpecifiedLineTradeSettlement>
							</ram:IncludedSupplyChainTradeLineItem>

							<ram:IncludedSupplyChainTradeLineItem>
								<ram:SpecifiedTradeProduct>
									<ram:Name>Hosting service</ram:Name>
								</ram:SpecifiedTradeProduct>

								<ram:SpecifiedLineTradeDelivery>
									<ram:BilledQuantity>1</ram:BilledQuantity>
								</ram:SpecifiedLineTradeDelivery>

								<ram:SpecifiedLineTradeAgreement>
									<ram:NetPriceProductTradePrice>
										<ram:ChargeAmount>49.99</ram:ChargeAmount>
									</ram:NetPriceProductTradePrice>
								</ram:SpecifiedLineTradeAgreement>

								<ram:SpecifiedLineTradeSettlement>
									<ram:SpecifiedTradeSettlementLineMonetarySummation>
										<ram:LineTotalAmount>49.99</ram:LineTotalAmount>
									</ram:SpecifiedTradeSettlementLineMonetarySummation>
								</ram:SpecifiedLineTradeSettlement>
							</ram:IncludedSupplyChainTradeLineItem>

							<ram:ApplicableHeaderTradeAgreement>
								<ram:SellerTradeParty>
									<ram:Name>Seller Company</ram:Name>
								</ram:SellerTradeParty>

								<ram:BuyerTradeParty>
									<ram:Name>Buyer Company</ram:Name>
								</ram:BuyerTradeParty>
							</ram:ApplicableHeaderTradeAgreement>

							<ram:ApplicableHeaderTradeSettlement>
								<ram:InvoiceCurrencyCode>EUR</ram:InvoiceCurrencyCode>

								<ram:SpecifiedTradeSettlementHeaderMonetarySummation>
									<ram:LineTotalAmount>250.99</ram:LineTotalAmount>
									<ram:TaxTotalAmount>50.20</ram:TaxTotalAmount>
									<ram:GrandTotalAmount>301.19</ram:GrandTotalAmount>
								</ram:SpecifiedTradeSettlementHeaderMonetarySummation>
							</ram:ApplicableHeaderTradeSettlement>

						</rsm:SupplyChainTradeTransaction>
					</rsm:CrossIndustryInvoice>
					""";

			InvoiceViewResponse result =
					parser.parse(xml.getBytes(StandardCharsets.UTF_8));

			assertThat(result.invoiceNumber()).isEqualTo("INV-002");
			assertThat(result.invoiceDate()).isEqualTo("2026-09-14");

			assertThat(result.lines()).hasSize(2);

			assertThat(result.lines().get(0).description())
					.isEqualTo("Consulting service");
			assertThat(result.lines().get(0).quantity())
					.isEqualByComparingTo("2");
			assertThat(result.lines().get(0).unitPrice())
					.isEqualByComparingTo("100.50");
			assertThat(result.lines().get(0).lineTotal())
					.isEqualByComparingTo("201.00");

			assertThat(result.lines().get(1).description())
					.isEqualTo("Hosting service");
			assertThat(result.lines().get(1).quantity())
					.isEqualByComparingTo("1");
			assertThat(result.lines().get(1).unitPrice())
					.isEqualByComparingTo("49.99");
			assertThat(result.lines().get(1).lineTotal())
					.isEqualByComparingTo("49.99");

			assertThat(result.subtotal())
					.isEqualByComparingTo("250.99");

			assertThat(result.vat())
					.isEqualByComparingTo("50.20");

			assertThat(result.total())
					.isEqualByComparingTo("301.19");
		}

		@Test
		void missingFieldsAreReturnedAsNullWithoutBreakingParsing() {
		String xml = """
				<?xml version="1.0" encoding="UTF-8"?>
				<rsm:CrossIndustryInvoice
					xmlns:rsm="urn:un:unece:uncefact:data:standard:CrossIndustryInvoice:100"
					xmlns:ram="urn:un:unece:uncefact:data:standard:ReusableAggregateBusinessInformationEntity:100"
					xmlns:udt="urn:un:unece:uncefact:data:standard:UnqualifiedDataType:100">

					<rsm:ExchangedDocument>
						<ram:ID>INV-003</ram:ID>
					</rsm:ExchangedDocument>

					<rsm:SupplyChainTradeTransaction>

						<ram:ApplicableHeaderTradeAgreement>
							<ram:SellerTradeParty>
								<ram:Name>Seller Company</ram:Name>
							</ram:SellerTradeParty>

							<ram:BuyerTradeParty>
							</ram:BuyerTradeParty>
						</ram:ApplicableHeaderTradeAgreement>

						<ram:ApplicableHeaderTradeSettlement>
							<ram:InvoiceCurrencyCode>EUR</ram:InvoiceCurrencyCode>

							<ram:SpecifiedTradeSettlementHeaderMonetarySummation>
								<ram:GrandTotalAmount>120.00</ram:GrandTotalAmount>
							</ram:SpecifiedTradeSettlementHeaderMonetarySummation>
						</ram:ApplicableHeaderTradeSettlement>

					</rsm:SupplyChainTradeTransaction>
				</rsm:CrossIndustryInvoice>
				""";

		InvoiceViewResponse result =
				parser.parse(xml.getBytes(StandardCharsets.UTF_8));

		assertThat(result.invoiceNumber()).isEqualTo("INV-003");

		// Missing date
		assertThat(result.invoiceDate()).isNull();

		// Seller exists
		assertThat(result.seller().name()).isEqualTo("Seller Company");

		// Missing seller information
		assertThat(result.seller().address()).isNull();
		assertThat(result.seller().vatId()).isNull();

		// Buyer exists structurally but has no data
		assertThat(result.buyer().name()).isNull();
		assertThat(result.buyer().address()).isNull();
		assertThat(result.buyer().vatId()).isNull();

		// No invoice lines
		assertThat(result.lines()).isEmpty();

		// Missing amounts
		assertThat(result.subtotal()).isNull();
		assertThat(result.vat()).isNull();

		// Existing amount still parsed correctly
		assertThat(result.total()).isEqualByComparingTo("120.00");
	}
}
