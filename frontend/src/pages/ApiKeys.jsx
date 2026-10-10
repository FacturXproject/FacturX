import { useState, useEffect } from 'react';
import { KeyRound, Plus, X, Copy, Check, Trash2, BookOpen, AlertTriangle, Info } from 'lucide-react';
import api from '../services/api';

const DOCS_URL = '/api/public/docs';

const scopeLabels = {
  'documents:read': 'Lecture',
  'documents:write': 'Écriture',
};

const scopeDescriptions = {
  'documents:read': 'Lister les documents, lire leur statut et leur rapport, télécharger les fichiers.',
  'documents:write': 'Déposer, contrôler, renommer et supprimer des documents.',
};

const roleLabels = {
  ADMIN: 'Administrateur',
  ACCOUNTANT: 'Comptable',
  CLIENT: 'Client',
};

const modalOverlay = {
  position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.4)',
  display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 50,
};

const modalBox = {
  background: '#fff', borderRadius: '12px', padding: '24px', width: '440px',
  maxWidth: 'calc(100vw - 32px)', boxShadow: '0 10px 30px rgba(0,0,0,0.15)',
};

const errorBox = {
  background: '#fee2e2', color: '#991b1b', padding: '8px 12px',
  borderRadius: '6px', fontSize: '13px', marginBottom: '12px',
};

const secondaryButton = {
  padding: '8px 16px', borderRadius: '8px', border: '1px solid #d1d5db',
  background: '#fff', cursor: 'pointer', fontSize: '13.5px', color: '#374151',
};

function formatDate(value) {
  if (!value) return '—';
  return new Date(value).toLocaleString('fr-FR', { dateStyle: 'medium', timeStyle: 'short' });
}

function CreateKeyModal({ organizations, onClose, onCreated }) {
  const [name, setName] = useState('');
  const [organizationId, setOrganizationId] = useState(organizations[0]?.id ?? '');
  const [scopes, setScopes] = useState(['documents:read']);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  const canSubmit = name.trim() && organizationId && scopes.length > 0 && !submitting;

  const toggleScope = (scope) => {
    setScopes((current) =>
      current.includes(scope) ? current.filter((s) => s !== scope) : [...current, scope]
    );
  };

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
      setError(err.response?.data?.message ?? 'La clé n’a pas pu être créée.');
      setSubmitting(false);
    }
  };

  return (
    <div style={modalOverlay}>
      <div style={modalBox}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
          <h2 style={{ fontSize: '17px', fontWeight: 600, color: '#111827', margin: 0 }}>
            Nouvelle clé API
          </h2>
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
            onChange={(e) => setName(e.target.value)}
            placeholder="Logiciel comptable"
            maxLength={100}
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
              width: '100%', padding: '9px 12px', border: '1px solid #d1d5db', background: '#fff',
              borderRadius: '8px', fontSize: '14px', marginBottom: '6px', boxSizing: 'border-box',
            }}
          >
            {organizations.map((organization) => (
              <option key={organization.id} value={organization.id}>
                {organization.name} — {roleLabels[organization.role] ?? organization.role}
              </option>
            ))}
          </select>
          <p style={{ fontSize: '12px', color: '#6b7280', margin: '0 0 14px' }}>
            La clé ne donne accès qu’à cette organisation, avec votre rôle dans celle-ci.
          </p>

          <div style={{ fontSize: '13px', color: '#374151', marginBottom: '6px' }}>Permissions</div>
          {Object.keys(scopeLabels).map((scope) => (
            <label key={scope} style={{ display: 'flex', gap: '9px', alignItems: 'flex-start', marginBottom: '9px', cursor: 'pointer' }}>
              <input
                type="checkbox"
                checked={scopes.includes(scope)}
                onChange={() => toggleScope(scope)}
                style={{ marginTop: '3px' }}
              />
              <span>
                <span style={{ fontSize: '13.5px', color: '#111827', fontWeight: 500 }}>{scopeLabels[scope]}</span>
                <span style={{ display: 'block', fontSize: '12px', color: '#6b7280' }}>{scopeDescriptions[scope]}</span>
              </span>
            </label>
          ))}

          {error && <div style={{ ...errorBox, marginTop: '8px' }}>{error}</div>}

          <div style={{ display: 'flex', gap: '8px', justifyContent: 'flex-end', marginTop: '14px' }}>
            <button type="button" onClick={onClose} style={secondaryButton}>
              Annuler
            </button>
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

// Shown once, right after creation: the server keeps only a hash of the key.
function CreatedKeyModal({ created, onClose }) {
  const [copied, setCopied] = useState(false);

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(created.key);
      setCopied(true);
    } catch {
      setCopied(false);
    }
  };

  return (
    <div style={modalOverlay}>
      <div style={{ ...modalBox, width: '560px' }}>
        <h2 style={{ fontSize: '17px', fontWeight: 600, color: '#111827', margin: '0 0 12px' }}>
          Clé « {created.apiKey.name} » créée
        </h2>

        <div style={{
          display: 'flex', gap: '9px', background: '#fffbeb', border: '1px solid #fde68a',
          color: '#92400e', padding: '10px 12px', borderRadius: '8px', fontSize: '13px', marginBottom: '14px',
        }}>
          <AlertTriangle size={17} style={{ flexShrink: 0, marginTop: '1px' }} />
          <span>Copiez cette clé maintenant : elle ne sera plus jamais affichée.</span>
        </div>

        <div style={{ display: 'flex', gap: '8px', marginBottom: '16px' }}>
          <code
            data-testid="created-key"
            style={{
              flex: 1, minWidth: 0, padding: '10px 12px', background: '#f3f4f6', borderRadius: '8px',
              fontSize: '12.5px', color: '#111827', wordBreak: 'break-all',
            }}
          >
            {created.key}
          </code>
          <button
            type="button"
            onClick={handleCopy}
            style={{ ...secondaryButton, display: 'flex', alignItems: 'center', gap: '6px', flexShrink: 0 }}
          >
            {copied ? <Check size={15} color="#15803d" /> : <Copy size={15} />}
            {copied ? 'Copiée' : 'Copier'}
          </button>
        </div>

        <div style={{ fontSize: '13px', color: '#374151', marginBottom: '6px' }}>Exemple d’appel</div>
        <pre style={{
          margin: '0 0 16px', padding: '10px 12px', background: '#111827', color: '#e5e7eb',
          borderRadius: '8px', fontSize: '12px', overflowX: 'auto',
        }}>
{`curl -H "X-API-Key: ${created.key}" \\
     ${window.location.origin}/api/public/v1/documents`}
        </pre>

        <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
          <button
            type="button"
            onClick={onClose}
            style={{
              padding: '8px 16px', borderRadius: '8px', border: 'none', background: '#1a2744',
              color: '#fff', cursor: 'pointer', fontSize: '13.5px',
            }}
          >
            J’ai copié la clé
          </button>
        </div>
      </div>
    </div>
  );
}

function RevokeKeyConfirm({ apiKey, onClose, onConfirm }) {
  const [revoking, setRevoking] = useState(false);
  const [error, setError] = useState(null);

  const handleRevoke = async () => {
    setRevoking(true);
    setError(null);
    try {
      await onConfirm();
    } catch (err) {
      setError(err.response?.data?.message ?? 'La clé n’a pas pu être révoquée.');
      setRevoking(false);
    }
  };

  return (
    <div style={modalOverlay}>
      <div style={modalBox}>
        <h2 style={{ fontSize: '17px', fontWeight: 600, color: '#111827', margin: '0 0 10px' }}>
          Révoquer la clé « {apiKey.name} » ?
        </h2>
        <p style={{ fontSize: '13.5px', color: '#4b5563', margin: '0 0 16px' }}>
          Les logiciels qui utilisent cette clé perdront l’accès immédiatement. Cette action est définitive.
        </p>

        {error && <div style={errorBox}>{error}</div>}

        <div style={{ display: 'flex', gap: '8px', justifyContent: 'flex-end' }}>
          <button type="button" onClick={onClose} style={secondaryButton}>
            Annuler
          </button>
          <button
            type="button"
            onClick={handleRevoke}
            disabled={revoking}
            style={{
              padding: '8px 16px', borderRadius: '8px', border: 'none',
              background: revoking ? '#9ca3af' : '#dc2626',
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

export default function ApiKeys() {
  const [apiKeys, setApiKeys] = useState([]);
  const [organizations, setOrganizations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showCreate, setShowCreate] = useState(false);
  const [created, setCreated] = useState(null);
  const [keyToRevoke, setKeyToRevoke] = useState(null);

  const loadKeys = async () => {
    const response = await api.get('/api-keys');
    setApiKeys(response.data);
  };

  useEffect(() => {
    const load = async () => {
      try {
        setError(null);
        await loadKeys();

        // GET /organizations returns memberships; the organization itself is organizationId.
        const memberships = await api.get('/organizations');
        const withNames = await Promise.all(
          memberships.data.map(async (membership) => {
            const id = membership.organizationId ?? membership.id;
            const details = await api.get(`/organizations/${id}`);
            return { id, name: details.data.name, role: membership.role };
          })
        );
        setOrganizations(withNames);
      } catch {
        setError('Impossible de charger les clés API.');
      } finally {
        setLoading(false);
      }
    };

    load();
  }, []);

  const handleCreated = async (response) => {
    setShowCreate(false);
    setCreated(response);
    setApiKeys((current) => [response.apiKey, ...current]);
  };

  const handleRevoke = async () => {
    await api.delete(`/api-keys/${keyToRevoke.id}`);
    setKeyToRevoke(null);
    await loadKeys();
  };

  const cell = { padding: '12px 16px', fontSize: '13.5px', color: '#374151', textAlign: 'left' };
  const headCell = { ...cell, fontSize: '12px', color: '#6b7280', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.4px' };

  return (
    <div style={{ padding: '28px 32px', maxWidth: '1100px' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: '16px', flexWrap: 'wrap', marginBottom: '20px' }}>
        <div>
          <h1 style={{ fontSize: '22px', fontWeight: 700, color: '#111827', margin: '0 0 4px' }}>Clés API</h1>
          <p style={{ fontSize: '14px', color: '#6b7280', margin: 0 }}>
            Connectez un logiciel comptable à vos documents, sans passer par l’interface.
          </p>
        </div>

        <div style={{ display: 'flex', gap: '8px' }}>
          <a
            href={DOCS_URL}
            target="_blank"
            rel="noreferrer"
            style={{ ...secondaryButton, display: 'flex', alignItems: 'center', gap: '7px', textDecoration: 'none' }}
          >
            <BookOpen size={15} />
            Documentation de l’API
          </a>
          <button
            type="button"
            onClick={() => setShowCreate(true)}
            disabled={loading || organizations.length === 0}
            style={{
              display: 'flex', alignItems: 'center', gap: '7px', padding: '8px 16px', borderRadius: '8px',
              border: 'none', fontSize: '13.5px', color: '#fff',
              background: loading || organizations.length === 0 ? '#9ca3af' : '#1a2744',
              cursor: loading || organizations.length === 0 ? 'not-allowed' : 'pointer',
            }}
          >
            <Plus size={15} />
            Nouvelle clé
          </button>
        </div>
      </div>

      <div style={{
        display: 'flex', gap: '9px', background: '#eff6ff', color: '#1e40af', padding: '10px 14px',
        borderRadius: '8px', fontSize: '13px', marginBottom: '18px',
      }}>
        <Info size={16} style={{ flexShrink: 0, marginTop: '1px' }} />
        <span>
          Envoyez la clé dans l’en-tête <code>X-API-Key</code> de chaque requête. Une clé est liée à une
          organisation et se comporte comme vous dans celle-ci : elle ne peut jamais faire plus que votre rôle.
        </span>
      </div>

      {error && <div style={errorBox}>{error}</div>}

      <div style={{ background: '#fff', border: '1px solid #e5e7eb', borderRadius: '12px', overflowX: 'auto' }}>
        {loading ? (
          <div style={{ padding: '32px', textAlign: 'center', color: '#6b7280', fontSize: '14px' }}>
            Chargement des clés API...
          </div>
        ) : apiKeys.length === 0 ? (
          <div style={{ padding: '40px 24px', textAlign: 'center' }}>
            <KeyRound size={30} color="#9ca3af" />
            <div style={{ fontSize: '15px', color: '#111827', fontWeight: 600, margin: '10px 0 4px' }}>
              Aucune clé API
            </div>
            <div style={{ fontSize: '13.5px', color: '#6b7280' }}>
              {organizations.length === 0
                ? 'Rejoignez ou créez une organisation pour pouvoir créer une clé.'
                : 'Créez une clé pour connecter un logiciel à vos documents.'}
            </div>
          </div>
        ) : (
          <table style={{ width: '100%', borderCollapse: 'collapse' }}>
            <thead>
              <tr style={{ borderBottom: '1px solid #e5e7eb', background: '#f9fafb' }}>
                <th style={headCell}>Nom</th>
                <th style={headCell}>Organisation</th>
                <th style={headCell}>Clé</th>
                <th style={headCell}>Permissions</th>
                <th style={headCell}>Créée le</th>
                <th style={headCell}>Dernière utilisation</th>
                <th style={headCell}>État</th>
                <th style={headCell}></th>
              </tr>
            </thead>
            <tbody>
              {apiKeys.map((apiKey) => (
                <tr key={apiKey.id} style={{ borderBottom: '1px solid #f3f4f6', opacity: apiKey.revokedAt ? 0.6 : 1 }}>
                  <td style={{ ...cell, color: '#111827', fontWeight: 500 }}>{apiKey.name}</td>
                  <td style={cell}>{apiKey.organizationName ?? '—'}</td>
                  <td style={cell}><code style={{ fontSize: '12.5px' }}>{apiKey.keyPrefix}…</code></td>
                  <td style={cell}>
                    {apiKey.scopes.map((scope) => scopeLabels[scope] ?? scope).join(', ')}
                  </td>
                  <td style={cell}>{formatDate(apiKey.createdAt)}</td>
                  <td style={cell}>{apiKey.lastUsedAt ? formatDate(apiKey.lastUsedAt) : 'Jamais'}</td>
                  <td style={cell}>
                    <span style={{
                      padding: '3px 9px', borderRadius: '999px', fontSize: '12px', fontWeight: 500,
                      background: apiKey.revokedAt ? '#f3f4f6' : '#dcfce7',
                      color: apiKey.revokedAt ? '#6b7280' : '#15803d',
                    }}>
                      {apiKey.revokedAt ? 'Révoquée' : 'Active'}
                    </span>
                  </td>
                  <td style={{ ...cell, textAlign: 'right' }}>
                    {!apiKey.revokedAt && (
                      <button
                        type="button"
                        onClick={() => setKeyToRevoke(apiKey)}
                        style={{
                          display: 'inline-flex', alignItems: 'center', gap: '6px', padding: '6px 10px',
                          borderRadius: '6px', border: '1px solid #fecaca', background: '#fff',
                          color: '#dc2626', cursor: 'pointer', fontSize: '12.5px',
                        }}
                      >
                        <Trash2 size={13} />
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
        <CreateKeyModal
          organizations={organizations}
          onClose={() => setShowCreate(false)}
          onCreated={handleCreated}
        />
      )}

      {created && <CreatedKeyModal created={created} onClose={() => setCreated(null)} />}

      {keyToRevoke && (
        <RevokeKeyConfirm
          apiKey={keyToRevoke}
          onClose={() => setKeyToRevoke(null)}
          onConfirm={handleRevoke}
        />
      )}
    </div>
  );
}
