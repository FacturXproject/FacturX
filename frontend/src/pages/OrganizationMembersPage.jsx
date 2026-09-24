import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  ArrowLeft,
  Trash2,
  AlertTriangle,
  Mail,
  Clock,
  Eye,
} from 'lucide-react';
import api from '../services/api';
import DocumentUploadForm from '../components/DocumentUploadForm';

const roleLabels = {
  ADMIN: 'Administrateur',
  ACCOUNTANT: 'Comptable',
  CLIENT: 'Client',
};

const roleBadgeStyles = {
  ADMIN: { background: '#dbeafe', color: '#1d4ed8' },
  ACCOUNTANT: { background: '#dcfce7', color: '#15803d' },
  CLIENT: { background: '#f3e8ff', color: '#7e22ce' },
};

export default function OrganizationMembersPage() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [orgName, setOrgName] = useState(null);
  const [members, setMembers] = useState([]);
  const [invitations, setInvitations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [inviteEmail, setInviteEmail] = useState('');
  const [selectedRole, setSelectedRole] = useState('CLIENT');
  const [adding, setAdding] = useState(false);
  const [addError, setAddError] = useState(null);

  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState(null);

  const [currentUserId, setCurrentUserId] = useState(null);

  const [documents, setDocuments] = useState([]);
  const [documentsLoading, setDocumentsLoading] = useState(true);
  const [documentsError, setDocumentsError] = useState(null);

  const [currentRole, setCurrentRole] = useState(null);
  const isAdmin = currentRole === 'ADMIN';

  const [documentsPage, setDocumentsPage] = useState(0);

  const documentsPerPage = 5;

  const startIndex = documentsPage * documentsPerPage;
  const visibleDocuments = documents.slice(
    startIndex,
    startIndex + documentsPerPage
  );

const totalPages = Math.ceil(documents.length / documentsPerPage);

  const fetchCurrentRole = async () => {
    try {
      const response = await api.get('/organizations');

      const membership = response.data.find((org) => {
        const organizationId = org.organizationId ?? org.id;
        return String(organizationId) === String(id);
      });

      setCurrentRole(membership?.role ?? null);
      setCurrentUserId(membership?.userId ?? null);
    } catch {
      setCurrentRole(null);
      setCurrentUserId(null);
    }
  };

  const fetchOrganization = async () => {
    try {
      const response = await api.get(`/organizations/${id}`);
      setOrgName(response.data.name);
    } catch {
      // fallback silencieux
    }
  };

  const fetchMembers = async () => {
    try {
      const response = await api.get(`/organizations/${id}/members`);
      setMembers(response.data);
      setError(null);
    } catch (err) {
      setError(err.response?.data?.message ?? err.message);
    } finally {
      setLoading(false);
    }
  };

  const fetchInvitations = async () => {
    try {
      const response = await api.get(
        `/organizations/${id}/invitations`
      );

      setInvitations(
        response.data.filter((inv) => inv.status === 'PENDING')
      );
    } catch {
      // liste optionnelle, on ignore l'erreur
    }
  };

  const fetchDocuments = async () => {
    setDocumentsLoading(true);

    try {
      const response = await api.get('/documents', {
        params: {
          organizationId: id,
        },
      });

      setDocuments(response.data.content || []);
      setDocumentsError(null);
    } catch (err) {
      setDocumentsError(
        err.response?.data?.message ?? err.message
      );
    } finally {
      setDocumentsLoading(false);
    }
  };

  useEffect(() => {
    fetchOrganization();
    fetchMembers();
    fetchCurrentRole();
    fetchDocuments();

    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  useEffect(() => {
    if (isAdmin) {
      fetchInvitations();
    } else {
      setInvitations([]);
    }

    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isAdmin, id]);

  const handleInvite = async (e) => {
    e.preventDefault();

    if (!inviteEmail.trim()) {
      return;
    }

    setAdding(true);
    setAddError(null);

    try {
      await api.post(`/organizations/${id}/invitations`, {
        email: inviteEmail.trim(),
        role: selectedRole,
      });

      setInviteEmail('');
      fetchInvitations();
    } catch (err) {
      setAddError(
        err.response?.data?.message ?? err.message
      );
    } finally {
      setAdding(false);
    }
  };

  const handleRevokeInvitation = async (invitationId) => {
    try {
      await api.patch(
        `/organizations/${id}/invitations/${invitationId}/revoke`
      );

      fetchInvitations();
    } catch (err) {
      setAddError(
        err.response?.data?.message ?? err.message
      );
    }
  };

  const handleRemoveMember = async (userId) => {
    try {
      await api.delete(
        `/organizations/${id}/members/${userId}`
      );

      fetchMembers();
    } catch (err) {
      setError(
        err.response?.data?.message ?? err.message
      );
    }
  };

  const handleRoleChange = async (userId, newRole) => {
    try {
      await api.patch(
        `/organizations/${id}/members/${userId}/role`,
        null,
        {
          params: {
            role: newRole,
          },
        }
      );

      fetchMembers();
    } catch (err) {
      setError(
        err.response?.data?.message ?? err.message
      );
    }
  };

  const handleDeleteOrganization = async () => {
    setDeleting(true);
    setDeleteError(null);

    try {
      await api.delete(`/organizations/${id}`);
      navigate('/organisations');
    } catch (err) {
      if (err.response?.status === 409) {
        setDeleteError(
          "Impossible de supprimer cette organisation tant qu'elle contient des documents."
        );
      } else {
        setDeleteError(
          "Une erreur est survenue lors de la suppression de l'organisation."
        );
      }

      setDeleting(false);
    }
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
        padding: '32px',
        maxWidth: '900px',
        margin: '0 auto',
      }}
    >
      <button
        onClick={() => navigate('/organisations')}
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
        Retour aux organisations
      </button>

      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'flex-start',
        }}
      >
        <div>
          <h1
            style={{
              fontSize: '24px',
              fontWeight: 700,
              marginBottom: '4px',
              color: '#111827',
            }}
          >
            Membres de {orgName ?? `l'organisation #${id}`}
          </h1>

          <p
            style={{
              color: '#6b7280',
              marginBottom: '24px',
              fontSize: '14px',
            }}
          >
            {isAdmin
              ? 'Gérez les membres de cette organisation et leurs rôles.'
              : 'Consultez les membres de cette organisation.'}
          </p>
        </div>

        {isAdmin && (
          <button
            onClick={() => setShowDeleteConfirm(true)}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              background: '#fff',
              color: '#dc2626',
              border: '1px solid #fecaca',
              borderRadius: '8px',
              padding: '9px 14px',
              fontSize: '13px',
              fontWeight: 500,
              cursor: 'pointer',
            }}
          >
            <Trash2 size={15} />
            Supprimer l'organisation
          </button>
        )}
      </div>

      {loading && (
        <p style={{ color: '#6b7280' }}>
          Chargement...
        </p>
      )}

      {error && (
        <div
          style={{
            background: '#fee2e2',
            color: '#991b1b',
            padding: '12px 16px',
            borderRadius: '8px',
            marginBottom: '16px',
          }}
        >
          {error}
        </div>
      )}

      {!loading && !error && (
        <div
          style={{
            background: '#fff',
            border: '1px solid #e5e7eb',
            borderRadius: '10px',
            overflow: 'hidden',
            marginBottom: '24px',
          }}
        >
          <table
            style={{
              width: '100%',
              borderCollapse: 'collapse',
              fontSize: '13.5px',
            }}
          >
            <thead>
              <tr
                style={{
                  background: '#f9fafb',
                  borderBottom: '1px solid #e5e7eb',
                }}
              >
                <th
                  style={{
                    textAlign: 'left',
                    padding: '12px 16px',
                    color: '#6b7280',
                    fontWeight: 500,
                  }}
                >
                  Nom
                </th>

                <th
                  style={{
                    textAlign: 'left',
                    padding: '12px 16px',
                    color: '#6b7280',
                    fontWeight: 500,
                  }}
                >
                  Email
                </th>

                <th
                  style={{
                    textAlign: 'left',
                    padding: '12px 16px',
                    color: '#6b7280',
                    fontWeight: 500,
                  }}
                >
                  Rôle
                </th>

                <th
                  style={{
                    textAlign: 'left',
                    padding: '12px 16px',
                    color: '#6b7280',
                    fontWeight: 500,
                  }}
                >
                  Actions
                </th>
              </tr>
            </thead>

            <tbody>
              {(() => {
                const adminCount = members.filter(
                  (m) => m.role === 'ADMIN'
                ).length;

                return members.map((member) => {
                  const badge =
                    roleBadgeStyles[member.role] ?? {
                      background: '#f3f4f6',
                      color: '#374151',
                    };

                  const isCurrentUser =
                    String(member.userId) ===
                    String(currentUserId);

                  const isLastAdmin =
                    member.role === 'ADMIN' &&
                    adminCount === 1;

                  const protectCurrentLastAdmin =
                    isCurrentUser && isLastAdmin;

                  return (
                    <tr
                      key={member.id}
                      style={{
                        borderBottom:
                          '1px solid #f3f4f6',
                      }}
                    >
                      <td
                        style={{
                          padding: '12px 16px',
                          fontWeight: 500,
                          color: '#111827',
                        }}
                      >
                        {member.firstName}{' '}
                        {member.lastName}
                      </td>

                      <td
                        style={{
                          padding: '12px 16px',
                          color: '#6b7280',
                        }}
                      >
                        {member.email}
                      </td>

                      <td
                        style={{
                          padding: '12px 16px',
                        }}
                      >
                        {isAdmin ? (
                          <select
                            value={member.role}
                            onChange={(e) =>
                              handleRoleChange(
                                member.userId,
                                e.target.value
                              )
                            }
                            disabled={
                              protectCurrentLastAdmin
                            }
                            style={{
                              padding: '5px 8px',
                              border:
                                '1px solid #d1d5db',
                              borderRadius: '6px',
                              fontSize: '12.5px',
                              background: '#fff',
                              color: '#374151',
                              cursor:
                                protectCurrentLastAdmin
                                  ? 'not-allowed'
                                  : 'pointer',
                              opacity:
                                protectCurrentLastAdmin
                                  ? 0.6
                                  : 1,
                            }}
                          >
                            <option value="ADMIN">
                              Administrateur
                            </option>
                            <option value="ACCOUNTANT">
                              Comptable
                            </option>
                            <option value="CLIENT">
                              Client
                            </option>
                          </select>
                        ) : (
                          <span
                            style={{
                              background:
                                badge.background,
                              color: badge.color,
                              padding: '3px 10px',
                              borderRadius: '999px',
                              fontSize: '12px',
                              fontWeight: 500,
                            }}
                          >
                            {roleLabels[
                              member.role
                            ] ?? member.role}
                          </span>
                        )}
                      </td>

                      <td
                        style={{
                          padding: '12px 16px',
                        }}
                      >
                        {isAdmin && (
                          <button
                            onClick={() =>
                              handleRemoveMember(
                                member.userId
                              )
                            }
                            disabled={
                              protectCurrentLastAdmin
                            }
                            style={{
                              display: 'flex',
                              alignItems: 'center',
                              gap: '4px',
                              background: 'none',
                              border: 'none',
                              cursor:
                                protectCurrentLastAdmin
                                  ? 'not-allowed'
                                  : 'pointer',
                              color:
                                protectCurrentLastAdmin
                                  ? '#9ca3af'
                                  : '#dc2626',
                              fontSize: '12.5px',
                              padding: '4px 8px',
                              borderRadius: '6px',
                            }}
                          >
                            <Trash2 size={14} />
                            Retirer
                          </button>
                        )}
                      </td>
                    </tr>
                  );
                });
              })()}
            </tbody>
          </table>

          {members.length === 0 && (
            <p
              style={{
                color: '#9ca3af',
                padding: '16px',
                fontSize: '14px',
              }}
            >
              Aucun membre pour l'instant.
            </p>
          )}
        </div>
      )}

      {/* Invitations en attente */}
      {isAdmin && invitations.length > 0 && (
        <div
          style={{
            background: '#fff',
            border: '1px solid #e5e7eb',
            borderRadius: '10px',
            overflow: 'hidden',
            marginBottom: '24px',
          }}
        >
          <div
            style={{
              padding: '14px 16px',
              borderBottom: '1px solid #e5e7eb',
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
            }}
          >
            <Clock size={16} color="#6b7280" />
            <span
              style={{
                fontSize: '14px',
                fontWeight: 600,
                color: '#111827',
              }}
            >
              Invitations en attente
            </span>
          </div>

          <table
            style={{
              width: '100%',
              borderCollapse: 'collapse',
              fontSize: '13.5px',
            }}
          >
            <tbody>
              {invitations.map((inv) => (
                <tr
                  key={inv.id}
                  style={{
                    borderBottom:
                      '1px solid #f3f4f6',
                  }}
                >
                  <td
                    style={{
                      padding: '10px 16px',
                      color: '#374151',
                    }}
                  >
                    {inv.email}
                  </td>

                  <td
                    style={{
                      padding: '10px 16px',
                    }}
                  >
                    <span
                      style={{
                        background: '#fef3c7',
                        color: '#a16207',
                        padding: '3px 10px',
                        borderRadius: '999px',
                        fontSize: '12px',
                        fontWeight: 500,
                      }}
                    >
                      {roleLabels[inv.role] ??
                        inv.role}
                    </span>
                  </td>

                  <td
                    style={{
                      padding: '10px 16px',
                    }}
                  >
                    <button
                      onClick={() =>
                        handleRevokeInvitation(
                          inv.id
                        )
                      }
                      style={{
                        background: 'none',
                        border: 'none',
                        cursor: 'pointer',
                        color: '#dc2626',
                        fontSize: '12.5px',
                        padding: '4px 8px',
                        borderRadius: '6px',
                      }}
                    >
                      Révoquer
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Formulaire d'invitation */}
      {isAdmin && (
        <div
          style={{
            background: '#fff',
            border: '1px solid #e5e7eb',
            borderRadius: '10px',
            padding: '20px',
            marginBottom: '24px',
          }}
        >
          <h2
            style={{
              fontSize: '15px',
              fontWeight: 600,
              marginBottom: '14px',
              color: '#111827',
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
            }}
          >
            <Mail size={17} />
            Inviter un membre
          </h2>

          <form
            onSubmit={handleInvite}
            style={{
              display: 'flex',
              gap: '10px',
              alignItems: 'flex-end',
              flexWrap: 'wrap',
            }}
          >
            <div
              style={{
                flex: 1,
                minWidth: '200px',
              }}
            >
              <label
                style={{
                  display: 'block',
                  fontSize: '12.5px',
                  color: '#374151',
                  marginBottom: '5px',
                }}
              >
                Adresse email
              </label>

              <input
                type="email"
                value={inviteEmail}
                onChange={(e) =>
                  setInviteEmail(e.target.value)
                }
                placeholder="email@exemple.fr"
                style={{
                  width: '100%',
                  padding: '8px 10px',
                  border: '1px solid #d1d5db',
                  borderRadius: '8px',
                  fontSize: '13.5px',
                  boxSizing: 'border-box',
                }}
              />
            </div>

            <div
              style={{
                minWidth: '150px',
              }}
            >
              <label
                style={{
                  display: 'block',
                  fontSize: '12.5px',
                  color: '#374151',
                  marginBottom: '5px',
                }}
              >
                Rôle
              </label>

              <select
                value={selectedRole}
                onChange={(e) =>
                  setSelectedRole(e.target.value)
                }
                style={{
                  width: '100%',
                  padding: '8px 10px',
                  border: '1px solid #d1d5db',
                  borderRadius: '8px',
                  fontSize: '13.5px',
                  boxSizing: 'border-box',
                }}
              >
                <option value="ADMIN">
                  Administrateur
                </option>
                <option value="ACCOUNTANT">
                  Comptable
                </option>
                <option value="CLIENT">
                  Client
                </option>
              </select>
            </div>

            <button
              type="submit"
              disabled={
                adding || !inviteEmail.trim()
              }
              style={{
                padding: '9px 18px',
                borderRadius: '8px',
                border: 'none',
                background:
                  adding || !inviteEmail.trim()
                    ? '#9ca3af'
                    : '#1a2744',
                color: '#fff',
                cursor:
                  adding || !inviteEmail.trim()
                    ? 'not-allowed'
                    : 'pointer',
                fontSize: '13.5px',
              }}
            >
              {adding ? 'Envoi...' : 'Inviter'}
            </button>
          </form>

          {addError && (
            <div
              style={{
                background: '#fee2e2',
                color: '#991b1b',
                padding: '8px 12px',
                borderRadius: '6px',
                fontSize: '13px',
                marginTop: '10px',
              }}
            >
              {addError}
            </div>
          )}
        </div>
      )}

      {/* Depot de documents */}
      <div
        style={{
          background: '#fff',
          border: '1px solid #e5e7eb',
          borderRadius: '10px',
          padding: '20px',
          marginBottom: '24px',
        }}
      >
        <h2
          style={{
            fontSize: '15px',
            fontWeight: 600,
            marginBottom: '14px',
            color: '#111827',
          }}
        >
          Déposer un document
        </h2>

        <DocumentUploadForm
          organizationId={id}
          onUploaded={() => fetchDocuments()}
        />
      </div>

           {/* Documents de l'organisation */}
      <div
        style={{
          background: '#fff',
          border: '1px solid #e5e7eb',
          borderRadius: '10px',
          overflow: 'hidden',
          marginBottom: '24px',
        }}
      >
        {/* HEADER */}
        <div
          style={{
            padding: '14px 16px',
            borderBottom: '1px solid #e5e7eb',
          }}
        >
          <h2
            style={{
              margin: 0,
              fontSize: '15px',
              fontWeight: 600,
              color: '#111827',
            }}
          >
            Documents de l'organisation
          </h2>
        </div>

        {/* LOADING */}
        {documentsLoading && (
          <p
            style={{
              padding: '16px',
              margin: 0,
              color: '#6b7280',
              fontSize: '13.5px',
            }}
          >
            Chargement des documents...
          </p>
        )}

        {/* ERROR */}
        {documentsError && (
          <p
            style={{
              padding: '16px',
              margin: 0,
              color: '#b42318',
              fontSize: '13.5px',
            }}
          >
            Erreur : {documentsError}
          </p>
        )}

        {/* AUCUN DOCUMENT */}
        {!documentsLoading &&
          !documentsError &&
          documents.length === 0 && (
            <p
              style={{
                padding: '16px',
                margin: 0,
                color: '#9ca3af',
                fontSize: '13.5px',
              }}
            >
              Aucun document pour cette organisation.
            </p>
          )}

        {/* DOCUMENTS */}
        {!documentsLoading &&
          !documentsError &&
          documents.length > 0 && (
            <>
              <table
                style={{
                  width: '100%',
                  borderCollapse: 'collapse',
                  fontSize: '13.5px',
                }}
              >
                <thead>
                  <tr
                    style={{
                      background: '#f9fafb',
                      borderBottom: '1px solid #e5e7eb',
                    }}
                  >
                    <th
                      style={{
                        textAlign: 'left',
                        padding: '12px 16px',
                        color: '#6b7280',
                        fontWeight: 500,
                      }}
                    >
                      Nom
                    </th>

                    <th
                      style={{
                        textAlign: 'left',
                        padding: '12px 16px',
                        color: '#6b7280',
                        fontWeight: 500,
                      }}
                    >
                      Type
                    </th>

                    <th
                      style={{
                        textAlign: 'left',
                        padding: '12px 16px',
                        color: '#6b7280',
                        fontWeight: 500,
                      }}
                    >
                      Statut
                    </th>

                    <th
                      style={{
                        textAlign: 'left',
                        padding: '12px 16px',
                        color: '#6b7280',
                        fontWeight: 500,
                      }}
                    >
                      Actions
                    </th>
                  </tr>
                </thead>

                <tbody>
                  {visibleDocuments.map((doc) => (
                    <tr
                      key={doc.id}
                      style={{
                        borderBottom: '1px solid #f3f4f6',
                      }}
                    >
                      {/* NOM */}
                      <td
                        style={{
                          padding: '12px 16px',
                          fontWeight: 500,
                          color: '#111827',
                        }}
                      >
                        {doc.filename}
                      </td>

                      {/* TYPE */}
                      <td
                        style={{
                          padding: '12px 16px',
                          color: '#374151',
                        }}
                      >
                        {formatType(doc.type)}
                      </td>

                      {/* STATUT */}
                      <td
                        style={{
                          padding: '12px 16px',
                          color: '#374151',
                        }}
                      >
                        {doc.status}
                      </td>

                      {/* ACTION */}
                      <td
                        style={{
                          padding: '12px 16px',
                        }}
                      >
                        <button
                          onClick={() =>
                            navigate(`/documents/${doc.id}`)
                          }
                          style={{
                            display: 'flex',
                            alignItems: 'center',
                            gap: '5px',
                            padding: '6px 11px',
                            border: '1px solid #d1d5db',
                            borderRadius: '7px',
                            background: '#fff',
                            color: '#374151',
                            cursor: 'pointer',
                            fontSize: '12.5px',
                          }}
                        >
                          <Eye size={14} />
                          Voir
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>

              {/* PAGINATION */}
              {totalPages > 1 && (
                <div
                  style={{
                    display: 'flex',
                    justifyContent: 'flex-end',
                    alignItems: 'center',
                    gap: '12px',
                    padding: '14px 16px',
                    borderTop: '1px solid #e5e7eb',
                    background: '#fff',
                  }}
                >
                  {/* PRECEDENT */}
                  <button
                    onClick={() =>
                      setDocumentsPage((page) =>
                        Math.max(page - 1, 0)
                      )
                    }
                    disabled={documentsPage === 0}
                    style={{
                      padding: '6px 12px',
                      border: '1px solid #d1d5db',
                      borderRadius: '6px',
                      background: '#fff',
                      color: '#374151',
                      fontSize: '12.5px',
                      cursor:
                        documentsPage === 0
                          ? 'not-allowed'
                          : 'pointer',
                      opacity:
                        documentsPage === 0
                          ? 0.5
                          : 1,
                    }}
                  >
                    Précédent
                  </button>

                  {/* PAGE X SUR Y */}
                  <span
                    style={{
                      fontSize: '12.5px',
                      color: '#6b7280',
                      minWidth: '90px',
                      textAlign: 'center',
                    }}
                  >
                    Page {documentsPage + 1} sur {totalPages}
                  </span>

                  {/* SUIVANT */}
                  <button
                    onClick={() =>
                      setDocumentsPage((page) =>
                        Math.min(
                          page + 1,
                          totalPages - 1
                        )
                      )
                    }
                    disabled={
                      documentsPage === totalPages - 1
                    }
                    style={{
                      padding: '6px 12px',
                      border: '1px solid #d1d5db',
                      borderRadius: '6px',
                      background: '#fff',
                      color: '#374151',
                      fontSize: '12.5px',
                      cursor:
                        documentsPage === totalPages - 1
                          ? 'not-allowed'
                          : 'pointer',
                      opacity:
                        documentsPage === totalPages - 1
                          ? 0.5
                          : 1,
                    }}
                  >
                    Suivant
                  </button>
                </div>
              )}
            </>
          )}
      </div>

      {/* Confirmation suppression organisation */}
      {isAdmin && showDeleteConfirm && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            background: 'rgba(0,0,0,0.4)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 50,
          }}
        >
          <div
            style={{
              background: '#fff',
              borderRadius: '12px',
              padding: '24px',
              width: '380px',
              boxShadow:
                '0 10px 30px rgba(0,0,0,0.15)',
            }}
          >
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '10px',
                marginBottom: '12px',
              }}
            >
              <AlertTriangle
                size={20}
                color="#dc2626"
              />

              <h2
                style={{
                  fontSize: '16px',
                  fontWeight: 600,
                  color: '#111827',
                  margin: 0,
                }}
              >
                Supprimer cette organisation ?
              </h2>
            </div>

            <p
              style={{
                color: '#6b7280',
                fontSize: '13.5px',
                marginBottom: '16px',
              }}
            >
              Cette action est irréversible. Tous les
              membres seront retirés et l'organisation
              sera définitivement supprimée.
            </p>

            {deleteError && (
              <div
                style={{
                  background: '#fee2e2',
                  color: '#991b1b',
                  padding: '8px 12px',
                  borderRadius: '6px',
                  fontSize: '13px',
                  marginBottom: '12px',
                }}
              >
                {deleteError}
              </div>
            )}

            <div
              style={{
                display: 'flex',
                gap: '8px',
                justifyContent: 'flex-end',
              }}
            >
              <button
                onClick={() =>
                  setShowDeleteConfirm(false)
                }
                style={{
                  padding: '8px 16px',
                  borderRadius: '8px',
                  border:
                    '1px solid #d1d5db',
                  background: '#fff',
                  cursor: 'pointer',
                  fontSize: '13.5px',
                  color: '#374151',
                }}
              >
                Annuler
              </button>

              <button
                onClick={
                  handleDeleteOrganization
                }
                disabled={deleting}
                style={{
                  padding: '8px 16px',
                  borderRadius: '8px',
                  border: 'none',
                  background: '#dc2626',
                  color: '#fff',
                  cursor: deleting
                    ? 'not-allowed'
                    : 'pointer',
                  fontSize: '13.5px',
                }}
              >
                {deleting
                  ? 'Suppression...'
                  : 'Supprimer définitivement'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
