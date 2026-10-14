import { useEffect, useState } from 'react';
import { useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import { Loader2 } from 'lucide-react';
import api from '../services/api';

// F11: seuil au-dessus duquel un champ extrait est affiché comme fiable.
// En dessous (ou champ non trouvé), il est marqué "à vérifier" (F12).
const CONFIDENCE_THRESHOLD = 0.6;

function confidenceLevel(score) {
  return score != null && score >= CONFIDENCE_THRESHOLD ? 'high' : 'low';
}

// Montants au format français : "1 080,00" (espace, y compris insécable, comme
// séparateur de milliers) ou "1.080,00". Le dernier séparateur est la décimale.
function toNumber(value) {
  if (value == null) return 0;
  let s = String(value).replace(/\s/g, '');
  if (s.lastIndexOf(',') > s.lastIndexOf('.')) {
    s = s.replace(/\./g, '').replace(',', '.');
  } else {
    s = s.replace(/,/g, '');
  }
  const n = Number.parseFloat(s);
  return Number.isFinite(n) ? n : 0;
}

function calculateTotals(lines, vatRate) {
  const totalHT = lines.reduce((sum, line) => {
    return sum + toNumber(line.qty) * toNumber(line.pu);
  }, 0);

  const rate = toNumber(vatRate);
  const totalVAT = totalHT * rate / 100;
  const totalTTC = totalHT + totalVAT;

  return {
    totalHT: Math.round((totalHT + Number.EPSILON) * 100) / 100,
    totalVAT: Math.round((totalVAT + Number.EPSILON) * 100) / 100,
    totalTTC: Math.round((totalTTC + Number.EPSILON) * 100) / 100,
  };
}

// Traduit le brouillon renvoyé par GET /documents/{id}/draft (F11, champs en
// anglais - voir ExtractionController) vers la forme attendue par cette page
// (mockData.conversionData) : mêmes clés que le formulaire de correction F12
// utilisera plus tard.
function toViewModel(draft) {
  const f = draft.fields || {};
  const field = (name) => ({
    value: f[name]?.value ?? '',
    confidence: confidenceLevel(f[name]?.confidence),
  });

  const lignes = (draft.lines || []).map((l) => ({
    ref: '',
    // Un <input> supprime les sauts de ligne : sans cela les mots d'une description
    // sur deux lignes (brouillons extraits avant la correction côté extracteur) se collent.
    description: (l.description ?? '').replace(/\s+/g, ' ').trim(),
    qty: toNumber(l.quantity) || l.quantity || '',
    unit: '',
    pu: toNumber(l.unitPrice),
    tva: toNumber(f.vatRate?.value),
    total: toNumber(l.total),
  }));

  return {
    fields: {
      numeroFacture: field('invoiceNumber'),
      dateFacture: field('invoiceDate'),
      dateEcheance: field('dueDate'),
      vendeurNom: field('sellerName'),
      vendeurSiren: field('sellerSiren'),
      vendeurTva: field('sellerVat'),
      vendeurAdresse: field('sellerAddress'),
      acheteurNom: field('buyerName'),
      acheteurSiren: field('buyerSiren'),
      acheteurAdresse: field('buyerAddress'),
    },
    lignes,
    totalHT: toNumber(f.totalHt?.value),
    tauxTva: toNumber(f.vatRate?.value),
    montantTva: toNumber(f.totalVat?.value),
    totalTTC: toNumber(f.totalTtc?.value),
  };
}

function ConfidenceDot({ level }) {
  return (
    <span
      title={level === 'high' ? 'Confiance élevée' : 'À vérifier'}
      style={{
        display: 'inline-block', width: '8px', height: '8px',
        borderRadius: '50%',
        background: level === 'high' ? '#16a34a' : '#f59e0b',
        marginRight: '6px', flexShrink: 0,
      }}
    />
  );
}

function Field({ label, fieldKey, data, onChange }) {
  const f = data.fields[fieldKey];
  return (
    <div style={{ marginBottom: '12px' }}>
      <label style={{ display: 'flex', alignItems: 'center', fontSize: '11.5px', fontWeight: 500, color: '#6b7280', marginBottom: '4px' }}>
        <ConfidenceDot level={f.confidence} />
        {label}
      </label>
      <input
        type="text"
        value={f.value}
        onChange={e => onChange(fieldKey, e.target.value)}
        style={{
          width: '100%', padding: '7px 9px',
          border: `1px solid ${f.confidence === 'low' ? '#fcd34d' : '#d1d5db'}`,
          borderRadius: '5px', fontSize: '13px', color: '#1a1a2e',
          background: f.confidence === 'low' ? '#fffdf0' : '#fff',
          outline: 'none', boxSizing: 'border-box',
        }}
      />
    </div>
  );
}

export default function Conversion() {
  const navigate = useNavigate();
  const location = useLocation();
  const [params] = useSearchParams();
  const documentId = params.get('documentId');

  // Retour : page précédente (Convertir ou fiche du document). Si la page a été
  // ouverte directement par son URL, il n'y a pas d'historique : on va sur Convertir.
  const goBack = () => (location.key !== 'default' ? navigate(-1) : navigate('/convertir'));

  const [data, setData] = useState(null);
  const [lignes, setLignes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [saving, setSaving] = useState(false);
  const [saveMessage, setSaveMessage] = useState('');

  useEffect(() => {
    if (!documentId) {
      setLoading(false);
      setError("Aucun document à convertir.");
      return;
    }

    setLoading(true);
    setError(null);

    api.get(`/documents/${documentId}/draft`)
      .then((response) => {
        const vm = toViewModel(response.data || {});
        setData(vm);
        setLignes(vm.lignes);
      })
      .catch((err) => {
        console.error('Draft error:', err);
        setError(err.response?.data?.message || "Impossible de charger les données extraites.");
      })
      .finally(() => setLoading(false));
  }, [documentId]);

  const updateField = (key, value) => {
    setData(d => ({ ...d, fields: { ...d.fields, [key]: { ...d.fields[key], value } } }));
  };

  const updateVatRate = (value) => {
	setData(d => ({ ...d, tauxTva: value }));
  };

  const updateLigne = (i, col, val) => {
	setLignes(currentLines =>
		currentLines.map((line, index) => {
		if (index !== i) return line;

		const updatedLine = { ...line, [col]: val };

		const quantity = toNumber(updatedLine.qty);
		const unitPrice = toNumber(updatedLine.pu);

		updatedLine.total =
			Math.round((quantity * unitPrice + Number.EPSILON) * 100) / 100;

		return updatedLine;
		})
	);
  };

  
	const addLigne = () => {
	setLignes(currentLines => [
		...currentLines,
		{
		description: '',
		qty: '1',
		pu: '0.00',
		total: 0,
		},
	]);
	};

	const removeLigne = (indexToRemove) => {
	setLignes(currentLines =>
		currentLines.filter((_, index) => index !== indexToRemove)
	);
	};
  const totals = calculateTotals(lignes, data?.tauxTva ?? 0);

	const handleSave = async () => {
	if (!data || !documentId) return;

	setSaving(true);
	setSaveMessage('');

	
	const errors = [];

	if (!data.fields.numeroFacture.value?.trim()) {
	errors.push('Invoice number is required.');
	}

	if (!data.fields.dateFacture.value?.trim()) {
	errors.push('Invoice date is required.');
	}

	if (!data.fields.vendeurNom.value?.trim()) {
	errors.push('Seller name is required.');
	}

	if (!data.fields.acheteurNom.value?.trim()) {
	errors.push('Buyer name is required.');
	}

	lignes.forEach((line, index) => {
	if (!line.description?.trim()) {
		errors.push(`Line ${index + 1}: description is required.`);
	}

	const quantity = Number(line.qty);
	const unitPrice = Number(line.pu);

	if (!Number.isFinite(quantity) || quantity <= 0) {
		errors.push(`Line ${index + 1}: quantity must be greater than zero.`);
	}

	if (!Number.isFinite(unitPrice) || unitPrice < 0) {
		errors.push(`Line ${index + 1}: unit price cannot be negative.`);
	}
	});

	const vatRate = Number(data.tauxTva);

	if (!Number.isFinite(vatRate) || vatRate < 0 || vatRate > 100) {
	errors.push('VAT rate must be between 0 and 100%.');
	}

	if (errors.length > 0) {
	setSaveMessage(errors.join(' '));
	setSaving(false);
	return;
	}

	const fields = {
		invoiceNumber: data.fields.numeroFacture.value,
		invoiceDate: data.fields.dateFacture.value,
		dueDate: data.fields.dateEcheance.value,
		sellerName: data.fields.vendeurNom.value,
		sellerSiren: data.fields.vendeurSiren.value,
		sellerVat: data.fields.vendeurTva.value,
		sellerAddress: data.fields.vendeurAdresse.value,
		buyerName: data.fields.acheteurNom.value,
		buyerSiren: data.fields.acheteurSiren.value,
		buyerAddress: data.fields.acheteurAdresse.value,
		vatRate: String(data.tauxTva ?? ''),
		totalHt: String(totals.totalHT),
		totalVat: String(totals.totalVAT),
		totalTtc: String(totals.totalTTC),
	};

	const payload = {
		fields,
		lines: lignes.map(line => ({
		description: line.description,
		quantity: String(line.qty),
		unitPrice: String(line.pu),
		total: String(line.total),
		})),
	};

	try {
		await api.put(`/documents/${documentId}/draft`, payload);
		setSaveMessage('Corrections enregistrées avec succès.');
	} catch (err) {
		console.error('Save draft error:', err);
		setSaveMessage(
		err.response?.data?.message ||
		err.response?.data?.error ||
		"Impossible d'enregistrer les corrections."
		);
	} finally {
		setSaving(false);
	}
	};

  if (loading) {
    return (
      <div style={{ padding: '24px 28px', display: 'flex', alignItems: 'center', gap: '10px', color: '#6b7280', fontSize: '13.5px' }}>
        <Loader2 size={18} style={{ animation: 'spin 1s linear infinite' }} />
        Chargement des données extraites…
        <style>{`@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }`}</style>
      </div>
    );
  }

  if (error || !data) {
    return (
      <div style={{ padding: '24px 28px', maxWidth: '600px' }}>
        <button
          onClick={goBack}
          style={{ background: 'none', border: 'none', color: '#4a9eff', fontSize: '13px', cursor: 'pointer', padding: 0, marginBottom: '16px', textDecoration: 'underline' }}
        >
          ← Retour
        </button>
        <div style={{
          border: '1px solid #fecaca', borderRadius: '10px', background: '#fff5f5',
          padding: '20px', color: '#991b1b', fontSize: '13.5px',
        }}>
          {error || "Document introuvable."}
        </div>
      </div>
    );
  }

  return (
    <div style={{ padding: '24px 28px' }}>
      <div style={{ marginBottom: '20px' }}>
        <button
          onClick={goBack}
          style={{ background: 'none', border: 'none', color: '#4a9eff', fontSize: '13px', cursor: 'pointer', padding: 0, textDecoration: 'underline' }}
        >
          ← Retour
        </button>
        <h1 style={{ margin: '8px 0 2px', fontSize: '19px', fontWeight: 700, color: '#1a1a2e' }}>
          Conversion — vérification des données
        </h1>
        <p style={{ margin: 0, fontSize: '12.5px', color: '#6b7280' }}>
          Vérifiez les champs extraits avant de générer la facture Factur-X.
          <span style={{ marginLeft: '8px', color: '#f59e0b', fontWeight: 500 }}>● À vérifier</span>
          <span style={{ marginLeft: '8px', color: '#16a34a', fontWeight: 500 }}>● Confiance élevée</span>
        </p>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px', alignItems: 'start' }}>
        {/* Champs extraits */}
        <div>
          <div style={{ border: '1px solid #e5e7eb', borderRadius: '8px', background: '#fff', padding: '16px 18px', marginBottom: '12px' }}>
            <div style={{ fontSize: '12px', fontWeight: 600, color: '#374151', marginBottom: '12px', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
              Identification
            </div>
            <Field label="Numéro de facture" fieldKey="numeroFacture" data={data} onChange={updateField} />
            <Field label="Date de facture" fieldKey="dateFacture" data={data} onChange={updateField} />
            <Field label="Date d'échéance" fieldKey="dateEcheance" data={data} onChange={updateField} />
          </div>

          <div style={{ border: '1px solid #e5e7eb', borderRadius: '8px', background: '#fff', padding: '16px 18px', marginBottom: '12px' }}>
            <div style={{ fontSize: '12px', fontWeight: 600, color: '#374151', marginBottom: '12px', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
              Vendeur
            </div>
            <Field label="Nom" fieldKey="vendeurNom" data={data} onChange={updateField} />
            <Field label="SIREN" fieldKey="vendeurSiren" data={data} onChange={updateField} />
            <Field label="N° TVA intracommunautaire" fieldKey="vendeurTva" data={data} onChange={updateField} />
            <Field label="Adresse" fieldKey="vendeurAdresse" data={data} onChange={updateField} />
          </div>

          <div style={{ border: '1px solid #e5e7eb', borderRadius: '8px', background: '#fff', padding: '16px 18px', marginBottom: '12px' }}>
            <div style={{ fontSize: '12px', fontWeight: 600, color: '#374151', marginBottom: '12px', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
              Acheteur
            </div>
            <Field label="Nom" fieldKey="acheteurNom" data={data} onChange={updateField} />
            <Field label="SIREN" fieldKey="acheteurSiren" data={data} onChange={updateField} />
            <Field label="Adresse" fieldKey="acheteurAdresse" data={data} onChange={updateField} />
          </div>
        </div>

        {/* Lignes et totaux */}
        <div>
          <div style={{ border: '1px solid #e5e7eb', borderRadius: '8px', background: '#fff', padding: '16px 18px', marginBottom: '12px' }}>
            <div style={{ fontSize: '12px', fontWeight: 600, color: '#374151', marginBottom: '12px', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
              Lignes
            </div>
            {lignes.length === 0 && (
			<p style={{ margin: 0, fontSize: '12.5px', color: '#9ca3af' }}>
				No invoice lines detected automatically — add them manually.
			</p>
			)}

			{lignes.length > 0 && (
			<table
				style={{
				width: '100%',
				borderCollapse: 'collapse',
				fontSize: '12px',
				}}
			>
				<thead>
				<tr style={{ background: '#f9fafb' }}>
					{['Description', 'Qty', 'Unit price', 'Total', 'Actions'].map((heading) => (
					<th
						key={heading}
						style={{
						padding: '6px 8px',
						textAlign: 'left',
						fontWeight: 500,
						color: '#6b7280',
						borderBottom: '1px solid #e5e7eb',
						}}
					>
						{heading}
					</th>
					))}
				</tr>
				</thead>

				<tbody>
				{lignes.map((line, index) => (
					<tr key={index} style={{ borderBottom: '1px solid #f3f4f6' }}>
					<td style={{ padding: '5px 6px' }}>
						<input
						value={line.description}
						onChange={(e) =>
							updateLigne(index, 'description', e.target.value)
						}
						aria-label={`Description for line ${index + 1}`}
						style={{
							width: '100%',
							border: '1px solid #e5e7eb',
							borderRadius: '4px',
							padding: '3px 5px',
							fontSize: '11.5px',
							boxSizing: 'border-box',
						}}
						/>
					</td>

					<td style={{ padding: '5px 6px', width: '55px' }}>
						<input
						type="number"
						min="0.01"
						step="any"
						value={line.qty}
						onChange={(e) => updateLigne(index, 'qty', e.target.value)}
						aria-label={`Quantity for line ${index + 1}`}
						style={{
							width: '100%',
							border: '1px solid #e5e7eb',
							borderRadius: '4px',
							padding: '3px 5px',
							fontSize: '11.5px',
							textAlign: 'right',
							boxSizing: 'border-box',
						}}
						/>
					</td>

					<td style={{ padding: '5px 6px', width: '85px' }}>
						<input
						type="number"
						min="0"
						step="any"
						value={line.pu}
						onChange={(e) =>
							updateLigne(index, 'pu', e.target.value)
						}
						aria-label={`Unit price for line ${index + 1}`}
						style={{
							width: '100%',
							border: '1px solid #e5e7eb',
							borderRadius: '4px',
							padding: '3px 5px',
							fontSize: '11.5px',
							textAlign: 'right',
							boxSizing: 'border-box',
						}}
						/>
					</td>

					<td
						style={{
						padding: '5px 6px 5px 10px',
						fontFamily: 'monospace',
						color: '#374151',
						whiteSpace: 'nowrap',
						}}
					>
						{toNumber(line.total).toFixed(2)} €
					</td>

					<td style={{ padding: '5px 6px', textAlign: 'center' }}>
						<button
						type="button"
						onClick={() => removeLigne(index)}
						aria-label={`Delete line ${index + 1}`}
						style={{
							border: 'none',
							background: 'transparent',
							color: '#dc2626',
							cursor: 'pointer',
							fontSize: '16px',
						}}
						>
						×
						</button>
					</td>
					</tr>
				))}
				</tbody>
			</table>
			)}

			<button
			type="button"
			onClick={addLigne}
			style={{
				marginTop: '8px',
				padding: '7px 12px',
				border: '1px solid #d1d5db',
				borderRadius: '6px',
				background: '#fff',
				cursor: 'pointer',
			}}
			>
			+ Add line
			</button>
            <div style={{ marginTop: '10px', borderTop: '1px solid #e5e7eb', paddingTop: '10px' }}>
				<div style={{
					display: 'flex',
					justifyContent: 'space-between',
					alignItems: 'center',
					fontSize: '12.5px',
					padding: '4px 0 8px',
					}}>
					<label htmlFor="vat-rate">TVA (%)</label>
					<input
						id="vat-rate"
						type="number"
						min="0"
						max="100"
						step="0.01"
						value={data.tauxTva}
						onChange={e => updateVatRate(e.target.value)}
						style={{
						width: '75px',
						padding: '4px 6px',
						border: '1px solid #d1d5db',
						borderRadius: '4px',
						textAlign: 'right',
						}}
					/>
				</div>
              {[
                ['Total HT', `${totals.totalHT.toFixed(2)} €`],
                [`TVA ${data.tauxTva}%`, `${totals.totalVAT.toFixed(2)} €`],
                ['Total TTC', `${totals.totalTTC.toFixed(2)} €`],
              ].map(([l, v], i) => (
                <div key={l} style={{ display: 'flex', justifyContent: 'space-between', fontSize: i === 2 ? '13px' : '12.5px', fontWeight: i === 2 ? 700 : 400, color: i === 2 ? '#1a1a2e' : '#6b7280', padding: '3px 0' }}>
                  <span>{l}</span>
                  <span className="mono">{v}</span>
                </div>
              ))}
            </div>
          </div>

          <button
            onClick={handleSave}
            disabled={saving}
            style={{
              width: '100%', padding: '11px',
              background: '#1a2744', color: '#fff',
              border: 'none', borderRadius: '7px',
              fontSize: '14px', fontWeight: 600, cursor: 'pointer',
            }}
          >
            {saving ? 'Enregistrement…' : 'Enregistrer les corrections'}
          </button>
		  {saveMessage && (
			<p
				role="status"
				style={{
				marginTop: '10px',
				fontSize: '13px',
				color: saveMessage.includes('succès') ? '#15803d' : '#b91c1c',
				}}
			>
				{saveMessage}
			</p>
		)}
        </div>
      </div>
    </div>
  );
}
