import api from '../services/api';
import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import ValidationReportCard from '../components/ValidationReportCard';

export default function DocumentDetail() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [document, setDocument] = useState(null);
  const [loading, setLoading] = useState(true);
  const [report, setReport] = useState(null);
  const [validating, setValidating] = useState(false);
  const [validationError, setValidationError] = useState(null);
  const [extracting, setExtracting] = useState(false);
  const [extractionError, setExtractionError] = useState(null);

  const formatDate = (date) => {
    if (!date) return '';
    return new Date(date).toLocaleString('fr-FR');
  };

  const formatSize = (size) => {
    if (!size) return '0 Ko';

    const ko = size / 1024;

    if (ko < 1024) {
      return `${ko.toFixed(1)} Ko`;
    }

    const mo = ko / 1024;
    return `${mo.toFixed(1)} Mo`;
  };

  // Affichage simple du type
  const formatType = (type) => {
    if (type === 'application/pdf') {
      return 'PDF';
    }

    if (
      type === 'application/xml' ||
      type === 'text/xml'
    ) {
      return 'XML';
    }

    return type;
  };

  const getStatusStyle = (status) => {
    switch (status) {
      case 'VALID':
        return {
          background: '#dcfce7',
          color: '#166534',
        };

      case 'INVALID':
      case 'FAILED':
        return {
          background: '#fee2e2',
          color: '#991b1b',
        };

      case 'PROCESSING':
      case 'QUEUED':
        return {
          background: '#fef3c7',
          color: '#92400e',
        };

      case 'UPLOADED':
      default:
        return {
          background: '#f3f4f6',
          color: '#374151',
        };
    }
  };

  const loadDocument = async () => {
    try {
      const response = await api.get(`/documents/${id}`);
      setDocument(response.data || null);
    } catch (error) {
      console.error('Documents error:', error);
      setDocument(null);
    } finally {
      setLoading(false);
    }
  };

  // F08/F09: rapport de la derniere validation, s'il en existe deja une pour ce document.
  const loadReport = async () => {
    try {
      const response = await api.get(`/documents/${id}/report`);
      setReport(response.data || null);
    } catch (error) {
      // 404 tant qu'aucune validation n'a encore ete lancee pour ce document.
      setReport(null);
    }
  };

  useEffect(() => {
    setDocument(null);
    setReport(null);
    setLoading(true);
    loadDocument();
    loadReport();
  }, [id]);

  // F11: lance l'extraction PDF (POST /api/documents/{id}/extract) sur ce document
  // deja depose, en reutilisant le documentId existant, puis ouvre la page de
  // verification des champs extraits (F12) une fois le brouillon persiste.
  const handleExtract = async () => {
    setExtracting(true);
    setExtractionError(null);
    try {
      await api.post(`/documents/${id}/extract`);
      navigate(`/conversion?documentId=${id}`);
    } catch (error) {
      console.error('Extraction error:', error);
      setExtractionError(
        error.response?.data?.message || "Échec de l'extraction des données."
      );
      setExtracting(false);
    }
  };

  // F08: lance la validation Factur-X sur ce document (POST /api/documents/{id}/validate),
  // en reutilisant le documentId existant plutot qu'un nouvel upload independant.
  const handleValidate = async () => {
    setValidating(true);
    setValidationError(null);
    try {
      await api.post(`/documents/${id}/validate`);
      await Promise.all([loadDocument(), loadReport()]);
    } catch (error) {
      console.error('Validation error:', error);
      setValidationError(
        error.response?.data?.message || "Échec de la validation du document."
      );
    } finally {
      setValidating(false);
    }
  };

  return (
    <div>
      {document && (
        <div
          style={{
            padding: '32px',
            maxWidth: '835px',
            margin: '0 auto',
          }}
        >
          {/* RETOUR */}
          <button
            onClick={() => navigate(`/organisations/${document.organizationId}`)}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              background: 'none',
              border: 'none',
              cursor: 'pointer',
              color: '#6b7280',
              fontSize: '13.5px',
              marginBottom: '16px',
              padding: 0,
            }}
          >
            <ArrowLeft size={16} />
            Retour à l'organisation
          </button>

          {/* HEADER */}
          <h1
            style={{
              fontSize: '24px',
              fontWeight: 700,
              marginBottom: '4px',
              color: '#111827',
            }}
          >
            Détails du document
          </h1>

          <p
            style={{
              color: '#6b7280',
              marginTop: 0,
              marginBottom: '28px',
              fontSize: '14px',
            }}
          >
            Consultez les informations et le statut de ce document.
          </p>

          {/* CARD */}
          <div
            style={{
              background: '#fff',
              border: '1px solid #e5e7eb',
              borderRadius: '10px',
              overflow: 'hidden',
              fontSize: '14px',
            }}
          >
            {/* TITRE INFORMATIONS GÉNÉRALES */}
            <div
              style={{
                padding: '13px 24px',
                background: '#f8fafc',
                borderBottom: '1px solid #e5e7eb',
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
                Informations générales
              </h3>
            </div>

            {/* INFORMATIONS GÉNÉRALES */}
            <div style={{ padding: '22px 24px' }}>
              <div
                style={{
                  display: 'grid',
                  gridTemplateColumns: '170px 1fr',
                  rowGap: '14px',
                }}
              >
                <span style={{ color: '#6b7280' }}>
                  Nom du fichier
                </span>
                <span style={{ fontWeight: 500 }}>
                  {document.filename}
                </span>

                <span style={{ color: '#6b7280' }}>
                  Utilisateur
                </span>
                <span style={{ fontWeight: 500 }}>
                  {document.ownerName}
                </span>

                <span style={{ color: '#6b7280' }}>
                  Organisation
                </span>
                <span style={{ fontWeight: 500 }}>
                  {document.organizationName}
                </span>
              </div>
            </div>

            {/* TITRE INFORMATIONS DU FICHIER */}
            <div
              style={{
                padding: '13px 24px',
                background: '#f8fafc',
                borderTop: '1px solid #e5e7eb',
                borderBottom: '1px solid #e5e7eb',
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
                Informations du fichier
              </h3>
            </div>

            {/* INFORMATIONS DU FICHIER */}
            <div style={{ padding: '22px 24px' }}>
              <div
                style={{
                  display: 'grid',
                  gridTemplateColumns: '170px 1fr',
                  rowGap: '14px',
                  alignItems: 'center',
                }}
              >
                <span style={{ color: '#6b7280' }}>
                  Type
                </span>
                <span style={{ fontWeight: 500 }}>
                  {formatType(document.type)}
                </span>

                <span style={{ color: '#6b7280' }}>
                  Taille
                </span>
                <span style={{ fontWeight: 500 }}>
                  {formatSize(document.size)}
                </span>

                <span style={{ color: '#6b7280' }}>
                  Date de dépôt
                </span>
                <span style={{ fontWeight: 500 }}>
                  {formatDate(document.uploadedAt)}
                </span>

                <span style={{ color: '#6b7280' }}>
                  Statut
                </span>

                <span
                  style={{
                    width: 'fit-content',
                    padding: '2px 10px',
                    borderRadius: '12px',
                    fontWeight: 500,
                    fontSize: '12px',
                    ...getStatusStyle(document.status),
                  }}
                >
                  {document.status}
                </span>
              </div>
            </div>
          </div>

          {/* BUTTONS */}
          <div
            style={{
              display: 'flex',
              justifyContent: 'flex-start',
              gap: '12px',
              marginTop: '24px',
              marginBottom: '32px',
              paddingLeft: '24px',
            }}
          >
            {/* F11: seul un PDF brut peut être converti (le service d'extraction
                interne rejette tout autre type - voir ExtractionService). */}
            {document.type === 'application/pdf' && (
              <button
                onClick={handleExtract}
                disabled={extracting}
                style={{
                  padding: '9px 18px',
                  background: '#fff',
                  color: '#1a2744',
                  border: '1px solid #d1d5db',
                  borderRadius: '6px',
                  fontSize: '13.5px',
                  cursor: extracting ? 'default' : 'pointer',
                  opacity: extracting ? 0.6 : 1,
                }}
              >
                {extracting ? 'Extraction en cours…' : 'Convertir en Factur-X'}
              </button>
            )}

            <button
              onClick={handleValidate}
              disabled={validating}
              style={{
                padding: '9px 18px',
                background: '#fff',
                color: '#1a2744',
                border: '1px solid #d1d5db',
                borderRadius: '6px',
                fontSize: '13.5px',
                cursor: validating ? 'default' : 'pointer',
                opacity: validating ? 0.6 : 1,
              }}
            >
              {validating ? 'Validation en cours…' : 'Lancer la validation Factur-X'}
            </button>
          </div>

          {/* RAPPORT DE VALIDATION (F08/F09) */}
          {validationError && (
            <p
              style={{
                color: '#991b1b',
                fontSize: '13.5px',
                marginTop: '-16px',
                marginBottom: '24px',
                paddingLeft: '24px',
              }}
            >
              {validationError}
            </p>
          )}

          {/* EXTRACTION (F11) */}
          {extractionError && (
            <p
              style={{
                color: '#991b1b',
                fontSize: '13.5px',
                marginTop: '-16px',
                marginBottom: '24px',
                paddingLeft: '24px',
              }}
            >
              {extractionError}
            </p>
          )}

          {report && (
            <div style={{ marginBottom: '32px' }}>
              <ValidationReportCard report={report} />
            </div>
          )}
        </div>
      )}

      {/* DOCUMENT INEXISTANT */}
      {!document && !loading && (
        <div
          style={{
            maxWidth: '700px',
            margin: '40px auto',
            padding: '0 24px',
          }}
        >
          <button
            onClick={() => navigate('/dashboard')}
            style={{
              background: 'transparent',
              border: 'none',
              padding: 0,
              marginBottom: '18px',
              color: '#6b7280',
              cursor: 'pointer',
              fontSize: '13px',
            }}
          >
            ← Retour
          </button>

          <div
            style={{
              background: '#fff',
              border: '1px solid #e5e7eb',
              borderRadius: '8px',
              padding: '24px',
            }}
          >
            <h2
              style={{
                margin: '0 0 8px',
                fontSize: '20px',
                color: '#1a1a2e',
              }}
            >
              Document
            </h2>

            <p
              style={{
                margin: 0,
                fontSize: '13px',
                color: '#b42318',
              }}
            >
              Document introuvable.
            </p>
          </div>
        </div>
      )}
    </div>
  );
}
