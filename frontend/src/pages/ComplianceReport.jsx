import { useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { CheckCircle2, XCircle, ChevronDown, ChevronRight, AlertTriangle, Info } from 'lucide-react';
import { reportWithErrors, reportNoErrors } from '../mockData';

function LevelBadge({ level, label }) {
  const styles = {
    error:   { bg: '#fef2f2', color: '#991b1b', border: '#fecaca' },
    warning: { bg: '#fffbeb', color: '#92400e', border: '#fde68a' },
    info:    { bg: '#eff6ff', color: '#1d4ed8', border: '#bfdbfe' },
  };
  const s = styles[level];
  const Icon = level === 'error' ? XCircle : level === 'warning' ? AlertTriangle : Info;
  return (
    <span style={{
      display: 'inline-flex', alignItems: 'center', gap: '4px',
      padding: '2px 7px', borderRadius: '4px', fontSize: '11.5px', fontWeight: 500,
      background: s.bg, color: s.color, border: `1px solid ${s.border}`,
    }}>
      <Icon size={11} />
      {label}
    </span>
  );
}

function IssueRow({ issue }) {
  const [open, setOpen] = useState(false);

  return (
    <div style={{ border: '1px solid #e5e7eb', borderRadius: '6px', overflow: 'hidden', marginBottom: '8px' }}>
      <button
        onClick={() => setOpen(!open)}
        style={{
          width: '100%', display: 'flex', alignItems: 'center', gap: '10px',
          padding: '11px 14px', background: '#fff', border: 'none', cursor: 'pointer',
          textAlign: 'left',
        }}
      >
        {open ? <ChevronDown size={15} color="#6b7280" /> : <ChevronRight size={15} color="#6b7280" />}
        <span style={{ flex: 1, fontSize: '13.5px', color: '#1a1a2e', fontWeight: 450 }}>{issue.title}</span>
        <LevelBadge level={issue.level} label={issue.levelLabel} />
      </button>

      {open && (
        <div style={{ padding: '0 14px 14px 14px', background: '#fafafa', borderTop: '1px solid #f3f4f6' }}>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px', marginBottom: '12px', marginTop: '12px' }}>
            <div>
              <div style={{ fontSize: '11px', color: '#9ca3af', marginBottom: '3px', textTransform: 'uppercase', letterSpacing: '0.5px' }}>Règle</div>
              <code className="mono" style={{ fontSize: '12px', background: '#f3f4f6', padding: '2px 6px', borderRadius: '4px', color: '#374151' }}>{issue.rule}</code>
            </div>
            <div>
              <div style={{ fontSize: '11px', color: '#9ca3af', marginBottom: '3px', textTransform: 'uppercase', letterSpacing: '0.5px' }}>Champ concerné</div>
              <code className="mono" style={{ fontSize: '12px', background: '#f3f4f6', padding: '2px 6px', borderRadius: '4px', color: '#374151' }}>{issue.field}</code>
            </div>
            <div>
              <div style={{ fontSize: '11px', color: '#9ca3af', marginBottom: '3px', textTransform: 'uppercase', letterSpacing: '0.5px' }}>Valeur actuelle</div>
              <span className="mono" style={{ fontSize: '12.5px', color: '#991b1b' }}>{issue.actual}</span>
            </div>
            <div>
              <div style={{ fontSize: '11px', color: '#9ca3af', marginBottom: '3px', textTransform: 'uppercase', letterSpacing: '0.5px' }}>Valeur attendue</div>
              <span className="mono" style={{ fontSize: '12.5px', color: '#166534' }}>{issue.expected}</span>
            </div>
          </div>
          <div style={{ background: '#eff6ff', border: '1px solid #bfdbfe', borderRadius: '6px', padding: '10px 12px' }}>
            <div style={{ fontSize: '11px', color: '#1d4ed8', fontWeight: 600, marginBottom: '4px', textTransform: 'uppercase', letterSpacing: '0.5px' }}>Comment corriger</div>
            <p style={{ margin: 0, fontSize: '13px', color: '#1e40af', lineHeight: '1.5' }}>{issue.fix}</p>
          </div>
        </div>
      )}
    </div>
  );
}

export default function ComplianceReport() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const type = params.get('type') || 'with-errors';
  const report = type === 'with-errors' ? reportWithErrors : reportNoErrors;
  const isOk = report.errors === 0;

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
        <p style={{ margin: '3px 0 0', fontSize: '12.5px', color: '#6b7280' }}>
          <span className="mono">{report.filename}</span>
        </p>
      </div>

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
            {report.verdict}
          </div>
          <div style={{ display: 'flex', gap: '16px', flexWrap: 'wrap' }}>
            <span style={{ fontSize: '12.5px', color: '#4b5563' }}>
              <strong>Profil :</strong> {report.profile}
            </span>
            <span style={{ fontSize: '12.5px', color: '#4b5563' }}>
              <strong>Version :</strong> {report.factureXVersion}
            </span>
          </div>
          <div style={{ display: 'flex', gap: '12px', marginTop: '12px', flexWrap: 'wrap' }}>
            <span style={{ background: '#fef2f2', color: '#991b1b', border: '1px solid #fecaca', padding: '3px 10px', borderRadius: '4px', fontSize: '12.5px', fontWeight: 500 }}>
              {report.errors} erreur{report.errors !== 1 ? 's' : ''}
            </span>
            <span style={{ background: '#fffbeb', color: '#92400e', border: '1px solid #fde68a', padding: '3px 10px', borderRadius: '4px', fontSize: '12.5px', fontWeight: 500 }}>
              {report.warnings} avertissement{report.warnings !== 1 ? 's' : ''}
            </span>
            <span style={{ background: '#eff6ff', color: '#1d4ed8', border: '1px solid #bfdbfe', padding: '3px 10px', borderRadius: '4px', fontSize: '12.5px', fontWeight: 500 }}>
              {report.infos} information{report.infos !== 1 ? 's' : ''}
            </span>
          </div>
        </div>
      </div>

      {/* Issues list */}
      <div>
        <h2 style={{ margin: '0 0 12px', fontSize: '14px', fontWeight: 600, color: '#1a1a2e' }}>
          Détail des contrôles ({report.issues.length})
        </h2>
        {report.issues.map(issue => (
          <IssueRow key={issue.id} issue={issue} />
        ))}
      </div>
    </div>
  );
}
