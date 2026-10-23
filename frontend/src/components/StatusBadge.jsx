const STATUS_STYLES = {
  VALID: { background: '#dcfce7', color: '#166534', border: '1px solid #bbf7d0' },
  INVALID: { background: '#fee2e2', color: '#991b1b', border: '1px solid #fecaca' },
  FAILED: { background: '#fee2e2', color: '#991b1b', border: '1px solid #fecaca' },
  PROCESSING: { background: '#fef3c7', color: '#92400e', border: '1px solid #fde68a' },
  QUEUED: { background: '#fef3c7', color: '#92400e', border: '1px solid #fde68a' },
  UPLOADED: { background: '#f3f4f6', color: '#374151', border: '1px solid #d1d5db' },
};

export default function StatusBadge({ status }) {
  return (
    <span
      style={{
        display: 'inline-block',
        padding: '2px 8px',
        borderRadius: '4px',
        fontSize: '12px',
        fontWeight: 500,
        ...(STATUS_STYLES[status] ?? STATUS_STYLES.UPLOADED),
      }}
    >
      {status}
    </span>
  );
}
