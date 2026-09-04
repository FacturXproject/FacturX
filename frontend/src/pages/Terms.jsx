import { Link } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';

export default function Terms() {
  return (
    <div style={{ padding: '32px', maxWidth: '900px' }}>
      <h1>Conditions d'utilisation</h1>

      <p>
        Les présentes conditions définissent les règles générales
        d'utilisation de l'application Factur-X.
      </p>

      <h2>Utilisation du service</h2>

      <p>
        L'utilisateur s'engage à utiliser l'application conformément
        à sa destination et aux lois applicables.
      </p>

      <h2>Compte utilisateur</h2>

      <p>
        L'utilisateur est responsable de la confidentialité de ses
        informations d'authentification et de l'utilisation de son compte.
      </p>

      <h2>Disponibilité du service</h2>

      <p>
        L'équipe du projet s'efforce de maintenir le service disponible
        mais ne garantit pas une disponibilité permanente.
      </p>
	  <Link
	    to="/dashboard"
		style={{
		  display: 'inline-flex',
		  alignItems: 'center',
		  justifyContent: 'center',
		  width: '40px',
		  height: '40px',
		  borderRadius: '50%',
		  border: '1px solid #ddd',
		  textDecoration: 'none',
		  color: '#333',
		  marginTop: '32px',
		}}
  		aria-label="Retour au tableau de bord"
	  >
		<ArrowLeft size={20} />
	  </Link>
    </div>
  );
}