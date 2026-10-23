import api from '../services/api';
import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import ValidationReportCard from '../components/ValidationReportCard';
import { GenerationList } from '../components/FacturXGenerationPanel';
import { downloadDocument } from '../utils/download';
import JobStatusPanel from '../components/JobStatusPanel';
import { useDocumentStatusEvent } from '../context/DocumentWebSocketContext';

export default function DocumentDetail() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [document, setDocument] = useState(null);
  const [loading, setLoading] = useState(true);
  const [report, setReport] = useState(null);
  const [validating, setValidating] = useState(false);
  const [validationError, setValidationError] = useState(null);
  const [validationJob, setValidationJob] = useState(null);
  const [extracting, setExtracting] = useState(false);
  const [extractionError, setExtractionError] = useState(null);
  const [generations, setGenerations] = useState([]);
  const [downloadError, setDownloadError] = useState(null);

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

  const formatType = (type) => {
    if (type === 'application/pdf') {
      return 'PDF';
    }

    if (type === 'application/xml' || type === 'text/xml') {
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

  const loadDocument = useCallback(async () => {
    try {
      const response = await api.get(`/documents/${id}`);
      setDocument(response.data || null);
    } catch (error) {
      console.error('Documents error:', error);
      setDocument(null);
    } finally {
      setLoading(false);
    }
  }, [id]);

  // F08/F09 : dernier rapport de validation.
  const loadReport = useCallback(async () => {
    try {
      const response = await api.get(`/documents/${id}/report`);
      setReport(response.data || null);
    } catch {
      setReport(null);
    }
  }, [id]);

  // F13 : factures Factur-X générées à partir du document.
  const loadGenerations = useCallback(async () => {
    try {
      const response = await api.get(`/documents/${id}/generations`);
      setGenerations(Array.isArray(response.data) ? response.data : []);
    } catch {
      setGenerations([]);
    }
  }, [id]);

  useEffect(() => {
    setDocument(null);
    setReport(null);
    setGenerations([]);
    setDownloadError(null);
    setLoading(true);
    setValidationJob(null);
    setValidationError(null);

    loadDocument();
    loadReport();
    loadGenerations();
  }, [loadDocument, loadReport, loadGenerations]);

  // F15 : chaque événement reçu est traité directement.
  useDocumentStatusEvent((event) => {
    if (event.type === 'websocket.reconnected') {
      loadDocument();
      loadReport();
      loadGenerations();
      return;
    }

    if (event.type !== 'document.status.changed') return;
    if (String(event.documentId) !== String(id)) return;

    setDocument(current => {
      if (
        !current
        || current.id !== event.documentId
        || current.organizationId !== event.organizationId
      ) {
        return current;
      }

      return { ...current, status: event.status };
    });

    if (event.status === 'VALID' || event.status === 'INVALID') {
      loadReport();
    }
  });

  const handleDownloadGenerated = async (generation) => {
    setDownloadError(null);

    try {
      await downloadDocument(
        generation.generatedDocumentId,
        generation.filename
      );
    } catch {
      setDownloadError('Téléchargement impossible.');
    }
  };

  // F11 : extraction du PDF puis ouverture du formulaire F12.
  const handleExtract = async () => {
    setExtracting(true);
    setExtractionError(null);

    try {
      await api.post(`/documents/${id}/extract`);
      navigate(`/conversion?documentId=${id}`);
    } catch (error) {
      console.error('Extraction error:', error);
      setExtractionError(
        error.response?.data?.message
          || "Échec de l'extraction des données."
      );
      setExtracting(false);
    }
  };

  // F14 : validation en arrière-plan via un job.
  const handleValidate = async () => {
    setValidating(true);
    setValidationError(null);
    setValidationJob(null);

    try {
      const response = await api.post(
        `/jobs/validation?documentId=${id}`
      );

      setValidationJob(response.data);
    } catch (error) {
      console.error('Validation job error:', error);

      setValidationError(
        error.response?.data?.message
          || error.response?.data?.error
          || 'Impossible de démarrer la validation.'
      );
    } finally {
      setValidating(false);
    }
  };

  const handleValidationJobDone = useCallback(async () => {
    await Promise.all([
      loadDocument(),
      loadReport(),
    ]);
  }, [loadDocument, loadReport]);

  const validationJobRunning =
    validating
    || validationJob?.status === 'QUEUED'
    || validationJob?.status === 'PROCESSING';

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
            onClick={() =>
              navigate(`/organisations/${document.organizationId}`)
            }
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
            {/* INFORMATIONS GÉNÉRALES */}
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

            {/* INFORMATIONS DU FICHIER */}
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

            <div style={{ padding: '22px 24px' }}>
              <div
                style={{
                  display: 'grid',
                  gridTemplateColumns: '170px 1fr',
                  rowGap: '14px',
                  alignItems: 'center',
                }}
              >
                <span style={{ color: '#6b7280' }}>Type</span>
                <span style={{ fontWeight: 500 }}>
                  {formatType(document.type)}
                </span>

                <span style={{ color: '#6b7280' }}>Taille</span>
                <span style={{ fontWeight: 500 }}>
                  {formatSize(document.size)}
                </span>

                <span style={{ color: '#6b7280' }}>
                  Date de dépôt
                </span>
                <span style={{ fontWeight: 500 }}>
                  {formatDate(document.uploadedAt)}
                </span>

                <span style={{ color: '#6b7280' }}>Statut</span>

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
                {extracting
                  ? 'Extraction en cours…'
                  : 'Convertir en Factur-X'}
              </button>
            )}

            <button
              onClick={handleValidate}
              disabled={validationJobRunning}
              style={{
                padding: '9px 18px',
                background: '#fff',
                color: '#1a2744',
                border: '1px solid #d1d5db',
                borderRadius: '6px',
                fontSize: '13.5px',
                cursor: validationJobRunning ? 'default' : 'pointer',
                opacity: validationJobRunning ? 0.6 : 1,
              }}
            >
              {validationJobRunning
                ? 'Validation en cours…'
                : 'Lancer la validation Factur-X'}
            </button>
          </div>

          {/* RAPPORT DE VALIDATION */}
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

          {validationJob && validationJob.status !== 'DONE' && (
            <div
              style={{
                paddingLeft: '24px',
                paddingRight: '24px',
                marginBottom: '24px',
              }}
            >
              <JobStatusPanel
                job={validationJob}
                onJobChange={setValidationJob}
                onDone={handleValidationJobDone}
              />
            </div>
          )}

          {/* EXTRACTION */}
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

          {/* FACTURES FACTUR-X GÉNÉRÉES */}
          {generations.length > 0 && (
            <div
              style={{
                background: '#fff',
                border: '1px solid #e5e7eb',
                borderRadius: '10px',
                overflow: 'hidden',
                fontSize: '14px',
                marginBottom: '32px',
              }}
            >
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
                  Factures Factur-X générées
                </h3>
              </div>

              <div style={{ padding: '12px 24px' }}>
                <GenerationList
                  generations={generations}
                  onDownload={handleDownloadGenerated}
                />

                {downloadError && (
                  <p
                    role="alert"
                    style={{
                      color: '#991b1b',
                      fontSize: '13px',
                      margin: '8px 0 0',
                    }}
                  >
                    {downloadError}
                  </p>
                )}
              </div>
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