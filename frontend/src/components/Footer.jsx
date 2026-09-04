import { Link } from 'react-router-dom';

export default function Footer() {
  return (
    <footer
      style={{
        padding: '16px 24px',
        borderTop: '1px solid #e5e7eb',
        background: '#fff',
        display: 'flex',
        justifyContent: 'center',
        gap: '24px',
        fontSize: '13px',
      }}
    >
      <Link to="/privacy">
        Politique de confidentialité
      </Link>

      <Link to="/terms">
        Conditions d'utilisation
      </Link>
    </footer>
  );
}