import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { FileCheck } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { validateEmail, validatePassword, validateName } from '../utils/validation';

const fields = [
  { name: 'firstName', label: 'Prénom', type: 'text', autoComplete: 'given-name' },
  { name: 'lastName', label: 'Nom', type: 'text', autoComplete: 'family-name' },
  {
    name: 'email',
    label: 'Adresse email',
    type: 'email',
    autoComplete: 'email',
    placeholder: 'prenom.nom@entreprise.fr',
  },
  {
    name: 'password',
    label: 'Mot de passe',
    type: 'password',
    autoComplete: 'new-password',
    placeholder: '••••••••',
  },
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
  const [values, setValues] = useState({
    firstName: '',
    lastName: '',
    email: '',
    password: '',
  });
  const [fieldErrors, setFieldErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const navigate = useNavigate();
  const { register } = useAuth();

  const handleChange = (name, value) => {
    setValues((prev) => ({
      ...prev,
      [name]: value,
    }));
  };

  const handleBlur = (name) => {
    const error = validateField(name, values[name]);

    setFieldErrors((prev) => ({
      ...prev,
      [name]: error ?? undefined,
    }));
  };

  const validateAll = () => {
    const errors = {};

    for (const { name } of fields) {
      const error = validateField(name, values[name]);

      if (error) {
        errors[name] = error;
      }
    }

    setFieldErrors(errors);

    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    setFormError('');

    if (!validateAll()) {
      return;
    }

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
    <div className="min-h-screen bg-[#f8f9fa] flex items-center justify-center p-6">
      <div className="w-full max-w-[380px]">

        <div className="text-center mb-8">
          <div className="flex items-center justify-center gap-2.5 mb-2">
            <div className="bg-[#1a2744] rounded-[10px] p-2.5 inline-flex">
              <FileCheck size={24} color="#4a9eff" />
            </div>
          </div>

          <h1 className="mt-3 mb-1 text-[22px] font-bold text-[#1a1a2e] tracking-[-0.4px]">
            Factur-X Validator
          </h1>

          <p className="text-[#6b7280] text-[13px]">
            Conformité & conversion e-factures
          </p>
        </div>

        <div className="bg-white border border-[#e5e7eb] rounded-[10px] p-7">
          <h2 className="mb-5 text-base font-semibold text-[#1a1a2e]">
            Créer un compte
          </h2>

          <form onSubmit={handleSubmit} noValidate>
            {fields.map(({ name, label, type, autoComplete, placeholder }) => (
              <div
                key={name}
                className={name === 'password' ? 'mb-5' : 'mb-3.5'}
              >
                <label
                  htmlFor={name}
                  className="block text-[12.5px] font-medium text-[#374151] mb-[5px]"
                >
                  {label}
                </label>

                <input
                  id={name}
                  name={name}
                  type={type}
                  autoComplete={autoComplete}
                  value={values[name]}
                  onChange={(e) => handleChange(name, e.target.value)}
                  onBlur={() => handleBlur(name)}
                  placeholder={placeholder}
                  className="w-full px-2.5 py-2 border border-[#d1d5db] rounded-md text-sm text-[#1a1a2e] outline-none bg-white box-border"
                  aria-invalid={Boolean(fieldErrors[name])}
                  aria-describedby={
                    fieldErrors[name] ? `${name}-error` : undefined
                  }
                />

                {fieldErrors[name] && (
                  <p
                    id={`${name}-error`}
                    className="text-[#dc2626] text-xs mt-1"
                  >
                    {fieldErrors[name]}
                  </p>
                )}
              </div>
            ))}

            {formError && (
              <p
                role="alert"
                className="text-[#dc2626] text-xs mb-3.5 text-center"
              >
                {formError}
              </p>
            )}

            <button
              type="submit"
              disabled={submitting}
              className="w-full py-[9px] bg-[#1a2744] text-white border-0 rounded-md text-sm font-medium cursor-pointer disabled:cursor-default disabled:opacity-70"
            >
              Créer mon compte
            </button>
          </form>

          <div className="mt-4 text-center">
            <Link
              to="/login"
              className="text-[#4a9eff] text-[13px] underline"
            >
              Déjà un compte ? Se connecter
            </Link>
          </div>
        </div>

      </div>
    </div>
  );
}
