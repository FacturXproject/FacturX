import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { FileCheck } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { validateEmail, validatePassword, validateName } from '../utils/validation';

const inputStyle = {
  width: '100%',
  padding: '8px 10px',
  border: '1px solid #d1d5db',
  borderRadius: '6px',
  fontSize: '14px',
  color: '#1a1a2e',
  outline: 'none',
  boxSizing: 'border-box',
  background: '#fff',
};

const labelStyle = {
  display: 'block',
  fontSize: '12.5px',
  fontWeight: 500,
  color: '#374151',
  marginBottom: '5px',
};

const errorStyle = {
  color: '#dc2626',
  fontSize: '12px',
  marginTop: '4px',
};

const fields = [
  { name: 'firstName', label: 'Prénom', type: 'text', autoComplete: 'given-name' },
  { name: 'lastName', label: 'Nom', type: 'text', autoComplete: 'family-name' },
  { name: 'email', label: 'Adresse email', type: 'email', autoComplete: 'email', placeholder: 'prenom.nom@entreprise.fr' },
  { name: 'password', label: 'Mot de passe', type: 'password', autoComplete: 'new-password', placeholder: '••••••••' },
];

function validateField(name, value) {
  switch (name) {
    case 'firstName':
      return validateName(value, 'Le prénom');
    case 'lastName':
      return validateName(value, 'Le nom');
    case 'email':
      return validateEmail(value);
    case 'password':
      return validatePassword(value);
    default:
      return null;
  }
}

export default function Register() {
  const [values, setValues] = useState({ firstName: '', lastName: '', email: '', password: '' });
  const [fieldErrors, setFieldErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const navigate = useNavigate();
  const { register } = useAuth();

  const handleChange = (name, value) => {
    setValues((prev) => ({ ...prev, [name]: value }));
  };

  const handleBlur = (name) => {
    const error = validateField(name, values[name]);
    setFieldErrors((prev) => ({ ...prev, [name]: error ?? undefined }));
  };

  const validateAll = () => {
    const errors = {};
    for (const { name } of fields) {
      const error = validateField(name, values[name]);
      if (error) errors[name] = error;
    }
    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setFormError('');
    if (!validateAll()) return;

    setSubmitting(true);
    try {
      await register(values);
      navigate('/');
    } catch (err) {
      const status = err.response?.status;
      const serverFields = err.response?.data?.fields;
      if (status === 400 && serverFields) {
        setFieldErrors(serverFields);
      } else if (status === 409) {
        setFormError('Cette adresse email est déjà utilisée.');
      } else {
        setFormError('Une erreur est survenue. Réessayez.');
      }
    } finally {
      setSubmitting(false);
    }
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

        <div style={{
          background: '#fff',
          border: '1px solid #e5e7eb',
          borderRadius: '10px',
          padding: '28px',
        }}>
          <h2 style={{ margin: '0 0 20px', fontSize: '16px', fontWeight: 600, color: '#1a1a2e' }}>
            Créer un compte
          </h2>

          <form onSubmit={handleSubmit} noValidate>
            {fields.map(({ name, label, type, autoComplete, placeholder }) => (
              <div key={name} style={{ marginBottom: name === 'password' ? '20px' : '14px' }}>
                <label htmlFor={name} style={labelStyle}>{label}</label>
                <input
                  id={name}
                  name={name}
                  type={type}
                  autoComplete={autoComplete}
                  value={values[name]}
                  onChange={(e) => handleChange(name, e.target.value)}
                  onBlur={() => handleBlur(name)}
                  placeholder={placeholder}
                  style={inputStyle}
                  aria-invalid={Boolean(fieldErrors[name])}
                  aria-describedby={fieldErrors[name] ? `${name}-error` : undefined}
                />
                {fieldErrors[name] && <p id={`${name}-error`} style={errorStyle}>{fieldErrors[name]}</p>}
              </div>
            ))}

            {formError && (
              <p role="alert" style={{ ...errorStyle, marginBottom: '14px', textAlign: 'center' }}>{formError}</p>
            )}

            <button
              type="submit"
              disabled={submitting}
              style={{
                width: '100%',
                padding: '9px',
                background: '#1a2744',
                color: '#fff',
                border: 'none',
                borderRadius: '6px',
                fontSize: '14px',
                fontWeight: 500,
                cursor: submitting ? 'default' : 'pointer',
                opacity: submitting ? 0.7 : 1,
              }}
            >
              Créer mon compte
            </button>
          </form>

          <div style={{ marginTop: '16px', textAlign: 'center' }}>
            <Link
              to="/login"
              style={{ background: 'none', border: 'none', color: '#4a9eff', fontSize: '13px', cursor: 'pointer', textDecoration: 'underline' }}
            >
              Déjà un compte ? Se connecter
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
