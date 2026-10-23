import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Loader2 } from 'lucide-react';
import api from '../services/api';
import { downloadDocument } from '../utils/download';
import JobStatusPanel from './JobStatusPanel';

// F13 : génération du Factur-X à partir du brouillon corrigé (F12), puis validation
// indépendante du fichier par le moteur Java. Le serveur refuse de générer à partir
// d'un brouillon jamais enregistré : la vérification humaine est obligatoire.

const PROFILES = [
  { value: 'EN16931', label: 'EN 16931 (recommandé)' },
  { value: 'BASIC', label: 'BASIC' },
];

const STATUS = {
  VALID: { color: '#15803d', background: '#f0fdf4', border: '#bbf7d0', label: 'Conforme' },
  INVALID: { color: '#b91c1c', background: '#fff5f5', border: '#fecaca', label: 'Non conforme' },
  FAILED: { color: '#92400e', background: '#fffbeb', border: '#fde68a', label: 'Échec' },
};

function formatDate(value) {
  if (!value) return '';
  return new Date(value).toLocaleString('fr-FR', { dateStyle: 'short', timeStyle: 'short' });
}

function GenerationResult({ generation, onDownload }) {
  const style = STATUS[generation.status] ?? STATUS.FAILED;
  return (
    <div
      role="status"
      style={{ border: `1px solid ${style.border}`, background: style.background, borderRadius: '8px', padding: '12px 14px', marginTop: '12px' }}
    >
      <div style={{ fontWeight: 600, color: style.color, fontSize: '13.5px' }}>
        {generation.status === 'VALID' && 'Facture Factur-X générée et conforme.'}
        {generation.status === 'INVALID' && `Le fichier généré n'est pas conforme (${generation.errorCount} erreur${generation.errorCount > 1 ? 's' : ''}).`}
        {generation.status === 'FAILED' && (generation.errorMessage || 'La génération a échoué.')}
      </div>
      <div style={{ fontSize: '12px', color: '#6b7280', marginTop: '4px' }}>
        Profil {generation.profile}
        {generation.warningCount > 0 && ` · ${generation.warningCount} avertissement${generation.warningCount > 1 ? 's' : ''}`}
        {' · '}{formatDate(generation.createdAt)}
      </div>
      {generation.generatedDocumentId && (
        <div style={{ display: 'flex', gap: '10px', marginTop: '10px', flexWrap: 'wrap' }}>
          <button
            type="button"
            onClick={() => onDownload(generation)}
            style={{ padding: '7px 12px', border: 'none', borderRadius: '6px', background: '#1a2744', color: '#fff', fontSize: '12.5px', cursor: 'pointer' }}
          >
            Télécharger {generation.filename}
          </button>
          {generation.validationRunId && (
            <Link
              to={`/rapport?documentId=${generation.generatedDocumentId}`}
              style={{ padding: '7px 12px', border: '1px solid #d1d5db', borderRadius: '6px', background: '#fff', color: '#1a2744', fontSize: '12.5px', textDecoration: 'none' }}
            >
              Voir le rapport de validation
            </Link>
          )}
        </div>
      )}
    </div>
  );
}

/**
 * @param documentId  document source (PDF déposé)
 * @param blockedReason texte affiché et bouton désactivé tant que la génération n'est
 *                      pas possible (modifications non enregistrées, brouillon jamais vérifié)
 */
export default function FacturXGenerationPanel({ documentId, blockedReason }) {
  const [profile, setProfile] = useState('EN16931');
  const [generating, setGenerating] = useState(false);
  const [history, setHistory] = useState([]);
  const [error, setError] = useState(null);
  const [latestId, setLatestId] = useState(null);
  const [job, setJob] = useState(null);

  const loadHistory = useCallback(async () => {
    if (!documentId) {
      return [];
    }

    try {
      const response = await api.get(
        `/documents/${documentId}/generations`
      );

      const generations =
        Array.isArray(response.data)
          ? response.data
          : [];

      setHistory(generations);

      return generations;
    } catch {
      setHistory([]);
      return [];
    }
  }, [documentId]);

  useEffect(() => {
    loadHistory();
  }, [loadHistory]);

  const handleGenerate = async () => {
    setGenerating(true);
    setError(null);
    setJob(null);

    try {
      const response = await api.post(
        `/jobs/generation?documentId=${documentId}`,
        { profile }
      );

      setJob(response.data);
    } catch (err) {
      const body = err.response?.data;

      setError({
        text:
          body?.errors?.length
            ? 'Génération impossible :'
            : body?.message
              || body?.error
              || 'Impossible de démarrer la génération.',
        details: body?.errors ?? [],
      });
    } finally {
      setGenerating(false);
    }
  };

  const handleJobDone = useCallback(async () => {
    const generations = await loadHistory();

    if (generations.length > 0) {
      setLatestId(generations[0].id);
    }
  }, [loadHistory]);

  const handleDownload = async (generation) => {
    try {
      await downloadDocument(generation.generatedDocumentId, generation.filename);
    } catch {
      setError({ text: 'Téléchargement impossible.', details: [] });
    }
  };

  const latest = history.find(g => g.id === latestId);
  const previous = history.filter(g => g.id !== latestId);

  const jobRunning =
    generating
    || job?.status === 'QUEUED'
    || job?.status === 'PROCESSING';

  return (
    <div style={{ border: '1px solid #e5e7eb', borderRadius: '8px', background: '#fff', padding: '16px 18px', marginTop: '12px' }}>
      <div style={{ fontSize: '12px', fontWeight: 600, color: '#374151', marginBottom: '10px', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
        Génération Factur-X
      </div>
      <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
        <label htmlFor="facturx-profile" style={{ fontSize: '12.5px', color: '#6b7280' }}>Profil</label>
        <select
          id="facturx-profile"
          value={profile}
          onChange={e => setProfile(e.target.value)}
          disabled={jobRunning}
          style={{ padding: '6px 8px', border: '1px solid #d1d5db', borderRadius: '5px', fontSize: '12.5px' }}
        >
          {PROFILES.map(p => <option key={p.value} value={p.value}>{p.label}</option>)}
        </select>
        <button
          type="button"
          onClick={handleGenerate}
          disabled={jobRunning || Boolean(blockedReason)}
          style={{
            flex: 1, padding: '9px', border: 'none', borderRadius: '7px',
            background: '#15803d', color: '#fff', fontSize: '13.5px', fontWeight: 600,
            cursor: jobRunning || blockedReason ? 'not-allowed' : 'pointer',
            opacity: jobRunning || blockedReason ? 0.55 : 1,
            display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px',
          }}
        >
          {jobRunning && <Loader2 size={15} style={{ animation: 'spin 1s linear infinite' }} />}
          {jobRunning ? 'Génération et validation…' : 'Générer la facture Factur-X'}        </button>
      </div>
      <style>{`@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }`}</style>

      {blockedReason && !jobRunning && (
        <p style={{ margin: '8px 0 0', fontSize: '12px', color: '#6b7280' }}>{blockedReason}</p>
      )}

      {error && (
        <div role="alert" style={{ marginTop: '10px', fontSize: '13px', color: '#b91c1c' }}>
          {error.text}
          {error.details.length > 0 && (
            <ul style={{ margin: '6px 0 0', paddingLeft: '18px', fontSize: '12.5px' }}>
              {error.details.map(d => <li key={d}>{d}</li>)}
            </ul>
          )}
        </div>
      )}
      {job && job.status !== 'DONE' && (
        <JobStatusPanel
          job={job}
          onJobChange={setJob}
          onDone={handleJobDone}
        />
      )}

      {latest && <GenerationResult generation={latest} onDownload={handleDownload} />}

      {previous.length > 0 && (
        <div style={{ marginTop: '14px' }}>
          <div style={{ fontSize: '11.5px', fontWeight: 600, color: '#6b7280', marginBottom: '6px' }}>
            Générations précédentes
          </div>
          <GenerationList generations={previous} onDownload={handleDownload} />
        </div>
      )}
    </div>
  );
}

export function GenerationList({ generations, onDownload }) {
  return (
    <ul style={{ listStyle: 'none', margin: 0, padding: 0 }}>
      {generations.map(g => {
        const style = STATUS[g.status] ?? STATUS.FAILED;
        return (
          <li key={g.id} style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '12px', padding: '5px 0', borderTop: '1px solid #f3f4f6' }}>
            <span style={{ color: style.color, fontWeight: 600, minWidth: '90px' }}>{style.label}</span>
            <span style={{ color: '#6b7280' }}>{g.profile} · {formatDate(g.createdAt)}</span>
            {g.generatedDocumentId && (
              <span style={{ marginLeft: 'auto', display: 'flex', gap: '10px' }}>
                <button type="button" onClick={() => onDownload(g)}
                  style={{ background: 'none', border: 'none', color: '#2563eb', cursor: 'pointer', fontSize: '12px', padding: 0 }}>
                  Télécharger
                </button>
                {g.validationRunId && (
                  <Link to={`/rapport?documentId=${g.generatedDocumentId}`} style={{ color: '#2563eb', fontSize: '12px' }}>Rapport</Link>
                )}
              </span>
            )}
          </li>
        );
      })}
    </ul>
  );
}
