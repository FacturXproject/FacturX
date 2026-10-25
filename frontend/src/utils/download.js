import api from '../services/api';

// Télécharge un document de l'organisation (GET /documents/{id}/download, session
// et cookies inclus) et le propose à l'enregistrement sous le nom donné.
export async function downloadDocument(documentId, filename) {
  const response = await api.get(`/documents/${documentId}/download`, { responseType: 'blob' });
  const url = URL.createObjectURL(new Blob([response.data], { type: 'application/pdf' }));
  const link = document.createElement('a');
  link.href = url;
  link.download = filename || 'facture-facturx.pdf';
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

// F20 : télécharge un export de l'organisation (CSV, JSON ou PDF). Le nom du fichier
// vient de l'en-tête Content-Disposition envoyé par le backend.
export async function downloadExport(organizationId, what, format) {
  const response = await api.get(`/organizations/${organizationId}/export/${what}`, {
    params: { format },
    responseType: 'blob',
  });
  const disposition = response.headers?.['content-disposition'] ?? '';
  const filename = /filename="([^"]+)"/.exec(disposition)?.[1] ?? `${what}.${format}`;
  saveBlob(response.data, filename);
}

export function saveBlob(blob, filename) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}
