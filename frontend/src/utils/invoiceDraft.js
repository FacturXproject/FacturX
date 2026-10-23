// F12 : règles du formulaire de correction (voir F12.txt), identiques à celles du
// serveur (backend .../extraction/DraftCorrection.java). Le front les applique pour
// afficher les erreurs avant l'envoi ; le serveur revalide et recalcule tout, c'est
// lui qui fait foi.
//
// Les montants sont calculés en entiers (BigInt) et non en flottants : le résultat
// affiché est ainsi exactement celui que le serveur enregistrera (arrondi au
// centime "half up", ligne par ligne).

export const MAX_TEXT_LENGTH = 500;
export const MAX_LINES = 500;

const DECIMAL_RE = /^-?\d{1,12}(\.\d{1,6})?$/;
const SPACES_RE = /[\s  ]+/g;
const FRENCH_DATE_RE = /^(\d{2})[/.-](\d{2})[/.-](\d{4})$/;
const ISO_DATE_RE = /^(\d{4})-(\d{2})-(\d{2})$/;

const SCALE = 6; // décimales maximales acceptées pour une quantité, un prix ou un taux
const MAX_TOTAL_CENTS = 10n ** 17n; // 10^15 €, même garde-fou que le serveur

/** Trim + espaces multiples réduits à un seul ; '' si vide. */
export function normalizeText(value) {
  return String(value ?? '').replace(SPACES_RE, ' ').trim();
}

/**
 * Nombre au format français ou anglais ("1 080,50", "1.080,50", "1,080.50") :
 * le dernier séparateur est la décimale. Retourne la forme normalisée ("1080.50")
 * ou null si la valeur est vide ou invalide (pas de notation scientifique).
 */
export function parseDecimal(value) {
  if (value == null) return null;
  let s = String(value).replace(SPACES_RE, '');
  const comma = s.lastIndexOf(',');
  const dot = s.lastIndexOf('.');
  if (comma >= 0 && dot >= 0) {
    s = comma > dot ? s.replace(/\./g, '').replace(',', '.') : s.replace(/,/g, '');
  } else if (comma >= 0) {
    s = s.replace(',', '.');
  }
  return DECIMAL_RE.test(s) ? s : null;
}

/** Taux de TVA entre 0 et 100 inclus, "%" final toléré. Forme normalisée ou null. */
export function parseRate(value) {
  if (value == null) return null;
  const s = parseDecimal(String(value).trim().replace(/%$/, ''));
  if (s == null) return null;
  const scaled = toScaled(s);
  return scaled >= 0n && scaled <= 100n * 10n ** BigInt(SCALE) ? s : null;
}

/** Date JJ/MM/AAAA, JJ.MM.AAAA, JJ-MM-AAAA ou AAAA-MM-JJ ; objet {y, m, d} ou null. */
export function parseDate(value) {
  const s = normalizeText(value);
  let y, m, d;
  let match = FRENCH_DATE_RE.exec(s);
  if (match) {
    [, d, m, y] = match.map(Number);
  } else if ((match = ISO_DATE_RE.exec(s))) {
    [, y, m, d] = match.map(Number);
  } else {
    return null;
  }
  const date = new Date(Date.UTC(y, m - 1, d));
  if (date.getUTCFullYear() !== y || date.getUTCMonth() !== m - 1 || date.getUTCDate() !== d) {
    return null;
  }
  return { y, m, d, time: date.getTime() };
}

// "12.5" -> 12500000n (valeur x 10^6), exact.
function toScaled(normalized) {
  const negative = normalized.startsWith('-');
  const [int, frac = ''] = normalized.replace('-', '').split('.');
  const scaled = BigInt(int + frac.padEnd(SCALE, '0'));
  return negative ? -scaled : scaled;
}

// Division entière arrondie "half up" (valeurs positives).
function divRound(numerator, denominator) {
  return (numerator * 2n + denominator) / (denominator * 2n);
}

/** Centimes (BigInt) -> "1 234,56 €". */
export function formatCents(cents) {
  const negative = cents < 0n;
  const abs = negative ? -cents : cents;
  const euros = (abs / 100n).toString().replace(/\B(?=(\d{3})+(?!\d))/g, ' ');
  const rest = (abs % 100n).toString().padStart(2, '0');
  return `${negative ? '-' : ''}${euros},${rest} €`;
}

/** Taux normalisé -> "20", "5,5". */
export function formatRate(rate) {
  return String(Number(rate)).replace('.', ',');
}

/** Total d'une ligne en centimes, ou null si quantité ou prix invalide. */
export function lineTotalCents(line) {
  const qty = parseDecimal(line.qty);
  const pu = parseDecimal(line.pu);
  if (qty == null || pu == null) return null;
  const product = toScaled(qty) * toScaled(pu); // x 10^12
  if (product < 0n) return null;
  return divRound(product, 10n ** BigInt(2 * SCALE - 2));
}

/**
 * Totaux et ventilation de TVA, calculés comme le serveur : total de ligne arrondi,
 * HT = somme des lignes, TVA par taux = base du taux x taux / 100 arrondie. Les
 * lignes invalides sont ignorées (le formulaire signale l'erreur par ailleurs).
 */
export function calculateTotals(lines, defaultRate) {
  const bases = new Map(); // taux (x 10^6, en string pour la clé) -> base en centimes
  let totalHT = 0n;

  for (const line of lines) {
    const total = lineTotalCents(line);
    if (total == null) continue;
    totalHT += total;
    const rate = parseRate(normalizeText(line.tva) ? line.tva : defaultRate);
    if (rate == null) continue;
    const key = toScaled(rate).toString();
    bases.set(key, (bases.get(key) ?? 0n) + total);
  }

  const taxes = [...bases.entries()]
    .map(([key, base]) => {
      const scaledRate = BigInt(key);
      return {
        rate: (Number(scaledRate) / 10 ** SCALE).toString(),
        scaledRate,
        baseCents: base,
        taxCents: divRound(base * scaledRate, 100n * 10n ** BigInt(SCALE)),
      };
    })
    .sort((a, b) => (b.scaledRate > a.scaledRate ? 1 : b.scaledRate < a.scaledRate ? -1 : 0));

  const totalVAT = taxes.reduce((sum, t) => sum + t.taxCents, 0n);
  return { totalHT, totalVAT, totalTTC: totalHT + totalVAT, taxes };
}

const REQUIRED_FIELDS = [
  ['numeroFacture', 'Le numéro de facture est obligatoire.'],
  ['dateFacture', 'La date de facture est obligatoire.'],
  ['vendeurNom', 'Le nom du vendeur est obligatoire.'],
  ['acheteurNom', "Le nom de l'acheteur est obligatoire."],
];

/**
 * Valide le formulaire. Retourne une liste [{ key, message }] ; key identifie le
 * champ en erreur ('numeroFacture', 'tauxTva', 'line:0:qty', ...) pour le surligner.
 */
export function validateDraft(fields, tauxTva, lines) {
  const errors = [];
  const add = (key, message) => errors.push({ key, message });

  for (const [key, message] of REQUIRED_FIELDS) {
    if (!normalizeText(fields[key]?.value)) add(key, message);
  }
  for (const [key, field] of Object.entries(fields)) {
    if (normalizeText(field.value).length > MAX_TEXT_LENGTH) {
      add(key, `Ce champ ne doit pas dépasser ${MAX_TEXT_LENGTH} caractères.`);
    }
  }

  const invoiceDate = normalizeText(fields.dateFacture?.value) ? parseDate(fields.dateFacture.value) : null;
  if (normalizeText(fields.dateFacture?.value) && !invoiceDate) {
    add('dateFacture', 'La date de facture doit être une date valide au format JJ/MM/AAAA.');
  }
  const dueDate = normalizeText(fields.dateEcheance?.value) ? parseDate(fields.dateEcheance.value) : null;
  if (normalizeText(fields.dateEcheance?.value) && !dueDate) {
    add('dateEcheance', "La date d'échéance doit être une date valide au format JJ/MM/AAAA.");
  }
  if (invoiceDate && dueDate && dueDate.time < invoiceDate.time) {
    add('dateEcheance', "La date d'échéance ne peut pas précéder la date de facture.");
  }

  if (!normalizeText(tauxTva)) {
    add('tauxTva', 'Le taux de TVA est obligatoire.');
  } else if (parseRate(tauxTva) == null) {
    add('tauxTva', 'Le taux de TVA doit être un nombre compris entre 0 et 100.');
  }

  if (lines.length === 0) {
    add('lines', 'La facture doit contenir au moins une ligne.');
  } else if (lines.length > MAX_LINES) {
    add('lines', `Une facture ne peut pas dépasser ${MAX_LINES} lignes.`);
  }

  lines.forEach((line, i) => {
    const prefix = `Ligne ${i + 1} : `;
    const description = normalizeText(line.description);
    if (!description) {
      add(`line:${i}:description`, `${prefix}la description est obligatoire.`);
    } else if (description.length > MAX_TEXT_LENGTH) {
      add(`line:${i}:description`, `${prefix}la description ne doit pas dépasser ${MAX_TEXT_LENGTH} caractères.`);
    }
    const qty = parseDecimal(line.qty);
    if (qty == null || toScaled(qty) <= 0n) {
      add(`line:${i}:qty`, `${prefix}la quantité doit être un nombre strictement positif.`);
    }
    const pu = parseDecimal(line.pu);
    if (pu == null || toScaled(pu) < 0n) {
      add(`line:${i}:pu`, `${prefix}le prix unitaire doit être un nombre positif ou nul.`);
    }
    if (normalizeText(line.tva) && parseRate(line.tva) == null) {
      add(`line:${i}:tva`, `${prefix}le taux de TVA doit être un nombre compris entre 0 et 100.`);
    }
  });

  if (errors.length === 0 && calculateTotals(lines, tauxTva).totalTTC >= MAX_TOTAL_CENTS) {
    add('lines', 'Le montant total de la facture est trop élevé.');
  }

  return errors;
}

// Clés du formulaire (français, mockData.conversionData) -> noms de champs de l'API (F11).
export const FIELD_API_NAMES = {
  numeroFacture: 'invoiceNumber',
  dateFacture: 'invoiceDate',
  dateEcheance: 'dueDate',
  vendeurNom: 'sellerName',
  vendeurSiren: 'sellerSiren',
  vendeurTva: 'sellerVat',
  vendeurAdresse: 'sellerAddress',
  acheteurNom: 'buyerName',
  acheteurSiren: 'buyerSiren',
  acheteurAdresse: 'buyerAddress',
};

/** Corps de PUT /documents/{id}/draft. Les totaux ne sont pas envoyés : le serveur les calcule. */
export function buildDraftPayload(fields, tauxTva, lines, updatedAt) {
  const apiFields = { vatRate: normalizeText(tauxTva) };
  for (const [key, apiName] of Object.entries(FIELD_API_NAMES)) {
    apiFields[apiName] = normalizeText(fields[key]?.value);
  }
  return {
    updatedAt: updatedAt ?? null,
    fields: apiFields,
    lines: lines.map((line) => ({
      description: normalizeText(line.description),
      quantity: normalizeText(line.qty),
      unitPrice: normalizeText(line.pu),
      vatRate: normalizeText(line.tva),
    })),
  };
}
