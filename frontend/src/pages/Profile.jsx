import { useAuth } from '../context/AuthContext';

export default function Profile() {
  const { user } = useAuth();

  return (
    <div style={{ padding: '32px' }}>
      <h1>Mon profil</h1>

      <div style={{ marginTop: '24px' }}>
        <div>
          <strong>Email</strong>
          <p>{user?.email ?? '—'}</p>
        </div>

        <div>
          <strong>Prénom</strong>
          <p>{user?.firstName ?? '—'}</p>
        </div>

        <div>
          <strong>Nom</strong>
          <p>{user?.lastName ?? '—'}</p>
        </div>

        <div>
          <strong>Organisation active</strong>
          <p>—</p>
        </div>
      </div>
    </div>
  );
}
//The ?. is called optional chaining. it means "If user exists, give me email. Otherwise give me undefined."
