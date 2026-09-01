import { useEffect, useState } from 'react';

export default function Invitations() {

  const [invitations, setInvitations] = useState([]);

  useEffect(() => {

    fetch('/api/organizations/1/invitations')
      .then(response => response.json())
      .then(data => setInvitations(data));

  }, []);

return (
  <div style={{ padding: '24px 28px', width: '100%', boxSizing: 'border-box'  }}>

    <h1 style={{
      margin: '0 0 6px',
      fontSize: '19px',
      fontWeight: 700,
      color: '#1a1a2e'
    }}>
      Invitations
    </h1>

    <div style={{
      display: 'flex',
      justifyContent: 'space-between',
      alignItems: 'center'
    }}>

      <p style={{
        margin: 0,
        fontSize: '13px',
        color: '#6b7280'
      }}>
        Gérez les invitations de votre organisation.
      </p>

      <button style={{
        padding: '8px 20px',
        background: '#1a2744',
        color: '#fff',
        border: 'none',
        borderRadius: '6px',
        fontSize: '13.5px',
        fontWeight: 500,
        cursor: 'pointer'
      }}>
        + Envoyer une invitation
      </button>
      </div>

    <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
        gap: '20px',
        marginTop: '28px'
    }}>
      {/* CARD 1 */}
     <div style={{
      padding: '20px',
      background: '#fff',
      borderRadius: '6px',
    }}>
    <div style={{ fontSize: '24px', fontWeight: 700 }}>8</div>
    <div style={{ fontSize: '14px' }}>Invitations envoyées</div>
    <div style={{ fontSize: '12px', color: '#6b7280' }}>Au total</div>
    </div>

      {/* CARD 2 */}
     <div style={{
      padding: '20px',
      background: '#fff',
      borderRadius: '6px',
      }}>
      <div style={{ fontSize: '24px', fontWeight: 700 }}>8</div>
      <div style={{ fontSize: '14px' }}>Invitations envoyées</div>
      <div style={{ fontSize: '12px', color: '#6b7280' }}>Au total</div>
      </div>



      {/* CARD 3 */}
     <div style={{
      padding: '20px',
      background: '#fff',
      borderRadius: '6px',
      }}>
      <div style={{ fontSize: '24px', fontWeight: 700 }}>8</div>
      <div style={{ fontSize: '14px' }}>Invitations envoyées</div>
      <div style={{ fontSize: '12px', color: '#6b7280' }}>Au total</div>
      </div>
      </div>
      
      <div style={{
      marginTop: '48px',
      padding: '16px 20px',
      background: '#fff',
      borderRadius: '6px',
      width: '100%',
      boxSizing: 'border-box'
    }}>
      <div style={{
        display: 'flex',
        gap: '16px',
        alignItems: 'center',
        flexWrap: 'wrap'
      }}>
        <div style={{ fontSize: '12px', color: '#4a9eff' }}>
          Toutes
        </div>

        <div style={{ fontSize: '12px', color: '#6b7280' }}>
          En attente
        </div>

        <div style={{ fontSize: '12px', color: '#6b7280' }}>
          Acceptées
        </div>

        <div style={{ fontSize: '12px', color: '#6b7280' }}>
          Expirées
        </div>

        <div style={{ fontSize: '12px', color: '#6b7280' }}>
          Révoquées
        </div>
      </div>
    </div>
      </div>

    );
}
