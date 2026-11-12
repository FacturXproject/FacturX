import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';
import StatusBadge from '../components/StatusBadge';
import { useDocumentStatusEvent } from '../context/DocumentWebSocketContext';

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
              `/documents?organizationId=${organizationId}&sort=uploadedAt,desc&size=5`
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


  useDocumentStatusEvent((event) => {
    if (event.type === 'websocket.reconnected') {
      loadRecentDocuments();
      return;
    }

    if (event.type !== 'document.status.changed') return;

    // Nouveau document (ex. facture Factur-X generee) : on recharge la liste.
    if (!documents.some(document => document.id === event.documentId)) {
      loadRecentDocuments();
      return;
    }

    setDocuments(current =>
      current.map(document =>
        document.id === event.documentId
          && document.organizationId === event.organizationId
          ? { ...document, status: event.status }
          : document
      )
    );
  });

  return (
    <div className="py-6 px-7 max-w-[900px]">
      {/* HEADER */}
      <div className="mb-6">
        <h1 className="m-0 text-[20px] font-bold text-[#1a1a2e]">
          Tableau de bord
        </h1>

        <p className="mt-1 mb-0 text-[13px] text-[#6b7280]">
          Consultez vos documents récents.
        </p>
      </div>

      {/* DOCUMENTS */}
      <div className="mt-8">
        <h2 className="mt-0 mb-3 text-[14px] font-semibold text-[#1a1a2e]">
          5 derniers documents
        </h2>

        <div className="border border-[#e5e7eb] rounded-lg overflow-hidden bg-white">
          <table className="w-full border-collapse text-[13px]">
            <thead>
              <tr className="bg-[#f9fafb]">
                {[
                  'Fichier',
                  'Organisation',
                  'Date',
                  'Type',
                  'Statut',
                ].map((title) => (
                  <th
                    key={title}
                    className="px-[14px] py-[10px] text-left font-semibold"
                  >
                    {title}
                  </th>
                ))}
              </tr>
            </thead>

            <tbody>
              {loading ? (
                <tr>
                  <td colSpan="5" className="p-5 text-center text-[#9ca3af]">
                    Chargement...
                  </td>
                </tr>
              ) : documents.length > 0 ? (
                documents.map((doc) => (
                  <tr key={doc.id} className="border-t border-[#f3f4f6]">
                    <td
                      onClick={() => handleDocumentClick(doc)}
                      className="px-[14px] py-[10px] cursor-pointer font-medium text-[#1a2744]"
                    >
                      {doc.filename}
                    </td>

                    <td className="px-[14px] py-[10px]">
                      {doc.organizationName}
                    </td>

                    <td className="px-[14px] py-[10px]">
                      {formatDate(doc.uploadedAt)}
                    </td>

                    <td className="px-[14px] py-[10px]">
                      {formatType(doc.type)}
                    </td>

                    <td className="px-[14px] py-[10px]">
                      <StatusBadge status={doc.status} />
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan="5" className="p-5 text-center text-[#9ca3af]">
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
