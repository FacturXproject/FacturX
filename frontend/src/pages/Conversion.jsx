import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { conversionData } from '../mockData';

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
  const [data, setData] = useState(conversionData);
  const [lignes, setLignes] = useState(conversionData.lignes);

  const updateField = (key, value) => {
    setData(d => ({ ...d, fields: { ...d.fields, [key]: { ...d.fields[key], value } } }));
  };

  const updateLigne = (i, col, val) => {
    setLignes(ls => ls.map((l, j) => j === i ? { ...l, [col]: val } : l));
  };

  return (
    <div style={{ padding: '24px 28px' }}>
      <div style={{ marginBottom: '20px' }}>
        <button
          onClick={() => navigate('/dashboard')}
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
        {/* Left: PDF preview */}
        <div style={{ border: '1px solid #e5e7eb', borderRadius: '8px', overflow: 'hidden', background: '#fff' }}>
          <div style={{ background: '#f9fafb', borderBottom: '1px solid #e5e7eb', padding: '8px 14px', fontSize: '12px', color: '#6b7280', fontWeight: 500 }}>
            Aperçu du document — FACT-2026-00142.pdf
          </div>
          {/* Faux PDF invoice */}
          <div style={{ padding: '28px 28px', fontFamily: 'inherit' }}>
            {/* Header */}
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '24px', paddingBottom: '16px', borderBottom: '2px solid #1a2744' }}>
              <div>
                <div style={{ fontSize: '18px', fontWeight: 800, color: '#1a2744', letterSpacing: '-0.5px' }}>SARL Dupont Informatique</div>
                <div style={{ fontSize: '11px', color: '#6b7280', lineHeight: '1.6', marginTop: '4px' }}>
                  14 rue des Lilas, 75011 Paris<br />
                  SIREN : 452 891 237<br />
                  TVA : FR45452891237
                </div>
              </div>
              <div style={{ textAlign: 'right' }}>
                <div style={{ fontSize: '13px', fontWeight: 700, color: '#1a2744', background: '#eef2ff', padding: '4px 10px', borderRadius: '4px' }}>FACTURE</div>
                <div style={{ fontSize: '12px', color: '#6b7280', marginTop: '6px', lineHeight: '1.5' }}>
                  N° FACT-2026-00142<br />
                  Date : 28/07/2026<br />
                  Échéance : 27/08/2026
                </div>
              </div>
            </div>

            {/* Client */}
            <div style={{ marginBottom: '20px', padding: '10px 12px', background: '#f9fafb', borderRadius: '6px' }}>
              <div style={{ fontSize: '10px', color: '#9ca3af', textTransform: 'uppercase', letterSpacing: '0.5px', marginBottom: '4px' }}>Facturé à</div>
              <div style={{ fontSize: '12.5px', fontWeight: 600, color: '#1a1a2e' }}>SAS Martin & Associés</div>
              <div style={{ fontSize: '11.5px', color: '#6b7280' }}>8 avenue Foch, 69002 Lyon</div>
              <div style={{ fontSize: '11.5px', color: '#6b7280' }}>SIREN : 789 012 345</div>
            </div>

            {/* Lines table */}
            <table style={{ width: '100%', borderCollapse: 'collapse', marginBottom: '16px', fontSize: '11px' }}>
              <thead>
                <tr style={{ background: '#1a2744', color: '#fff' }}>
                  {['Réf.', 'Description', 'Qté', 'P.U. HT', 'Total HT'].map(h => (
                    <th key={h} style={{ padding: '6px 8px', textAlign: h === 'Description' ? 'left' : 'right', fontWeight: 500 }}>{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {lignes.map((l, i) => (
                  <tr key={i} style={{ borderBottom: '1px solid #f3f4f6' }}>
                    <td style={{ padding: '6px 8px', color: '#6b7280', fontFamily: 'monospace', fontSize: '10.5px' }}>{l.ref}</td>
                    <td style={{ padding: '6px 8px', color: '#1a1a2e' }}>{l.description}</td>
                    <td style={{ padding: '6px 8px', textAlign: 'right', color: '#4b5563' }}>{l.qty} {l.unit}</td>
                    <td style={{ padding: '6px 8px', textAlign: 'right', fontFamily: 'monospace', color: '#4b5563' }}>{l.pu.toFixed(2)} €</td>
                    <td style={{ padding: '6px 8px', textAlign: 'right', fontFamily: 'monospace', fontWeight: 600, color: '#1a1a2e' }}>{l.total.toFixed(2)} €</td>
                  </tr>
                ))}
              </tbody>
            </table>

            {/* Totals */}
            <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
              <div style={{ width: '200px' }}>
                {[
                  ['Total HT', `${data.totalHT.toFixed(2)} €`],
                  [`TVA ${data.tauxTva}%`, `${data.montantTva.toFixed(2)} €`],
                ].map(([l, v]) => (
                  <div key={l} style={{ display: 'flex', justifyContent: 'space-between', padding: '4px 0', fontSize: '12px', color: '#6b7280', borderBottom: '1px solid #f3f4f6' }}>
                    <span>{l}</span>
                    <span className="mono">{v}</span>
                  </div>
                ))}
                <div style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 0 0', fontSize: '13.5px', fontWeight: 700, color: '#1a2744' }}>
                  <span>Total TTC</span>
                  <span className="mono">{data.totalTTC.toFixed(2)} €</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Right: extracted fields */}
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

          <div style={{ border: '1px solid #e5e7eb', borderRadius: '8px', background: '#fff', padding: '16px 18px', marginBottom: '12px' }}>
            <div style={{ fontSize: '12px', fontWeight: 600, color: '#374151', marginBottom: '12px', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
              Lignes
            </div>
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
