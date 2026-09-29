export const recentDocuments = [
  {
    id: 1,
    filename: 'FACT-2026-00142.pdf',
    date: '2026-07-28',
    action: 'Vérification',
    status: 'erreurs',
    statusLabel: '3 erreurs',
    reportId: 'with-errors',
  },
  {
    id: 2,
    filename: 'FACT-2026-00139.xml',
    date: '2026-07-25',
    action: 'Conversion',
    status: 'converti',
    statusLabel: 'Converti',
    reportId: null,
  },
  {
    id: 3,
    filename: 'FACT-2026-00135.pdf',
    date: '2026-07-22',
    action: 'Vérification',
    status: 'conforme',
    statusLabel: 'Conforme',
    reportId: 'no-errors',
  },
  {
    id: 4,
    filename: 'FACT-2026-00128.pdf',
    date: '2026-07-18',
    action: 'Vérification',
    status: 'conforme',
    statusLabel: 'Conforme',
    reportId: 'no-errors',
  },
  {
    id: 5,
    filename: 'FACT-FOURNISSEUR-045.pdf',
    date: '2026-07-15',
    action: 'Conversion',
    status: 'converti',
    statusLabel: 'Converti',
    reportId: null,
  },
];

export const conversionData = {
  filename: 'FACT-2026-00142.pdf',
  fields: {
    numeroFacture: { value: 'FACT-2026-00142', confidence: 'high' },
    dateFacture: { value: '28/07/2026', confidence: 'high' },
    dateEcheance: { value: '27/08/2026', confidence: 'high' },
    vendeurNom: { value: 'SARL Dupont Informatique', confidence: 'high' },
    vendeurSiren: { value: '452 891 237', confidence: 'low' },
    vendeurTva: { value: 'FR45452891237', confidence: 'low' },
    vendeurAdresse: { value: '14 rue des Lilas, 75011 Paris', confidence: 'high' },
    acheteurNom: { value: 'SAS Martin & Associés', confidence: 'high' },
    acheteurSiren: { value: '789 012 345', confidence: 'high' },
    acheteurAdresse: { value: '8 avenue Foch, 69002 Lyon', confidence: 'high' },
  },
  lignes: [
    { ref: 'CONS-001', description: 'Prestation conseil IT — juillet 2026', qty: 5, unit: 'jour', pu: 750.00, tva: 20, total: 3750.00 },
    { ref: 'MAINT-12', description: 'Maintenance serveur mensuelle', qty: 1, unit: 'forfait', pu: 98.00, tva: 20, total: 98.00 },
    { ref: 'LOG-SAP', description: 'Licence logiciel SAP — 3 mois', qty: 3, unit: 'mois', pu: 320.00, tva: 20, total: 960.00 },
  ],
  totalHT: 4808.00,
  tauxTva: 20,
  montantTva: 961.60,
  totalTTC: 5769.60,
};

export const xmlContent = `<?xml version="1.0" encoding="UTF-8"?>
<rsm:CrossIndustryInvoice xmlns:rsm="urn:un:unece:uncefact:data:standard:CrossIndustryInvoice:100"
  xmlns:ram="urn:un:unece:uncefact:data:standard:ReusableAggregateBusinessInformationEntity:100"
  xmlns:udt="urn:un:unece:uncefact:data:standard:UnqualifiedDataType:100">
  <rsm:ExchangedDocumentContext>
    <ram:GuidelineSpecifiedDocumentContextParameter>
      <ram:ID>urn:factur-x.eu:1p0:en16931</ram:ID>
    </ram:GuidelineSpecifiedDocumentContextParameter>
  </rsm:ExchangedDocumentContext>
  <rsm:ExchangedDocument>
    <ram:ID>FACT-2026-00139</ram:ID>
    <ram:TypeCode>380</ram:TypeCode>
    <ram:IssueDateTime>
      <udt:DateTimeString format="102">20260725</udt:DateTimeString>
    </ram:IssueDateTime>
  </rsm:ExchangedDocument>
  <rsm:SupplyChainTradeTransaction>
    <ram:IncludedSupplyChainTradeLineItem>
      <ram:AssociatedDocumentLineDocument>
        <ram:LineID>1</ram:LineID>
      </ram:AssociatedDocumentLineDocument>
      <ram:SpecifiedTradeProduct>
        <ram:Name>Prestation conseil IT — juillet 2026</ram:Name>
      </ram:SpecifiedTradeProduct>
      <ram:SpecifiedLineTradeAgreement>
        <ram:NetPriceProductTradePrice>
          <ram:ChargeAmount>750.00</ram:ChargeAmount>
        </ram:NetPriceProductTradePrice>
      </ram:SpecifiedLineTradeAgreement>
      <ram:SpecifiedLineTradeDelivery>
        <ram:BilledQuantity unitCode="DAY">5</ram:BilledQuantity>
      </ram:SpecifiedLineTradeDelivery>
      <ram:SpecifiedLineTradeSettlement>
        <ram:ApplicableTradeTax>
          <ram:TypeCode>VAT</ram:TypeCode>
          <ram:RateApplicablePercent>20</ram:RateApplicablePercent>
        </ram:ApplicableTradeTax>
        <ram:SpecifiedTradeSettlementLineMonetarySummation>
          <ram:LineTotalAmount>3750.00</ram:LineTotalAmount>
        </ram:SpecifiedTradeSettlementLineMonetarySummation>
      </ram:SpecifiedLineTradeSettlement>
    </ram:IncludedSupplyChainTradeLineItem>
  </rsm:SupplyChainTradeTransaction>
</rsm:CrossIndustryInvoice>`;
