import api from '../services/api';
import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';

export default function DocumentDetail() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [document, setDocument] = useState(null);
  const [loading, setLoading] = useState(true);

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

<<<<<<< HEAD
=======
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

>>>>>>> origin/feature/documents-status_avalent2
  const loadDocument = async () => {
    try {
      const response = await api.get(`/documents/${id}`);
      setDocument(response.data || null);
    } catch (error) {
      console.error('Documents error:', error);
<<<<<<< HEAD
=======
      setDocument(null);
>>>>>>> origin/feature/documents-status_avalent2
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
<<<<<<< HEAD
    loadDocument();
  }, []);
=======
    setDocument(null);
    setLoading(true);
    loadDocument();
  }, [id]);
>>>>>>> origin/feature/documents-status_avalent2

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
            onClick={() => navigate('/dashboard')}
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
            Retour au tableau de bord
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
<<<<<<< HEAD
                  {document.organizationId}
=======
                  {document.organizationName}
>>>>>>> origin/feature/documents-status_avalent2
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
<<<<<<< HEAD
                  {document.type}
=======
                  {formatType(document.type)}
>>>>>>> origin/feature/documents-status_avalent2
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
<<<<<<< HEAD
                    background: '#dcfce7',
                    color: '#166534',
                    fontWeight: 500,
                    fontSize: '12px',
=======
                    fontWeight: 500,
                    fontSize: '12px',
                    ...getStatusStyle(document.status),
>>>>>>> origin/feature/documents-status_avalent2
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
            <button
              onClick={() =>
                navigate(`/verifier?documentId=${document.id}`)
              }
              style={{
                padding: '9px 18px',
                background: '#1a2744',
                color: '#fff',
                border: 'none',
                borderRadius: '6px',
                fontSize: '13.5px',
                cursor: 'pointer',
              }}
            >
              Vérifier la conformité
            </button>

            <button
              onClick={() =>
                navigate(`/convertir?documentId=${document.id}`)
              }
              style={{
                padding: '9px 18px',
                background: '#fff',
                color: '#1a2744',
                border: '1px solid #d1d5db',
                borderRadius: '6px',
                fontSize: '13.5px',
                cursor: 'pointer',
              }}
            >
              Convertir en Factur-X
            </button>
          </div>
        </div>
      )}

      {/* DOCUMENT INEXISTANT */}
      {!document && !loading && (
<<<<<<< HEAD
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
=======
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
>>>>>>> origin/feature/documents-status_avalent2
}