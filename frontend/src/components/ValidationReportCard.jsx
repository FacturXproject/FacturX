const badgeStyle = (valid) =>
  valid
    ? { background: '#dcfce7', color: '#166534' }
    : { background: '#fee2e2', color: '#991b1b' };

// F08/F09: carte du rapport de validation Factur-X, partagee entre DocumentDetail
// et ComplianceReport (page "Verifier") pour ne pas dupliquer l'affichage du rapport.
export default function ValidationReportCard({ report }) {
  if (!report) return null;

  return (
    <div
      style={{
        background: '#fff',
        border: '1px solid #e5e7eb',
        borderRadius: '10px',
        overflow: 'hidden',
        fontSize: '14px',
      }}
    >
      <div
        style={{
          padding: '13px 24px',
          background: '#f8fafc',
          borderBottom: '1px solid #e5e7eb',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
        }}
      >
        <h3
          style={{
            margin: 0,
            fontSize: '12px',
            fontWeight: 600,
            color: '#475569',
            textTransform: 'uppercase',
            letterSpacing: '0.5px',
          }}
        >
          Rapport de validation Factur-X
        </h3>
        <span
          style={{
            padding: '2px 10px',
            borderRadius: '12px',
            fontWeight: 500,
            fontSize: '12px',
            ...badgeStyle(report.valid),
          }}
        >
          {report.valid ? 'Conforme' : 'Non conforme'}
        </span>
      </div>

      <div style={{ padding: '18px 24px' }}>
        <p style={{ margin: '0 0 16px', color: '#6b7280', fontSize: '13px' }}>
          {report.errorCount} erreur(s), {report.warningCount} avertissement(s), {report.infoCount} information(s)
        </p>

        {report.errors.length > 0 && (
          <ul style={{ margin: 0, padding: 0, listStyle: 'none' }}>
            {report.errors.map((err, index) => (
              <li
                key={index}
                style={{
                  borderTop: index === 0 ? 'none' : '1px solid #f1f5f9',
                  padding: '12px 0',
                }}
              >
                <div
                  style={{
                    display: 'flex',
                    alignItems: 'baseline',
                    gap: '8px',
                  }}
                >
                  <span style={{ fontWeight: 600, color: '#111827', fontSize: '13.5px' }}>
                    {err.titleFr}
                  </span>
                  {err.ruleCode && (
                    <span
                      style={{
                        fontFamily: 'monospace',
                        fontSize: '11.5px',
                        color: '#6b7280',
                        background: '#f3f4f6',
                        padding: '1px 6px',
                        borderRadius: '4px',
                      }}
                    >
                      {err.ruleCode}
                    </span>
                  )}
                </div>
                <div style={{ color: '#6b7280', fontSize: '13px', marginTop: '2px' }}>
                  {err.descriptionFr}
                </div>
                {err.correctionHintFr && (
                  <div style={{ color: '#1a2744', fontSize: '12.5px', marginTop: '4px' }}>
                    Conseil : {err.correctionHintFr}
                  </div>
                )}
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}
