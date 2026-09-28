-- F09: rule_catalog is data, not code. Each row is the French explanation of one
-- validation rule Mustangproject can raise. Runs on every startup (spring.sql.init.mode
-- = always); ON CONFLICT keeps it idempotent and lets an edited row here simply
-- overwrite the previous one on the next deploy.
--
-- One INSERT per row, single-quoted with '' escaping - Spring's default script
-- splitter (ScriptUtils) only tracks '...' string state, not PostgreSQL's $$...$$
-- dollar-quoting, so a dollar-quoted multi-row INSERT gets mis-split mid-statement.
--
-- Coverage: 19 codes catalogued so far (16 core EN16931 cardinality/presence rules
-- BR-01..BR-16, 6 EN16931 calculation rules BR-CO-09/10/13/15/16/25, the PEPPOL
-- business-process rule seen in our own valid sample, and MUSTANG-ERROR-23 - the
-- PDF/A-3 failure documented in backend/README.md). Any other rule code Mustang
-- reports falls back to its raw message in the report (see ValidationReportService)
-- instead of a bare, unexplained code.

INSERT INTO rule_catalog (code, layer, raw_text, title_fr, description_fr, correction_hint_fr)
VALUES ('BR-01', 'SCHEMATRON',
 'An Invoice shall have a Specification identifier (BT-24).',
 'Identifiant de spécification manquant',
 'La facture doit indiquer quelle norme elle respecte (par exemple EN 16931) via un identifiant de spécification. Sans cette information, le destinataire ne sait pas selon quelles règles vérifier la facture.',
 'Ajoutez l''identifiant de spécification (BT-24), généralement généré automatiquement par le logiciel de facturation.')
ON CONFLICT (code) DO UPDATE SET layer = EXCLUDED.layer, raw_text = EXCLUDED.raw_text,
 title_fr = EXCLUDED.title_fr, description_fr = EXCLUDED.description_fr, correction_hint_fr = EXCLUDED.correction_hint_fr;

INSERT INTO rule_catalog (code, layer, raw_text, title_fr, description_fr, correction_hint_fr)
VALUES ('BR-02', 'SCHEMATRON',
 'An Invoice shall have an Invoice number (BT-1).',
 'Numéro de facture manquant',
 'Chaque facture doit porter un numéro unique qui permet de l''identifier et de la retrouver.',
 'Renseignez un numéro de facture (BT-1), unique pour l''émetteur.')
ON CONFLICT (code) DO UPDATE SET layer = EXCLUDED.layer, raw_text = EXCLUDED.raw_text,
 title_fr = EXCLUDED.title_fr, description_fr = EXCLUDED.description_fr, correction_hint_fr = EXCLUDED.correction_hint_fr;

INSERT INTO rule_catalog (code, layer, raw_text, title_fr, description_fr, correction_hint_fr)
VALUES ('BR-03', 'SCHEMATRON',
 'An Invoice shall have an Invoice issue date (BT-2).',
 'Date d''émission manquante',
 'La date à laquelle la facture a été émise doit être indiquée.',
 'Renseignez la date d''émission de la facture (BT-2).')
ON CONFLICT (code) DO UPDATE SET layer = EXCLUDED.layer, raw_text = EXCLUDED.raw_text,
 title_fr = EXCLUDED.title_fr, description_fr = EXCLUDED.description_fr, correction_hint_fr = EXCLUDED.correction_hint_fr;

INSERT INTO rule_catalog (code, layer, raw_text, title_fr, description_fr, correction_hint_fr)
VALUES ('BR-04', 'SCHEMATRON',
 'An Invoice shall have an Invoice type code (BT-3).',
 'Type de document manquant',
 'La facture doit préciser son type (facture, avoir, etc.) au moyen d''un code normalisé.',
 'Renseignez le code de type de document (BT-3), par exemple 380 pour une facture commerciale.')
ON CONFLICT (code) DO UPDATE SET layer = EXCLUDED.layer, raw_text = EXCLUDED.raw_text,
 title_fr = EXCLUDED.title_fr, description_fr = EXCLUDED.description_fr, correction_hint_fr = EXCLUDED.correction_hint_fr;

INSERT INTO rule_catalog (code, layer, raw_text, title_fr, description_fr, correction_hint_fr)
VALUES ('BR-05', 'SCHEMATRON',
 'An Invoice shall have an Invoice currency code (BT-5).',
 'Devise de la facture manquante',
 'La devise dans laquelle les montants sont exprimés doit être précisée.',
 'Renseignez le code de devise (BT-5), par exemple EUR.')
ON CONFLICT (code) DO UPDATE SET layer = EXCLUDED.layer, raw_text = EXCLUDED.raw_text,
 title_fr = EXCLUDED.title_fr, description_fr = EXCLUDED.description_fr, correction_hint_fr = EXCLUDED.correction_hint_fr;

INSERT INTO rule_catalog (code, layer, raw_text, title_fr, description_fr, correction_hint_fr)
VALUES ('BR-06', 'SCHEMATRON',
 'An Invoice shall contain the Seller name (BT-27).',
 'Nom du vendeur manquant',
 'Le nom de l''entreprise qui émet la facture doit figurer sur le document.',
 'Renseignez le nom du vendeur (BT-27).')
ON CONFLICT (code) DO UPDATE SET layer = EXCLUDED.layer, raw_text = EXCLUDED.raw_text,
 title_fr = EXCLUDED.title_fr, description_fr = EXCLUDED.description_fr, correction_hint_fr = EXCLUDED.correction_hint_fr;

INSERT INTO rule_catalog (code, layer, raw_text, title_fr, description_fr, correction_hint_fr)
VALUES ('BR-07', 'SCHEMATRON',
 'An Invoice shall contain the Buyer name (BT-44).',
 'Nom de l''acheteur manquant',
 'Le nom du client destinataire de la facture doit figurer sur le document.',
 'Renseignez le nom de l''acheteur (BT-44).')
ON CONFLICT (code) DO UPDATE SET layer = EXCLUDED.layer, raw_text = EXCLUDED.raw_text,
 title_fr = EXCLUDED.title_fr, description_fr = EXCLUDED.description_fr, correction_hint_fr = EXCLUDED.correction_hint_fr;

INSERT INTO rule_catalog (code, layer, raw_text, title_fr, description_fr, correction_hint_fr)
VALUES ('BR-08', 'SCHEMATRON',
 'An Invoice shall contain the Seller postal address (BG-5).',
 'Adresse du vendeur manquante',
 'L''adresse postale complète du vendeur doit figurer sur la facture.',
 'Renseignez l''adresse postale du vendeur (rue, code postal, ville, pays).')
ON CONFLICT (code) DO UPDATE SET layer = EXCLUDED.layer, raw_text = EXCLUDED.raw_text,
 title_fr = EXCLUDED.title_fr, description_fr = EXCLUDED.description_fr, correction_hint_fr = EXCLUDED.correction_hint_fr;

INSERT INTO rule_catalog (code, layer, raw_text, title_fr, description_fr, correction_hint_fr)
VALUES ('BR-09', 'SCHEMATRON',
 'The Seller postal address shall contain a Seller country code (BT-40).',
 'Pays du vendeur manquant',
 'Le code pays de l''adresse du vendeur est obligatoire, même si le reste de l''adresse est renseigné.',
 'Ajoutez le code pays du vendeur (BT-40), au format ISO 3166-1 alpha-2 (ex. FR).')
ON CONFLICT (code) DO UPDATE SET layer = EXCLUDED.layer, raw_text = EXCLUDED.raw_text,
 title_fr = EXCLUDED.title_fr, description_fr = EXCLUDED.description_fr, correction_hint_fr = EXCLUDED.correction_hint_fr;

INSERT INTO rule_catalog (code, layer, raw_text, title_fr, description_fr, correction_hint_fr)
VALUES ('BR-10', 'SCHEMATRON',
 'An Invoice shall contain the Buyer postal address (BG-8).',
 'Adresse de l''acheteur manquante',
 'L''adresse postale complète de l''acheteur doit figurer sur la facture.',
 'Renseignez l''adresse postale de l''acheteur (rue, code postal, ville, pays).')
ON CONFLICT (code) DO UPDATE SET layer = EXCLUDED.layer, raw_text = EXCLUDED.raw_text,
 title_fr = EXCLUDED.title_fr, description_fr = EXCLUDED.description_fr, correction_hint_fr = EXCLUDED.correction_hint_fr;

INSERT INTO rule_catalog (code, layer, raw_text, title_fr, description_fr, correction_hint_fr)
VALUES ('BR-11', 'SCHEMATRON',
 'The Buyer postal address shall contain a Buyer country code (BT-55).',
 'Pays de l''acheteur manquant',
 'Le code pays de l''adresse de l''acheteur est obligatoire, même si le reste de l''adresse est renseigné.',
 'Ajoutez le code pays de l''acheteur (BT-55), au format ISO 3166-1 alpha-2 (ex. FR).')
ON CONFLICT (code) DO UPDATE SET layer = EXCLUDED.layer, raw_text = EXCLUDED.raw_text,
 title_fr = EXCLUDED.title_fr, description_fr = EXCLUDED.description_fr, correction_hint_fr = EXCLUDED.correction_hint_fr;

INSERT INTO rule_catalog (code, layer, raw_text, title_fr, description_fr, correction_hint_fr)
VALUES ('BR-12', 'SCHEMATRON',
 'An Invoice shall have the Sum of Invoice line net amount (BT-106).',
 'Total des lignes hors taxes manquant',
 'La somme des montants nets de chaque ligne de facture doit être indiquée.',
 'Renseignez le total des montants nets des lignes (BT-106).')
ON CONFLICT (code) DO UPDATE SET layer = EXCLUDED.layer, raw_text = EXCLUDED.raw_text,
 title_fr = EXCLUDED.title_fr, description_fr = EXCLUDED.description_fr, correction_hint_fr = EXCLUDED.correction_hint_fr;

INSERT INTO rule_catalog (code, layer, raw_text, title_fr, description_fr, correction_hint_fr)
VALUES ('BR-13', 'SCHEMATRON',
 'An Invoice shall have the Invoice total amount without VAT (BT-109).',
 'Montant total hors taxes manquant',
 'Le montant total de la facture avant application de la TVA doit être indiqué.',
 'Renseignez le montant total hors taxes (BT-109).')
ON CONFLICT (code) DO UPDATE SET layer = EXCLUDED.layer, raw_text = EXCLUDED.raw_text,
 title_fr = EXCLUDED.title_fr, description_fr = EXCLUDED.description_fr, correction_hint_fr = EXCLUDED.correction_hint_fr;

INSERT INTO rule_catalog (code, layer, raw_text, title_fr, description_fr, correction_hint_fr)
VALUES ('BR-14', 'SCHEMATRON',
 'An Invoice shall have the Invoice total amount with VAT (BT-112).',
 'Montant total TTC manquant',
 'Le montant total de la facture, TVA comprise, doit être indiqué.',
 'Renseignez le montant total toutes taxes comprises (BT-112).')
ON CONFLICT (code) DO UPDATE SET layer = EXCLUDED.layer, raw_text = EXCLUDED.raw_text,
 title_fr = EXCLUDED.title_fr, description_fr = EXCLUDED.description_fr, correction_hint_fr = EXCLUDED.correction_hint_fr;

INSERT INTO rule_catalog (code, layer, raw_text, title_fr, description_fr, correction_hint_fr)
VALUES ('BR-15', 'SCHEMATRON',
 'An Invoice shall have the Amount due for payment (BT-115).',
 'Montant à payer manquant',
 'Le montant restant dû par l''acheteur doit être indiqué explicitement.',
 'Renseignez le montant net à payer (BT-115).')
ON CONFLICT (code) DO UPDATE SET layer = EXCLUDED.layer, raw_text = EXCLUDED.raw_text,
 title_fr = EXCLUDED.title_fr, description_fr = EXCLUDED.description_fr, correction_hint_fr = EXCLUDED.correction_hint_fr;

INSERT INTO rule_catalog (code, layer, raw_text, title_fr, description_fr, correction_hint_fr)
VALUES ('BR-16', 'SCHEMATRON',
 'An Invoice shall have at least one Invoice line (BG-25).',
 'Aucune ligne de facture',
 'Une facture doit détailler au moins un bien ou service facturé.',
 'Ajoutez au moins une ligne de facture décrivant ce qui est vendu.')
ON CONFLICT (code) DO UPDATE SET layer = EXCLUDED.layer, raw_text = EXCLUDED.raw_text,
 title_fr = EXCLUDED.title_fr, description_fr = EXCLUDED.description_fr, correction_hint_fr = EXCLUDED.correction_hint_fr;

INSERT INTO rule_catalog (code, layer, raw_text, title_fr, description_fr, correction_hint_fr)
VALUES ('BR-CO-09', 'SCHEMATRON',
 'The Seller VAT identifier, the Seller tax representative VAT identifier and the Buyer VAT identifier shall have a prefix in accordance with ISO 3166-1 alpha-2.',
 'Numéro de TVA mal formaté',
 'Un numéro de TVA intracommunautaire doit commencer par le code pays à deux lettres de l''entité concernée (par exemple FR pour la France), suivi du numéro.',
 'Vérifiez que le numéro de TVA commence par le bon préfixe pays (ex. FR12345678901) et corrigez-le si besoin.')
ON CONFLICT (code) DO UPDATE SET layer = EXCLUDED.layer, raw_text = EXCLUDED.raw_text,
 title_fr = EXCLUDED.title_fr, description_fr = EXCLUDED.description_fr, correction_hint_fr = EXCLUDED.correction_hint_fr;

INSERT INTO rule_catalog (code, layer, raw_text, title_fr, description_fr, correction_hint_fr)
VALUES ('BR-CO-10', 'SCHEMATRON',
 'Sum of Invoice line net amount = SUM(Invoice line net amount).',
 'Le total des lignes ne correspond pas à la somme des lignes',
 'Le total hors taxes annoncé pour l''ensemble des lignes doit être égal à la somme exacte des montants nets de chaque ligne.',
 'Recalculez la somme des montants nets de toutes les lignes et corrigez le total pour qu''il corresponde.')
ON CONFLICT (code) DO UPDATE SET layer = EXCLUDED.layer, raw_text = EXCLUDED.raw_text,
 title_fr = EXCLUDED.title_fr, description_fr = EXCLUDED.description_fr, correction_hint_fr = EXCLUDED.correction_hint_fr;

INSERT INTO rule_catalog (code, layer, raw_text, title_fr, description_fr, correction_hint_fr)
VALUES ('BR-CO-13', 'SCHEMATRON',
 'Invoice total amount without VAT = SUM(Invoice line net amount) - Sum of allowances + Sum of charges.',
 'Le montant total hors taxes est incohérent',
 'Le montant total hors taxes doit correspondre à la somme des lignes, diminuée des remises globales et augmentée des frais éventuels.',
 'Vérifiez le calcul (total des lignes moins remises plus frais) et corrigez le montant total hors taxes.')
ON CONFLICT (code) DO UPDATE SET layer = EXCLUDED.layer, raw_text = EXCLUDED.raw_text,
 title_fr = EXCLUDED.title_fr, description_fr = EXCLUDED.description_fr, correction_hint_fr = EXCLUDED.correction_hint_fr;

INSERT INTO rule_catalog (code, layer, raw_text, title_fr, description_fr, correction_hint_fr)
VALUES ('BR-CO-15', 'SCHEMATRON',
 'Invoice total amount with VAT = Invoice total amount without VAT + Invoice total VAT amount.',
 'Le montant total TTC est incohérent',
 'Le montant total TTC doit être égal au montant total hors taxes additionné du montant total de TVA.',
 'Vérifiez que montant TTC = montant HT + montant de TVA, et corrigez si besoin.')
ON CONFLICT (code) DO UPDATE SET layer = EXCLUDED.layer, raw_text = EXCLUDED.raw_text,
 title_fr = EXCLUDED.title_fr, description_fr = EXCLUDED.description_fr, correction_hint_fr = EXCLUDED.correction_hint_fr;

INSERT INTO rule_catalog (code, layer, raw_text, title_fr, description_fr, correction_hint_fr)
VALUES ('BR-CO-16', 'SCHEMATRON',
 'Amount due for payment = Invoice total amount with VAT - Paid amount + Rounding amount.',
 'Le montant à payer est incohérent',
 'Le montant restant dû doit correspondre au montant TTC, diminué des sommes déjà payées et ajusté de l''arrondi éventuel.',
 'Vérifiez le calcul (montant TTC moins montant déjà payé plus arrondi) et corrigez le montant à payer.')
ON CONFLICT (code) DO UPDATE SET layer = EXCLUDED.layer, raw_text = EXCLUDED.raw_text,
 title_fr = EXCLUDED.title_fr, description_fr = EXCLUDED.description_fr, correction_hint_fr = EXCLUDED.correction_hint_fr;

INSERT INTO rule_catalog (code, layer, raw_text, title_fr, description_fr, correction_hint_fr)
VALUES ('BR-CO-25', 'SCHEMATRON',
 'In case the Amount due for payment (BT-115) is positive, either the Payment due date (BT-9) or the Payment terms (BT-20) shall be present.',
 'Modalités de paiement manquantes',
 'Lorsqu''un montant reste dû, la facture doit préciser soit une date d''échéance, soit des conditions de paiement en texte libre.',
 'Ajoutez une date d''échéance de paiement (BT-9) ou une description des conditions de paiement (BT-20).')
ON CONFLICT (code) DO UPDATE SET layer = EXCLUDED.layer, raw_text = EXCLUDED.raw_text,
 title_fr = EXCLUDED.title_fr, description_fr = EXCLUDED.description_fr, correction_hint_fr = EXCLUDED.correction_hint_fr;

INSERT INTO rule_catalog (code, layer, raw_text, title_fr, description_fr, correction_hint_fr)
VALUES ('PEPPOL-EN16931-R001', 'SCHEMATRON',
 'Business process MUST be provided.',
 'Identifiant de processus métier manquant',
 'Pour l''échange via un réseau Peppol, la facture doit indiquer l''identifiant du processus métier concerné, afin que le destinataire puisse la router et la traiter correctement.',
 'Renseignez l''identifiant de processus métier (BT-23), fourni par l''accord commercial ou le réseau d''échange utilisé.')
ON CONFLICT (code) DO UPDATE SET layer = EXCLUDED.layer, raw_text = EXCLUDED.raw_text,
 title_fr = EXCLUDED.title_fr, description_fr = EXCLUDED.description_fr, correction_hint_fr = EXCLUDED.correction_hint_fr;

INSERT INTO rule_catalog (code, layer, raw_text, title_fr, description_fr, correction_hint_fr)
VALUES ('MUSTANG-ERROR-23', 'PDF_A3',
 'Not a PDF/A-3',
 'Le fichier n''est pas un PDF/A-3 valide',
 'Une facture Factur-X doit être un PDF conforme au format d''archivage PDF/A-3, qui permet d''embarquer les données structurées de la facture (XML) dans le document. Ce fichier n''y est pas conforme.',
 'Régénérez le PDF avec un outil qui produit du PDF/A-3 (la plupart des logiciels de facturation Factur-X le font automatiquement), plutôt qu''un PDF classique.')
ON CONFLICT (code) DO UPDATE SET layer = EXCLUDED.layer, raw_text = EXCLUDED.raw_text,
 title_fr = EXCLUDED.title_fr, description_fr = EXCLUDED.description_fr, correction_hint_fr = EXCLUDED.correction_hint_fr;
