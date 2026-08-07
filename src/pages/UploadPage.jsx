import { useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Upload } from 'lucide-react';

export default function UploadPage({ mode }) {
  const navigate = useNavigate();
  const fileRef = useRef();
  const [dragging, setDragging] = useState(false);

  const isVerif = mode === 'verifier';

  const go = () => navigate(`/traitement?action=${mode}`);

  return (
    <div style={{ padding: '24px 28px', maxWidth: '600px' }}>
      <h1 style={{ margin: '0 0 6px', fontSize: '19px', fontWeight: 700, color: '#1a1a2e' }}>
        {isVerif ? 'Vérifier la conformité' : 'Convertir en Factur-X'}
      </h1>
      <p style={{ margin: '0 0 24px', fontSize: '13px', color: '#6b7280' }}>
        {isVerif
          ? 'Déposez une facture PDF ou XML pour analyser sa conformité Factur-X (EN 16931).'
          : 'Déposez un PDF de facture pour en extraire les données et générer un fichier Factur-X.'}
      </p>

      <div
        onDragOver={e => { e.preventDefault(); setDragging(true); }}
        onDragLeave={() => setDragging(false)}
        onDrop={e => { e.preventDefault(); setDragging(false); go(); }}
        onClick={() => { fileRef.current?.click(); go(); }}
        style={{
          border: `2px dashed ${dragging ? '#4a9eff' : '#d1d5db'}`,
          borderRadius: '10px', padding: '60px 24px',
          textAlign: 'center', cursor: 'pointer',
          background: dragging ? '#f0f7ff' : '#fafafa',
          transition: 'all 0.15s',
        }}
      >
        <input ref={fileRef} type="file" accept=".pdf,.xml" style={{ display: 'none' }} />
        <div style={{ display: 'flex', justifyContent: 'center', marginBottom: '14px' }}>
          <div style={{ background: '#eef2ff', borderRadius: '50%', padding: '16px', display: 'inline-flex' }}>
            <Upload size={28} color="#4a9eff" />
          </div>
        </div>
        <p style={{ margin: '0 0 6px', fontWeight: 600, fontSize: '15px', color: '#1a1a2e' }}>
          Déposez votre fichier ici
        </p>
        <p style={{ margin: '0 0 16px', fontSize: '12.5px', color: '#9ca3af' }}>
          PDF ou XML — jusqu'à 10 Mo
        </p>
        <button
          style={{
            padding: '8px 20px',
            background: '#1a2744', color: '#fff',
            border: 'none', borderRadius: '6px',
            fontSize: '13.5px', fontWeight: 500, cursor: 'pointer',
          }}
        >
          Choisir un fichier
        </button>
      </div>
    </div>
  );
}
