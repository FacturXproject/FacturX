import { useState, useEffect } from 'react';
import { Building2, Users, Star, Plus, X, Search, Filter, Eye, MoreVertical, ChevronLeft, ChevronRight, Info } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
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

const orgIconPalette = [
  { bg: '#dbeafe', color: '#2563eb' },
  { bg: '#ede9fe', color: '#7c3aed' },
  { bg: '#fef3c7', color: '#d97706' },
  { bg: '#fee2e2', color: '#dc2626' },
];

function CreateOrganizationModal({ onClose, onCreated }) {
  const [name, setName] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!name.trim()) return;
    setSubmitting(true);
    setError(null);
    try {
      const response = await api.post(`/organizations?name=${encodeURIComponent(name.trim())}`);
      onCreated(response.data);
      onClose();
    } catch (err) {
      setError(err.response?.data?.message ?? err.message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div style={{
      position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.4)',
      display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 50,
    }}>
      <div style={{ background: '#fff', borderRadius: '12px', padding: '24px', width: '380px', boxShadow: '0 10px 30px rgba(0,0,0,0.15)' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
          <h2 style={{ fontSize: '17px', fontWeight: 600, color: '#111827', margin: 0 }}>
            Créer une organisation
          </h2>
          <button onClick={onClose} style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#9ca3af' }}>
            <X size={18} />
          </button>
        </div>

        <form onSubmit={handleSubmit}>
          <label style={{ display: 'block', fontSize: '13px', color: '#374151', marginBottom: '6px' }}>
            Nom de l'organisation
          </label>
          <input
            type="text"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="Cabinet Dupont"
            autoFocus
            style={{
              width: '100%', padding: '9px 12px', border: '1px solid #d1d5db',
              borderRadius: '8px', fontSize: '14px', marginBottom: '12px', boxSizing: 'border-box',
            }}
          />

          {error && (
            <div style={{ background: '#fee2e2', color: '#991b1b', padding: '8px 12px', borderRadius: '6px', fontSize: '13px', marginBottom: '12px' }}>
              {error}
            </div>
          )}

          <div style={{ display: 'flex', gap: '8px', justifyContent: 'flex-end', marginTop: '8px' }}>
            <button
              type="button"
              onClick={onClose}
              style={{ padding: '8px 16px', borderRadius: '8px', border: '1px solid #d1d5db', background: '#fff', cursor: 'pointer', fontSize: '13.5px', color: '#374151' }}
            >
              Annuler
            </button>
            <button
              type="submit"
              disabled={submitting || !name.trim()}
              style={{
                padding: '8px 16px', borderRadius: '8px', border: 'none',
                background: submitting || !name.trim() ? '#9ca3af' : '#1a2744',
                color: '#fff', cursor: submitting || !name.trim() ? 'not-allowed' : 'pointer', fontSize: '13.5px',
              }}
            >
              {submitting ? 'Création...' : 'Créer'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default function OrganizationsPage() {
  const navigate = useNavigate();
  const [organizations, setOrganizations] = useState([]);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('ALL');
  const [search, setSearch] = useState('');
  const [showCreateModal, setShowCreateModal] = useState(false);

  const fetchOrganizations = async () => {
    try {
      const response = await api.get('/organizations');
      setOrganizations(response.data);
      setError(null);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchOrganizations(); }, []);

  const handleCreated = () => {
    setLoading(true);
    fetchOrganizations();
  };

  const total = organizations.length;
  const asAdmin = organizations.filter((o) => o.role === 'ADMIN').length;
  const asMember = total - asAdmin;

  const filtered = organizations
    .filter((org) => {
      if (filter === 'ALL') return true;
      if (filter === 'ADMIN') return org.role === 'ADMIN';
      return org.role !== 'ADMIN';
    })
    .filter((org) => `organisation #${org.id}`.includes(search.toLowerCase()));

  const statCards = [
    { icon: Building2, iconBg: '#dbeafe', iconColor: '#2563eb', value: total, label: 'Organisations', sub: 'Au total' },
    { icon: Users, iconBg: '#dcfce7', iconColor: '#16a34a', value: asMember, label: 'Dont vous êtes membre', sub: 'Membre' },
    { icon: Star, iconBg: '#fef3c7', iconColor: '#d97706', value: asAdmin, label: 'Dont vous administrez', sub: 'Administrateur' },
  ];

  return (
    <div style={{ padding: '32px 40px', maxWidth: '1200px', margin: '0 auto', background: '#f8f9fa', minHeight: '100vh' }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '24px' }}>
        <div>
          <h1 style={{ fontSize: '26px', fontWeight: 700, marginBottom: '4px', color: '#111827' }}>
            Organisations
          </h1>
          <p style={{ color: '#6b7280', fontSize: '14px', margin: 0 }}>
            Gérez les organisations dont vous êtes membre ou administrateur.
          </p>
        </div>
        <button
          onClick={() => setShowCreateModal(true)}
          style={{
            display: 'flex', alignItems: 'center', gap: '6px',
            background: '#1a2744', color: '#fff', border: 'none',
            borderRadius: '8px', padding: '10px 18px', fontSize: '13.5px',
            fontWeight: 500, cursor: 'pointer',
          }}
        >
          <Plus size={16} />
          Créer une organisation
        </button>
      </div>

      {/* Stats */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '16px', marginBottom: '20px' }}>
        {statCards.map(({ icon: Icon, iconBg, iconColor, value, label, sub }) => (
          <div
            key={label}
            style={{
              background: '#fff', border: '1px solid #e5e7eb', borderRadius: '10px',
              padding: '18px', display: 'flex', alignItems: 'center', gap: '14px',
            }}
          >
            <div style={{
              width: '44px', height: '44px', borderRadius: '10px', background: iconBg,
              display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
            }}>
              <Icon size={20} color={iconColor} strokeWidth={2} />
            </div>
            <div>
              <div style={{ fontSize: '24px', fontWeight: 700, color: '#111827', lineHeight: 1 }}>{value}</div>
              <div style={{ fontSize: '13px', color: '#374151', marginTop: '2px' }}>{label}</div>
              <div style={{ fontSize: '11.5px', color: '#9ca3af' }}>{sub}</div>
            </div>
          </div>
        ))}
      </div>

      {/* Card principale : tabs + recherche + tableau */}
      <div style={{ background: '#fff', border: '1px solid #e5e7eb', borderRadius: '10px', overflow: 'hidden' }}>
        {/* Tabs + recherche */}
        <div style={{
          display: 'flex', justifyContent: 'space-between', alignItems: 'center',
          padding: '14px 20px', borderBottom: '1px solid #e5e7eb', flexWrap: 'wrap', gap: '12px',
        }}>
          <div style={{ display: 'flex', gap: '20px' }}>
            {[
              { key: 'ALL', label: 'Toutes' },
              { key: 'MEMBER', label: 'Membre' },
              { key: 'ADMIN', label: 'Administrateur' },
            ].map(({ key, label }) => (
              <button
                key={key}
                onClick={() => setFilter(key)}
                style={{
                  background: 'none', border: 'none', cursor: 'pointer',
                  padding: '4px 0', fontSize: '13.5px',
                  fontWeight: filter === key ? 600 : 400,
                  color: filter === key ? '#2563eb' : '#6b7280',
                  borderBottom: filter === key ? '2px solid #2563eb' : '2px solid transparent',
                }}
              >
                {label}
              </button>
            ))}
          </div>

          <div style={{ display: 'flex', gap: '8px' }}>
            <div style={{ position: 'relative' }}>
              <Search size={14} color="#9ca3af" style={{ position: 'absolute', left: '10px', top: '50%', transform: 'translateY(-50%)' }} />
              <input
                type="text"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder="Rechercher par nom..."
                style={{
                  padding: '8px 12px 8px 30px', border: '1px solid #d1d5db',
                  borderRadius: '8px', fontSize: '13px', width: '220px', outline: 'none',
                }}
              />
            </div>
            <button style={{
              display: 'flex', alignItems: 'center', gap: '6px',
              padding: '8px 14px', border: '1px solid #d1d5db', borderRadius: '8px',
              background: '#fff', fontSize: '13px', color: '#374151', cursor: 'pointer',
            }}>
              <Filter size={14} />
              Filtres
            </button>
          </div>
        </div>

        {loading && <p style={{ padding: '24px 20px', color: '#6b7280', fontSize: '14px' }}>Chargement des organisations...</p>}

        {error && (
          <div style={{ background: '#fee2e2', color: '#991b1b', padding: '12px 20px', fontSize: '13.5px' }}>
            Erreur : {error}
          </div>
        )}

        {!loading && !error && (
          <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '13.5px' }}>
            <thead>
              <tr style={{ background: '#f9fafb', borderBottom: '1px solid #e5e7eb' }}>
                <th style={{ textAlign: 'left', padding: '12px 20px', color: '#6b7280', fontWeight: 500 }}>Nom</th>
                <th style={{ textAlign: 'left', padding: '12px 20px', color: '#6b7280', fontWeight: 500 }}>Votre rôle</th>
                <th style={{ textAlign: 'left', padding: '12px 20px', color: '#6b7280', fontWeight: 500 }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((org, i) => {
                const palette = orgIconPalette[i % orgIconPalette.length];
                const badge = roleBadgeStyles[org.role] ?? { background: '#f3f4f6', color: '#374151' };
                return (
                  <tr key={org.id} style={{ borderBottom: '1px solid #f3f4f6' }}>
                    <td style={{ padding: '14px 20px' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                        <div style={{
                          width: '36px', height: '36px', borderRadius: '8px', background: palette.bg,
                          display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
                        }}>
                          <Building2 size={17} color={palette.color} />
                        </div>
                        <div>
                          {/* TODO: remplacer par org.name quand disponible */}
                          <div style={{ fontWeight: 500, color: '#111827' }}>Organisation #{org.id}</div>
                        </div>
                      </div>
                    </td>
                    <td style={{ padding: '14px 20px' }}>
                      <span style={{
                        background: badge.background, color: badge.color,
                        padding: '3px 10px', borderRadius: '999px', fontSize: '12px', fontWeight: 500,
                      }}>
                        {roleLabels[org.role] ?? org.role}
                      </span>
                    </td>
                    <td style={{ padding: '14px 20px' }}>
                      <div style={{ display: 'flex', gap: '8px' }}>
                        <button
                          onClick={() => navigate(`/organisations/${org.organizationId ?? org.id}`)}
                          style={{
                            display: 'flex', alignItems: 'center', gap: '5px',
                            padding: '6px 12px', border: '1px solid #d1d5db', borderRadius: '7px',
                            background: '#fff', fontSize: '12.5px', color: '#374151', cursor: 'pointer',
                          }}
                        >
                          <Eye size={13} />
                          Voir
                        </button>
                        <button style={{
                          padding: '6px 8px', border: '1px solid #d1d5db', borderRadius: '7px',
                          background: '#fff', cursor: 'pointer', color: '#6b7280',
                        }}>
                          <MoreVertical size={14} />
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}

        {!loading && !error && filtered.length === 0 && (
          <p style={{ color: '#9ca3af', padding: '32px', fontSize: '14px', textAlign: 'center' }}>
            Aucune organisation à afficher.
          </p>
        )}

        {!loading && !error && filtered.length > 0 && (
          <div style={{
            display: 'flex', justifyContent: 'space-between', alignItems: 'center',
            padding: '12px 20px', borderTop: '1px solid #e5e7eb',
          }}>
            <span style={{ fontSize: '12.5px', color: '#6b7280' }}>
              1–{filtered.length} sur {filtered.length} organisations
            </span>
            <div style={{ display: 'flex', gap: '6px' }}>
              <button style={{ width: '28px', height: '28px', border: '1px solid #d1d5db', borderRadius: '6px', background: '#fff', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <ChevronLeft size={14} color="#9ca3af" />
              </button>
              <span style={{ width: '28px', height: '28px', display: 'flex', alignItems: 'center', justifyContent: 'center', background: '#dbeafe', color: '#2563eb', borderRadius: '6px', fontSize: '12.5px', fontWeight: 600 }}>1</span>
              <button style={{ width: '28px', height: '28px', border: '1px solid #d1d5db', borderRadius: '6px', background: '#fff', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <ChevronRight size={14} color="#9ca3af" />
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Encart info */}
      <div style={{
        background: '#eff6ff', border: '1px solid #dbeafe', borderRadius: '10px',
        padding: '14px 18px', marginTop: '16px', display: 'flex', gap: '10px',
      }}>
        <Info size={17} color="#2563eb" style={{ flexShrink: 0, marginTop: '1px' }} />
        <div>
          <div style={{ fontSize: '13px', fontWeight: 600, color: '#1e3a8a', marginBottom: '3px' }}>
            À propos des organisations
          </div>
          <p style={{ fontSize: '12.5px', color: '#3b5998', margin: 0, lineHeight: 1.5 }}>
            Les organisations vous permettent de collaborer avec votre équipe, d'envoyer des invitations et de gérer les accès.
            Vous pouvez créer une nouvelle organisation ou demander à rejoindre une organisation existante.
          </p>
        </div>
      </div>

      {showCreateModal && (
        <CreateOrganizationModal
          onClose={() => setShowCreateModal(false)}
          onCreated={handleCreated}
        />
      )}
    </div>
  );
}