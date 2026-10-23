import { useState, useEffect } from 'react';
import { KeyRound, Plus, X, Copy, Check, AlertTriangle, BookOpen, Trash2 } from 'lucide-react';
import api from '../services/api';

// F17 : documentation interactive (Swagger UI) servie par le backend, derriere Nginx.
export const API_DOCS_URL = '/api/docs/swagger-ui/index.html';

const SCOPES = [
  { value: 'DOCUMENTS_READ', label: 'Lecture', description: 'Lister les documents, lire leur statut et leur rapport, les télécharger' },
  { value: 'DOCUMENTS_WRITE', label: 'Dépôt', description: 'Déposer des documents' },
  { value: 'DOCUMENTS_VALIDATE', label: 'Validation', description: 'Lancer la validation Factur-X' },
];

const scopeLabels = Object.fromEntries(SCOPES.map((s) => [s.value, s.label]));

function formatDate(value) {
  if (!value) return '—';
  return new Date(value).toLocaleString('fr-FR', { dateStyle: 'short', timeStyle: 'short' });
}

function errorMessage(err) {
  const data = err.response?.data;
  if (data?.fields) return Object.values(data.fields).join(' ');
  return data?.message ?? err.message;
}

const modalBackdrop = {
  position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.4)',
  display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 50,
};
const modalCard = {
  background: '#fff', borderRadius: '12px', padding: '24px', width: '440px',
  maxWidth: 'calc(100vw - 32px)', boxShadow: '0 10px 30px rgba(0,0,0,0.15)',
};
const secondaryButton = {
  padding: '8px 16px', borderRadius: '8px', border: '1px solid #d1d5db',
  background: '#fff', cursor: 'pointer', fontSize: '13.5px', color: '#374151',
};
const errorBox = {
  background: '#fee2e2', color: '#991b1b', padding: '8px 12px',
  borderRadius: '6px', fontSize: '13px', marginBottom: '12px',
};

function CreateApiKeyModal({ organizations, onClose, onCreated }) {
  const [name, setName] = useState('');
  const [organizationId, setOrganizationId] = useState(organizations[0]?.id ?? '');
  const [scopes, setScopes] = useState(['DOCUMENTS_READ']);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  const toggleScope = (value) => {
    setScopes((prev) => (prev.includes(value) ? prev.filter((s) => s !== value) : [...prev, value]));
  };

  const canSubmit = name.trim() && organizationId && scopes.length > 0 && !submitting;

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!canSubmit) return;
    setSubmitting(true);
    setError(null);
    try {
      const response = await api.post('/api-keys', {
        name: name.trim(),
        organizationId: Number(organizationId),
        scopes,
      });
      onCreated(response.data);
    } catch (err) {
      setError(errorMessage(err));
      setSubmitting(false);
    }
  };

  return (
    <div style={modalBackdrop}>
      <div style={modalCard}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
          <h2 style={{ fontSize: '17px', fontWeight: 600, color: '#111827', margin: 0 }}>Créer une clé API</h2>
          <button onClick={onClose} aria-label="Fermer" style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#9ca3af' }}>
            <X size={18} />
          </button>
        </div>

        <form onSubmit={handleSubmit}>
          <label htmlFor="api-key-name" style={{ display: 'block', fontSize: '13px', color: '#374151', marginBottom: '6px' }}>
            Nom de la clé
          </label>
          <input
            id="api-key-name"
            type="text"
            value={name}
            maxLength={100}
            onChange={(e) => setName(e.target.value)}
            placeholder="Logiciel comptable"
            autoFocus
            style={{
              width: '100%', padding: '9px 12px', border: '1px solid #d1d5db',
              borderRadius: '8px', fontSize: '14px', marginBottom: '14px', boxSizing: 'border-box',
            }}
          />

          <label htmlFor="api-key-organization" style={{ display: 'block', fontSize: '13px', color: '#374151', marginBottom: '6px' }}>
            Organisation
          </label>
          <select
            id="api-key-organization"
            value={organizationId}
            onChange={(e) => setOrganizationId(e.target.value)}
            style={{
              width: '100%', padding: '9px 12px', border: '1px solid #d1d5db',
              borderRadius: '8px', fontSize: '14px', marginBottom: '14px', background: '#fff', boxSizing: 'border-box',
            }}
          >
            {organizations.map((org) => (
              <option key={org.id} value={org.id}>{org.name}</option>
            ))}
          </select>

          <div style={{ fontSize: '13px', color: '#374151', marginBottom: '6px' }}>Autorisations (scopes)</div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', marginBottom: '14px' }}>
            {SCOPES.map((scope) => (
              <label key={scope.value} style={{ display: 'flex', gap: '10px', alignItems: 'flex-start', fontSize: '13.5px', cursor: 'pointer' }}>
                <input
                  type="checkbox"
                  checked={scopes.includes(scope.value)}
                  onChange={() => toggleScope(scope.value)}
                  style={{ marginTop: '3px' }}
                />
                <span>
                  <span style={{ fontWeight: 600, color: '#111827' }}>{scope.label}</span>
                  <span style={{ color: '#6b7280' }}> — {scope.description}</span>
                </span>
              </label>
            ))}
          </div>

          <p style={{ fontSize: '12.5px', color: '#6b7280', margin: '0 0 14px' }}>
            La clé agit avec vos droits dans cette organisation, jamais plus.
          </p>

          {error && <div style={errorBox}>{error}</div>}

          <div style={{ display: 'flex', gap: '8px', justifyContent: 'flex-end' }}>
            <button type="button" onClick={onClose} style={secondaryButton}>Annuler</button>
            <button
              type="submit"
              disabled={!canSubmit}
              style={{
                padding: '8px 16px', borderRadius: '8px', border: 'none',
                background: canSubmit ? '#1a2744' : '#9ca3af',
                color: '#fff', cursor: canSubmit ? 'pointer' : 'not-allowed', fontSize: '13.5px',
              }}
            >
              {submitting ? 'Création...' : 'Créer la clé'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

function NewKeySecretModal({ secret, onClose }) {
  const [copied, setCopied] = useState(false);

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(secret);
      setCopied(true);
    } catch {
      // Presse-papiers indisponible (http, permissions) : la cle reste selectionnable.
    }
  };

  return (
    <div style={modalBackdrop}>
      <div style={modalCard}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '12px' }}>
          <KeyRound size={20} color="#16a34a" />
          <h2 style={{ fontSize: '16px', fontWeight: 600, color: '#111827', margin: 0 }}>Votre nouvelle clé API</h2>
        </div>
        <div style={{ display: 'flex', gap: '8px', alignItems: 'flex-start', background: '#fef3c7', color: '#92400e', padding: '10px 12px', borderRadius: '8px', fontSize: '13px', marginBottom: '12px' }}>
          <AlertTriangle size={16} style={{ flexShrink: 0, marginTop: '1px' }} />
          Copiez-la maintenant : elle ne sera plus jamais affichée. Seule son empreinte est conservée.
        </div>
        <div style={{ display: 'flex', gap: '8px', marginBottom: '16px' }}>
          <code
            data-testid="api-key-secret"
            style={{
              flex: 1, padding: '9px 12px', background: '#f3f4f6', borderRadius: '8px',
              fontSize: '12.5px', wordBreak: 'break-all', userSelect: 'all',
            }}
          >
            {secret}
          </code>
          <button onClick={handleCopy} style={{ ...secondaryButton, display: 'flex', alignItems: 'center', gap: '6px' }}>
            {copied ? <Check size={15} /> : <Copy size={15} />}
            {copied ? 'Copiée' : 'Copier'}
          </button>
        </div>
        <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
          <button
            onClick={onClose}
            style={{ padding: '8px 16px', borderRadius: '8px', border: 'none', background: '#1a2744', color: '#fff', cursor: 'pointer', fontSize: '13.5px' }}
          >
            J'ai copié la clé
          </button>
        </div>
      </div>
    </div>
  );
}

function RevokeConfirm({ apiKey, onClose, onConfirm }) {
  const [revoking, setRevoking] = useState(false);
  const [error, setError] = useState(null);

  const handleRevoke = async () => {
    setRevoking(true);
    setError(null);
    try {
      await onConfirm();
      onClose();
    } catch (err) {
      setError(errorMessage(err));
      setRevoking(false);
    }
  };

  return (
    <div style={modalBackdrop}>
      <div style={modalCard}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '12px' }}>
          <AlertTriangle size={20} color="#dc2626" />
          <h2 style={{ fontSize: '16px', fontWeight: 600, color: '#111827', margin: 0 }}>
            Révoquer la clé « {apiKey.name} » ?
          </h2>
        </div>
        <p style={{ color: '#6b7280', fontSize: '13.5px', marginBottom: '16px' }}>
          Les applications qui l'utilisent seront immédiatement refusées. Cette action est irréversible.
        </p>
        {error && <div style={errorBox}>{error}</div>}
        <div style={{ display: 'flex', gap: '8px', justifyContent: 'flex-end' }}>
          <button onClick={onClose} style={secondaryButton}>Annuler</button>
          <button
            onClick={handleRevoke}
            disabled={revoking}
            style={{
              padding: '8px 16px', borderRadius: '8px', border: 'none', background: '#dc2626',
              color: '#fff', cursor: revoking ? 'not-allowed' : 'pointer', fontSize: '13.5px',
            }}
          >
            {revoking ? 'Révocation...' : 'Révoquer'}
          </button>
        </div>
      </div>
    </div>
  );
}

export default function ApiKeysPage() {
  const [apiKeys, setApiKeys] = useState([]);
  const [organizations, setOrganizations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showCreate, setShowCreate] = useState(false);
  const [newSecret, setNewSecret] = useState(null);
  const [revokeTarget, setRevokeTarget] = useState(null);

  const fetchApiKeys = async () => {
    const response = await api.get('/api-keys');
    setApiKeys(response.data);
  };

  // GET /organizations renvoie les adhesions ; le nom vient de GET /organizations/{id}.
  const fetchOrganizations = async () => {
    const response = await api.get('/organizations');
    const memberships = response.data ?? [];
    const orgs = await Promise.all(
      memberships.map(async (membership) => {
        const id = membership.organizationId ?? membership.id;
        try {
          const detail = await api.get(`/organizations/${id}`);
          return { id, name: detail.data.name };
        } catch {
          return { id, name: `Organisation #${id}` };
        }
      })
    );
    setOrganizations(orgs);
  };

  useEffect(() => {
    Promise.all([fetchApiKeys(), fetchOrganizations()])
      .catch((err) => setError(errorMessage(err)))
      .finally(() => setLoading(false));
  }, []);

  const handleCreated = (created) => {
    setShowCreate(false);
    setNewSecret(created.key);
    setApiKeys((prev) => [created.apiKey, ...prev]);
  };

  const handleRevoke = async (apiKey) => {
    const response = await api.delete(`/api-keys/${apiKey.id}`);
    setApiKeys((prev) => prev.map((k) => (k.id === apiKey.id ? response.data : k)));
  };

  const thStyle = {
    textAlign: 'left', padding: '10px 14px', fontSize: '12px', fontWeight: 600,
    color: '#6b7280', textTransform: 'uppercase', letterSpacing: '0.03em', borderBottom: '1px solid #e5e7eb',
  };
  const tdStyle = { padding: '12px 14px', fontSize: '13.5px', color: '#374151', borderBottom: '1px solid #f3f4f6' };

  return (
    <div style={{ padding: '32px 40px', maxWidth: '1200px', margin: '0 auto', background: '#f8f9fa', minHeight: '100vh' }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: '16px', flexWrap: 'wrap', marginBottom: '24px' }}>
        <div>
          <h1 style={{ fontSize: '26px', fontWeight: 700, marginBottom: '4px', color: '#111827' }}>Clés API</h1>
          <p style={{ color: '#6b7280', fontSize: '14px', margin: 0 }}>
            Connectez votre logiciel comptable à FacturX : dépôt, statut, rapport et téléchargement des factures.
          </p>
        </div>
        <div style={{ display: 'flex', gap: '8px' }}>
          <a
            href={API_DOCS_URL}
            target="_blank"
            rel="noreferrer"
            style={{
              display: 'flex', alignItems: 'center', gap: '6px', textDecoration: 'none',
              border: '1px solid #d1d5db', background: '#fff', color: '#374151',
              borderRadius: '8px', padding: '10px 16px', fontSize: '13.5px', fontWeight: 500,
            }}
          >
            <BookOpen size={16} />
            Documentation de l'API
          </a>
          <button
            onClick={() => setShowCreate(true)}
            disabled={organizations.length === 0}
            title={organizations.length === 0 ? 'Rejoignez ou créez une organisation pour créer une clé' : undefined}
            style={{
              display: 'flex', alignItems: 'center', gap: '6px',
              background: organizations.length === 0 ? '#9ca3af' : '#1a2744', color: '#fff', border: 'none',
              borderRadius: '8px', padding: '10px 18px', fontSize: '13.5px', fontWeight: 500,
              cursor: organizations.length === 0 ? 'not-allowed' : 'pointer',
            }}
          >
            <Plus size={16} />
            Créer une clé
          </button>
        </div>
      </div>

      {/* Usage */}
      <div style={{ background: '#fff', border: '1px solid #e5e7eb', borderRadius: '10px', padding: '16px 18px', marginBottom: '20px' }}>
        <div style={{ fontSize: '14px', fontWeight: 600, color: '#111827', marginBottom: '6px' }}>Utilisation</div>
        <p style={{ fontSize: '13px', color: '#6b7280', margin: '0 0 10px' }}>
          Envoyez la clé dans l'en-tête <code>X-API-Key</code>. Limite : 60 requêtes par minute et par clé.
        </p>
        <pre style={{ margin: 0, background: '#111827', color: '#e5e7eb', padding: '12px 14px', borderRadius: '8px', fontSize: '12.5px', overflowX: 'auto' }}>
{`curl -H "X-API-Key: fxk_..." https://localhost:8443/api/public/v1/documents`}
        </pre>
      </div>

      {error && <div style={errorBox}>Erreur : {error}</div>}

      {/* Keys table */}
      <div style={{ background: '#fff', border: '1px solid #e5e7eb', borderRadius: '10px', overflowX: 'auto' }}>
        {loading ? (
          <div style={{ padding: '32px', textAlign: 'center', color: '#6b7280', fontSize: '14px' }}>Chargement des clés API...</div>
        ) : apiKeys.length === 0 ? (
          <div style={{ padding: '40px', textAlign: 'center', color: '#6b7280', fontSize: '14px' }}>
            <KeyRound size={28} color="#9ca3af" style={{ marginBottom: '8px' }} />
            <div>Aucune clé API pour le moment.</div>
          </div>
        ) : (
          <table style={{ width: '100%', borderCollapse: 'collapse', minWidth: '820px' }}>
            <thead>
              <tr>
                <th style={thStyle}>Nom</th>
                <th style={thStyle}>Organisation</th>
                <th style={thStyle}>Clé</th>
                <th style={thStyle}>Scopes</th>
                <th style={thStyle}>Créée le</th>
                <th style={thStyle}>Dernière utilisation</th>
                <th style={thStyle}>Statut</th>
                <th style={thStyle} aria-label="Actions" />
              </tr>
            </thead>
            <tbody>
              {apiKeys.map((apiKey) => (
                <tr key={apiKey.id} style={{ opacity: apiKey.active ? 1 : 0.6 }}>
                  <td style={{ ...tdStyle, fontWeight: 600, color: '#111827' }}>{apiKey.name}</td>
                  <td style={tdStyle}>{apiKey.organizationName}</td>
                  <td style={tdStyle}><code style={{ fontSize: '12.5px' }}>{apiKey.prefix}…</code></td>
                  <td style={tdStyle}>
                    <div style={{ display: 'flex', gap: '4px', flexWrap: 'wrap' }}>
                      {apiKey.scopes.map((scope) => (
                        <span key={scope} style={{ background: '#eff6ff', color: '#1d4ed8', padding: '2px 8px', borderRadius: '999px', fontSize: '12px' }}>
                          {scopeLabels[scope] ?? scope}
                        </span>
                      ))}
                    </div>
                  </td>
                  <td style={tdStyle}>{formatDate(apiKey.createdAt)}</td>
                  <td style={tdStyle}>{formatDate(apiKey.lastUsedAt)}</td>
                  <td style={tdStyle}>
                    <span style={{
                      padding: '2px 10px', borderRadius: '999px', fontSize: '12px', fontWeight: 500,
                      background: apiKey.active ? '#dcfce7' : '#f3f4f6',
                      color: apiKey.active ? '#15803d' : '#6b7280',
                    }}>
                      {apiKey.active ? 'Active' : 'Révoquée'}
                    </span>
                  </td>
                  <td style={{ ...tdStyle, textAlign: 'right' }}>
                    {apiKey.active && (
                      <button
                        onClick={() => setRevokeTarget(apiKey)}
                        style={{ display: 'inline-flex', alignItems: 'center', gap: '5px', background: 'none', border: 'none', color: '#dc2626', cursor: 'pointer', fontSize: '13px' }}
                      >
                        <Trash2 size={14} />
                        Révoquer
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {showCreate && (
        <CreateApiKeyModal organizations={organizations} onClose={() => setShowCreate(false)} onCreated={handleCreated} />
      )}
      {newSecret && <NewKeySecretModal secret={newSecret} onClose={() => setNewSecret(null)} />}
      {revokeTarget && (
        <RevokeConfirm apiKey={revokeTarget} onClose={() => setRevokeTarget(null)} onConfirm={() => handleRevoke(revokeTarget)} />
      )}
    </div>
  );
}
