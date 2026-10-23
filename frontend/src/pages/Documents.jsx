import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Search, ChevronLeft, ChevronRight, ArrowUp, ArrowDown, X } from 'lucide-react';
import api from '../services/api';
import StatusBadge from '../components/StatusBadge';

// F16 - recherche avancee. Tout l'etat de la recherche vit dans l'URL
// (?org=..&q=..&status=..), donc une recherche se partage par lien et le bouton
// "retour" du navigateur fonctionne. Le backend (GET /documents/search) fait le
// filtrage, le tri et la pagination ; cette page ne fait qu'afficher.

const STATUSES = ['UPLOADED', 'QUEUED', 'PROCESSING', 'VALID', 'INVALID', 'FAILED'];

const SORT_OPTIONS = [
  { value: 'uploadedAt,desc', label: 'Plus récents' },
  { value: 'uploadedAt,asc', label: 'Plus anciens' },
  { value: 'filename,asc', label: 'Nom (A → Z)' },
  { value: 'filename,desc', label: 'Nom (Z → A)' },
  { value: 'status,asc', label: 'Statut' },
  { value: 'size,desc', label: 'Taille' },
];

const PAGE_SIZES = [10, 20, 50];
const DEBOUNCE_MS = 300;

// Cles de l'URL qui sont des filtres : en changer une revient a la page 1.
const FILTER_KEYS = ['q', 'status', 'type', 'owner', 'from', 'to', 'error', 'sort', 'size'];

const inputStyle = {
  padding: '7px 10px',
  border: '1px solid #d1d5db',
  borderRadius: '6px',
  fontSize: '13px',
  background: '#fff',
  color: '#1a1a2e',
};

const labelStyle = {
  display: 'flex',
  flexDirection: 'column',
  gap: '4px',
  fontSize: '12px',
  fontWeight: 500,
  color: '#6b7280',
};

function formatDate(date) {
  return date ? new Date(date).toLocaleDateString('fr-FR') : '';
}

function formatType(type) {
  if (type === 'application/pdf') return 'PDF';
  if (type === 'application/xml' || type === 'text/xml') return 'XML';
  return type;
}

function formatSize(bytes) {
  if (bytes < 1024) return `${bytes} o`;
  if (bytes < 1024 * 1024) return `${Math.round(bytes / 1024)} Ko`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} Mo`;
}

// Entier >= 0 lu dans l'URL ; toute valeur absente, non numerique ou hors borne
// retombe sur la valeur par defaut au lieu d'envoyer "NaN" au backend.
function parsePositiveInt(raw, fallback, { min = 0, max = Number.MAX_SAFE_INTEGER } = {}) {
  const n = Number(raw);
  return raw != null && Number.isInteger(n) && n >= min && n <= max ? n : fallback;
}

// Champ texte qui ne pousse sa valeur qu'apres une pause de frappe.
function useDebouncedParam(value, onCommit) {
  const [draft, setDraft] = useState(value);

  // Resynchronise le champ quand l'URL change (navigation, reinitialisation), mais
  // pas quand l'URL ne fait que refleter la saisie en cours sans ses espaces de
  // bord : sinon "Facture " redeviendrait "Facture" sous les doigts de l'utilisateur.
  useEffect(() => {
    setDraft((current) => (current.trim() === value ? current : value));
  }, [value]);

  useEffect(() => {
    if (draft.trim() === value) return undefined;
    const timer = setTimeout(() => onCommit(draft), DEBOUNCE_MS);
    return () => clearTimeout(timer);
    // onCommit change a chaque rendu ; seule la saisie doit relancer le minuteur
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [draft, value]);

  return [draft, setDraft];
}

export default function Documents() {
  const navigate = useNavigate();
  const [params, setParams] = useSearchParams();

  const [organizations, setOrganizations] = useState([]);
  const [orgsLoading, setOrgsLoading] = useState(true);
  const [members, setMembers] = useState([]);

  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  // --- etat derive de l'URL ---------------------------------------------------
  const orgId = params.get('org') ?? (organizations[0] ? String(organizations[0].id) : '');
  const currentOrg = organizations.find((o) => String(o.id) === orgId);
  const canSeeAll = currentOrg && currentOrg.role !== 'CLIENT';

  const selectedStatuses = useMemo(
    () => (params.get('status') ? params.get('status').split(',') : []),
    [params]
  );
  const sort = params.get('sort') ?? 'uploadedAt,desc';
  const page = parsePositiveInt(params.get('page'), 0);
  const size = parsePositiveInt(params.get('size'), 20, { min: 1, max: 100 });

  const updateParams = (changes) => {
    setParams(
      (prev) => {
        const next = new URLSearchParams(prev);
        Object.entries(changes).forEach(([key, value]) => {
          if (value === '' || value == null) next.delete(key);
          else next.set(key, value);
        });
        if (!('page' in changes) && Object.keys(changes).some((k) => FILTER_KEYS.includes(k))) {
          next.delete('page');
        }
        return next;
      },
      { replace: true }
    );
  };

  const [queryDraft, setQueryDraft] = useDebouncedParam(params.get('q') ?? '', (v) => updateParams({ q: v.trim() }));
  const [errorDraft, setErrorDraft] = useDebouncedParam(params.get('error') ?? '', (v) => updateParams({ error: v.trim() }));

  // --- organisations de l'utilisateur ---------------------------------------
  useEffect(() => {
    let cancelled = false;

    (async () => {
      try {
        const response = await api.get('/organizations');
        const memberships = response.data || [];
        const withNames = await Promise.all(
          memberships.map(async (m) => {
            const id = m.organizationId ?? m.id;
            try {
              const details = await api.get(`/organizations/${id}`);
              return { id, role: m.role, name: details.data.name ?? `Organisation #${id}` };
            } catch {
              return { id, role: m.role, name: `Organisation #${id}` };
            }
          })
        );
        if (!cancelled) setOrganizations(withNames);
      } catch (e) {
        console.error('Organizations error:', e);
        if (!cancelled) setError('Impossible de charger vos organisations.');
      } finally {
        if (!cancelled) setOrgsLoading(false);
      }
    })();

    return () => {
      cancelled = true;
    };
  }, []);

  // --- membres (filtre "deposé par"), seulement pour qui voit tout ----------
  useEffect(() => {
    if (!orgId || !canSeeAll) {
      setMembers([]);
      return;
    }
    let cancelled = false;
    api
      .get(`/organizations/${orgId}/members`)
      .then((response) => !cancelled && setMembers(Array.isArray(response.data) ? response.data : []))
      .catch(() => !cancelled && setMembers([]));
    return () => {
      cancelled = true;
    };
  }, [orgId, canSeeAll]);

  // --- la recherche elle-meme -----------------------------------------------
  const searchKey = params.toString();

  useEffect(() => {
    if (!orgId) return undefined;
    let cancelled = false;

    const query = {
      organizationId: orgId,
      q: params.get('q') || undefined,
      // chaine "A,B" : Spring la decoupe en liste (axios enverrait status[]=A sinon)
      status: params.get('status') || undefined,
      type: params.get('type') || undefined,
      ownerId: params.get('owner') || undefined,
      from: params.get('from') || undefined,
      to: params.get('to') || undefined,
      error: params.get('error') || undefined,
      sort,
      page,
      size,
    };

    setLoading(true);
    setError('');
    api
      .get('/documents/search', { params: query })
      .then((response) => !cancelled && setResult(response.data))
      .catch((e) => {
        if (cancelled) return;
        setResult(null);
        setError(
          e.response?.status === 403
            ? "Vous n'avez pas accès aux documents de cette organisation."
            : e.response?.data?.message ?? 'La recherche a échoué.'
        );
      })
      .finally(() => !cancelled && setLoading(false));

    return () => {
      cancelled = true;
    };
    // searchKey resume tous les parametres de l'URL
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchKey, orgId]);

  // --- actions ----------------------------------------------------------------
  const toggleStatus = (status) => {
    const next = selectedStatuses.includes(status)
      ? selectedStatuses.filter((s) => s !== status)
      : [...selectedStatuses, status];
    updateParams({ status: next.join(',') });
  };

  const toggleColumnSort = (field) => {
    const [currentField, direction] = sort.split(',');
    const nextDirection = currentField === field && direction === 'asc' ? 'desc' : 'asc';
    updateParams({ sort: `${field},${nextDirection}` });
  };

  const hasFilters = ['q', 'status', 'type', 'owner', 'from', 'to', 'error'].some((k) => params.get(k));

  const resetFilters = () => {
    setQueryDraft('');
    setErrorDraft('');
    setParams(orgId ? { org: orgId } : {}, { replace: true });
  };

  const changeOrganization = (id) => {
    setQueryDraft('');
    setErrorDraft('');
    setParams({ org: id }, { replace: true });
  };

  const documents = result?.content ?? [];
  const totalPages = result?.totalPages ?? 0;
  const totalElements = result?.totalElements ?? 0;

  const columns = [
    { key: 'filename', label: 'Fichier', sortable: true },
    { key: 'owner', label: 'Déposé par' },
    { key: 'uploadedAt', label: 'Date', sortable: true },
    { key: 'type', label: 'Type', sortable: true },
    { key: 'size', label: 'Taille', sortable: true },
    { key: 'status', label: 'Statut', sortable: true },
  ];

  const [sortField, sortDirection] = sort.split(',');

  // --- rendu ------------------------------------------------------------------
  if (!orgsLoading && organizations.length === 0) {
    return (
      <div style={{ padding: '24px 28px' }}>
        <h1 style={{ margin: 0, fontSize: '20px', fontWeight: 700, color: '#1a1a2e' }}>Documents</h1>
        <p style={{ fontSize: '13px', color: '#6b7280' }}>
          {error || "Vous n'appartenez à aucune organisation pour le moment."}
        </p>
      </div>
    );
  }

  return (
    <div style={{ padding: '24px 28px', maxWidth: '1100px' }}>
      {/* HEADER */}
      <div style={{ display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between', gap: '16px', marginBottom: '20px', flexWrap: 'wrap' }}>
        <div>
          <h1 style={{ margin: 0, fontSize: '20px', fontWeight: 700, color: '#1a1a2e' }}>Documents</h1>
          <p style={{ margin: '4px 0 0', fontSize: '13px', color: '#6b7280' }}>
            Retrouvez une facture par nom, statut, date ou erreur de conformité.
          </p>
        </div>

        {organizations.length > 1 ? (
          <label style={labelStyle}>
            Organisation
            <select
              aria-label="Organisation"
              value={orgId}
              onChange={(e) => changeOrganization(e.target.value)}
              style={inputStyle}
            >
              {organizations.map((o) => (
                <option key={o.id} value={o.id}>{o.name}</option>
              ))}
            </select>
          </label>
        ) : (
          currentOrg && <span style={{ fontSize: '13px', color: '#374151', fontWeight: 500 }}>{currentOrg.name}</span>
        )}
      </div>

      {/* BARRE DE RECHERCHE */}
      <div style={{ position: 'relative', marginBottom: '12px' }}>
        <Search size={16} color="#9ca3af" style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)' }} />
        <input
          type="search"
          aria-label="Rechercher par nom de fichier"
          placeholder="Rechercher par nom de fichier…"
          value={queryDraft}
          onChange={(e) => setQueryDraft(e.target.value)}
          maxLength={255}
          style={{ ...inputStyle, width: '100%', boxSizing: 'border-box', padding: '10px 12px 10px 36px', fontSize: '14px' }}
        />
      </div>

      {/* FILTRES */}
      <div style={{ background: '#fff', border: '1px solid #e5e7eb', borderRadius: '8px', padding: '14px', marginBottom: '16px' }}>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px', marginBottom: '12px' }} role="group" aria-label="Filtrer par statut">
          {STATUSES.map((s) => {
            const active = selectedStatuses.includes(s);
            return (
              <button
                key={s}
                type="button"
                aria-pressed={active}
                onClick={() => toggleStatus(s)}
                style={{
                  padding: '4px 10px',
                  borderRadius: '999px',
                  fontSize: '12px',
                  cursor: 'pointer',
                  border: active ? '1px solid #1a2744' : '1px solid #d1d5db',
                  background: active ? '#1a2744' : '#fff',
                  color: active ? '#fff' : '#374151',
                }}
              >
                {s}
              </button>
            );
          })}
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(150px, 1fr))', gap: '12px', alignItems: 'end' }}>
          <label style={labelStyle}>
            Erreur (code ou libellé)
            <input
              type="text"
              placeholder="ex. BR-02, montant…"
              value={errorDraft}
              onChange={(e) => setErrorDraft(e.target.value)}
              maxLength={255}
              style={inputStyle}
            />
          </label>

          <label style={labelStyle}>
            Type
            <select value={params.get('type') ?? ''} onChange={(e) => updateParams({ type: e.target.value })} style={inputStyle}>
              <option value="">Tous</option>
              <option value="PDF">PDF</option>
              <option value="XML">XML</option>
            </select>
          </label>

          {canSeeAll && (
            <label style={labelStyle}>
              Déposé par
              <select value={params.get('owner') ?? ''} onChange={(e) => updateParams({ owner: e.target.value })} style={inputStyle}>
                <option value="">Tout le monde</option>
                {members.map((m) => (
                  <option key={m.userId} value={m.userId}>
                    {m.firstName} {m.lastName}
                  </option>
                ))}
              </select>
            </label>
          )}

          <label style={labelStyle}>
            Du
            <input
              type="date"
              value={params.get('from') ?? ''}
              max={params.get('to') ?? undefined}
              onChange={(e) => updateParams({ from: e.target.value })}
              style={inputStyle}
            />
          </label>

          <label style={labelStyle}>
            Au
            <input
              type="date"
              value={params.get('to') ?? ''}
              min={params.get('from') ?? undefined}
              onChange={(e) => updateParams({ to: e.target.value })}
              style={inputStyle}
            />
          </label>

          <label style={labelStyle}>
            Trier par
            <select value={sort} onChange={(e) => updateParams({ sort: e.target.value })} style={inputStyle}>
              {!SORT_OPTIONS.some((o) => o.value === sort) && <option value={sort}>Personnalisé</option>}
              {SORT_OPTIONS.map((o) => (
                <option key={o.value} value={o.value}>{o.label}</option>
              ))}
            </select>
          </label>
        </div>

        {hasFilters && (
          <button
            type="button"
            onClick={resetFilters}
            style={{ marginTop: '12px', display: 'inline-flex', alignItems: 'center', gap: '4px', background: 'none', border: 'none', color: '#4a9eff', fontSize: '12px', cursor: 'pointer', padding: 0 }}
          >
            <X size={13} /> Réinitialiser les filtres
          </button>
        )}
      </div>

      {/* RESULTATS */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px', fontSize: '13px', color: '#6b7280' }}>
        <span aria-live="polite">
          {loading ? 'Recherche…' : result ? `${totalElements} document${totalElements > 1 ? 's' : ''}` : ''}
        </span>
        <label style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
          Par page
          <select value={size} onChange={(e) => updateParams({ size: e.target.value })} style={{ ...inputStyle, padding: '4px 6px' }}>
            {PAGE_SIZES.map((n) => (
              <option key={n} value={n}>{n}</option>
            ))}
          </select>
        </label>
      </div>

      {error && (
        <div role="alert" style={{ padding: '10px 14px', marginBottom: '12px', borderRadius: '6px', background: '#fee2e2', color: '#991b1b', fontSize: '13px' }}>
          {error}
        </div>
      )}

      <div style={{ border: '1px solid #e5e7eb', borderRadius: '8px', overflow: 'auto', background: '#fff', opacity: loading ? 0.6 : 1 }}>
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '13px' }}>
          <thead>
            <tr style={{ background: '#f9fafb' }}>
              {columns.map((col) => (
                <th
                  key={col.key}
                  aria-sort={sortField === col.key ? (sortDirection === 'asc' ? 'ascending' : 'descending') : undefined}
                  style={{ padding: '10px 14px', textAlign: 'left', fontWeight: 600, whiteSpace: 'nowrap' }}
                >
                  {col.sortable ? (
                    <button
                      type="button"
                      onClick={() => toggleColumnSort(col.key)}
                      style={{ display: 'inline-flex', alignItems: 'center', gap: '4px', background: 'none', border: 'none', padding: 0, font: 'inherit', cursor: 'pointer', color: 'inherit' }}
                    >
                      {col.label}
                      {sortField === col.key && (sortDirection === 'asc' ? <ArrowUp size={12} /> : <ArrowDown size={12} />)}
                    </button>
                  ) : (
                    col.label
                  )}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {documents.length > 0 ? (
              documents.map((doc) => (
                <tr
                  key={doc.id}
                  onClick={() => navigate(`/documents/${doc.id}`)}
                  style={{ borderTop: '1px solid #f3f4f6', cursor: 'pointer' }}
                >
                  <td style={{ padding: '10px 14px', fontWeight: 500, color: '#1a2744', wordBreak: 'break-all' }}>{doc.filename}</td>
                  <td style={{ padding: '10px 14px' }}>{doc.ownerName}</td>
                  <td style={{ padding: '10px 14px', whiteSpace: 'nowrap' }}>{formatDate(doc.uploadedAt)}</td>
                  <td style={{ padding: '10px 14px' }}>{formatType(doc.type)}</td>
                  <td style={{ padding: '10px 14px', whiteSpace: 'nowrap' }}>{formatSize(doc.size)}</td>
                  <td style={{ padding: '10px 14px' }}><StatusBadge status={doc.status} /></td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan={columns.length} style={{ padding: '24px', textAlign: 'center', color: '#9ca3af' }}>
                  {loading || orgsLoading ? 'Chargement…' : hasFilters ? 'Aucun document ne correspond à ces critères.' : 'Aucun document'}
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {/* PAGINATION */}
      {totalPages > 1 && (
        <nav aria-label="Pagination" style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', gap: '12px', marginTop: '16px', fontSize: '13px' }}>
          <button
            type="button"
            aria-label="Page précédente"
            disabled={page <= 0}
            onClick={() => updateParams({ page: page - 1 || '' })}
            style={{ ...inputStyle, display: 'inline-flex', cursor: page <= 0 ? 'default' : 'pointer', opacity: page <= 0 ? 0.4 : 1 }}
          >
            <ChevronLeft size={16} />
          </button>
          <span>
            Page {page + 1} / {totalPages}
          </span>
          <button
            type="button"
            aria-label="Page suivante"
            disabled={page + 1 >= totalPages}
            onClick={() => updateParams({ page: page + 1 })}
            style={{ ...inputStyle, display: 'inline-flex', cursor: page + 1 >= totalPages ? 'default' : 'pointer', opacity: page + 1 >= totalPages ? 0.4 : 1 }}
          >
            <ChevronRight size={16} />
          </button>
        </nav>
      )}
    </div>
  );
}
