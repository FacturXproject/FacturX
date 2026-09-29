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
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadRecentDocuments();
  }, []);

  const loadRecentDocuments = async () => {
    try {
      setLoading(true);

      // 1. Récupérer toutes les organisations de l'utilisateur
      const organizationsResponse = await api.get('/organizations');

      const organizations = organizationsResponse.data || [];

      // 2. Pour chaque organisation :
      //    - récupérer son nom
      //    - récupérer ses documents
      const results = await Promise.all(
        organizations.map(async (organization) => {
          const organizationId =
            organization.organizationId ?? organization.id;

          let organizationName = `Organisation #${organizationId}`;

          try {
            const organizationDetails = await api.get(
              `/organizations/${organizationId}`
            );

            organizationName =
              organizationDetails.data.name ?? organizationName;
          } catch (error) {
            console.error(
              `Organization ${organizationId} details error:`,
              error
            );
          }

          try {
            const documentsResponse = await api.get(
              `/documents?organizationId=${organizationId}`
            );

            const organizationDocuments =
              documentsResponse.data.content || [];

            return organizationDocuments.map((document) => ({
              ...document,
              organizationName,
            }));
          } catch (error) {
            console.error(
              `Documents for organization ${organizationId} error:`,
              error
            );

            return [];
          }
        })
      );

      // 3. Fusionner les documents de toutes les organisations
      const allDocuments = results.flat();

      // 4. Trier du plus récent au plus ancien
      //    puis garder uniquement les 5 derniers
      const recentDocuments = allDocuments
        .sort(
          (a, b) =>
            new Date(b.uploadedAt) - new Date(a.uploadedAt)
        )
        .slice(0, 5);

      setDocuments(recentDocuments);
    } catch (error) {
      console.error('Dashboard error:', error);
      setDocuments([]);
    } finally {
      setLoading(false);
    }
  };

  // Ouvrir le détail du document
  const handleDocumentClick = (doc) => {
    if (onFileSelect) {
      onFileSelect(doc.filename);
    }

    navigate(`/documents/${doc.id}`);
  };

  const formatDate = (date) => {
    if (!date) return '';

    return new Date(date).toLocaleDateString('fr-FR');
  };

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
                  'Organisation',
                  'Date',
                  'Type',
                  'Statut',
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
              {loading ? (
                <tr>
                  <td
                    colSpan="5"
                    style={{
                      padding: '20px',
                      textAlign: 'center',
                      color: '#9ca3af',
                    }}
                  >
                    Chargement...
                  </td>
                </tr>
              ) : documents.length > 0 ? (
                documents.map((doc) => (
                  <tr
                    key={doc.id}
                    style={{
                      borderTop: '1px solid #f3f4f6',
                    }}
                  >
                    <td
                      onClick={() => handleDocumentClick(doc)}
                      style={{
                        padding: '10px 14px',
                        cursor: 'pointer',
                        fontWeight: 500,
                        color: '#1a2744',
                      }}
                    >
                      {doc.filename}
                    </td>

                    <td style={{ padding: '10px 14px' }}>
                      {doc.organizationName}
                    </td>

                    <td style={{ padding: '10px 14px' }}>
                      {formatDate(doc.uploadedAt)}
                    </td>

                    <td style={{ padding: '10px 14px' }}>
                      {formatType(doc.type)}
                    </td>

                    <td style={{ padding: '10px 14px' }}>
                      <StatusBadge status={doc.status} />
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td
                    colSpan="5"
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
