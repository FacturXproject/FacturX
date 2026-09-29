import { useEffect, useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { CheckCircle2, XCircle, Loader2 } from 'lucide-react';
import api from '../services/api';
import ValidationReportCard from '../components/ValidationReportCard';

export default function ComplianceReport() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const documentId = params.get('documentId');

  const [report, setReport] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!documentId) {
      setLoading(false);
      setError("Aucun document à afficher.");
      return;
    }

    setLoading(true);
    setError(null);

    api.get(`/documents/${documentId}/report`)
      .then((response) => setReport(response.data || null))
      .catch((err) => {
        console.error('Report error:', err);
        setError(err.response?.data?.message || "Impossible de charger le rapport de validation.");
      })
      .finally(() => setLoading(false));
  }, [documentId]);

  const isOk = report?.valid;

  return (
    <div style={{ padding: '24px 28px', maxWidth: '820px' }}>
      {/* Header */}
      <div style={{ marginBottom: '20px' }}>
        <button
          onClick={() => navigate('/dashboard')}
          style={{ background: 'none', border: 'none', color: '#4a9eff', fontSize: '13px', cursor: 'pointer', padding: '0 0 12px', textDecoration: 'underline' }}
        >
          ← Retour au tableau de bord
        </button>
        <h1 style={{ margin: 0, fontSize: '19px', fontWeight: 700, color: '#1a1a2e' }}>
          Rapport de conformité
        </h1>
        {report?.filename && (
          <p style={{ margin: '3px 0 0', fontSize: '12.5px', color: '#6b7280' }}>
            <span className="mono">{report.filename}</span>
          </p>
        )}
      </div>

      {loading && (
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', color: '#6b7280', fontSize: '13.5px' }}>
          <Loader2 size={18} style={{ animation: 'spin 1s linear infinite' }} />
          Chargement du rapport…
          <style>{`@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }`}</style>
        </div>
      )}

      {!loading && error && (
        <div style={{
          border: '1px solid #fecaca', borderRadius: '10px', background: '#fff5f5',
          padding: '20px', color: '#991b1b', fontSize: '13.5px',
        }}>
          {error}
        </div>
      )}

      {!loading && !error && report && (
        <>
          {/* Summary card */}
          <div style={{
            border: `1px solid ${isOk ? '#bbf7d0' : '#fecaca'}`,
            borderRadius: '10px',
            background: isOk ? '#f0fdf4' : '#fff5f5',
            padding: '20px',
            marginBottom: '20px',
            display: 'flex',
            alignItems: 'flex-start',
            gap: '16px',
          }}>
            <div style={{ marginTop: '2px' }}>
              {isOk
                ? <CheckCircle2 size={28} color="#16a34a" />
                : <XCircle size={28} color="#dc2626" />
              }
            </div>
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: '18px', fontWeight: 700, color: isOk ? '#166534' : '#991b1b', marginBottom: '8px' }}>
                {isOk ? 'Conforme' : 'Non conforme'}
              </div>
              <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap' }}>
                <span style={{ background: '#fef2f2', color: '#991b1b', border: '1px solid #fecaca', padding: '3px 10px', borderRadius: '4px', fontSize: '12.5px', fontWeight: 500 }}>
                  {report.errorCount} erreur{report.errorCount !== 1 ? 's' : ''}
                </span>
                <span style={{ background: '#fffbeb', color: '#92400e', border: '1px solid #fde68a', padding: '3px 10px', borderRadius: '4px', fontSize: '12.5px', fontWeight: 500 }}>
                  {report.warningCount} avertissement{report.warningCount !== 1 ? 's' : ''}
                </span>
                <span style={{ background: '#eff6ff', color: '#1d4ed8', border: '1px solid #bfdbfe', padding: '3px 10px', borderRadius: '4px', fontSize: '12.5px', fontWeight: 500 }}>
                  {report.infoCount} information{report.infoCount !== 1 ? 's' : ''}
                </span>
              </div>
            </div>
          </div>

          <ValidationReportCard report={report} />
        </>
      )}
    </div>
  );
}
