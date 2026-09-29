import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import api from '../services/api';

export default function InvitationCheck() {

  const { token } = useParams();
  const navigate = useNavigate();

  const [invitation, setInvitation] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [accepting, setAccepting] = useState(false);
  const [accepted, setAccepted] = useState(false);


  useEffect(() => {

    const fetchCheck = async () => {
      try {

        const response = await api.get(
          `/invitations/${token}`
        );

        setInvitation(response.data);

      } catch (err) {

        setError(
          err.response?.data?.message ??
          'Invitation invalide ou expirée.'
        );

      } finally {

        setLoading(false);

      }
    };

    fetchCheck();

  }, [token]);


  const handleAccept = async () => {

    try {

      setAccepting(true);
      setError(null);

      const response = await api.post(
        `/invitations/accept?token=${token}`
      );

      setInvitation(response.data);
      setAccepted(true);

    } catch (err) {

      setError(
        err.response?.data?.message ??
        'Impossible d’accepter l’invitation.'
      );

    } finally {

      setAccepting(false);

    }
  };


  if (loading) {
    return (
      <div style={{ padding: '40px' }}>
        Chargement...
      </div>
    );
  }


if (!invitation && error) {
  return (
    <div
      style={{
        maxWidth: '700px',
        margin: '40px auto',
        padding: '0 24px',
      }}
    >
      <button
        onClick={() => navigate('/')}
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
          Invitation
        </h2>

        <p
          style={{
            margin: 0,
            fontSize: '13px',
            color: '#b42318',
          }}
        >
          {error}
        </p>
      </div>
    </div>
  );
}

  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'flex-start',
        justifyContent: 'center',
        paddingTop: '100px',
        background: '#f5f7fa'
      }}
    >

      <div
        style={{
          width: '420px',
          background: '#fff',
          border: '1px solid #e5e7eb',
          borderRadius: '10px',
          padding: '24px',
          boxSizing: 'border-box'
        }}
      >

        <h1
          style={{
            margin: '0 0 20px',
            fontSize: '20px'
          }}
        >
          Invitation
        </h1>


        <p>
          <strong>Email :</strong> {invitation.email}
        </p>

        <p>
          <strong>Rôle :</strong> {invitation.role}
        </p>

        <p>
          <strong>Statut :</strong> {invitation.status}
        </p>


        {error && (
          <div
            style={{
              marginTop: '18px',
              padding: '10px 12px',
              background: '#fff0f0',
              color: '#b42318',
              border: '1px solid #ffd1d1',
              borderRadius: '6px',
              fontSize: '13px'
            }}
          >
            {error}
          </div>
        )}


        {accepted && (
          <div
            style={{
              marginTop: '18px',
              padding: '10px 12px',
              background: '#ecfdf3',
              color: '#067647',
              border: '1px solid #abefc6',
              borderRadius: '6px',
              fontSize: '13px'
            }}
          >
            Invitation acceptée.
          </div>
        )}


        <div
          style={{
            display: 'flex',
            justifyContent: 'flex-end',
            gap: '10px',
            marginTop: '24px'
          }}
        >

          <button
            type="button"
            onClick={() => navigate('/')}
            style={{
              padding: '9px 16px',
              background: '#fff',
              border: '1px solid #d1d5db',
              borderRadius: '7px',
              cursor: 'pointer'
            }}
          >
            Annuler
          </button>


          {invitation.status === 'PENDING' && !accepted && (
            <button
              type="button"
              onClick={handleAccept}
              disabled={accepting}
              style={{
                padding: '9px 16px',
                background: '#152846',
                color: '#fff',
                border: 'none',
                borderRadius: '7px',
                cursor: 'pointer'
              }}
            >
              {accepting
                ? 'Acceptation...'
                : 'Accepter l’invitation'}
            </button>
          )}


          {accepted && (
            <button
              type="button"
              onClick={() => navigate('/organisations')}
              style={{
                padding: '9px 16px',
                background: '#152846',
                color: '#fff',
                border: 'none',
                borderRadius: '7px',
                cursor: 'pointer'
              }}
            >
              Voir les organisations
            </button>
          )}

        </div>

      </div>

    </div>
  );
}