// Meme regle que le backend (DocumentService.deleteDocument) : l'admin supprime tout
// document ; comptable et client, leurs propres documents sauf pendant un traitement.
export function canDeleteDocument(document, role, userId) {
  if (role === 'ADMIN') return true;
  const inProgress = document.status === 'QUEUED' || document.status === 'PROCESSING';
  return String(document.ownerId) === String(userId) && !inProgress;
}
