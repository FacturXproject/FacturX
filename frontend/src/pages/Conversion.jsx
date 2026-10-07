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
    description: l.description ?? '',
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

  const updateLigne = (i, col, val) => {
    setLignes(ls => ls.map((l, j) => j === i ? { ...l, [col]: val } : l));
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
                Aucune ligne détectée automatiquement — ajoutez-les manuellement.
              </p>
            )}
            {lignes.length > 0 && (
              <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '12px' }}>
                <thead>
                  <tr style={{ background: '#f9fafb' }}>
                    {['Description', 'Qté', 'P.U.', 'Total'].map(h => (
                      <th key={h} style={{ padding: '6px 8px', textAlign: 'left', fontWeight: 500, color: '#6b7280', borderBottom: '1px solid #e5e7eb' }}>{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {lignes.map((l, i) => (
                    <tr key={i} style={{ borderBottom: '1px solid #f3f4f6' }}>
                      <td style={{ padding: '5px 6px' }}>
                        <input value={l.description} onChange={e => updateLigne(i, 'description', e.target.value)}
                          style={{ width: '100%', border: '1px solid #e5e7eb', borderRadius: '4px', padding: '3px 5px', fontSize: '11.5px', color: '#1a1a2e' }} />
                      </td>
                      <td style={{ padding: '5px 6px', width: '40px' }}>
                        <input value={l.qty} onChange={e => updateLigne(i, 'qty', e.target.value)}
                          style={{ width: '100%', border: '1px solid #e5e7eb', borderRadius: '4px', padding: '3px 5px', fontSize: '11.5px', textAlign: 'right' }} />
                      </td>
                      <td style={{ padding: '5px 6px', width: '70px' }}>
                        <input value={l.pu} onChange={e => updateLigne(i, 'pu', e.target.value)}
                          style={{ width: '100%', border: '1px solid #e5e7eb', borderRadius: '4px', padding: '3px 5px', fontSize: '11.5px', textAlign: 'right', fontFamily: 'monospace' }} />
                      </td>
                      <td style={{ padding: '5px 6px 5px 10px', fontFamily: 'monospace', color: '#374151', fontSize: '11.5px', whiteSpace: 'nowrap' }}>
                        {(l.total || 0).toFixed(2)} €
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}

            <div style={{ marginTop: '10px', borderTop: '1px solid #e5e7eb', paddingTop: '10px' }}>
              {[
                ['Total HT', `${data.totalHT.toFixed(2)} €`],
                [`TVA ${data.tauxTva}%`, `${data.montantTva.toFixed(2)} €`],
                ['Total TTC', `${data.totalTTC.toFixed(2)} €`],
              ].map(([l, v], i) => (
                <div key={l} style={{ display: 'flex', justifyContent: 'space-between', fontSize: i === 2 ? '13px' : '12.5px', fontWeight: i === 2 ? 700 : 400, color: i === 2 ? '#1a1a2e' : '#6b7280', padding: '3px 0' }}>
                  <span>{l}</span>
                  <span className="mono">{v}</span>
                </div>
              ))}
            </div>
          </div>

          <button
            onClick={() => navigate('/succes')}
            style={{
              width: '100%', padding: '11px',
              background: '#1a2744', color: '#fff',
              border: 'none', borderRadius: '7px',
              fontSize: '14px', fontWeight: 600, cursor: 'pointer',
            }}
          >
            Générer la facture Factur-X
          </button>
        </div>
      </div>
    </div>
  );
}
