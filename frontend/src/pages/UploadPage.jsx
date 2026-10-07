import { useRef, useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Upload, Loader2 } from 'lucide-react';
import api from '../services/api';

const MAX_SIZE = 10 * 1024 * 1024; // 10 Mo
const ACCEPTED_TYPES = ['application/pdf', 'application/xml', 'text/xml'];

// Roles pouvant valider (Permission.VALIDATE_DOCUMENT) ou extraire
// (Permission.EXTRACT_DOCUMENT) un document cote backend (voir
// PermissionService) : seules ces organisations sont proposees sur les
// pages "Verifier" et "Convertir".
const VALIDATOR_ROLES = ['ADMIN', 'ACCOUNTANT'];
const EXTRACTOR_ROLES = ['ADMIN', 'ACCOUNTANT'];

function validateFile(file, { pdfOnly = false } = {}) {
  if (file.size > MAX_SIZE) {
    return `${file.name} : dépasse la taille maximale (10 Mo).`;
  }
  if (pdfOnly) {
    if (file.type !== 'application/pdf' && !file.name.match(/\.pdf$/i)) {
      return `${file.name} : seuls les fichiers PDF peuvent être convertis en Factur-X.`;
    }
    return null;
  }
  if (!ACCEPTED_TYPES.includes(file.type) && !file.name.match(/\.(pdf|xml)$/i)) {
    return `${file.name} : seuls les fichiers PDF et XML sont acceptés.`;
  }
  return null;
}

export default function UploadPage({ mode }) {
  const navigate = useNavigate();
  const fileRef = useRef();
  const [dragging, setDragging] = useState(false);

  const isVerif = mode === 'verifier';
  const isConvertir = mode === 'convertir';
  const needsOrg = isVerif || isConvertir;
  const eligibleRoles = isVerif ? VALIDATOR_ROLES : EXTRACTOR_ROLES;

  // F06/F08: la « verification » depose reellement le fichier dans
  // l'organisation choisie par l'utilisateur, puis lance la validation
  // Factur-X sur le document cree. F06/F11: "Convertir" fait de meme puis
  // lance l'extraction PDF - le tout dans un seul gestionnaire d'evenement
  // declenche par le depot du fichier (pas dans un useEffect), pour que
  // React 18 StrictMode ne puisse pas dedoubler les appels POST.
  const [orgsLoading, setOrgsLoading] = useState(needsOrg);
  const [eligibleOrgs, setEligibleOrgs] = useState([]);
  const [selectedOrgId, setSelectedOrgId] = useState(null);
  const [stage, setStage] = useState(null); // null | 'uploading' | 'validating' | 'extracting'
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!needsOrg) return;

    let cancelled = false;

    const loadOrganizations = async () => {
      try {
        const response = await api.get('/organizations');
        const memberships = response.data || [];
        const eligible = memberships.filter((m) => eligibleRoles.includes(m.role));

        const withNames = await Promise.all(
          eligible.map(async (m) => {
            const id = m.organizationId ?? m.id;
            try {
              const orgResponse = await api.get(`/organizations/${id}`);
              return { id, name: orgResponse.data?.name ?? `Organisation #${id}` };
            } catch {
              return { id, name: `Organisation #${id}` };
            }
          })
        );

        if (cancelled) return;
        setEligibleOrgs(withNames);
        setSelectedOrgId(withNames[0]?.id ?? null);
      } catch (err) {
        if (cancelled) return;
        console.error('Organizations error:', err);
        setError('Impossible de récupérer vos organisations.');
      } finally {
        if (!cancelled) setOrgsLoading(false);
      }
    };

    loadOrganizations();

    return () => { cancelled = true; };
    // eligibleRoles and needsOrg are both derived deterministically from mode.
  }, [mode]);

  const uploadAndVerify = async (file) => {
    const clientError = validateFile(file);
    if (clientError) {
      setError(clientError);
      return;
    }
    if (!selectedOrgId) {
      setError('Aucune organisation disponible pour déposer ce document.');
      return;
    }

    setError(null);
    setStage('uploading');

    const formData = new FormData();
    formData.append('file', file);

    let documentId;
    try {
      const response = await api.post(
        `/documents?organizationId=${selectedOrgId}`,
        formData,
        { headers: { 'Content-Type': 'multipart/form-data' } }
      );
      documentId = response.data.id;
    } catch (err) {
      console.error('Upload error:', err);
      setError(err.response?.data?.message || "Échec du dépôt du fichier.");
      setStage(null);
      return;
    }

    setStage('validating');

    try {
      await api.post(`/documents/${documentId}/validate`);
      navigate(`/rapport?documentId=${documentId}`);
    } catch (err) {
      console.error('Validation error:', err);
      setError(err.response?.data?.message || "Échec de la validation du document.");
      setStage(null);
    }
  };

  // F06/F11: depose le PDF puis lance l'extraction (POST /documents/{id}/extract)
  // sur le document cree, avant d'ouvrir la page de vérification des champs (F12).
  const uploadAndExtract = async (file) => {
    const clientError = validateFile(file, { pdfOnly: true });
    if (clientError) {
      setError(clientError);
      return;
    }
    if (!selectedOrgId) {
      setError('Aucune organisation disponible pour déposer ce document.');
      return;
    }

    setError(null);
    setStage('uploading');

    const formData = new FormData();
    formData.append('file', file);

    let documentId;
    try {
      const response = await api.post(
        `/documents?organizationId=${selectedOrgId}`,
        formData,
        { headers: { 'Content-Type': 'multipart/form-data' } }
      );
      documentId = response.data.id;
    } catch (err) {
      console.error('Upload error:', err);
      setError(err.response?.data?.message || "Échec du dépôt du fichier.");
      setStage(null);
      return;
    }

    setStage('extracting');

    try {
      await api.post(`/documents/${documentId}/extract`);
      navigate(`/conversion?documentId=${documentId}`);
    } catch (err) {
      console.error('Extraction error:', err);
      setError(err.response?.data?.message || "Échec de l'extraction des données.");
      setStage(null);
    }
  };

  const handleFiles = (files) => {
    if (!files || files.length === 0) return;
    const file = files[0];

    if (isVerif) {
      uploadAndVerify(file);
    } else if (isConvertir) {
      uploadAndExtract(file);
    }
  };

  const busy = needsOrg && stage !== null;

  const handleDrop = (e) => {
    e.preventDefault();
    setDragging(false);
    if (busy) return;
    handleFiles(e.dataTransfer.files);
  };

  const handleClick = () => {
    if (busy) return;
    fileRef.current?.click();
  };

  const handleInputChange = (e) => {
    handleFiles(e.target.files);
    e.target.value = '';
  };

  const stageLabel =
    stage === 'uploading' ? 'Dépôt du fichier…'
    : stage === 'validating' ? 'Validation en cours…'
    : stage === 'extracting' ? 'Extraction des données…'
    : null;

  const pageTitle = isVerif ? 'Vérifier la conformité' : 'Convertir en Factur-X';

  if (needsOrg && orgsLoading) {
    return (
      <div style={{ padding: '24px 28px', maxWidth: '600px' }}>
        <h1 style={{ margin: '0 0 6px', fontSize: '19px', fontWeight: 700, color: '#1a1a2e' }}>
          {pageTitle}
        </h1>
        <p style={{ margin: 0, fontSize: '13px', color: '#6b7280' }}>Chargement…</p>
      </div>
    );
  }

  if (needsOrg && eligibleOrgs.length === 0) {
    return (
      <div style={{ padding: '24px 28px', maxWidth: '600px' }}>
        <h1 style={{ margin: '0 0 6px', fontSize: '19px', fontWeight: 700, color: '#1a1a2e' }}>
          {pageTitle}
        </h1>
        <div style={{
          border: '1px solid #fecaca', borderRadius: '10px', background: '#fff5f5',
          padding: '16px 18px', color: '#991b1b', fontSize: '13.5px', marginTop: '12px',
        }}>
          {isVerif
            ? "Vous n'avez pas le droit de vérifier des factures dans vos organisations."
            : "Vous n'avez pas le droit de convertir des factures dans vos organisations."}
        </div>
      </div>
    );
  }

  return (
    <div style={{ padding: '24px 28px', maxWidth: '600px' }}>
      <h1 style={{ margin: '0 0 6px', fontSize: '19px', fontWeight: 700, color: '#1a1a2e' }}>
        {pageTitle}
      </h1>
      <p style={{ margin: '0 0 24px', fontSize: '13px', color: '#6b7280' }}>
        {isVerif
          ? 'Déposez une facture PDF ou XML pour analyser sa conformité Factur-X (EN 16931).'
          : 'Déposez un PDF de facture pour en extraire les données et générer un fichier Factur-X.'}
      </p>

      {needsOrg && eligibleOrgs.length === 1 && (
        <p style={{ margin: '0 0 8px', fontSize: '13px', color: '#374151' }}>
          <strong>Organisation :</strong> {eligibleOrgs[0].name}
        </p>
      )}

      {needsOrg && eligibleOrgs.length > 1 && (
        <div style={{ marginBottom: '10px' }}>
          <label style={{ display: 'block', fontSize: '12px', color: '#6b7280', marginBottom: '4px' }}>
            Organisation
          </label>
          <select
            value={selectedOrgId ?? ''}
            onChange={(e) => setSelectedOrgId(Number(e.target.value))}
            disabled={busy}
            style={{
              width: '100%', padding: '8px 10px', border: '1px solid #d1d5db',
              borderRadius: '6px', fontSize: '13.5px', color: '#1a1a2e', background: '#fff',
            }}
          >
            {eligibleOrgs.map((org) => (
              <option key={org.id} value={org.id}>{org.name}</option>
            ))}
          </select>
        </div>
      )}

      {needsOrg && (
        <p style={{ margin: '0 0 20px', fontSize: '12px', color: '#9ca3af' }}>
          Le document sera enregistré dans l'organisation sélectionnée.
        </p>
      )}

      <div
        onDragOver={e => { e.preventDefault(); setDragging(true); }}
        onDragLeave={() => setDragging(false)}
        onDrop={handleDrop}
        onClick={handleClick}
        style={{
          border: `2px dashed ${dragging ? '#4a9eff' : '#d1d5db'}`,
          borderRadius: '10px', padding: '60px 24px',
          textAlign: 'center', cursor: busy ? 'default' : 'pointer',
          background: dragging ? '#f0f7ff' : '#fafafa',
          transition: 'all 0.15s',
          opacity: busy ? 0.7 : 1,
        }}
      >
        <input ref={fileRef} type="file" accept={isConvertir ? '.pdf' : '.pdf,.xml'} style={{ display: 'none' }} onChange={handleInputChange} />
        <div style={{ display: 'flex', justifyContent: 'center', marginBottom: '14px' }}>
          <div style={{ background: '#eef2ff', borderRadius: '50%', padding: '16px', display: 'inline-flex' }}>
            {busy
              ? <Loader2 size={28} color="#4a9eff" style={{ animation: 'spin 1s linear infinite' }} />
              : <Upload size={28} color="#4a9eff" />
            }
          </div>
        </div>
        <p style={{ margin: '0 0 6px', fontWeight: 600, fontSize: '15px', color: '#1a1a2e' }}>
          {busy ? stageLabel : 'Déposez votre fichier ici'}
        </p>
        <p style={{ margin: '0 0 16px', fontSize: '12.5px', color: '#9ca3af' }}>
          {isConvertir ? 'PDF — jusqu\'à 10 Mo' : 'PDF ou XML — jusqu\'à 10 Mo'}
        </p>
        <button
          disabled={busy}
          style={{
            padding: '8px 20px',
            background: '#1a2744', color: '#fff',
            border: 'none', borderRadius: '6px',
            fontSize: '13.5px', fontWeight: 500,
            cursor: busy ? 'default' : 'pointer',
          }}
        >
          Choisir un fichier
        </button>
      </div>

      {error && (
        <p style={{ margin: '16px 0 0', fontSize: '13px', color: '#991b1b' }}>
          {error}
        </p>
      )}

      <style>{`@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }`}</style>
    </div>
  );
}
