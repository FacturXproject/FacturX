import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api from '../services/api';

export default function Profile() {
	const { user, setUser } = useAuth();

	const [firstName, setFirstName] = useState('');
	const [lastName, setLastName] = useState('');
	const [organizations, setOrganizations] = useState([]);
	const [saving, setSaving] = useState(false);

	useEffect(() => {
	const loadProfileData = async () => {
		if (!user) {
		return;
		}

		setFirstName(user.firstName ?? '');
		setLastName(user.lastName ?? '');

		try {
		const response = await api.get('/organizations');

		const organizationsWithNames = await Promise.all(
			response.data.map(async (organization) => {
			const organizationId =
				organization.organizationId ?? organization.id;

			const organizationResponse = await api.get(
				`/organizations/${organizationId}`
			);

			return {
				id: organizationId,
				name: organizationResponse.data.name,
			};
			})
		);

		setOrganizations(organizationsWithNames);
		} catch (error) {
		console.error(
			'Erreur lors du chargement des organisations :',
			error
		);
		}
	};

	loadProfileData();
	}, [user]);

	const handleSave = async () => {
	try {
		setSaving(true);

		const response = await api.put('/me', {
		firstName,
		lastName,
		});

		setUser(response.data);

		alert('Profil sauvegardé !');
	} catch (error) {
		console.error(
		'Erreur lors de la sauvegarde du profil :',
		error
		);

		alert('Erreur lors de la sauvegarde du profil.');
	} finally {
		setSaving(false);
	}
	};

	const inputStyle = {
	width: '100%',
	minHeight: '46px',
	padding: '0 14px',
	border: '1px solid #d1d5db',
	borderRadius: '8px',
	background: '#fff',
	color: '#111827',
	fontSize: '14px',
	boxSizing: 'border-box',
	outline: 'none',
	};

	const readOnlyStyle = {
	...inputStyle,
	display: 'flex',
	alignItems: 'center',
	background: '#f9fafb',
	color: '#6b7280',
	};

	const labelStyle = {
	display: 'block',
	marginBottom: '7px',
	fontSize: '13.5px',
	fontWeight: 600,
	color: '#374151',
	};

	const primaryButtonStyle = {
	padding: '10px 18px',
	borderRadius: '7px',
	border: '1px solid #1a2744',
	background: '#1a2744',
	color: '#fff',
	textDecoration: 'none',
	cursor: 'pointer',
	fontSize: '13.5px',
	fontWeight: 600,
	};

	const secondaryButtonStyle = {
	padding: '10px 18px',
	borderRadius: '7px',
	border: '1px solid #1a2744',
	background: '#fff',
	color: '#1a2744',
	textDecoration: 'none',
	cursor: 'pointer',
	fontSize: '13.5px',
	fontWeight: 600,
	};

	return (
	<div
		style={{
		padding: '32px 40px',
		maxWidth: '1100px',
		margin: '0 auto',
		minHeight: '100vh',
		background: '#f8f9fa',
		}}
	>
		<div
		style={{
			background: '#fff',
			border: '1px solid #e5e7eb',
			borderRadius: '10px',
			overflow: 'hidden',
		}}
		>
		<div
			style={{
			padding: '24px',
			borderBottom: '1px solid #e5e7eb',
			}}
		>
			<div
			style={{
				display: 'flex',
				alignItems: 'center',
				gap: '16px',
			}}
			>
			<div
				style={{
				width: '58px',
				height: '58px',
				borderRadius: '50%',
				background: '#eff6ff',
				color: '#2563eb',
				display: 'flex',
				alignItems: 'center',
				justifyContent: 'center',
				fontSize: '20px',
				fontWeight: 700,
				}}
			>
				{user?.firstName?.charAt(0).toUpperCase() ?? '?'}
				{user?.lastName?.charAt(0).toUpperCase() ?? '?'}
			</div>

			<div>
				<h1
				style={{
					margin: 0,
					fontSize: '24px',
					color: '#111827',
				}}
				>
				Mon profil
				</h1>

				<p
				style={{
					margin: '5px 0 0',
					color: '#6b7280',
					fontSize: '14px',
				}}
				>
				Informations personnelles
				</p>
			</div>
			</div>
		</div>

		<div
			style={{
			padding: '24px',
			display: 'grid',
			gap: '20px',
			}}
		>
			<div>
			<label style={labelStyle}>
				Email
			</label>

			<div style={readOnlyStyle}>
				{user?.email ?? '—'}
			</div>
			</div>

			<div
			style={{
				display: 'grid',
				gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))',
				gap: '16px',
			}}
			>
			<div>
				<label style={labelStyle}>
				Prénom
				</label>

				<input
				type="text"
				value={firstName}
				onChange={(e) => setFirstName(e.target.value)}
				style={inputStyle}
				/>
			</div>

			<div>
				<label style={labelStyle}>
				Nom
				</label>

				<input
				type="text"
				value={lastName}
				onChange={(e) => setLastName(e.target.value)}
				style={inputStyle}
				/>
			</div>
			</div>

			<div>
			<label style={labelStyle}>
				Organisations
			</label>

			<div
				style={{
				...readOnlyStyle,
				minHeight: '46px',
				height: 'auto',
				paddingTop: '10px',
				paddingBottom: '10px',
				flexDirection: 'column',
				alignItems: 'flex-start',
				gap: '6px',
				}}
			>
				{organizations.length > 0 ? (
				organizations.map((organization) => (
					<span key={organization.id}>
					{organization.name}
					</span>
				))
				) : (
				<span>—</span>
				)}
			</div>
			</div>
		</div>

		<div
			style={{
			display: 'flex',
			justifyContent: 'flex-end',
			gap: '10px',
			padding: '18px 24px',
			borderTop: '1px solid #e5e7eb',
			background: '#f9fafb',
			}}
		>
			<Link
			to="/my-data"
			style={secondaryButtonStyle}
			>
			Mes données
			</Link>

			<button
			type="button"
			onClick={handleSave}
			disabled={saving}
			style={{
				...primaryButtonStyle,
				opacity: saving ? 0.6 : 1,
				cursor: saving ? 'not-allowed' : 'pointer',
			}}
			>
			{saving
				? 'Enregistrement...'
				: 'Sauvegarder'}
			</button>
		</div>
		</div>
	</div>
	);
}
