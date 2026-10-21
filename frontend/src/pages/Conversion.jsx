import { useEffect, useState } from 'react';
import { useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import { Loader2 } from 'lucide-react';
import api from '../services/api';
import FacturXGenerationPanel from '../components/FacturXGenerationPanel';
import {
  FIELD_API_NAMES,
  buildDraftPayload,
  calculateTotals,
  formatCents,
  formatRate,
  lineTotalCents,
  normalizeText,
  validateDraft,
} from '../utils/invoiceDraft';

// F11: seuil au-dessus duquel un champ extrait est affiché comme fiable.
// En dessous (ou champ non trouvé), il est marqué "à vérifier" (F12).
const CONFIDENCE_THRESHOLD = 0.6;

// F12 : un champ corrigé par l'utilisateur (source "manual") n'est plus "à vérifier",
// quelle que soit la confiance d'origine de l'extracteur.
function confidenceLevel(score, source) {
  if (source === 'manual') return 'manual';
  return score != null && score >= CONFIDENCE_THRESHOLD ? 'high' : 'low';
}

const CONFIDENCE_STYLES = {
  high: { color: '#16a34a', title: 'Confiance élevée' },
  low: { color: '#f59e0b', title: 'À vérifier' },
  manual: { color: '#2563eb', title: 'Corrigé manuellement' },
};

let nextLineId = 0;

// Traduit le brouillon renvoyé par GET/PUT /documents/{id}/draft (champs en anglais,
// voir ExtractionController) vers la forme du formulaire (mockData.conversionData).
// Les montants restent en texte : ils sont validés et calculés par utils/invoiceDraft.
function toViewModel(draft) {
  const f = draft.fields || {};
  const fields = {};
  for (const [key, apiName] of Object.entries(FIELD_API_NAMES)) {
    fields[key] = {
      value: f[apiName]?.value ?? '',
      confidence: confidenceLevel(f[apiName]?.confidence, f[apiName]?.source),
    };
  }

  const lignes = (draft.lines || []).map((l) => ({
    id: nextLineId++,
    // Un <input> supprime les sauts de ligne : sans cela les mots d'une description
    // sur deux lignes (brouillons extraits avant la correction côté extracteur) se collent.
    description: normalizeText(l.description),
    qty: normalizeText(l.quantity),
    pu: normalizeText(l.unitPrice),
    tva: normalizeText(l.vatRate),
    confidence: confidenceLevel(l.confidence, l.source),
  }));

  return {
    fields,
    lignes,
    tauxTva: normalizeText(f.vatRate?.value).replace(/\s*%$/, ''),
    tauxTvaConfidence: confidenceLevel(f.vatRate?.confidence, f.vatRate?.source),
    updatedAt: draft.updatedAt ?? null,
    // F13 : enregistré au moins une fois depuis ce formulaire (même règle que le serveur).
    humanChecked: Boolean(draft.updatedAt && draft.createdAt && new Date(draft.updatedAt) > new Date(draft.createdAt)),
  };
}

function ConfidenceDot({ level }) {
  const style = CONFIDENCE_STYLES[level] ?? CONFIDENCE_STYLES.low;
  return (
    <span
      title={style.title}
      aria-label={style.title}
      style={{
        display: 'inline-block', width: '8px', height: '8px',
        borderRadius: '50%',
        background: style.color,
        marginRight: '6px', flexShrink: 0,
      }}
    />
  );
}

function inputBorder(hasError, confidence) {
  if (hasError) return '#dc2626';
  return confidence === 'low' ? '#fcd34d' : '#d1d5db';
}

function Field({ label, fieldKey, data, onChange, error, required }) {
  const f = data.fields[fieldKey];
  const id = `field-${fieldKey}`;
  return (
    <div style={{ marginBottom: '12px' }}>
      <label htmlFor={id} style={{ display: 'flex', alignItems: 'center', fontSize: '11.5px', fontWeight: 500, color: '#6b7280', marginBottom: '4px' }}>
        <ConfidenceDot level={f.confidence} />
        {label}{required && <span style={{ color: '#dc2626', marginLeft: '2px' }}>*</span>}
      </label>
      <input
        id={id}
        type="text"
        value={f.value}
        onChange={e => onChange(fieldKey, e.target.value)}
        aria-invalid={error ? 'true' : undefined}
        style={{
          width: '100%', padding: '7px 9px',
          border: `1px solid ${inputBorder(error, f.confidence)}`,
          borderRadius: '5px', fontSize: '13px', color: '#1a1a2e',
          background: f.confidence === 'low' ? '#fffdf0' : '#fff',
          outline: 'none', boxSizing: 'border-box',
        }}
      />
      {error && <div style={{ marginTop: '3px', fontSize: '11.5px', color: '#b91c1c' }}>{error}</div>}
    </div>
  );
}

const cellInputStyle = (hasError, extra = {}) => ({
  width: '100%',
  border: `1px solid ${hasError ? '#dc2626' : '#e5e7eb'}`,
  borderRadius: '4px',
  padding: '3px 5px',
  fontSize: '11.5px',
  color: '#1a1a2e',
  boxSizing: 'border-box',
  ...extra,
});

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
  // { type: 'success' | 'error', text, details: [] } ; affiché sous le bouton.
  const [saveStatus, setSaveStatus] = useState(null);
  // Erreurs de validation locales, par clé de champ ('numeroFacture', 'line:0:qty', ...).
  const [fieldErrors, setFieldErrors] = useState({});
  const [dirty, setDirty] = useState(false);

  const applyDraft = (draft) => {
    const vm = toViewModel(draft || {});
    setData(vm);
    setLignes(vm.lignes);
    setFieldErrors({});
    setDirty(false);
  };

  useEffect(() => {
    if (!documentId) {
      setLoading(false);
      setError("Aucun document à convertir.");
      return;
    }

    setLoading(true);
    setError(null);

    api.get(`/documents/${documentId}/draft`)
      .then((response) => applyDraft(response.data))
      .catch((err) => {
        console.error('Draft error:', err);
        setError(err.response?.data?.message || "Impossible de charger les données extraites.");
      })
      .finally(() => setLoading(false));
  }, [documentId]);

  // Avertit avant de quitter la page avec des corrections non enregistrées.
  useEffect(() => {
    if (!dirty) return undefined;
    const warn = (e) => { e.preventDefault(); e.returnValue = ''; };
    window.addEventListener('beforeunload', warn);
    return () => window.removeEventListener('beforeunload', warn);
  }, [dirty]);

  const touch = () => {
    setDirty(true);
    setSaveStatus(null);
  };

  const updateField = (key, value) => {
    touch();
    setData(d => ({ ...d, fields: { ...d.fields, [key]: { value, confidence: 'manual' } } }));
  };

  const updateVatRate = (value) => {
    touch();
    setData(d => ({ ...d, tauxTva: value, tauxTvaConfidence: 'manual' }));
  };

  const updateLigne = (i, col, val) => {
    touch();
    setLignes(ls => ls.map((l, j) => (j === i ? { ...l, [col]: val, confidence: 'manual' } : l)));
  };

  const addLigne = () => {
    touch();
    setLignes(ls => [
      ...ls,
      { id: nextLineId++, description: '', qty: '1', pu: '', tva: '', confidence: 'manual' },
    ]);
  };

  const removeLigne = (indexToRemove) => {
    touch();
    setLignes(ls => ls.filter((_, index) => index !== indexToRemove));
  };

  const handleSave = async () => {
    if (!data || !documentId) return;

    const errors = validateDraft(data.fields, data.tauxTva, lignes);
    if (errors.length > 0) {
      const byKey = {};
      for (const { key, message } of errors) byKey[key] ??= message;
      setFieldErrors(byKey);
      setSaveStatus({
        type: 'error',
        text: 'Corrigez les erreurs avant d’enregistrer :',
        details: errors.map(e => e.message),
      });
      return;
    }

    setFieldErrors({});
    setSaving(true);
    setSaveStatus(null);

    try {
      const response = await api.put(
        `/documents/${documentId}/draft`,
        buildDraftPayload(data.fields, data.tauxTva, lignes, data.updatedAt),
      );
      // Le serveur renvoie le brouillon recalculé : c'est lui qui fait foi.
      applyDraft(response.data);
      setSaveStatus({ type: 'success', text: 'Corrections enregistrées avec succès.', details: [] });
    } catch (err) {
      console.error('Save draft error:', err);
      const body = err.response?.data;
      setSaveStatus({
        type: 'error',
        text: body?.errors?.length
          ? 'Le serveur a refusé les corrections :'
          : body?.message || "Impossible d'enregistrer les corrections.",
        details: body?.errors ?? [],
      });
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

  const totals = calculateTotals(lignes, data.tauxTva);
  const fieldProps = (fieldKey) => ({
    fieldKey, data, onChange: updateField, error: fieldErrors[fieldKey],
  });

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
          Vérifiez et corrigez les champs extraits, enregistrez le brouillon, puis générez la facture Factur-X.
          <span style={{ marginLeft: '8px', color: '#f59e0b', fontWeight: 500 }}>● À vérifier</span>
          <span style={{ marginLeft: '8px', color: '#16a34a', fontWeight: 500 }}>● Confiance élevée</span>
          <span style={{ marginLeft: '8px', color: '#2563eb', fontWeight: 500 }}>● Corrigé manuellement</span>
        </p>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px', alignItems: 'start' }}>
        {/* Champs extraits */}
        <div>
          <div style={{ border: '1px solid #e5e7eb', borderRadius: '8px', background: '#fff', padding: '16px 18px', marginBottom: '12px' }}>
            <div style={{ fontSize: '12px', fontWeight: 600, color: '#374151', marginBottom: '12px', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
              Identification
            </div>
            <Field label="Numéro de facture" required {...fieldProps('numeroFacture')} />
            <Field label="Date de facture (JJ/MM/AAAA)" required {...fieldProps('dateFacture')} />
            <Field label="Date d'échéance (JJ/MM/AAAA)" {...fieldProps('dateEcheance')} />
          </div>

          <div style={{ border: '1px solid #e5e7eb', borderRadius: '8px', background: '#fff', padding: '16px 18px', marginBottom: '12px' }}>
            <div style={{ fontSize: '12px', fontWeight: 600, color: '#374151', marginBottom: '12px', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
              Vendeur
            </div>
            <Field label="Nom" required {...fieldProps('vendeurNom')} />
            <Field label="SIREN" {...fieldProps('vendeurSiren')} />
            <Field label="N° TVA intracommunautaire" {...fieldProps('vendeurTva')} />
            <Field label="Adresse" {...fieldProps('vendeurAdresse')} />
          </div>

          <div style={{ border: '1px solid #e5e7eb', borderRadius: '8px', background: '#fff', padding: '16px 18px', marginBottom: '12px' }}>
            <div style={{ fontSize: '12px', fontWeight: 600, color: '#374151', marginBottom: '12px', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
              Acheteur
            </div>
            <Field label="Nom" required {...fieldProps('acheteurNom')} />
            <Field label="SIREN" {...fieldProps('acheteurSiren')} />
            <Field label="Adresse" {...fieldProps('acheteurAdresse')} />
          </div>
        </div>

        {/* Lignes et totaux */}
        <div>
          <div style={{ border: '1px solid #e5e7eb', borderRadius: '8px', background: '#fff', padding: '16px 18px', marginBottom: '12px' }}>
            <div style={{ fontSize: '12px', fontWeight: 600, color: '#374151', marginBottom: '12px', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
              Lignes
            </div>
            {lignes.length === 0 && (
              <p style={{ margin: 0, fontSize: '12.5px', color: fieldErrors.lines ? '#b91c1c' : '#9ca3af' }}>
                Aucune ligne détectée automatiquement — ajoutez-les manuellement.
              </p>
            )}
            {lignes.length > 0 && (
              <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '12px' }}>
                <thead>
                  <tr style={{ background: '#f9fafb' }}>
                    {['Description', 'Qté', 'P.U. HT', 'TVA %', 'Total HT', ''].map(h => (
                      <th key={h} style={{ padding: '6px 8px', textAlign: 'left', fontWeight: 500, color: '#6b7280', borderBottom: '1px solid #e5e7eb' }}>{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {lignes.map((l, i) => {
                    const total = lineTotalCents(l);
                    return (
                      <tr key={l.id} style={{ borderBottom: '1px solid #f3f4f6' }}>
                        <td style={{ padding: '5px 6px' }}>
                          <div style={{ display: 'flex', alignItems: 'center' }}>
                            <ConfidenceDot level={l.confidence} />
                            <input value={l.description} onChange={e => updateLigne(i, 'description', e.target.value)}
                              aria-label={`Description de la ligne ${i + 1}`}
                              aria-invalid={fieldErrors[`line:${i}:description`] ? 'true' : undefined}
                              style={cellInputStyle(fieldErrors[`line:${i}:description`])} />
                          </div>
                        </td>
                        <td style={{ padding: '5px 6px', width: '55px' }}>
                          <input value={l.qty} inputMode="decimal" onChange={e => updateLigne(i, 'qty', e.target.value)}
                            aria-label={`Quantité de la ligne ${i + 1}`}
                            aria-invalid={fieldErrors[`line:${i}:qty`] ? 'true' : undefined}
                            style={cellInputStyle(fieldErrors[`line:${i}:qty`], { textAlign: 'right' })} />
                        </td>
                        <td style={{ padding: '5px 6px', width: '80px' }}>
                          <input value={l.pu} inputMode="decimal" onChange={e => updateLigne(i, 'pu', e.target.value)}
                            aria-label={`Prix unitaire de la ligne ${i + 1}`}
                            aria-invalid={fieldErrors[`line:${i}:pu`] ? 'true' : undefined}
                            style={cellInputStyle(fieldErrors[`line:${i}:pu`], { textAlign: 'right', fontFamily: 'monospace' })} />
                        </td>
                        <td style={{ padding: '5px 6px', width: '55px' }}>
                          <input value={l.tva} inputMode="decimal" onChange={e => updateLigne(i, 'tva', e.target.value)}
                            placeholder={data.tauxTva}
                            title="Vide : taux de TVA par défaut de la facture"
                            aria-label={`Taux de TVA de la ligne ${i + 1}`}
                            aria-invalid={fieldErrors[`line:${i}:tva`] ? 'true' : undefined}
                            style={cellInputStyle(fieldErrors[`line:${i}:tva`], { textAlign: 'right' })} />
                        </td>
                        <td style={{ padding: '5px 6px 5px 10px', fontFamily: 'monospace', color: '#374151', fontSize: '11.5px', whiteSpace: 'nowrap' }}>
                          {total == null ? '—' : formatCents(total)}
                        </td>
                        <td style={{ padding: '5px 2px', textAlign: 'center' }}>
                          <button
                            type="button"
                            onClick={() => removeLigne(i)}
                            aria-label={`Supprimer la ligne ${i + 1}`}
                            title="Supprimer la ligne"
                            style={{ border: 'none', background: 'transparent', color: '#dc2626', cursor: 'pointer', fontSize: '16px', lineHeight: 1 }}
                          >
                            ×
                          </button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            )}

            <button
              type="button"
              onClick={addLigne}
              style={{ marginTop: '8px', padding: '6px 12px', border: '1px solid #d1d5db', borderRadius: '6px', background: '#fff', fontSize: '12.5px', cursor: 'pointer' }}
            >
              + Ajouter une ligne
            </button>

            <div style={{ marginTop: '10px', borderTop: '1px solid #e5e7eb', paddingTop: '10px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: '12.5px', color: '#6b7280', padding: '4px 0 8px' }}>
                <label htmlFor="vat-rate" style={{ display: 'flex', alignItems: 'center' }}>
                  <ConfidenceDot level={data.tauxTvaConfidence} />
                  Taux de TVA par défaut (%)<span style={{ color: '#dc2626', marginLeft: '2px' }}>*</span>
                </label>
                <input
                  id="vat-rate"
                  inputMode="decimal"
                  value={data.tauxTva}
                  onChange={e => updateVatRate(e.target.value)}
                  aria-invalid={fieldErrors.tauxTva ? 'true' : undefined}
                  style={{ width: '75px', padding: '4px 6px', border: `1px solid ${fieldErrors.tauxTva ? '#dc2626' : '#d1d5db'}`, borderRadius: '4px', textAlign: 'right' }}
                />
              </div>
              {fieldErrors.tauxTva && (
                <div style={{ marginTop: '-4px', marginBottom: '6px', fontSize: '11.5px', color: '#b91c1c', textAlign: 'right' }}>{fieldErrors.tauxTva}</div>
              )}
              {[
                ['Total HT', formatCents(totals.totalHT)],
                ...totals.taxes.map(t => [
                  `TVA ${formatRate(t.rate)} % sur ${formatCents(t.baseCents)}`,
                  formatCents(t.taxCents),
                ]),
                ['Total TTC', formatCents(totals.totalTTC)],
              ].map(([l, v], i, rows) => {
                const last = i === rows.length - 1;
                return (
                  <div key={l} style={{ display: 'flex', justifyContent: 'space-between', fontSize: last ? '13px' : '12.5px', fontWeight: last ? 700 : 400, color: last ? '#1a1a2e' : '#6b7280', padding: '3px 0' }}>
                    <span>{l}</span>
                    <span className="mono">{v}</span>
                  </div>
                );
              })}
            </div>
          </div>

          <button
            onClick={handleSave}
            disabled={saving}
            style={{
              width: '100%', padding: '11px',
              background: '#1a2744', color: '#fff',
              border: 'none', borderRadius: '7px',
              fontSize: '14px', fontWeight: 600, cursor: saving ? 'wait' : 'pointer',
              opacity: saving ? 0.7 : 1,
            }}
          >
            {saving ? 'Enregistrement…' : 'Enregistrer les corrections'}
          </button>
          {dirty && !saveStatus && (
            <p style={{ margin: '8px 0 0', fontSize: '12px', color: '#6b7280' }}>
              Modifications non enregistrées.
            </p>
          )}
          {saveStatus && (
            <div
              role={saveStatus.type === 'error' ? 'alert' : 'status'}
              style={{ marginTop: '10px', fontSize: '13px', color: saveStatus.type === 'success' ? '#15803d' : '#b91c1c' }}
            >
              {saveStatus.text}
              {saveStatus.details.length > 0 && (
                <ul style={{ margin: '6px 0 0', paddingLeft: '18px', fontSize: '12.5px' }}>
                  {saveStatus.details.map(d => <li key={d}>{d}</li>)}
                </ul>
              )}
            </div>
          )}

          <FacturXGenerationPanel
            documentId={documentId}
            blockedReason={
              dirty ? 'Enregistrez d\u2019abord vos corrections avant de générer.'
                : !data.humanChecked ? 'Vérifiez puis enregistrez le brouillon au moins une fois avant de générer : une facture n\u2019est jamais générée à partir de l\u2019extraction seule.'
                : null
            }
          />
        </div>
      </div>
    </div>
  );
}
