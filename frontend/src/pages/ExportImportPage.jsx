import { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Download, FileUp, CircleCheck, CircleAlert, AlertTriangle } from 'lucide-react';
import api from '../services/api';
import { downloadExport, saveBlob } from '../utils/download';

// F20 : modèle proposé au téléchargement, au format attendu par l'import.
export const IMPORT_TEMPLATE = 'email,role\nprenom.nom@exemple.fr,CLIENT\ncomptable@exemple.fr,ACCOUNTANT\n';

const roleLabels = {
  ADMIN: 'Administrateur',
  ACCOUNTANT: 'Comptable',
  CLIENT: 'Client',
};

const card = {
  background: '#fff', border: '1px solid #e5e7eb', borderRadius: '10px',
  padding: '20px', marginBottom: '20px',
};
const cardTitle = {
  fontSize: '15px', fontWeight: 600, color: '#111827', margin: '0 0 6px',
  display: 'flex', alignItems: 'center', gap: '8px',
};
const cardText = { fontSize: '13.5px', color: '#6b7280', margin: '0 0 14px' };
const secondaryButton = {
  display: 'flex', alignItems: 'center', gap: '6px', padding: '8px 14px',
  borderRadius: '8px', border: '1px solid #d1d5db', background: '#fff',
  cursor: 'pointer', fontSize: '13px', color: '#374151',
};
const errorBox = {
  background: '#fee2e2', color: '#991b1b', padding: '10px 12px',
  borderRadius: '8px', fontSize: '13px', marginTop: '12px',
};
const successBox = {
  display: 'flex', alignItems: 'center', gap: '8px', background: '#dcfce7', color: '#166534',
  padding: '10px 12px', borderRadius: '8px', fontSize: '13px', marginTop: '12px',
};
const th = { textAlign: 'left', padding: '9px 12px', color: '#6b7280', fontWeight: 500 };
const td = { padding: '9px 12px', color: '#374151' };

function readBlobText(blob) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result);
    reader.onerror = () => reject(reader.error);
    reader.readAsText(blob);
  });
}

// Un export en erreur arrive sous forme de Blob (responseType: 'blob') : on relit le JSON.
async function errorMessage(err) {
  let data = err.response?.data;
  if (data instanceof Blob) {
    try {
      data = JSON.parse(await readBlobText(data));
    } catch {
      data = null;
    }
  }
  return data?.message ?? err.message ?? 'Une erreur est survenue.';
}

function ExportButtons({ organizationId, what, formats, onError }) {
  const [busy, setBusy] = useState(null);

  const handleExport = async (format) => {
    setBusy(format);
    onError(null);
    try {
      await downloadExport(organizationId, what, format);
    } catch (err) {
      onError(await errorMessage(err));
    } finally {
      setBusy(null);
    }
  };

  return (
    <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
      {formats.map((format) => (
        <button
          key={format}
          onClick={() => handleExport(format)}
          disabled={busy !== null}
          aria-label={`Exporter les ${what === 'documents' ? 'documents' : 'membres'} en ${format.toUpperCase()}`}
          style={{ ...secondaryButton, cursor: busy !== null ? 'wait' : 'pointer' }}
        >
          <Download size={15} />
          {busy === format ? 'Export...' : format.toUpperCase()}
        </button>
      ))}
    </div>
  );
}

function ImportPreview({ rows }) {
  return (
    <div style={{ border: '1px solid #e5e7eb', borderRadius: '8px', overflow: 'auto', marginTop: '14px', maxHeight: '360px' }}>
      <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '13px' }}>
        <thead>
          <tr style={{ background: '#f9fafb', borderBottom: '1px solid #e5e7eb' }}>
            <th style={th}>Ligne</th>
            <th style={th}>Email</th>
            <th style={th}>Rôle</th>
            <th style={th}>Résultat</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={row.lineNumber} style={{ borderBottom: '1px solid #f3f4f6', background: row.valid ? '#fff' : '#fef2f2' }}>
              <td style={td}>{row.lineNumber}</td>
              <td style={td}>{row.email || '—'}</td>
              <td style={td}>{roleLabels[row.role] ?? (row.role || '—')}</td>
              <td style={{ ...td, color: row.valid ? '#166534' : '#991b1b' }}>
                {row.valid ? 'Prête' : row.error}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function InvitationImport({ organizationId }) {
  const inputRef = useRef(null);
  const [file, setFile] = useState(null);
  const [rows, setRows] = useState(null);
  const [checking, setChecking] = useState(false);
  const [importing, setImporting] = useState(false);
  const [error, setError] = useState(null);
  const [imported, setImported] = useState(null);

  const send = (selectedFile, dryRun) => {
    const formData = new FormData();
    formData.append('file', selectedFile);
    return api.post(`/organizations/${organizationId}/import/invitations`, formData, {
      params: { dryRun },
    });
  };

  const reset = () => {
    setFile(null);
    setRows(null);
    if (inputRef.current) inputRef.current.value = '';
  };

  // Chaque fichier choisi est d'abord vérifié à blanc (dryRun) : rien n'est créé.
  const handleFileChange = async (e) => {
    const selected = e.target.files?.[0];
    setError(null);
    setImported(null);
    setRows(null);
    setFile(selected ?? null);
    if (!selected) return;

    setChecking(true);
    try {
      const response = await send(selected, true);
      setRows(response.data);
    } catch (err) {
      setError(await errorMessage(err));
    } finally {
      setChecking(false);
    }
  };

  const handleImport = async () => {
    setImporting(true);
    setError(null);
    try {
      const response = await send(file, false);
      setImported(response.data.length);
      reset();
    } catch (err) {
      // 400 avec la liste des lignes : le fichier a changé entre-temps, on réaffiche l'aperçu.
      if (Array.isArray(err.response?.data)) {
        setRows(err.response.data);
      } else {
        setError(await errorMessage(err));
      }
    } finally {
      setImporting(false);
    }
  };

  const validCount = rows?.filter((row) => row.valid).length ?? 0;
  const errorCount = rows ? rows.length - validCount : 0;
  const canImport = rows && rows.length > 0 && errorCount === 0 && !importing;

  return (
    <div style={card}>
      <h2 style={cardTitle}><FileUp size={17} /> Importer des invitations</h2>
      <p style={cardText}>
        Invitez plusieurs personnes en une fois à partir d'un fichier CSV. Le fichier est
        d'abord vérifié : aucune invitation n'est envoyée tant qu'une ligne est en erreur.
      </p>

      <ul style={{ fontSize: '13px', color: '#4b5563', margin: '0 0 14px', paddingLeft: '18px', lineHeight: 1.7 }}>
        <li>Colonnes <code>email</code> et <code>role</code> (l'export CSV des membres est aussi accepté).</li>
        <li>Rôles : <code>ADMIN</code>, <code>ACCOUNTANT</code>, <code>CLIENT</code> (ou Administrateur, Comptable, Client).</li>
        <li>Séparateur virgule ou point-virgule, encodage UTF-8, 500 lignes au maximum.</li>
      </ul>

      <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap', alignItems: 'center' }}>
        <label style={{ ...secondaryButton, background: '#1a2744', color: '#fff', border: 'none' }}>
          <FileUp size={15} />
          Choisir un fichier CSV
          <input
            ref={inputRef}
            type="file"
            accept=".csv,text/csv"
            onChange={handleFileChange}
            aria-label="Fichier CSV d'invitations"
            style={{ display: 'none' }}
          />
        </label>
        <button
          type="button"
          onClick={() => saveBlob(new Blob([IMPORT_TEMPLATE], { type: 'text/csv' }), 'modele-invitations.csv')}
          style={secondaryButton}
        >
          <Download size={15} />
          Télécharger un modèle
        </button>
        {file && <span style={{ fontSize: '13px', color: '#6b7280' }}>{file.name}</span>}
      </div>

      {checking && <p style={{ fontSize: '13px', color: '#6b7280', marginTop: '12px' }}>Vérification du fichier...</p>}
      {error && <div role="alert" style={errorBox}>{error}</div>}
      {imported !== null && (
        <div role="status" style={successBox}>
          <CircleCheck size={16} />
          {imported} invitation{imported > 1 ? 's' : ''} envoyée{imported > 1 ? 's' : ''}.
        </div>
      )}

      {rows && (
        <>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginTop: '14px', fontSize: '13.5px' }}>
            {errorCount === 0 ? <CircleCheck size={16} color="#16a34a" /> : <CircleAlert size={16} color="#dc2626" />}
            <span data-testid="import-summary">
              {validCount} ligne{validCount > 1 ? 's' : ''} prête{validCount > 1 ? 's' : ''},{' '}
              {errorCount} en erreur
            </span>
          </div>

          <ImportPreview rows={rows} />

          {errorCount > 0 && (
            <div style={{ display: 'flex', gap: '8px', alignItems: 'flex-start', background: '#fef3c7', color: '#92400e', padding: '10px 12px', borderRadius: '8px', fontSize: '13px', marginTop: '12px' }}>
              <AlertTriangle size={16} style={{ flexShrink: 0, marginTop: '1px' }} />
              Corrigez les lignes en erreur puis choisissez à nouveau le fichier.
            </div>
          )}

          <div style={{ display: 'flex', gap: '8px', justifyContent: 'flex-end', marginTop: '14px' }}>
            <button type="button" onClick={reset} style={secondaryButton}>Annuler</button>
            <button
              type="button"
              onClick={handleImport}
              disabled={!canImport}
              style={{
                padding: '8px 16px', borderRadius: '8px', border: 'none', fontSize: '13.5px', color: '#fff',
                background: canImport ? '#1a2744' : '#9ca3af', cursor: canImport ? 'pointer' : 'not-allowed',
              }}
            >
              {importing ? 'Envoi...' : `Envoyer ${validCount} invitation${validCount > 1 ? 's' : ''}`}
            </button>
          </div>
        </>
      )}
    </div>
  );
}

export default function ExportImportPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [orgName, setOrgName] = useState(null);
  const [role, setRole] = useState(null);
  const [loading, setLoading] = useState(true);
  const [documentsError, setDocumentsError] = useState(null);
  const [membersError, setMembersError] = useState(null);

  useEffect(() => {
    let cancelled = false;
    Promise.all([
      api.get('/organizations').catch(() => ({ data: [] })),
      api.get(`/organizations/${id}`).catch(() => ({ data: null })),
    ]).then(([memberships, organization]) => {
      if (cancelled) return;
      const membership = memberships.data.find(
        (org) => String(org.organizationId ?? org.id) === String(id)
      );
      setRole(membership?.role ?? null);
      setOrgName(organization.data?.name ?? null);
      setLoading(false);
    });
    return () => { cancelled = true; };
  }, [id]);

  const isAdmin = role === 'ADMIN';
  const canExportDocuments = isAdmin || role === 'ACCOUNTANT';

  return (
    <div style={{ padding: '32px', maxWidth: '900px', margin: '0 auto' }}>
      <button
        onClick={() => navigate(`/organisations/${id}`)}
        style={{
          display: 'flex', alignItems: 'center', gap: '6px', background: 'none', border: 'none',
          cursor: 'pointer', color: '#6b7280', fontSize: '13.5px', marginBottom: '16px', padding: 0,
        }}
      >
        <ArrowLeft size={16} />
        Retour à l'organisation
      </button>

      <h1 style={{ fontSize: '24px', fontWeight: 700, marginBottom: '4px', color: '#111827' }}>
        Export / import
      </h1>
      <p style={{ color: '#6b7280', marginBottom: '24px', fontSize: '14px' }}>
        {orgName ?? `Organisation #${id}`}
      </p>

      {loading && <p style={{ color: '#6b7280' }}>Chargement...</p>}

      {!loading && !canExportDocuments && (
        <div style={{ ...card, color: '#6b7280', fontSize: '14px' }}>
          L'export et l'import sont réservés aux administrateurs et aux comptables de l'organisation.
        </div>
      )}

      {!loading && canExportDocuments && (
        <div style={card}>
          <h2 style={cardTitle}><Download size={17} /> Exporter les documents</h2>
          <p style={cardText}>
            Tous les documents de l'organisation, avec leur statut et le résultat de leur dernière
            validation (conformité, nombre d'erreurs, codes des règles).
          </p>
          <ExportButtons organizationId={id} what="documents" formats={['csv', 'json', 'pdf']} onError={setDocumentsError} />
          {documentsError && <div role="alert" style={errorBox}>{documentsError}</div>}
        </div>
      )}

      {!loading && isAdmin && (
        <div style={card}>
          <h2 style={cardTitle}><Download size={17} /> Exporter les membres</h2>
          <p style={cardText}>Nom, adresse e-mail et rôle de chaque membre.</p>
          <ExportButtons organizationId={id} what="members" formats={['csv', 'json']} onError={setMembersError} />
          {membersError && <div role="alert" style={errorBox}>{membersError}</div>}
        </div>
      )}

      {!loading && isAdmin && <InvitationImport organizationId={id} />}
    </div>
  );
}
