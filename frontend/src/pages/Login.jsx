import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { FileCheck } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { validateEmail } from '../utils/validation';

export default function Login() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const navigate = useNavigate();
  const { login } = useAuth();

  const validate = () => {
    const errors = {};

    const emailError = validateEmail(email);

    if (emailError) {
      errors.email = emailError;
    }

    if (!password) {
      errors.password = 'Le mot de passe est obligatoire.';
    }

    setFieldErrors(errors);

    return Object.keys(errors).length === 0;
  };

  const handleBlur = (field) => {
    if (field === 'email') {
      const error = validateEmail(email);

      setFieldErrors((prev) => ({
        ...prev,
        email: error ?? undefined,
      }));
    }

    if (field === 'password' && !password) {
      setFieldErrors((prev) => ({
        ...prev,
        password: 'Le mot de passe est obligatoire.',
      }));
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    setFormError('');

    if (!validate()) {
      return;
    }

    setSubmitting(true);

    try {
      await login(email, password);
      navigate('/');
    } catch (err) {
      const status = err.response?.status;

      if (status === 401) {
        setFormError('Identifiants invalides.');
      } else if (status === 429) {
        setFormError('Trop de tentatives. Réessayez dans 15 minutes.');
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
            Connexion
          </h2>

          <form onSubmit={handleSubmit} noValidate>

            <div className="mb-3.5">
              <label
                htmlFor="email"
                className="block text-[12.5px] font-medium text-[#374151] mb-[5px]"
              >
                Adresse email
              </label>

              <input
                id="email"
                name="email"
                type="email"
                autoComplete="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                onBlur={() => handleBlur('email')}
                placeholder="prenom.nom@entreprise.fr"
                className="w-full px-2.5 py-2 border border-[#d1d5db] rounded-md text-sm text-[#1a1a2e] outline-none bg-white box-border"
                aria-invalid={Boolean(fieldErrors.email)}
                aria-describedby={
                  fieldErrors.email ? 'email-error' : undefined
                }
              />

              {fieldErrors.email && (
                <p
                  id="email-error"
                  className="text-[#dc2626] text-xs mt-1"
                >
                  {fieldErrors.email}
                </p>
              )}
            </div>

            <div className="mb-5">
              <label
                htmlFor="password"
                className="block text-[12.5px] font-medium text-[#374151] mb-[5px]"
              >
                Mot de passe
              </label>

              <input
                id="password"
                name="password"
                type="password"
                autoComplete="current-password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                onBlur={() => handleBlur('password')}
                placeholder="••••••••"
                className="w-full px-2.5 py-2 border border-[#d1d5db] rounded-md text-sm text-[#1a1a2e] outline-none bg-white box-border"
                aria-invalid={Boolean(fieldErrors.password)}
                aria-describedby={
                  fieldErrors.password ? 'password-error' : undefined
                }
              />

              {fieldErrors.password && (
                <p
                  id="password-error"
                  className="text-[#dc2626] text-xs mt-1"
                >
                  {fieldErrors.password}
                </p>
              )}
            </div>

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
              Se connecter
            </button>
          </form>

          <div className="mt-4 text-center">
            <Link
              to="/register"
              className="text-[#4a9eff] text-[13px] underline"
            >
              Créer un compte
            </Link>
          </div>
        </div>

        <div className="mt-5 px-4 py-3.5 bg-white border border-[#e5e7eb] rounded-lg">
          <p className="text-[#6b7280] text-[11.5px] leading-[1.6] text-center m-0">
            Service de vérification de conformité et de conversion de factures
            électroniques au format{' '}
            <strong className="text-[#4b5563]">
              Factur-X
            </strong>
            , conformément à la réforme française d&apos;e-invoicing obligatoire
            à partir du{' '}
            <strong className="text-[#4b5563]">
              1er septembre 2026
            </strong>
            .
          </p>
        </div>

      </div>
    </div>
  );
}
