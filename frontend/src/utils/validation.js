const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function validateEmail(value) {
  if (!value.trim()) return 'L\'adresse email est obligatoire.';
  if (value.length > 255 || !EMAIL_RE.test(value)) return 'Adresse email invalide.';
  return null;
}

export function validatePassword(value) {
  if (!value) return 'Le mot de passe est obligatoire.';
  if (value.length < 10 || value.length > 72) {
    return 'Le mot de passe doit contenir au moins 10 caractères.';
  }
  return null;
}

export function validateName(value, label) {
  if (!value.trim()) return `${label} est obligatoire.`;
  if (value.length > 50) return `${label} ne doit pas dépasser 50 caractères.`;
  return null;
}
