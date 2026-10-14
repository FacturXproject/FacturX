import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api from '../services/api';

export default function MyData() {
	const { setUser } = useAuth();
	const navigate = useNavigate();

	const [deleting, setDeleting] = useState(false);
	const [error, setError] = useState('');
	const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);

	const handleExport = async () => {
	try {
		setError('');

		const response = await api.get('/gdpr/export');

		const blob = new Blob(
		[JSON.stringify(response.data, null, 2)],
		{ type: 'application/json' }
		);

		const url = URL.createObjectURL(blob);

		const link = document.createElement('a');
		link.href = url;
		link.download = 'my-data.json';
		link.click();

		URL.revokeObjectURL(url);
	} catch {
		setError('Impossible de télécharger vos données.');
	}
	};

	const handleDelete = async () => {
	try {
		setDeleting(true);
		setError('');

		await api.post('/gdpr/delete', {
		confirmation: 'DELETE'
		});

		setUser(null);
		navigate('/login');
	} catch (err) {
		const backendError = err.response?.data?.error;

		if (
		backendError ===
		'User is the last admin of an organization'
		) {
		setError(
			'Vous êtes le dernier administrateur d’une organisation. Vous devez d’abord nommer un autre administrateur avant de supprimer votre compte.'
		);
		} else {
		setError(
			'Impossible de supprimer votre compte.'
		);
		}
	} finally {
		setDeleting(false);
	}
	};

	const buttonStyle = {
	width: '100%',
	padding: '11px 16px',
	border: 'none',
	borderRadius: '8px',
	background: '#1a2744',
	color: '#fff',
	cursor: 'pointer',
	fontSize: '14px',
	fontWeight: 600,
	};

	return (
	<div
		style={{
		padding: '20px 26px',
		}}
	>
		<div
		style={{
			background: '#fff',
			borderRadius: '16px',
			padding: '32px',
			boxShadow: '0 4px 18px rgba(0, 0, 0, 0.06)',
		}}
		>
		<h1
			style={{
			marginTop: 0,
			marginBottom: '6px',
			fontSize: '26px',
			color: '#111827',
			}}
		>
			Mes données
		</h1>

		<p
			style={{
			color: '#6b7280',
			marginTop: 0,
			}}
		>
			Export et suppression de vos données personnelles
		</p>

		<hr
			style={{
			border: 0,
			borderTop: '1px solid #e5e7eb',
			margin: '24px 0',
			}}
		/>

		<div style={{ marginBottom: '24px' }}>
			<p
			style={{
				fontWeight: 600,
				marginBottom: '6px',
			}}
			>
			Exporter mes données
			</p>

			<p style={{ marginTop: 0 }}>
			Téléchargez les données personnelles associées à votre compte.
			</p>

			<button
			type="button"
			onClick={handleExport}
			style={buttonStyle}
			>
			Télécharger mes données
			</button>
		</div>

		<div>
			<p
			style={{
				fontWeight: 600,
				marginBottom: '6px',
			}}
			>
			Supprimer mon compte
			</p>

			<p style={{ marginTop: 0 }}>
			Cette action anonymisera définitivement vos données personnelles.
			</p>

			<button
			type="button"
			onClick={() => setShowDeleteConfirm(true)}
			disabled={deleting}
			style={{
				...buttonStyle,
				opacity: deleting ? 0.6 : 1,
				cursor: deleting ? 'not-allowed' : 'pointer',
			}}
			>
			Supprimer mon compte
			</button>

			{showDeleteConfirm && (
			<div
				style={{
				marginTop: '16px',
				padding: '16px',
				background: '#f9fafb',
				border: '1px solid #e5e7eb',
				borderRadius: '8px',
				}}
			>
				<p
				style={{
					marginTop: 0,
					marginBottom: '8px',
					fontWeight: 600,
					color: '#111827',
				}}
				>
				Êtes-vous sûr de vouloir supprimer votre compte ?
				</p>

				<p
				style={{
					marginTop: 0,
					marginBottom: '14px',
					color: '#6b7280',
					fontSize: '14px',
				}}
				>
				Cette action est irréversible et vos données personnelles seront anonymisées.
				</p>

				<div
				style={{
					display: 'flex',
					gap: '10px',
				}}
				>
				<button
					type="button"
					onClick={() => setShowDeleteConfirm(false)}
					disabled={deleting}
					style={{
					padding: '9px 16px',
					borderRadius: '7px',
					border: '1px solid #1a2744',
					background: '#fff',
					color: '#1a2744',
					cursor: deleting ? 'not-allowed' : 'pointer',
					fontWeight: 600,
					}}
				>
					Annuler
				</button>

				<button
					type="button"
					onClick={handleDelete}
					disabled={deleting}
					style={{
					padding: '9px 16px',
					borderRadius: '7px',
					border: 'none',
					background: '#1a2744',
					color: '#fff',
					cursor: deleting ? 'not-allowed' : 'pointer',
					fontWeight: 600,
					opacity: deleting ? 0.6 : 1,
					}}
				>
					{deleting
					? 'Suppression...'
					: 'Confirmer la suppression'}
				</button>
				</div>
			</div>
			)}
		</div>

		{error && (
			<div
			style={{
				marginTop: '18px',
				background: '#fee2e2',
				color: '#991b1b',
				padding: '12px 14px',
				borderRadius: '8px',
				fontSize: '14px',
			}}
			>
			{error}
			</div>
		)}
		</div>
	</div>
	);
}
