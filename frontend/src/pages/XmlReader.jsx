import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { FileCode2, Eye, Building2 } from 'lucide-react';

import api from '../services/api';
import DocumentUploadForm from '../components/DocumentUploadForm';

export default function XmlReader() {
	const navigate = useNavigate();

	const [organizations, setOrganizations] = useState([]);
	const [selectedOrganizationId, setSelectedOrganizationId] = useState('');
	const [documents, setDocuments] = useState([]);

	const [loadingOrganizations, setLoadingOrganizations] = useState(true);
	const [loadingDocuments, setLoadingDocuments] = useState(false);
	const [error, setError] = useState(null);

	useEffect(() => {
		async function loadOrganizations() {
			try {
				const response = await api.get('/organizations');

				const organizationsWithNames = await Promise.all(
					response.data.map(async (organization) => {
						const id = organization.organizationId ?? organization.id;

						try {
							const details = await api.get(`/organizations/${id}`);

							return {
								id,
								name: details.data.name ?? `Organisation #${id}`,
							};
						} catch {
							return {
								id,
								name: `Organisation #${id}`,
							};
						}
					})
				);

				setOrganizations(organizationsWithNames);
			} catch {
				setError('Impossible de charger les organisations.');
			} finally {
				setLoadingOrganizations(false);
			}
		}

		loadOrganizations();
	}, []);

	async function loadDocuments(organizationId) {
		if (!organizationId) {
			setDocuments([]);
			return;
		}

		setLoadingDocuments(true);
		setError(null);

		try {
			const response = await api.get(
				`/documents?organizationId=${organizationId}`
			);

			const xmlDocuments =
				response.data.content?.filter(
					(document) =>
						document.type === 'text/xml' ||
						document.type === 'application/xml'
				) ?? [];

			setDocuments(xmlDocuments);
		} catch {
			setError('Impossible de charger les fichiers XML.');
		} finally {
			setLoadingDocuments(false);
		}
	}

	useEffect(() => {
		loadDocuments(selectedOrganizationId);
	}, [selectedOrganizationId]);

	const handleUploaded = () => {
		loadDocuments(selectedOrganizationId);
	};

	if (loadingOrganizations) {
		return (
			<div style={{ padding: '32px 40px' }}>
				Chargement...
			</div>
		);
	}

	return (
		<div
			style={{
				padding: '32px 40px',
				maxWidth: '1200px',
				margin: '0 auto',
				background: '#f8f9fa',
				minHeight: '100vh',
			}}
		>
			<div style={{ marginBottom: '24px' }}>
				<h1
					style={{
						fontSize: '26px',
						fontWeight: 700,
						color: '#111827',
						margin: '0 0 4px',
					}}
				>
					Lecture XML
				</h1>

				<p
					style={{
						color: '#6b7280',
						fontSize: '14px',
						margin: 0,
					}}
				>
					Déposez une facture XML et consultez-la sous une forme lisible.
				</p>
			</div>

			{error && (
				<div
					style={{
						background: '#fee2e2',
						color: '#991b1b',
						borderRadius: '8px',
						padding: '12px 16px',
						marginBottom: '18px',
						fontSize: '13.5px',
					}}
				>
					{error}
				</div>
			)}

			<div
				style={{
					background: '#fff',
					border: '1px solid #e5e7eb',
					borderRadius: '10px',
					padding: '22px',
					marginBottom: '20px',
				}}
			>
				<div
					style={{
						display: 'flex',
						alignItems: 'center',
						gap: '10px',
						marginBottom: '16px',
					}}
				>
					<Building2 size={18} color="#2563eb" />

					<h2
						style={{
							fontSize: '16px',
							color: '#111827',
							margin: 0,
						}}
					>
						Organisation
					</h2>
				</div>

				<select
					value={selectedOrganizationId}
					onChange={(e) => setSelectedOrganizationId(e.target.value)}
					style={{
						width: '100%',
						maxWidth: '420px',
						padding: '9px 12px',
						border: '1px solid #d1d5db',
						borderRadius: '8px',
						fontSize: '14px',
						background: '#fff',
						color: '#374151',
					}}
				>
					<option value="">Sélectionnez une organisation</option>

					{organizations.map((organization) => (
						<option key={organization.id} value={organization.id}>
							{organization.name}
						</option>
					))}
				</select>
			</div>

			<div
				style={{
					background: '#fff',
					border: '1px solid #e5e7eb',
					borderRadius: '10px',
					padding: '22px',
					marginBottom: '20px',
				}}
			>
				<div style={{ marginBottom: '16px' }}>
					<h2
						style={{
							fontSize: '16px',
							color: '#111827',
							margin: '0 0 4px',
						}}
					>
						Déposer une facture XML
					</h2>

					<p
						style={{
							fontSize: '13px',
							color: '#6b7280',
							margin: 0,
						}}
					>
						Formats XML uniquement, jusqu’à 10 Mo.
					</p>
				</div>

				{!selectedOrganizationId && (
					<p
						style={{
							fontSize: '12.5px',
							color: '#9ca3af',
							margin: '0 0 12px',
						}}
					>
						Sélectionnez une organisation pour pouvoir déposer une facture XML.
					</p>
				)}

				<div
					style={{
						opacity: selectedOrganizationId ? 1 : 0.5,
						pointerEvents: selectedOrganizationId ? 'auto' : 'none',
					}}
				>
					<DocumentUploadForm
						organizationId={selectedOrganizationId}
						onUploaded={handleUploaded}
						xmlOnly
					/>
				</div>
			</div>

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
						padding: '16px 20px',
						borderBottom: '1px solid #e5e7eb',
						display: 'flex',
						alignItems: 'center',
						gap: '8px',
					}}
				>
					<FileCode2 size={18} color="#2563eb" />

					<h2
						style={{
							fontSize: '16px',
							color: '#111827',
							margin: 0,
						}}
					>
						Factures XML
					</h2>
				</div>

				{!selectedOrganizationId ? (
					<p
						style={{
							padding: '30px',
							color: '#9ca3af',
							textAlign: 'center',
							fontSize: '14px',
						}}
					>
						Sélectionnez une organisation pour afficher ses factures XML.
					</p>
				) : loadingDocuments ? (
					<p
						style={{
							padding: '24px 20px',
							color: '#6b7280',
							fontSize: '14px',
						}}
					>
						Chargement des fichiers XML...
					</p>
				) : documents.length === 0 ? (
					<p
						style={{
							padding: '30px',
							color: '#9ca3af',
							textAlign: 'center',
							fontSize: '14px',
						}}
					>
						Aucun fichier XML disponible pour cette organisation.
					</p>
				) : (
					<div style={{ overflowX: 'auto' }}>
						<table
							style={{
								width: '100%',
								borderCollapse: 'collapse',
								fontSize: '13.5px',
							}}
						>
							<thead>
								<tr
									style={{
										background: '#f9fafb',
										borderBottom: '1px solid #e5e7eb',
									}}
								>
									<th style={headerStyle}>Fichier</th>
									<th style={headerStyle}>Date de dépôt</th>
									<th style={headerStyle}>Statut</th>
									<th style={headerStyle}>Action</th>
								</tr>
							</thead>

							<tbody>
								{documents.map((document) => (
									<tr
										key={document.id}
										style={{
											borderBottom: '1px solid #f3f4f6',
										}}
									>
										<td style={cellStyle}>
											<div
												style={{
													display: 'flex',
													alignItems: 'center',
													gap: '8px',
												}}
											>
												<FileCode2 size={16} color="#2563eb" />

												<span
													style={{
														fontWeight: 500,
														color: '#111827',
													}}
												>
													{document.filename}
												</span>
											</div>
										</td>

										<td style={cellStyle}>
											{document.uploadedAt
												? new Date(document.uploadedAt).toLocaleString('fr-FR')
												: 'Non renseignée'}
										</td>

										<td style={cellStyle}>
											<span
												style={{
													background: '#dcfce7',
													color: '#15803d',
													padding: '3px 10px',
													borderRadius: '999px',
													fontSize: '12px',
													fontWeight: 500,
												}}
											>
												{document.status ?? 'Non renseigné'}
											</span>
										</td>

										<td style={cellStyle}>
											<button
												onClick={() =>
													navigate(`/documents/${document.id}/invoice`)
												}
												style={{
													display: 'flex',
													alignItems: 'center',
													gap: '6px',
													padding: '7px 13px',
													borderRadius: '7px',
													border: 'none',
													background: '#1a2744',
													color: '#fff',
													cursor: 'pointer',
													fontSize: '12.5px',
													fontWeight: 500,
												}}
											>
												<Eye size={14} />
												Lire la facture
											</button>
										</td>
									</tr>
								))}
							</tbody>
						</table>
					</div>
				)}
			</div>
		</div>
	);
}

const headerStyle = {
	textAlign: 'left',
	padding: '12px 20px',
	color: '#6b7280',
	fontWeight: 500,
};

const cellStyle = {
	padding: '14px 20px',
	color: '#374151',
};
