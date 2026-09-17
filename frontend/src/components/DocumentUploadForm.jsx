import { useState, useRef } from 'react';
import { Upload, X, CheckCircle2, AlertCircle } from 'lucide-react';
import api from '../services/api';

const MAX_SIZE = 10 * 1024 * 1024; // 10 Mo
const ACCEPTED_TYPES = ['application/pdf', 'application/xml', 'text/xml'];

function validateFile(file) {
  if (file.size > MAX_SIZE) {
    return `${file.name} : dépasse la taille maximale (10 Mo).`;
  }
  if (!ACCEPTED_TYPES.includes(file.type) && !file.name.match(/\.(pdf|xml)$/i)) {
    return `${file.name} : seuls les fichiers PDF et XML sont acceptés.`;
  }
  return null;
}

export default function DocumentUploadForm({ organizationId, onUploaded }) {
  const [pendingFiles, setPendingFiles] = useState([]);
  const [dragOver, setDragOver] = useState(false);
  const fileInputRef = useRef(null);

  const addFiles = (fileList) => {
    const files = Array.from(fileList).map((file) => {
      const clientError = validateFile(file);
      return {
        id: `${file.name}-${file.size}-${Date.now()}`,
        file,
        progress: 0,
        status: clientError ? 'error' : 'pending',
        error: clientError,
      };
    });
    setPendingFiles((prev) => [...prev, ...files]);
  };

  const handleInputChange = (e) => {
    if (e.target.files.length > 0) addFiles(e.target.files);
    e.target.value = '';
  };

  const handleDrop = (e) => {
    e.preventDefault();
    setDragOver(false);
    if (e.dataTransfer.files.length > 0) addFiles(e.dataTransfer.files);
  };

  const removeFile = (id) => {
    setPendingFiles((prev) => prev.filter((f) => f.id !== id));
  };

  const uploadOne = async (entry) => {
    setPendingFiles((prev) =>
      prev.map((f) => (f.id === entry.id ? { ...f, status: 'uploading' } : f))
    );

    const formData = new FormData();
    formData.append('file', entry.file);

    try {
      const response = await api.post(
        `/documents?organizationId=${organizationId}`,
        formData,
        {
          headers: { 'Content-Type': 'multipart/form-data' },
          onUploadProgress: (event) => {
            const progress = Math.round((event.loaded * 100) / event.total);
            setPendingFiles((prev) =>
              prev.map((f) => (f.id === entry.id ? { ...f, progress } : f))
            );
          },
        }
      );

      setPendingFiles((prev) =>
        prev.map((f) => (f.id === entry.id ? { ...f, status: 'done', progress: 100 } : f))
      );

      onUploaded?.(response.data);
    } catch (err) {
      setPendingFiles((prev) =>
        prev.map((f) =>
          f.id === entry.id
            ? { ...f, status: 'error', error: err.response?.data?.message ?? err.message }
            : f
        )
      );
    }
  };

  const handleUploadAll = () => {
    pendingFiles
      .filter((f) => f.status === 'pending')
      .forEach((entry) => uploadOne(entry));
  };

  const hasUploadable = pendingFiles.some((f) => f.status === 'pending');

  return (
    <div style={{ maxWidth: '600px' }}>
      {/* Zone de drop */}
      <div
        onClick={() => fileInputRef.current.click()}
        onDragOver={(e) => { e.preventDefault(); setDragOver(true); }}
        onDragLeave={() => setDragOver(false)}
        onDrop={handleDrop}
        style={{
          background: dragOver ? '#eff6ff' : '#fff',
          border: `2px dashed ${dragOver ? '#2563eb' : '#d1d5db'}`,
          borderRadius: '12px',
          padding: '32px',
          textAlign: 'center',
          cursor: 'pointer',
          transition: 'all 0.15s',
        }}
      >
        <Upload size={28} color={dragOver ? '#2563eb' : '#9ca3af'} style={{ marginBottom: '8px' }} />
        <p style={{ fontSize: '14px', color: '#374151', margin: 0, fontWeight: 500 }}>
          Glissez vos fichiers ici ou cliquez pour choisir
        </p>
        <p style={{ fontSize: '12.5px', color: '#9ca3af', marginTop: '4px' }}>
          PDF ou XML, 10 Mo max par fichier
        </p>
        <input
          ref={fileInputRef}
          type="file"
          accept=".pdf,.xml,application/pdf,application/xml,text/xml"
          multiple
          onChange={handleInputChange}
          style={{ display: 'none' }}
        />
      </div>

      {/* Liste des fichiers en attente / en cours / termines */}
      {pendingFiles.length > 0 && (
        <div style={{ marginTop: '16px', display: 'flex', flexDirection: 'column', gap: '8px' }}>
          {pendingFiles.map((entry) => (
            <div
              key={entry.id}
              style={{
                background: '#fff', border: '1px solid #e5e7eb', borderRadius: '8px',
                padding: '10px 14px',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '10px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', minWidth: 0 }}>
                  {entry.status === 'done' && <CheckCircle2 size={16} color="#16a34a" />}
                  {entry.status === 'error' && <AlertCircle size={16} color="#dc2626" />}
                  <span style={{
                    fontSize: '13px', color: '#374151', overflow: 'hidden',
                    textOverflow: 'ellipsis', whiteSpace: 'nowrap',
                  }}>
                    {entry.file.name}
                  </span>
                </div>

                {entry.status === 'pending' || entry.status === 'error' ? (
                  <button
                    onClick={() => removeFile(entry.id)}
                    style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#9ca3af', flexShrink: 0 }}
                  >
                    <X size={15} />
                  </button>
                ) : null}
              </div>

              {entry.status === 'uploading' && (
                <div style={{ marginTop: '8px', background: '#f3f4f6', borderRadius: '4px', height: '5px', overflow: 'hidden' }}>
                  <div style={{
                    width: `${entry.progress}%`, height: '100%', background: '#2563eb',
                    transition: 'width 0.2s',
                  }} />
                </div>
              )}

              {entry.status === 'error' && entry.error && (
                <p style={{ fontSize: '12px', color: '#dc2626', margin: '6px 0 0' }}>
                  {entry.error}
                </p>
              )}
            </div>
          ))}

          {hasUploadable && (
            <button
              onClick={handleUploadAll}
              style={{
                marginTop: '4px', padding: '9px 18px', borderRadius: '8px', border: 'none',
                background: '#1a2744', color: '#fff', fontSize: '13.5px', fontWeight: 500,
                cursor: 'pointer', alignSelf: 'flex-start',
              }}
            >
              Déposer {pendingFiles.filter((f) => f.status === 'pending').length} fichier(s)
            </button>
          )}
        </div>
      )}
    </div>
  );
}