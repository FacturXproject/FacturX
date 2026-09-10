import { useEffect, useState } from 'react';
import api from '../services/api';
import { useNavigate } from 'react-router-dom';

import {
  Users,
  Send,
  Clock3,
  CircleCheck,
  Building2,
  Search,
  SlidersHorizontal,
  RefreshCw,
  Trash2,
  ChevronLeft,
  ChevronRight,
  Info
} from 'lucide-react';

export default function Invitations() {

  const navigate = useNavigate();

  const [invitations, setInvitations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [adminOrganizations, setAdminOrganizations] = useState([]);

  const [searching, setSearching] = useState('');
  const [activeTab, setActiveTab] = useState('Toutes');

const getErrorMessage = (err, fallback) => {
  const status = err.response?.status;

  if (status === 403) {
    return 'Vous n’avez pas les permissions nécessaires pour effectuer cette action.';  }

  if (status === 404) {
    return 'La ressource demandée est introuvable.';
  }

  if (status === 401) {
    return 'Votre session a expiré. Veuillez vous reconnecter.';
  }

  return fallback;
};

const fetchAllInvitations = async () => {
  try {
    setError(null);
    setLoading(true);

    const orgsResponse = await api.get('/organizations');

    const adminOrgs = orgsResponse.data.filter(
      (org) => org.role === 'ADMIN'
    );

    setAdminOrganizations(adminOrgs);

    let allInvitations = [];

    for (const org of adminOrgs) {
      const orgId = org.organizationId ?? org.id;

      const orgDetails = await api.get(
        `/organizations/${orgId}`
      );

      const invResponse = await api.get(
        `/organizations/${orgId}/invitations`
      );

      const withOrgName = invResponse.data.map(
        (invitation) => ({
          ...invitation,
          organisation: orgDetails.data.name,
          organizationId: orgId
        })
      );

      allInvitations = allInvitations.concat(withOrgName);
    }

    setInvitations(allInvitations);

  } catch (err) {
    setError(
      getErrorMessage(
        err,
      'Impossible de charger les invitations.'
      )
    );

  } finally {
    setLoading(false);
  }
};


  useEffect(() => {
    fetchAllInvitations();
  }, []);


  const statusLabels = {
    PENDING: 'En attente',
    ACCEPTED: 'Acceptée',
    EXPIRED: 'Expirée',
    REVOKED: 'Révoquée'
  };


  const roleLabels = {
    ADMIN: 'Administrateur',
    ACCOUNTANT: 'Comptable',
    CLIENT: 'Client'
  };

  const filteredInvitations = invitations.filter((invitation) => {

    const text = searching.toLowerCase();
    const matchesSearch =
    invitation.email.toLowerCase().includes(text) ||
    invitation.organisation.toLowerCase().includes(text);

  const matchesTab =
    activeTab === 'Toutes' ||
    (activeTab === 'En attente' && invitation.status === 'PENDING') ||
    (activeTab === 'Acceptées' && invitation.status === 'ACCEPTED') ||
    (activeTab === 'Expirées' && invitation.status === 'EXPIRED') ||
    (activeTab === 'Révoquées' && invitation.status === 'REVOKED');
    return matchesSearch && matchesTab;
  });

  const statusStyle = (status) => {

    if (status === 'PENDING') {
      return {
        background: '#fff7df',
        color: '#d78b13',
        border: '1px solid #fde6a8'
      };
    }

    if (status === 'ACCEPTED') {
      return {
        background: '#eafaf1',
        color: '#28a76a',
        border: '1px solid #c9f0db'
      };
    }

    return {
      background: '#fff0f0',
      color: '#ef5555',
      border: '1px solid #ffd1d1'
    };
  };

    const formatDate = (date) => {
      if (!date) return '—';

      return new Date(date).toLocaleDateString('fr-FR');
  };

  const handleRevoke = async (organizationId, invitationId) => {
      try {

        await api.patch(
          `/organizations/${organizationId}/invitations/${invitationId}/revoke`
        );

        fetchAllInvitations();

      } catch (err) {

        setError(
          getErrorMessage(
            err,
          'Impossible de révoquer l’invitation.'
          )
        );
      }
    };

      const handleResend = async (organizationId, invitationId) => {
        try {

          await api.patch(
            `/organizations/${organizationId}/invitations/${invitationId}/resend`
          );

          fetchAllInvitations();

        } catch (err) {

          setError(
            getErrorMessage(
              err,
            'Impossible de renvoyer l’invitation.'
            )
          );

        }
      };


  return (
    <div
      style={{
        padding: '24px 28px',
        width: '100%',
        boxSizing: 'border-box'
      }}
    >

      {/* HEADER */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'flex-start'
        }}
      >

        <div>

          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '10px'
            }}
          >

            <Users size={18} />

            <h1
              style={{
                margin: 0,
                fontSize: '20px',
                fontWeight: 700,
                color: '#111827'
              }}
            >
              Invitations
            </h1>

          </div>


          <p
            style={{
              margin: '7px 0 0',
              fontSize: '12px',
              color: '#6b7280'
            }}
          >
            Gérez les invitations envoyées à rejoindre vos organisations.
          </p>

        </div>

      {adminOrganizations.length > 0 && (
        <button
          onClick={() => navigate('/invitations/new')}
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '7px',
            padding: '9px 16px',
            background: '#152846',
            color: '#fff',
            border: 'none',
            borderRadius: '6px',
            fontSize: '12px',
            cursor: 'pointer'
          }}
        >
          <span style={{ fontSize: '18px' }}>+</span>
          Envoyer une invitation
        </button>
      )}

      </div>


      {/* LOADING */}
      {loading && (
        <p
          style={{
            marginTop: '20px',
            color: '#6b7280',
            fontSize: '13px'
          }}
        >
          Chargement...
        </p>
      )}


      {/* ERROR */}
      {error && (
        <div
          style={{
            marginTop: '20px',
            padding: '10px 14px',
            background: '#fee2e2',
            color: '#991b1b',
            borderRadius: '6px',
            fontSize: '13px'
          }}
        >
          {error}
        </div>
      )}


      {/* STAT CARDS */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(3, 1fr)',
          gap: '20px',
          marginTop: '28px'
        }}
      >

        {/* CARD 1 */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '18px',
            padding: '20px',
            background: '#fff',
            border: '1px solid #e5e7eb',
            borderRadius: '7px'
          }}
        >

          <div
            style={{
              width: '42px',
              height: '42px',
              borderRadius: '50%',
              background: '#edf6ff',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center'
            }}
          >
            <Send size={19} color="#4a9eff" />
          </div>

          <div>

            <div
              style={{
                fontSize: '23px',
                fontWeight: 700
              }}
            >
              {invitations.length}
            </div>

            <div style={{ fontSize: '12px' }}>
              Invitations envoyées
            </div>

            <div
              style={{
                fontSize: '11px',
                color: '#8b93a1',
                marginTop: '4px'
              }}
            >
              Au total
            </div>

          </div>

        </div>


        {/* CARD 2 */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '18px',
            padding: '20px',
            background: '#fff',
            border: '1px solid #e5e7eb',
            borderRadius: '7px'
          }}
        >

          <div
            style={{
              width: '42px',
              height: '42px',
              borderRadius: '50%',
              background: '#fff6e8',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center'
            }}
          >
            <Clock3 size={19} color="#e8a32b" />
          </div>

          <div>

            <div
              style={{
                fontSize: '23px',
                fontWeight: 700
              }}
            >
              {invitations.filter(
                inv => inv.status === 'PENDING'
              ).length}
            </div>

            <div style={{ fontSize: '12px' }}>
              En attente
            </div>

            <div
              style={{
                fontSize: '11px',
                color: '#8b93a1',
                marginTop: '4px'
              }}
            >
              Invitations en cours
            </div>

          </div>

        </div>


        {/* CARD 3 */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '18px',
            padding: '20px',
            background: '#fff',
            border: '1px solid #e5e7eb',
            borderRadius: '7px'
          }}
        >

          <div
            style={{
              width: '42px',
              height: '42px',
              borderRadius: '50%',
              background: '#eafaf1',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center'
            }}
          >
            <CircleCheck size={19} color="#2fb778" />
          </div>

          <div>

            <div
              style={{
                fontSize: '23px',
                fontWeight: 700
              }}
            >
              {invitations.filter(
                inv => inv.status === 'ACCEPTED'
              ).length}
            </div>

            <div style={{ fontSize: '12px' }}>
              Acceptées
            </div>

            <div
              style={{
                fontSize: '11px',
                color: '#8b93a1',
                marginTop: '4px'
              }}
            >
              Invitations acceptées
            </div>

          </div>

        </div>

      </div>


      {/* TABLE CONTAINER */}
      <div
        style={{
          marginTop: '20px',
          padding: '0 14px 16px',
          background: '#fff',
          border: '1px solid #e5e7eb',
          borderRadius: '7px',
          width: '100%',
          boxSizing: 'border-box'
        }}
      >

        {/* TABS + SEARCH */}
        <div

          style={{
            minHeight: '52px',
            display: 'flex',
            alignItems: 'center',
            borderBottom: '1px solid #e5e7eb',
            gap: '24px'
          }}
        >

          {[
            'Toutes',
            'En attente',
            'Acceptées',
            'Expirées',
            'Révoquées'
          ].map((tab, index) => (

            <div
              key={tab}
              onClick={() => setActiveTab(tab)}
              style={{
                height: '52px',
                display: 'flex',
                alignItems: 'center',
                fontSize: '11px',
                cursor: 'pointer',
                color: activeTab === tab
                  ? '#4a8ff0'
                  : '#6b7280',

                borderBottom: activeTab === tab
                  ? '2px solid #79aef8'
                  : '2px solid transparent'
              }}
            >
              {tab}
            </div>

          ))}


          <div
            style={{
              marginLeft: 'auto',
              display: 'flex',
              gap: '8px'
            }}
          >

            <div
              style={{
                width: '270px',
                height: '34px',
                display: 'flex',
                alignItems: 'center',
                padding: '0 10px',
                border: '1px solid #e5e7eb',
                borderRadius: '5px',
                boxSizing: 'border-box'
              }}
            >

              <input
                type="text"
                value={searching}
                onChange = {(e) => setSearching(e.target.value)}
                placeholder="Rechercher par email ou organisation..."
                style={{
                  width: '100%',
                  border: 'none',
                  outline: 'none',
                  fontSize: '11px'
                }}
              />

              <Search size={14} color="#7b8493" />

            </div>


            <button
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '7px',
                height: '34px',
                padding: '0 12px',
                background: '#fff',
                border: '1px solid #e5e7eb',
                borderRadius: '5px',
                cursor: 'pointer',
                fontSize: '11px'
              }}
            >
              <SlidersHorizontal size={14} />
              Filtres
            </button>

          </div>

        </div>


        {/* TABLE */}
        <div
          style={{
            overflowX: 'auto',
            marginTop: '14px'
          }}
        >

          <table
            style={{
              width: '100%',
              borderCollapse: 'collapse',
              fontSize: '11px'
            }}
          >

            <thead>

              <tr
                style={{
                  background: '#fafbfc',
                  color: '#77808f'
                }}
              >

                {[
                  'Email',
                  'Organisation',
                  'Rôle',
                  'Statut',
                  'Envoyée le',
                  'Expire le',
                  'Actions'
                ].map(title => (

                  <th
                    key={title}
                    style={{
                      textAlign: 'left',
                      padding: '12px',
                      borderTop: '1px solid #e5e7eb',
                      borderBottom: '1px solid #e5e7eb',
                      fontWeight: 500
                    }}
                  >
                    {title}
                  </th>

                ))}

              </tr>

            </thead>


            <tbody>

              {filteredInvitations.map(row => (

                <tr
                  key={row.id}
                  style={{
                    borderBottom: '1px solid #edf0f3'
                  }}
                >

                  {/* EMAIL */}
                  <td style={{ padding: '14px 12px' }}>
                    {row.email}
                  </td>


                  {/* ORGANISATION */}
                  <td style={{ padding: '14px 12px' }}>

                    <div
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: '9px'
                      }}
                    >

                      <div
                        style={{
                          width: '28px',
                          height: '28px',
                          background: '#edf5ff',
                          borderRadius: '5px',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center'
                        }}
                      >
                        <Building2
                          size={14}
                          color="#4a8ff0"
                        />
                      </div>


                      <div
                        style={{
                          color: '#253044',
                          fontWeight: 500
                        }}
                      >
                        {row.organisation}
                      </div>

                    </div>

                  </td>


                  {/* ROLE */}
                  <td style={{ padding: '14px 12px' }}>

                    <span
                      style={{
                        background: '#eef4ff',
                        color: '#4b77ba',
                        padding: '3px 6px',
                        borderRadius: '4px',
                        fontSize: '10px'
                      }}
                    >
                      {roleLabels[row.role] ?? row.role}
                    </span>

                  </td>


                  {/* STATUS */}
                  <td style={{ padding: '14px 12px' }}>

                    <span
                      style={{
                        ...statusStyle(row.status),
                        padding: '3px 7px',
                        borderRadius: '4px',
                        fontSize: '10px'
                      }}
                    >
                      {statusLabels[row.status] ?? row.status}
                    </span>

                  </td>


                  {/* CREATED */}
                  <td style={{ padding: '14px 12px' }}>
                    {formatDate(row.createdAt)}
                  </td>


                  {/* EXPIRES */}
                  <td style={{ padding: '14px 12px' }}>
                    {formatDate(row.expiresAt)}
                  </td>


                  {/* ACTIONS */}
                  <td style={{ padding: '14px 12px' }}>

                    {row.status === 'ACCEPTED' ? (

                      <span style={{ color: '#9ca3af' }}>
                        —
                      </span>

                    ) : (

                      <div
                        style={{
                          display: 'flex',
                          gap: '7px'
                        }}
                      >

                        <button
                          onClick={() => handleResend(row.organizationId, row.id)}
                          style={{
                            width: '30px',
                            height: '30px',
                            background: '#fff',
                            border: '1px solid #e5e7eb',
                            borderRadius: '5px',
                            cursor: 'pointer'
                          }}
                        >
                          <RefreshCw size={13} />
                        </button>


                        {row.status === 'PENDING' && (

                          <button
                            onClick={() => handleRevoke(row.organizationId, row.id)}
                            style={{
                              width: '30px',
                              height: '30px',
                              background: '#fff4f4',
                              color: '#ef5555',
                              border: '1px solid #ffe2e2',
                              borderRadius: '5px',
                              cursor: 'pointer'
                            }}
                          >
                            <Trash2 size={13} />
                          </button>

                        )}

                      </div>

                    )}

                  </td>

                </tr>

              ))}

            </tbody>

          </table>

        </div>


        {/* PAGINATION */}
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            marginTop: '16px',
            fontSize: '11px',
            color: '#6b7280'
          }}
        >

          <span>
            {invitations.length} invitation(s)
          </span>


          <div
            style={{
              display: 'flex',
              gap: '6px'
            }}
          >

            <button style={pageButton}>
              <ChevronLeft size={14} />
            </button>

            <button
              style={{
                ...pageButton,
                color: '#357de8',
                background: '#f4f8ff',
                borderColor: '#cfe0fb'
              }}
            >
              1
            </button>

            <button style={pageButton}>
              <ChevronRight size={14} />
            </button>

          </div>

        </div>

      </div>


      {/* INFORMATION */}
      <div
        style={{
          marginTop: '18px',
          padding: '15px 16px',
          background: '#eff7ff',
          border: '1px solid #dbeafe',
          borderRadius: '6px',
          width: '100%',
          boxSizing: 'border-box'
        }}
      >

        <div
          style={{
            display: 'flex',
            alignItems: 'flex-start',
            gap: '12px'
          }}
        >

          <Info size={16} color="#3985e6" />

          <div>

            <div
              style={{
                fontSize: '11px',
                fontWeight: 700,
                marginBottom: '4px'
              }}
            >
              Comment ça fonctionne ?
            </div>

            <div
              style={{
                fontSize: '11px',
                color: '#667085',
                lineHeight: '18px'
              }}
            >
              Envoyez une invitation à un utilisateur. Il recevra un
              email avec un lien sécurisé pour rejoindre votre organisation.
              <br />
              Le lien expire automatiquement à la date indiquée.
            </div>

          </div>

        </div>

      </div>

    </div>
  );
}


const pageButton = {
  width: '32px',
  height: '32px',
  background: '#fff',
  border: '1px solid #e5e7eb',
  borderRadius: '5px',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  cursor: 'pointer',
  fontSize: '11px'
};
