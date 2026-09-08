
import { useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import api from '../services/api';

export default function Profile() {
  const { user } = useAuth();

  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');

  useEffect(() => {
    if (user) {
      setFirstName(user.firstName ?? '');
      setLastName(user.lastName ?? '');
    }
  }, [user]);

  const handleSave = async () => {
    try {
      const response = await api.put('/me', {
        firstName: firstName,
        lastName: lastName,
      });

      console.log('Profil mis à jour :', response.data);
      alert('Profil sauvegardé !');

    } catch (error) {
      console.error('Erreur lors de la sauvegarde :', error);
      alert('Erreur lors de la sauvegarde du profil.');
    }
  };

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
          <input
            type="text"
            value={firstName}
            onChange={(e) => setFirstName(e.target.value)}
          />
        </div>

        <div>
          <strong>Nom</strong>
          <input
            type="text"
            value={lastName}
            onChange={(e) => setLastName(e.target.value)}
          />
        </div>

        <div>
          <strong>Organisation active</strong>
          <p>—</p>
        </div>

        <button onClick={handleSave}>
          Sauvegarder
        </button>
      </div>
    </div>
  );
}


