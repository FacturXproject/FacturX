import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, UserPlus, Trash2, AlertTriangle } from 'lucide-react';
import api from '../services/api';

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

  const [members, setMembers] = useState([]);
  const [allUsers, setAllUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [selectedUserId, setSelectedUserId] = useState('');
  const [selectedRole, setSelectedRole] = useState('CLIENT');
  const [adding, setAdding] = useState(false);
  const [addError, setAddError] = useState(null);

  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState(null);

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

  const fetchAllUsers = async () => {
    try {
      const response = await api.get('/users');
      setAllUsers(response.data);
    } catch {
      // liste des users optionnelle pour le formulaire d'ajout, on ignore l'erreur ici
    }
  };

  useEffect(() => {
    fetchMembers();
    fetchAllUsers();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  const handleAddMember = async (e) => {
    e.preventDefault();
    if (!selectedUserId) return;

    setAdding(true);
    setAddError(null);
    try {
      await api.post(`/organizations/${id}/members?userId=${selectedUserId}&role=${selectedRole}`);
      setSelectedUserId('');
      fetchMembers();
    } catch (err) {
      setAddError(err.response?.data?.message ?? err.message);
    } finally {
      setAdding(false);
    }
  };

  const handleRemoveMember = async (userId) => {
    try {
      await api.delete(`/organizations/${id}/members/${userId}`);
      fetchMembers();
    } catch (err) {
      setError(err.response?.data?.message ?? err.message);
    }
  };

  const handleDeleteOrganization = async () => {
    setDeleting(true);
    setDeleteError(null);
    try {
      await api.delete(`/organizations/${id}`);
      navigate('/organisations');
    } catch (err) {
      setDeleteError(err.response?.data?.message ?? err.message);
      setDeleting(false);
    }
  };

  // Users qui ne sont pas deja membres, pour ne pas proposer un doublon
  const memberUserIds = new Set(members.map((m) => m.userId));
  const availableUsers = allUsers.filter((u) => !memberUserIds.has(u.id));

  return (
    <div style={{ padding: '32px', maxWidth: '900px', margin: '0 auto' }}>
      <button
        onClick={() => navigate('/organisations')}
        style={{
          display: 'flex', alignItems: 'center', gap: '6px',
          background: 'none', border: 'none', cursor: 'pointer',
          color: '#6b7280', fontSize: '13.5px', marginBottom: '16px', padding: 0,
        }}
      >
        <ArrowLeft size={16} />
        Retour aux organisations
      </button>

      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <div>
          <h1 style={{ fontSize: '24px', fontWeight: 700, marginBottom: '4px', color: '#111827' }}>
            Membres de l'organisation #{id}
          </h1>
          <p style={{ color: '#6b7280', marginBottom: '24px', fontSize: '14px' }}>
            Gérez qui appartient à cette organisation et avec quel rôle.
          </p>
        </div>
        <button
          onClick={() => setShowDeleteConfirm(true)}
          style={{
            display: 'flex', alignItems: 'center', gap: '6px',
            background: '#fff', color: '#dc2626', border: '1px solid #fecaca',
            borderRadius: '8px', padding: '9px 14px', fontSize: '13px',
            fontWeight: 500, cursor: 'pointer',
          }}
        >
          <Trash2 size={15} />
          Supprimer l'organisation
        </button>
      </div>

      {loading && <p style={{ color: '#6b7280' }}>Chargement...</p>}

      {error && (
        <div style={{ background: '#fee2e2', color: '#991b1b', padding: '12px 16px', borderRadius: '8px', marginBottom: '16px' }}>
          {error}
        </div>
      )}

      {!loading && !error && (
        <div style={{ background: '#fff', border: '1px solid #e5e7eb', borderRadius: '10px', overflow: 'hidden', marginBottom: '24px' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '13.5px' }}>
            <thead>
              <tr style={{ background: '#f9fafb', borderBottom: '1px solid #e5e7eb' }}>
                <th style={{ textAlign: 'left', padding: '12px 16px', color: '#6b7280', fontWeight: 500 }}>Nom</th>
                <th style={{ textAlign: 'left', padding: '12px 16px', color: '#6b7280', fontWeight: 500 }}>Email</th>
                <th style={{ textAlign: 'left', padding: '12px 16px', color: '#6b7280', fontWeight: 500 }}>Rôle</th>
                <th style={{ textAlign: 'left', padding: '12px 16px', color: '#6b7280', fontWeight: 500 }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {members.map((member) => {
                const badge = roleBadgeStyles[member.role] ?? { background: '#f3f4f6', color: '#374151' };
                return (
                  <tr key={member.id} style={{ borderBottom: '1px solid #f3f4f6' }}>
                    <td style={{ padding: '12px 16px', fontWeight: 500, color: '#111827' }}>
                      {member.firstName} {member.lastName}
                    </td>
                    <td style={{ padding: '12px 16px', color: '#6b7280' }}>{member.email}</td>
                    <td style={{ padding: '12px 16px' }}>
                      <span style={{
                        background: badge.background, color: badge.color,
                        padding: '3px 10px', borderRadius: '999px', fontSize: '12px', fontWeight: 500,
                      }}>
                        {roleLabels[member.role] ?? member.role}
                      </span>
                    </td>
                    <td style={{ padding: '12px 16px' }}>
                      <button
                        onClick={() => handleRemoveMember(member.userId)}
                        style={{
                          display: 'flex', alignItems: 'center', gap: '4px',
                          background: 'none', border: 'none', cursor: 'pointer',
                          color: '#dc2626', fontSize: '12.5px', padding: '4px 8px', borderRadius: '6px',
                        }}
                      >
                        <Trash2 size={14} />
                        Retirer
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>

          {members.length === 0 && (
            <p style={{ color: '#9ca3af', padding: '16px', fontSize: '14px' }}>Aucun membre pour l'instant.</p>
          )}
        </div>
      )}

      {/* Formulaire d'ajout */}
      <div style={{ background: '#fff', border: '1px solid #e5e7eb', borderRadius: '10px', padding: '20px' }}>
        <h2 style={{ fontSize: '15px', fontWeight: 600, marginBottom: '14px', color: '#111827', display: 'flex', alignItems: 'center', gap: '8px' }}>
          <UserPlus size={17} />
          Ajouter un membre
        </h2>

        <form onSubmit={handleAddMember} style={{ display: 'flex', gap: '10px', alignItems: 'flex-end', flexWrap: 'wrap' }}>
          <div style={{ flex: 1, minWidth: '200px' }}>
            <label style={{ display: 'block', fontSize: '12.5px', color: '#374151', marginBottom: '5px' }}>
              Utilisateur
            </label>
            <select
              value={selectedUserId}
              onChange={(e) => setSelectedUserId(e.target.value)}
              style={{ width: '100%', padding: '8px 10px', border: '1px solid #d1d5db', borderRadius: '8px', fontSize: '13.5px', boxSizing: 'border-box' }}
            >
              <option value="">Sélectionner un utilisateur</option>
              {availableUsers.map((u) => (
                <option key={u.id} value={u.id}>
                  {u.firstName} {u.lastName} ({u.email})
                </option>
              ))}
            </select>
          </div>

          <div style={{ minWidth: '150px' }}>
            <label style={{ display: 'block', fontSize: '12.5px', color: '#374151', marginBottom: '5px' }}>
              Rôle
            </label>
            <select
              value={selectedRole}
              onChange={(e) => setSelectedRole(e.target.value)}
              style={{ width: '100%', padding: '8px 10px', border: '1px solid #d1d5db', borderRadius: '8px', fontSize: '13.5px', boxSizing: 'border-box' }}
            >
              <option value="ADMIN">Administrateur</option>
              <option value="ACCOUNTANT">Comptable</option>
              <option value="CLIENT">Client</option>
            </select>
          </div>

          <button
            type="submit"
            disabled={adding || !selectedUserId}
            style={{
              padding: '9px 18px', borderRadius: '8px', border: 'none',
              background: adding || !selectedUserId ? '#9ca3af' : '#1a2744',
              color: '#fff', cursor: adding || !selectedUserId ? 'not-allowed' : 'pointer', fontSize: '13.5px',
            }}
          >
            {adding ? 'Ajout...' : 'Ajouter'}
          </button>
        </form>

        {addError && (
          <div style={{ background: '#fee2e2', color: '#991b1b', padding: '8px 12px', borderRadius: '6px', fontSize: '13px', marginTop: '10px' }}>
            {addError}
          </div>
        )}
      </div>

      {showDeleteConfirm && (
        <div style={{
          position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.4)',
          display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 50,
        }}>
          <div style={{ background: '#fff', borderRadius: '12px', padding: '24px', width: '380px', boxShadow: '0 10px 30px rgba(0,0,0,0.15)' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '12px' }}>
              <AlertTriangle size={20} color="#dc2626" />
              <h2 style={{ fontSize: '16px', fontWeight: 600, color: '#111827', margin: 0 }}>
                Supprimer cette organisation ?
              </h2>
            </div>
            <p style={{ color: '#6b7280', fontSize: '13.5px', marginBottom: '16px' }}>
              Cette action est irréversible. Tous les membres seront retirés et l'organisation sera définitivement supprimée.
            </p>

            {deleteError && (
              <div style={{ background: '#fee2e2', color: '#991b1b', padding: '8px 12px', borderRadius: '6px', fontSize: '13px', marginBottom: '12px' }}>
                {deleteError}
              </div>
            )}

            <div style={{ display: 'flex', gap: '8px', justifyContent: 'flex-end' }}>
              <button
                onClick={() => setShowDeleteConfirm(false)}
                style={{ padding: '8px 16px', borderRadius: '8px', border: '1px solid #d1d5db', background: '#fff', cursor: 'pointer', fontSize: '13.5px', color: '#374151' }}
              >
                Annuler
              </button>
              <button
                onClick={handleDeleteOrganization}
                disabled={deleting}
                style={{
                  padding: '8px 16px', borderRadius: '8px', border: 'none',
                  background: '#dc2626', color: '#fff',
                  cursor: deleting ? 'not-allowed' : 'pointer', fontSize: '13.5px',
                }}
              >
                {deleting ? 'Suppression...' : 'Supprimer définitivement'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}