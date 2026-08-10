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

export const reportWithErrors = {
  filename: 'FACT-2026-00142.pdf',
  profile: 'EN 16931',
  factureXVersion: 'Factur-X 1.0.07',
  verdict: 'Non conforme',
  errors: 3,
  warnings: 1,
  infos: 2,
  issues: [
    {
      id: 'issue-1',
      title: 'Montant de TVA incorrect pour la ligne 2',
      level: 'error',
      levelLabel: 'Erreur bloquante',
      rule: 'BR-CO-10',
      field: 'BT-109',
      actual: '21.00',
      expected: '19.60',
      fix: 'Recalculez le montant de TVA en appliquant le taux de 20% au montant HT de 98.00 €. Le montant correct est 19.60 €, non 21.00 €.',
    },
    {
      id: 'issue-2',
      title: 'Numéro SIREN du vendeur absent ou invalide',
      level: 'error',
      levelLabel: 'Erreur bloquante',
      rule: 'BR-FR-01',
      field: 'BT-30',
      actual: '— (absent)',
      expected: 'SIREN à 9 chiffres',
      fix: 'Renseignez le numéro SIREN du vendeur dans le champ BT-30. Ce champ est obligatoire pour les factures émises par des entités françaises.',
    },
    {
      id: 'issue-3',
      title: 'Numéro de TVA intracommunautaire mal formaté',
      level: 'error',
      levelLabel: 'Erreur bloquante',
      rule: 'BR-CO-09',
      field: 'BT-31',
      actual: 'FR123456789',
      expected: 'FR + 11 caractères (ex: FR12345678901)',
      fix: 'Le numéro de TVA intracommunautaire doit respecter le format FR suivi de 11 caractères alphanumériques. Vérifiez le numéro auprès de votre service comptable.',
    },
    {
      id: 'issue-4',
      title: 'Date d\'échéance antérieure à la date de facture',
      level: 'warning',
      levelLabel: 'Avertissement',
      rule: 'BR-CO-25',
      field: 'BT-9',
      actual: '2026-07-20',
      expected: '≥ 2026-07-28 (date de facture)',
      fix: 'Vérifiez la date d\'échéance. Elle semble incohérente avec la date d\'émission. Corrigez si nécessaire avant d\'envoyer la facture.',
    },
    {
      id: 'issue-5',
      title: 'Référence de commande recommandée',
      level: 'info',
      levelLabel: 'Information',
      rule: 'BR-OPT-01',
      field: 'BT-13',
      actual: '— (absent)',
      expected: 'Numéro de commande acheteur (optionnel)',
      fix: 'Bien que facultatif, il est fortement recommandé de renseigner la référence de commande pour faciliter le rapprochement comptable chez l\'acheteur.',
    },
  ],
};

export const reportNoErrors = {
  filename: 'FACT-2026-00135.pdf',
  profile: 'EN 16931',
  factureXVersion: 'Factur-X 1.0.07',
  verdict: 'Conforme',
  errors: 0,
  warnings: 0,
  infos: 1,
  issues: [
    {
      id: 'issue-info-1',
      title: 'Champ description optionnel non renseigné',
      level: 'info',
      levelLabel: 'Information',
      rule: 'BR-OPT-02',
      field: 'BT-154',
      actual: '— (absent)',
      expected: 'Description de l\'article (optionnel)',
      fix: 'Renseigner la description des articles améliore la lisibilité pour l\'acheteur mais n\'est pas obligatoire.',
    },
  ],
};

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
