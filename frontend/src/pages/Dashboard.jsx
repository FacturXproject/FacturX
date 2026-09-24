import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';

function StatusBadge({ status }) {
  const getStatusStyle = () => {
    switch (status) {
      case 'VALID':
        return {
          background: '#dcfce7',
          color: '#166534',
          border: '1px solid #bbf7d0',
        };

      case 'INVALID':
      case 'FAILED':
        return {
          background: '#fee2e2',
          color: '#991b1b',
          border: '1px solid #fecaca',
        };

      case 'PROCESSING':
      case 'QUEUED':
        return {
          background: '#fef3c7',
          color: '#92400e',
          border: '1px solid #fde68a',
        };

      case 'UPLOADED':
      default:
        return {
          background: '#f3f4f6',
          color: '#374151',
          border: '1px solid #d1d5db',
        };
    }
  };

  return (
    <span
      style={{
        display: 'inline-block',
        padding: '2px 8px',
        borderRadius: '4px',
        fontSize: '12px',
        fontWeight: 500,
        ...getStatusStyle(),
      }}
    >
      {status}
    </span>
  );
}

export default function Dashboard({ onFileSelect }) {
  const navigate = useNavigate();

  const [documents, setDocuments] = useState([]);
  const [organizationId, setOrganizationId] = useState(null);

  // Récupérer l'organisation de l'utilisateur
  useEffect(() => {
    api
      .get('/organizations')
      .then((response) => {
        if (response.data.length > 0) {
          const org = response.data[0];
          setOrganizationId(org.organizationId ?? org.id);
        }
      })
      .catch((error) => {
        console.error('Organizations error:', error);
      });
  }, []);

  // Charger les documents quand on connaît l'organisation
  useEffect(() => {
    if (!organizationId) return;

    loadDocuments();
  }, [organizationId]);

  const loadDocuments = async () => {
    try {
      const response = await api.get(
        `/documents?organizationId=${organizationId}`
      );

      const allDocuments = response.data.content || [];

      // Garder seulement les 5 documents les plus récents
      const recentDocuments = [...allDocuments]
        .sort(
          (a, b) =>
            new Date(b.uploadedAt) - new Date(a.uploadedAt)
        )
        .slice(0, 5);

      setDocuments(recentDocuments);
    } catch (error) {
      console.error('Documents error:', error);
    }
  };

  // Supprimer un document
  const handleDelete = async (event, documentId) => {
    event.stopPropagation();

    try {
      await api.delete(`/documents/${documentId}`);

      setDocuments((previousDocuments) =>
        previousDocuments.filter(
          (doc) => doc.id !== documentId
        )
      );
    } catch (error) {
      console.error('Delete error:', error);
    }
  };

  // Ouvrir le détail du document
  const handleRowClick = (doc) => {
    if (onFileSelect) {
      onFileSelect(doc.filename);
    }

    navigate(`/documents/${doc.id}`);
  };

  // Date uniquement
  const formatDate = (date) => {
    if (!date) return '';

    return new Date(date).toLocaleDateString('fr-FR');
  };

  // Heure uniquement
  const formatTime = (date) => {
    if (!date) return '';

    return new Date(date).toLocaleTimeString('fr-FR', {
      hour: '2-digit',
      minute: '2-digit',
    });
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

  return (
    <div
      style={{
        padding: '24px 28px',
        maxWidth: '900px',
      }}
    >
      {/* HEADER */}
      <div style={{ marginBottom: '24px' }}>
        <h1
          style={{
            margin: 0,
            fontSize: '20px',
            fontWeight: 700,
            color: '#1a1a2e',
          }}
        >
          Tableau de bord
        </h1>

        <p
          style={{
            margin: '4px 0 0',
            fontSize: '13px',
            color: '#6b7280',
          }}
        >
          Consultez vos documents récents.
        </p>
      </div>

      {/* DOCUMENTS */}
      <div style={{ marginTop: '32px' }}>
        <h2
          style={{
            margin: '0 0 12px',
            fontSize: '14px',
            fontWeight: 600,
            color: '#1a1a2e',
          }}
        >
          5 derniers documents
        </h2>

        <div
          style={{
            border: '1px solid #e5e7eb',
            borderRadius: '8px',
            overflow: 'hidden',
            background: '#fff',
          }}
        >
          <table
            style={{
              width: '100%',
              borderCollapse: 'collapse',
              fontSize: '13px',
            }}
          >
            <thead>
              <tr style={{ background: '#f9fafb' }}>
                {[
                  'Fichier',
                  'Date',
                  'Heure',
                  'Type',
                  'Statut',
                  'Action',
                ].map((title) => (
                  <th
                    key={title}
                    style={{
                      padding: '10px 14px',
                      textAlign: 'left',
                      fontWeight: 600,
                    }}
                  >
                    {title}
                  </th>
                ))}
              </tr>
            </thead>

            <tbody>
              {documents.map((doc) => (
                <tr
                  key={doc.id}
                  style={{
                    borderTop: '1px solid #f3f4f6',
                  }}
                >
                  {/* Seul le nom du fichier ouvre le détail */}
                  <td
                    onClick={() => handleRowClick(doc)}
                    style={{
                      padding: '10px 14px',
                      cursor: 'pointer',
                      fontWeight: 500,
                    }}
                  >
                    {doc.filename}
                  </td>

                  <td style={{ padding: '10px 14px' }}>
                    {formatDate(doc.uploadedAt)}
                  </td>

                  <td style={{ padding: '10px 14px' }}>
                    {formatTime(doc.uploadedAt)}
                  </td>

                  <td style={{ padding: '10px 14px' }}>
                    {formatType(doc.type)}
                  </td>

                  <td style={{ padding: '10px 14px' }}>
                    <StatusBadge status={doc.status} />
                  </td>

                  <td style={{ padding: '10px 14px' }}>
                    <button
                      onClick={(event) =>
                        handleDelete(event, doc.id)
                      }
                      style={{
                        background: 'transparent',
                        border: 'none',
                        cursor: 'pointer',
                        color: '#b91c1c',
                        fontSize: '12px',
                      }}
                    >
                      Supprimer
                    </button>
                  </td>
                </tr>
              ))}

              {documents.length === 0 && (
                <tr>
                  <td
                    colSpan="6"
                    style={{
                      padding: '20px',
                      textAlign: 'center',
                      color: '#9ca3af',
                    }}
                  >
                    Aucun document
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}