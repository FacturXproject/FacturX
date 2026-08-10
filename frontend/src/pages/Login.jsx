import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { FileCheck } from 'lucide-react';

export default function Login({ onLogin }) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const navigate = useNavigate();

  const handleSubmit = (e) => {
    e.preventDefault();
    if (onLogin) onLogin();
    navigate('/dashboard');
  };

  return (
    <div style={{
      minHeight: '100vh',
      background: '#f8f9fa',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      padding: '24px',
    }}>
      <div style={{ width: '100%', maxWidth: '380px' }}>
        {/* Logo */}
        <div style={{ textAlign: 'center', marginBottom: '32px' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '10px', marginBottom: '8px' }}>
            <div style={{ background: '#1a2744', borderRadius: '10px', padding: '10px', display: 'inline-flex' }}>
              <FileCheck size={24} color="#4a9eff" />
            </div>
          </div>
          <h1 style={{ margin: '12px 0 4px', fontSize: '22px', fontWeight: 700, color: '#1a1a2e', letterSpacing: '-0.4px' }}>
            Factur-X Validator
          </h1>
          <p style={{ color: '#6b7280', fontSize: '13px' }}>
            Conformité & conversion e-factures
          </p>
        </div>

        {/* Card */}
        <div style={{
          background: '#fff',
          border: '1px solid #e5e7eb',
          borderRadius: '10px',
          padding: '28px',
        }}>
          <h2 style={{ margin: '0 0 20px', fontSize: '16px', fontWeight: 600, color: '#1a1a2e' }}>
            Connexion
          </h2>

          <form onSubmit={handleSubmit}>
            <div style={{ marginBottom: '14px' }}>
              <label style={{ display: 'block', fontSize: '12.5px', fontWeight: 500, color: '#374151', marginBottom: '5px' }}>
                Adresse e-mail
              </label>
              <input
                type="email"
                value={email}
                onChange={e => setEmail(e.target.value)}
                placeholder="prenom.nom@entreprise.fr"
                style={{
                  width: '100%',
                  padding: '8px 10px',
                  border: '1px solid #d1d5db',
                  borderRadius: '6px',
                  fontSize: '14px',
                  color: '#1a1a2e',
                  outline: 'none',
                  boxSizing: 'border-box',
                  background: '#fff',
                }}
              />
            </div>

            <div style={{ marginBottom: '20px' }}>
              <label style={{ display: 'block', fontSize: '12.5px', fontWeight: 500, color: '#374151', marginBottom: '5px' }}>
                Mot de passe
              </label>
              <input
                type="password"
                value={password}
                onChange={e => setPassword(e.target.value)}
                placeholder="••••••••"
                style={{
                  width: '100%',
                  padding: '8px 10px',
                  border: '1px solid #d1d5db',
                  borderRadius: '6px',
                  fontSize: '14px',
                  color: '#1a1a2e',
                  outline: 'none',
                  boxSizing: 'border-box',
                  background: '#fff',
                }}
              />
            </div>

            <button
              type="submit"
              style={{
                width: '100%',
                padding: '9px',
                background: '#1a2744',
                color: '#fff',
                border: 'none',
                borderRadius: '6px',
                fontSize: '14px',
                fontWeight: 500,
                cursor: 'pointer',
              }}
            >
              Se connecter
            </button>
          </form>

          <div style={{ marginTop: '16px', textAlign: 'center' }}>
            <button
              onClick={handleSubmit}
              style={{ background: 'none', border: 'none', color: '#4a9eff', fontSize: '13px', cursor: 'pointer', textDecoration: 'underline' }}
            >
              Créer un compte
            </button>
          </div>
        </div>

        {/* Footer description */}
        <div style={{
          marginTop: '20px',
          padding: '14px 16px',
          background: '#fff',
          border: '1px solid #e5e7eb',
          borderRadius: '8px',
        }}>
          <p style={{ color: '#6b7280', fontSize: '11.5px', lineHeight: '1.6', margin: 0, textAlign: 'center' }}>
            Service de vérification de conformité et de conversion de factures électroniques au format{' '}
            <strong style={{ color: '#4b5563' }}>Factur-X</strong>, conformément à la réforme française
            d'e-invoicing obligatoire à partir du{' '}
            <strong style={{ color: '#4b5563' }}>1er septembre 2026</strong>.
          </p>
        </div>
      </div>
    </div>
  );
}
