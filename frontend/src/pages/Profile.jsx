
import { useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import api from '../services/api';
import './Profile.css';

export default function Profile() {
  const { user, setUser } = useAuth();

  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [organization, setOrganization] = useState(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
  const loadProfileData = async () => {
		if (user) {
		setFirstName(user.firstName ?? '');
		setLastName(user.lastName ?? '');

		try {
			const response = await api.get('/organizations');

			if (response.data && response.data.length > 0) {
			const firstOrganization = response.data[0];
			const organizationId =
				firstOrganization.organizationId ?? firstOrganization.id;

			const organizationResponse = await api.get(
				`/organizations/${organizationId}`
			);

			setOrganization(organizationResponse.data);
			}
		} catch (error) {
			console.error('Erreur lors du chargement de l’organisation :', error);
		}
		}
	};

	loadProfileData();
	}, [user]);

  const handleSave = async () => {
	try {
		setSaving(true);

		const response = await api.put('/me', {
		firstName: firstName,
		lastName: lastName,
		});
		setUser(response.data);
		
		console.log('Profil mis à jour :', response.data);
		alert('Profil sauvegardé !');

	} catch (error) {
		console.error('Erreur lors de la sauvegarde :', error);
		alert('Erreur lors de la sauvegarde du profil.');

	} finally {
		setSaving(false);
	}
	};

  return (
	<div className="profile-page">
		<div className="profile-card">
			<div className="profile-header">
				<div className="profile-avatar">
					{user?.firstName?.charAt(0).toUpperCase() ?? '?'}
					{user?.lastName?.charAt(0).toUpperCase() ?? '?'}
				</div>

				<div>
					<h1>Mon profil</h1>
					<p>Informations personnelles</p>
				</div>
			</div>

			<div className="profile-fields">
				<div className="profile-field">
					<label>Email</label>
					<div className="profile-input disabled">
					<p>{user?.email ?? '—'}</p>
					</div>
				</div>

				<div className="profile-field">
					<label>Prénom</label>
					<div className="profile-input">
					<input
						type="text"
						value={firstName}
						onChange={(e) => setFirstName(e.target.value)}
					/>
					</div>
				</div>

				<div className="profile-field">
					<label>Nom</label>
					<div className="profile-input">
					<input
						type="text"
						value={lastName}
						onChange={(e) => setLastName(e.target.value)}
					/>
					</div>
				</div>

				<div className="profile-field">
					<label>Organisation active</label>
					<div className="profile-input disabled">
					<p>{organization?.name ?? '—'}</p>
					</div>
				</div>

			</div>
			<div className="profile-actions">
				<button
					type="button"
					onClick={handleSave}
					disabled={saving}
					className="save-button"
				>
					{saving ? 'Enregistrement...' : '✓ Sauvegarder'}
				</button>
			</div>
		</div>
	</div>
  );
}


