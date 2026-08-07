import { useState } from 'react';
import { xmlContent, conversionData } from '../mockData';

function XmlHighlight({ xml }) {
  // Tokenize XML into segments and colorize each
  const tokens = [];
  const re = /(<\/?)([\w:]+)([^>]*)(\/?>)|(<!--[\s\S]*?-->)|("([^"]*)")|([^<>"]+)/g;
  let m;
  while ((m = re.exec(xml)) !== null) {
    if (m[1] != null) {
      // Opening/closing tag
      tokens.push({ type: 'punct', text: m[1] });
      tokens.push({ type: 'tag', text: m[2] });
      // Attributes inside the tag
      const attrs = m[3];
      const attrRe = /\s+([\w:]+)(="([^"]*)")?/g;
      let am;
      while ((am = attrRe.exec(attrs)) !== null) {
        tokens.push({ type: 'text', text: ' ' });
        tokens.push({ type: 'attr', text: am[1] });
        if (am[2]) {
          tokens.push({ type: 'punct', text: '=' });
          tokens.push({ type: 'string', text: `"${am[3]}"` });
        }
      }
      tokens.push({ type: 'punct', text: m[4] });
    } else if (m[5] != null) {
      tokens.push({ type: 'comment', text: m[5] });
    } else if (m[6] != null) {
      tokens.push({ type: 'string', text: m[6] });
    } else if (m[8] != null) {
      tokens.push({ type: 'text', text: m[8] });
    }
  }

  const colors = { tag: '#4a9eff', attr: '#f59e0b', string: '#86efac', punct: '#94a3b8', comment: '#64748b', text: '#e2e8f0' };

  return (
    <pre style={{ margin: 0, fontSize: '12px', lineHeight: '1.6', color: '#e2e8f0', overflow: 'auto', padding: '16px' }}>
      {tokens.map((t, i) => (
        <span key={i} style={{ color: colors[t.type] || '#e2e8f0' }}>{t.text}</span>
      ))}
    </pre>
  );
}

export default function XmlReader() {
  const [view, setView] = useState('lisible');
  const d = conversionData;

  return (
    <div style={{ padding: '24px 28px', maxWidth: '900px' }}>
      <div style={{ marginBottom: '20px', display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '12px' }}>
        <div>
          <h1 style={{ margin: 0, fontSize: '19px', fontWeight: 700, color: '#1a1a2e' }}>Lecture XML</h1>
          <p style={{ margin: '3px 0 0', fontSize: '12.5px', color: '#6b7280' }}>
            <span className="mono">FACT-2026-00139.xml</span>
          </p>
        </div>

        {/* Toggle */}
        <div style={{ display: 'flex', border: '1px solid #d1d5db', borderRadius: '7px', overflow: 'hidden', background: '#fff', flexShrink: 0 }}>
          {['lisible', 'brut'].map(v => (
            <button
              key={v}
              onClick={() => setView(v)}
              style={{
                padding: '7px 16px', border: 'none', cursor: 'pointer',
                fontSize: '13px', fontWeight: view === v ? 600 : 400,
                background: view === v ? '#1a2744' : '#fff',
                color: view === v ? '#fff' : '#4b5563',
              }}
            >
              {v === 'lisible' ? 'Vue lisible' : 'XML brut'}
            </button>
          ))}
        </div>
      </div>

      {view === 'lisible' ? (
        <div style={{ background: '#fff', border: '1px solid #e5e7eb', borderRadius: '10px', overflow: 'hidden' }}>
          {/* Invoice header */}
          <div style={{ background: '#1a2744', color: '#fff', padding: '20px 24px', display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
            <div>
              <div style={{ fontSize: '20px', fontWeight: 700, letterSpacing: '-0.4px' }}>SARL Dupont Informatique</div>
              <div style={{ fontSize: '12px', color: 'rgba(255,255,255,0.6)', lineHeight: '1.7', marginTop: '6px' }}>
                14 rue des Lilas, 75011 Paris<br />
                SIREN : 452 891 237 · TVA : FR45452891237
              </div>
            </div>
            <div style={{ textAlign: 'right' }}>
              <div style={{ fontSize: '11px', color: 'rgba(255,255,255,0.5)', textTransform: 'uppercase', letterSpacing: '1px' }}>Facture</div>
              <div style={{ fontSize: '18px', fontWeight: 700, fontFamily: 'monospace' }}>FACT-2026-00139</div>
              <div style={{ fontSize: '12px', color: 'rgba(255,255,255,0.6)', marginTop: '4px' }}>25 juillet 2026</div>
            </div>
          </div>

          <div style={{ padding: '20px 24px' }}>
            {/* Parties */}
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px', marginBottom: '20px' }}>
              {[
                { title: 'Vendeur', nom: 'SARL Dupont Informatique', addr: '14 rue des Lilas, 75011 Paris', siren: '452 891 237' },
                { title: 'Acheteur', nom: 'SAS Martin & Associés', addr: '8 avenue Foch, 69002 Lyon', siren: '789 012 345' },
              ].map(p => (
                <div key={p.title} style={{ background: '#f9fafb', borderRadius: '7px', padding: '14px 16px' }}>
                  <div style={{ fontSize: '11px', color: '#9ca3af', textTransform: 'uppercase', letterSpacing: '0.5px', marginBottom: '6px' }}>{p.title}</div>
                  <div style={{ fontSize: '13.5px', fontWeight: 600, color: '#1a1a2e' }}>{p.nom}</div>
                  <div style={{ fontSize: '12px', color: '#6b7280', marginTop: '3px', lineHeight: '1.5' }}>
                    {p.addr}<br />
                    SIREN : {p.siren}
                  </div>
                </div>
              ))}
            </div>

            {/* Lines */}
            <table style={{ width: '100%', borderCollapse: 'collapse', marginBottom: '16px', fontSize: '13px' }}>
              <thead>
                <tr style={{ background: '#f3f4f6', borderBottom: '2px solid #e5e7eb' }}>
                  {['Réf.', 'Description', 'Qté', 'P.U. HT', 'TVA', 'Total HT'].map(h => (
                    <th key={h} style={{ padding: '8px 12px', textAlign: h === 'Description' ? 'left' : 'right', fontWeight: 500, color: '#6b7280', fontSize: '12px' }}>{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {d.lignes.map((l, i) => (
                  <tr key={i} style={{ borderBottom: '1px solid #f3f4f6' }}>
                    <td style={{ padding: '9px 12px', fontFamily: 'monospace', fontSize: '11.5px', color: '#6b7280' }}>{l.ref}</td>
                    <td style={{ padding: '9px 12px', color: '#1a1a2e' }}>{l.description}</td>
                    <td style={{ padding: '9px 12px', textAlign: 'right', color: '#4b5563' }}>{l.qty} {l.unit}</td>
                    <td style={{ padding: '9px 12px', textAlign: 'right', fontFamily: 'monospace' }}>{l.pu.toFixed(2)} €</td>
                    <td style={{ padding: '9px 12px', textAlign: 'right', color: '#6b7280' }}>{l.tva}%</td>
                    <td style={{ padding: '9px 12px', textAlign: 'right', fontFamily: 'monospace', fontWeight: 600 }}>{l.total.toFixed(2)} €</td>
                  </tr>
                ))}
              </tbody>
            </table>

            {/* Totals */}
            <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
              <div style={{ width: '240px', borderTop: '2px solid #e5e7eb', paddingTop: '10px' }}>
                {[
                  ['Total HT', `${d.totalHT.toFixed(2)} €`, false],
                  [`TVA ${d.tauxTva}%`, `${d.montantTva.toFixed(2)} €`, false],
                  ['Total TTC', `${d.totalTTC.toFixed(2)} €`, true],
                ].map(([l, v, bold]) => (
                  <div key={l} style={{ display: 'flex', justifyContent: 'space-between', padding: '5px 0', fontSize: bold ? '15px' : '13px', fontWeight: bold ? 700 : 400, color: bold ? '#1a1a2e' : '#6b7280', borderTop: bold ? '1px solid #e5e7eb' : 'none', marginTop: bold ? '6px' : 0, paddingTop: bold ? '8px' : '5px' }}>
                    <span>{l}</span>
                    <span className="mono">{v}</span>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      ) : (
        <div style={{ background: '#0f172a', borderRadius: '10px', overflow: 'hidden', border: '1px solid #1e293b' }}>
          <div style={{ background: '#1e293b', padding: '8px 16px', fontSize: '12px', color: '#94a3b8', display: 'flex', justifyContent: 'space-between' }}>
            <span>FACT-2026-00139.xml — Factur-X EN 16931</span>
            <span>{xmlContent.split('\n').length} lignes</span>
          </div>
          <XmlHighlight xml={xmlContent} />
        </div>
      )}
    </div>
  );
}
