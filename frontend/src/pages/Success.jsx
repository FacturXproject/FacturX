import { useNavigate } from 'react-router-dom';
import { CheckCircle2, Download, ShieldCheck, LayoutDashboard } from 'lucide-react';

export default function Success() {
  const navigate = useNavigate();

  return (
    <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '24px' }}>
      <div style={{ textAlign: 'center', maxWidth: '420px' }}>
        <div style={{ display: 'flex', justifyContent: 'center', marginBottom: '20px' }}>
          <div style={{ background: '#f0fdf4', borderRadius: '50%', padding: '18px', display: 'inline-flex' }}>
            <CheckCircle2 size={40} color="#16a34a" />
          </div>
        </div>

        <h1 style={{ margin: '0 0 8px', fontSize: '22px', fontWeight: 700, color: '#1a1a2e', letterSpacing: '-0.4px' }}>
          Facture Factur-X générée
        </h1>
        <p style={{ margin: '0 0 28px', fontSize: '13.5px', color: '#6b7280', lineHeight: '1.6' }}>
          Votre facture <strong style={{ color: '#4b5563' }}>FACT-2026-00142</strong> a été convertie avec succès
          au format Factur-X EN 16931 et est prête à être téléchargée.
        </p>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', marginBottom: '24px' }}>
          <button
            onClick={() => {}}
            style={{
              display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px',
              padding: '11px',
              background: '#1a2744', color: '#fff',
              border: 'none', borderRadius: '7px',
              fontSize: '14px', fontWeight: 600, cursor: 'pointer',
            }}
          >
            <Download size={16} />
            Télécharger le Factur-X
          </button>

          <button
            onClick={() => navigate('/rapport?type=no-errors')}
            style={{
              display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px',
              padding: '10px',
              background: '#fff', color: '#1a2744',
              border: '1px solid #d1d5db', borderRadius: '7px',
              fontSize: '13.5px', fontWeight: 500, cursor: 'pointer',
            }}
          >
            <ShieldCheck size={16} />
            Vérifier ce fichier
          </button>
        </div>

        <button
          onClick={() => navigate('/dashboard')}
          style={{ background: 'none', border: 'none', color: '#4a9eff', fontSize: '13px', cursor: 'pointer', textDecoration: 'underline', display: 'inline-flex', alignItems: 'center', gap: '6px' }}
        >
          <LayoutDashboard size={13} />
          Retour au tableau de bord
        </button>

        <div style={{
          marginTop: '28px', padding: '12px 14px',
          background: '#f0fdf4', border: '1px solid #bbf7d0', borderRadius: '8px',
          fontSize: '12px', color: '#166534', textAlign: 'left',
        }}>
          <strong>Fichier généré :</strong> FACT-2026-00142_facturx.pdf<br />
          <strong>Profil :</strong> EN 16931 · Factur-X 1.0.07<br />
          <strong>Statut :</strong> Conforme — 0 erreur
        </div>
      </div>
    </div>
  );
}
