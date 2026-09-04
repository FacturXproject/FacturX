import { Link } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';

export default function PrivacyPolicy() {
  return (
    <div style={{ padding: '32px', maxWidth: '900px' }}>
      <h1>Politique de confidentialité</h1>

      <p>
        Cette page présente les principes de traitement des données
        personnelles utilisés par l'application Factur-X.
      </p>

      <h2>Données collectées</h2>

      <p>
        L'application peut traiter les informations nécessaires à la
        création et à la gestion du compte utilisateur, notamment
        l'adresse e-mail, le prénom et le nom.
      </p>

      <h2>Utilisation des données</h2>

      <p>
        Ces données sont utilisées pour permettre l'authentification,
        la gestion du profil et l'utilisation des fonctionnalités
        proposées par l'application.
      </p>

      <h2>Sécurité</h2>

      <p>
        Des mesures techniques et organisationnelles sont mises en place
        afin de protéger les données contre les accès non autorisés.
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