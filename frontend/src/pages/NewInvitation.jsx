import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Mail } from 'lucide-react';
import api from '../services/api';

export default function NewInvitation() {

  const navigate = useNavigate();

  const [organizations, setOrganizations] = useState([]);
  const [organizationId, setOrganizationId] = useState('');
  const [email, setEmail] = useState('');
  const [role, setRole] = useState('CLIENT');

  const [error, setError] = useState(null);


  useEffect(() => {

    const fetchOrganizations = async () => {
      try {

        const response = await api.get('/organizations');

        const organizationsWithNames = await Promise.all(
          response.data.map(async (org) => {

            const id = org.organizationId ?? org.id;

            const details = await api.get(`/organizations/${id}`);

            return {
              id: id,
              name: details.data.name
            };

          })
        );

        setOrganizations(organizationsWithNames);

      } catch (err) {

        console.error(err);

      }
    };

    fetchOrganizations();

  }, []);

  const handleInvite = async () => {
    try {
      setError(null);

      await api.post(
        `/organizations/${organizationId}/invitations`,
        {
          email: email.trim(),
          role: role
        }
      );

      navigate('/invitations');

    } catch (err) {
      setError(
        err.response?.data?.message ??
        'Impossible d’envoyer l’invitation.'
      );
    }
  };


  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        background: 'rgba(0,0,0,0.4)',
        display: 'flex',
        alignItems: 'flex-start',
        justifyContent: 'center',
        paddingTop: '110px',
        zIndex: 50
      }}
    >

      {/* BOX ALB */}
      <div
        style={{
          background: '#fff',
          border: '1px solid #e5e7eb',
          borderRadius: '10px',
          padding: '24px',
          width: '380px',
          boxSizing: 'border-box'
        }}
      >

        {/* TITRE */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '10px',
            marginBottom: '6px'
          }}
        >
          <Mail size={20} />

          <h1
            style={{
              margin: 0,
              fontSize: '19px',
              fontWeight: 700,
              color: '#111827'
            }}
          >
            Envoyer une invitation
          </h1>
        </div>


        {/* DESCRIPTION */}
        <p
          style={{
            color: '#6b7280',
            fontSize: '12px',
            margin: '0 0 24px'
          }}
        >
          Invitez un utilisateur à rejoindre une organisation.
        </p>


        {/* ORGANISATION */}
        <div style={{ marginBottom: '18px' }}>

          <label
            style={{
              display: 'block',
              fontSize: '13px',
              color: '#374151',
              marginBottom: '6px'
            }}
          >
            Organisation
          </label>

          <select
            value={organizationId}
            onChange={(e) => setOrganizationId(e.target.value)}
            style={{
              width: '100%',
              padding: '9px 12px',
              border: '1px solid #d1d5db',
              borderRadius: '8px',
              fontSize: '14px',
              boxSizing: 'border-box'
            }}
          >
            <option value="">
              Choisir une organisation
            </option>

            {organizations.map((org) => (
              <option
                key={org.id}
                value={org.id}
              >
                {org.name}
              </option>
            ))}

          </select>

        </div>


        {/* EMAIL */}
        <div style={{ marginBottom: '18px' }}>

          <label
            style={{
              display: 'block',
              fontSize: '13px',
              color: '#374151',
              marginBottom: '6px'
            }}
          >
            Adresse email
          </label>

          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="email@exemple.fr"
            style={{
              width: '100%',
              padding: '9px 12px',
              border: '1px solid #d1d5db',
              borderRadius: '8px',
              fontSize: '14px',
              boxSizing: 'border-box'
            }}
          />

        </div>


        {/* ROLE */}
        <div style={{ marginBottom: '24px' }}>

          <label
            style={{
              display: 'block',
              fontSize: '13px',
              color: '#374151',
              marginBottom: '6px'
            }}
          >
            Rôle
          </label>

          <select
            value={role}
            onChange={(e) => setRole(e.target.value)}
            style={{
              width: '100%',
              padding: '9px 12px',
              border: '1px solid #d1d5db',
              borderRadius: '8px',
              fontSize: '14px',
              boxSizing: 'border-box'
            }}
          >
            <option value="CLIENT">
              Client
            </option>

            <option value="ACCOUNTANT">
              Comptable
            </option>

            <option value="ADMIN">
              Administrateur
            </option>
          </select>

        </div>
        {error && (
          <div
            style={{
              marginBottom: '16px',
              padding: '10px 12px',
              background: '#fff0f0',
              color: '#b42318',
              border: '1px solid #ffd1d1',
              borderRadius: '6px',
              fontSize: '12px'
            }}
          >
            {error}
          </div>
        )}
        {/* BOUTONS */}
        <div
          style={{
            display: 'flex',
            justifyContent: 'flex-end',
            gap: '10px'
          }}
        >

          <button
            type="button"
            onClick={() => navigate('/invitations')}
            style={{
              padding: '9px 16px',
              background: '#fff',
              border: '1px solid #d1d5db',
              borderRadius: '8px',
              cursor: 'pointer',
              fontSize: '13px'
            }}
          >
            Annuler
          </button>


          <button
            type="button"
            onClick={handleInvite}
            style={{
              padding: '9px 18px',
              background: '#1a2744',
              color: '#fff',
              border: 'none',
              borderRadius: '8px',
              cursor: 'pointer',
              fontSize: '13px'
            }}
          >
            Envoyer
          </button>

        </div>

      </div>

    </div>
  );
}