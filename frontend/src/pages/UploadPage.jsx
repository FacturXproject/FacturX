import { useRef, useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Upload, Loader2 } from 'lucide-react';
import api from '../services/api';

const MAX_SIZE = 10 * 1024 * 1024; // 10 Mo
const ACCEPTED_TYPES = ['application/pdf', 'application/xml', 'text/xml'];

function validateFile(file) {
  if (file.size > MAX_SIZE) {
    return `${file.name} : dépasse la taille maximale (10 Mo).`;
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

  // F06/F08: la « verification » depose reellement le fichier puis lance la
  // validation Factur-X sur le document cree, au lieu de naviguer directement
  // vers l'ecran de traitement sans rien envoyer.
  const [organizationId, setOrganizationId] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!isVerif) return;

    api.get('/organizations')
      .then((response) => {
        const orgs = response.data || [];
        if (orgs.length > 0) {
          const org = orgs[0];
          setOrganizationId(org.organizationId ?? org.id);
        } else {
          setError("Aucune organisation associée à votre compte.");
        }
      })
      .catch((err) => {
        console.error('Organizations error:', err);
        setError("Impossible de récupérer votre organisation.");
      });
  }, [isVerif]);

  const go = () => navigate(`/traitement?action=${mode}`);

  const uploadAndVerify = async (file) => {
    const clientError = validateFile(file);
    if (clientError) {
      setError(clientError);
      return;
    }
    if (!organizationId) {
      setError("Aucune organisation associée à votre compte.");
      return;
    }

    setError(null);
    setUploading(true);

    const formData = new FormData();
    formData.append('file', file);

    try {
      const response = await api.post(
        `/documents?organizationId=${organizationId}`,
        formData,
        { headers: { 'Content-Type': 'multipart/form-data' } }
      );
      navigate(`/traitement?action=verifier&documentId=${response.data.id}`);
    } catch (err) {
      console.error('Upload error:', err);
      setError(err.response?.data?.message || "Échec du dépôt du fichier.");
      setUploading(false);
    }
  };

  const handleFiles = (files) => {
    if (!files || files.length === 0) return;
    const file = files[0];

    if (isVerif) {
      uploadAndVerify(file);
    } else {
      go();
    }
  };

  const handleDrop = (e) => {
    e.preventDefault();
    setDragging(false);
    if (isVerif && uploading) return;
    handleFiles(e.dataTransfer.files);
  };

  const handleClick = () => {
    if (isVerif) {
      if (uploading) return;
      fileRef.current?.click();
    } else {
      fileRef.current?.click();
      go();
    }
  };

  const handleInputChange = (e) => {
    handleFiles(e.target.files);
    e.target.value = '';
  };

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
        onDrop={handleDrop}
        onClick={handleClick}
        style={{
          border: `2px dashed ${dragging ? '#4a9eff' : '#d1d5db'}`,
          borderRadius: '10px', padding: '60px 24px',
          textAlign: 'center', cursor: isVerif && uploading ? 'default' : 'pointer',
          background: dragging ? '#f0f7ff' : '#fafafa',
          transition: 'all 0.15s',
          opacity: isVerif && uploading ? 0.7 : 1,
        }}
      >
        <input ref={fileRef} type="file" accept=".pdf,.xml" style={{ display: 'none' }} onChange={handleInputChange} />
        <div style={{ display: 'flex', justifyContent: 'center', marginBottom: '14px' }}>
          <div style={{ background: '#eef2ff', borderRadius: '50%', padding: '16px', display: 'inline-flex' }}>
            {isVerif && uploading
              ? <Loader2 size={28} color="#4a9eff" style={{ animation: 'spin 1s linear infinite' }} />
              : <Upload size={28} color="#4a9eff" />
            }
          </div>
        </div>
        <p style={{ margin: '0 0 6px', fontWeight: 600, fontSize: '15px', color: '#1a1a2e' }}>
          {isVerif && uploading ? 'Dépôt du fichier…' : 'Déposez votre fichier ici'}
        </p>
        <p style={{ margin: '0 0 16px', fontSize: '12.5px', color: '#9ca3af' }}>
          PDF ou XML — jusqu'à 10 Mo
        </p>
        <button
          disabled={isVerif && uploading}
          style={{
            padding: '8px 20px',
            background: '#1a2744', color: '#fff',
            border: 'none', borderRadius: '6px',
            fontSize: '13.5px', fontWeight: 500,
            cursor: isVerif && uploading ? 'default' : 'pointer',
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
