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
