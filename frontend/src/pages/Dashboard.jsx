import { useState, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { Upload, CheckCircle, RefreshCw } from 'lucide-react';
import { recentDocuments } from '../mockData';

function StatusBadge({ status, label }) {
  const colors = {
    conforme: { bg: '#f0fdf4', color: '#166534', border: '#bbf7d0' },
    erreurs:  { bg: '#fef2f2', color: '#991b1b', border: '#fecaca' },
    converti: { bg: '#eff6ff', color: '#1d4ed8', border: '#bfdbfe' },
  };
  const c = colors[status] || colors.conforme;
  return (
    <span style={{
      display: 'inline-block',
      padding: '2px 8px',
      borderRadius: '4px',
      fontSize: '12px',
      fontWeight: 500,
      background: c.bg,
      color: c.color,
      border: `1px solid ${c.border}`,
    }}>
      {label}
    </span>
  );
}

export default function Dashboard({ onFileSelect }) {
  const navigate = useNavigate();
  const [dragging, setDragging] = useState(false);
  const fileRef = useRef();

  const handleAction = (type) => {
    if (onFileSelect) onFileSelect('FACT-2026-00142.pdf');
    navigate(`/traitement?action=${type}`);
  };

  const handleRowClick = (doc) => {
    if (doc.reportId) {
      if (onFileSelect) onFileSelect(doc.filename);
      navigate(`/rapport?type=${doc.reportId}`);
    } else {
      if (onFileSelect) onFileSelect(doc.filename);
      navigate('/conversion');
    }
  };

  return (
    <div style={{ padding: '24px 28px', maxWidth: '900px' }}>
      <div style={{ marginBottom: '24px' }}>
        <h1 style={{ margin: 0, fontSize: '20px', fontWeight: 700, color: '#1a1a2e', letterSpacing: '-0.3px' }}>
          Tableau de bord
        </h1>
        <p style={{ margin: '4px 0 0', fontSize: '13px', color: '#6b7280' }}>
          Déposez une facture pour la vérifier ou la convertir en Factur-X
        </p>
      </div>

      {/* Drop zone */}
      <div
        onDragOver={e => { e.preventDefault(); setDragging(true); }}
        onDragLeave={() => setDragging(false)}
        onDrop={e => { e.preventDefault(); setDragging(false); }}
        onClick={() => fileRef.current?.click()}
        style={{
          border: `2px dashed ${dragging ? '#4a9eff' : '#d1d5db'}`,
          borderRadius: '10px',
          padding: '36px 24px',
          textAlign: 'center',
          background: dragging ? '#f0f7ff' : '#fafafa',
          cursor: 'pointer',
          transition: 'all 0.15s',
          marginBottom: '16px',
        }}
      >
        <input ref={fileRef} type="file" accept=".pdf,.xml" style={{ display: 'none' }} />
        <div style={{ display: 'flex', justifyContent: 'center', marginBottom: '12px' }}>
          <div style={{ background: '#eef2ff', borderRadius: '50%', padding: '14px', display: 'inline-flex' }}>
            <Upload size={24} color="#4a9eff" />
          </div>
        </div>
        <p style={{ margin: '0 0 4px', fontWeight: 600, fontSize: '14px', color: '#1a1a2e' }}>
          Déposez une facture ici
        </p>
        <p style={{ margin: 0, fontSize: '12.5px', color: '#9ca3af' }}>
          PDF ou XML — jusqu'à 10 Mo
        </p>
      </div>

      {/* Action buttons */}
      <div style={{ display: 'flex', gap: '12px', marginBottom: '32px' }}>
        <button
          onClick={() => handleAction('verifier')}
          style={{
            display: 'flex', alignItems: 'center', gap: '8px',
            padding: '9px 18px',
            background: '#1a2744', color: '#fff',
            border: 'none', borderRadius: '6px',
            fontSize: '13.5px', fontWeight: 500, cursor: 'pointer',
          }}
        >
          <CheckCircle size={15} />
          Vérifier la conformité
        </button>
        <button
          onClick={() => handleAction('convertir')}
          style={{
            display: 'flex', alignItems: 'center', gap: '8px',
            padding: '9px 18px',
            background: '#fff', color: '#1a2744',
            border: '1px solid #d1d5db', borderRadius: '6px',
            fontSize: '13.5px', fontWeight: 500, cursor: 'pointer',
          }}
        >
          <RefreshCw size={15} />
          Convertir en Factur-X
        </button>
      </div>

      {/* Recent documents table */}
      <div>
        <h2 style={{ margin: '0 0 12px', fontSize: '14px', fontWeight: 600, color: '#1a1a2e' }}>
          Documents récents
        </h2>
        <div style={{ border: '1px solid #e5e7eb', borderRadius: '8px', overflow: 'hidden', background: '#fff' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '13px' }}>
            <thead>
              <tr style={{ background: '#f9fafb', borderBottom: '1px solid #e5e7eb' }}>
                {['Fichier', 'Date', 'Action', 'Statut'].map(h => (
                  <th key={h} style={{ padding: '9px 14px', textAlign: 'left', fontWeight: 500, color: '#6b7280', fontSize: '12px' }}>
                    {h}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {recentDocuments.map((doc, i) => (
                <tr
                  key={doc.id}
                  onClick={() => handleRowClick(doc)}
                  style={{
                    borderBottom: i < recentDocuments.length - 1 ? '1px solid #f3f4f6' : 'none',
                    cursor: 'pointer',
                    transition: 'background 0.1s',
                  }}
                  onMouseEnter={e => e.currentTarget.style.background = '#f9fafb'}
                  onMouseLeave={e => e.currentTarget.style.background = 'transparent'}
                >
                  <td style={{ padding: '10px 14px', color: '#1a1a2e', fontWeight: 450 }}>
                    <span className="mono" style={{ fontSize: '12.5px' }}>{doc.filename}</span>
                  </td>
                  <td style={{ padding: '10px 14px', color: '#6b7280' }}>{doc.date}</td>
                  <td style={{ padding: '10px 14px', color: '#4b5563' }}>{doc.action}</td>
                  <td style={{ padding: '10px 14px' }}>
                    <StatusBadge status={doc.status} label={doc.statusLabel} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
