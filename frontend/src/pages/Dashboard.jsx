import { useEffect, useState, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';

function StatusBadge({ status }) {
  return (
    <span
      style={{
        display: 'inline-block',
        padding: '2px 8px',
        borderRadius: '4px',
        fontSize: '12px',
        fontWeight: 500,
        background: '#f0fdf4',
        color: '#166534',
        border: '1px solid #bbf7d0',
      }}
    >
      {status}
    </span>
  );
}

export default function Dashboard({ onFileSelect }) {
  const navigate = useNavigate();

  const [dragging, setDragging] = useState(false);
  const [documents, setDocuments] = useState([]);
  const [organizationId, setOrganizationId] = useState(null);

  const fileRef = useRef();

  // 1. Récupérer l'organisation de l'utilisateur
  useEffect(() => {
    api.get('/organizations')
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

  // 2. Charger les documents quand on connaît l'organisation
  useEffect(() => {
    if (!organizationId) return;

    loadDocuments();
  }, [organizationId]);

  const loadDocuments = async () => {
    try {
      const response = await api.get(
        `/documents?organizationId=${organizationId}`
      );

      setDocuments(response.data.content || []);
    } catch (error) {
      console.error('Documents error:', error);
    }
  };

  // 3. Upload
  const handleFileUpload = async (event) => {
    const file = event.target.files[0];

    if (!file || !organizationId) return;

    const formData = new FormData();
    formData.append('file', file);

    try {
      await api.post(
        `/documents?organizationId=${organizationId}`,
        formData
      );

      await loadDocuments();

      // permet de sélectionner à nouveau le même fichier
      event.target.value = '';
    } catch (error) {
      console.error('Upload error:', error);
    }
  };

  // 4. Delete
  const handleDelete = async (event, documentId) => {
    event.stopPropagation();

    try {
      await api.delete(`/documents/${documentId}`);

      // On retire directement le document du tableau
      setDocuments((previousDocuments) =>
        previousDocuments.filter((doc) => doc.id !== documentId)
      );
    } catch (error) {
      console.error('Delete error:', error);
    }
  };

  const handleAction = (type) => {
    navigate(`/traitement?action=${type}`);
  };

  const handleRowClick = (doc) => {
    if (onFileSelect) {
      onFileSelect(doc.filename);
    }

    navigate(`/documents/${doc.id}`);
  };

  const formatDate = (date) => {
    if (!date) return '';

    return new Date(date).toLocaleString('fr-FR');
  };

  return (
    <div style={{ padding: '24px 28px', maxWidth: '900px' }}>

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
          Déposez une facture pour la vérifier ou la convertir en Factur-X
        </p>
      </div>

      {/* UPLOAD */}
      <div
        onDragOver={(event) => {
          event.preventDefault();
          setDragging(true);
        }}
        onDragLeave={() => setDragging(false)}
        onDrop={(event) => {
          event.preventDefault();
          setDragging(false);
        }}
        onClick={() => fileRef.current?.click()}
        style={{
          border: `2px dashed ${dragging ? '#4a9eff' : '#d1d5db'}`,
          borderRadius: '10px',
          padding: '36px 24px',
          textAlign: 'center',
          background: dragging ? '#f0f7ff' : '#fafafa',
          cursor: 'pointer',
          marginBottom: '16px',
        }}
      >
        <input
          ref={fileRef}
          type="file"
          accept=".pdf,.xml"
          style={{ display: 'none' }}
          onChange={handleFileUpload}
        />

        <p
          style={{
            margin: '0 0 4px',
            fontWeight: 600,
            fontSize: '14px',
            color: '#1a1a2e',
          }}
        >
          Déposez une facture ici
        </p>

        <p
          style={{
            margin: 0,
            fontSize: '12.5px',
            color: '#9ca3af',
          }}
        >
          PDF ou XML — jusqu'à 10 Mo
        </p>
      </div>

      {/* BUTTONS */}
      <div
        style={{
          display: 'flex',
          gap: '12px',
          marginBottom: '32px',
        }}
      >
        <button
          onClick={() => handleAction('verifier')}
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
          onClick={() => handleAction('convertir')}
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

      {/* DOCUMENTS */}
      <div>
        <h2
          style={{
            margin: '0 0 12px',
            fontSize: '14px',
            fontWeight: 600,
            color: '#1a1a2e',
          }}
        >
          Documents récents
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
                {['Fichier', 'Date', 'Type', 'Statut', ''].map((title) => (
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
              {documents.map((doc, index) => (
                <tr
                  key={doc.id}
                  onClick={() => handleRowClick(doc)}
                  style={{
                    borderTop: '1px solid #f3f4f6',
                    cursor: 'pointer',
                  }}
                >
                  <td style={{ padding: '10px 14px' }}>
                    {doc.filename}
                  </td>

                  <td style={{ padding: '10px 14px' }}>
                    {formatDate(doc.uploadedAt)}
                  </td>

                  <td style={{ padding: '10px 14px' }}>
                    {doc.type}
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