import { useEffect, useState } from 'react';
import { Loader2 } from 'lucide-react';
import api from '../services/api';

const STATUS = {
QUEUED: {
	label: 'En attente',
	color: '#92400e',
	background: '#fffbeb',
	border: '#fde68a',
},
PROCESSING: {
	label: 'Traitement en cours',
	color: '#1d4ed8',
	background: '#eff6ff',
	border: '#bfdbfe',
},
DONE: {
	label: 'Terminé',
	color: '#15803d',
	background: '#f0fdf4',
	border: '#bbf7d0',
},
FAILED: {
	label: 'Échec',
	color: '#b91c1c',
	background: '#fff5f5',
	border: '#fecaca',
},
};

export default function JobStatusPanel({
job,
onJobChange,
onDone,
}) {
const [pollError, setPollError] = useState(null);
const [retrying, setRetrying] = useState(false);

useEffect(() => {
	if (!job) {
	return;
	}

	if (job.status === 'DONE') {
	onDone?.(job);
	return;
	}

	if (job.status === 'FAILED') {
	return;
	}

	let cancelled = false;
	let timeoutId;

	const poll = async () => {
	try {
		const response = await api.get(`/jobs/${job.id}`);

		if (cancelled) {
		return;
		}

		const updatedJob = response.data;

		onJobChange(updatedJob);
		setPollError(null);

		if (updatedJob.status === 'DONE') {
		await onDone?.(updatedJob);
		return;
		}

		if (updatedJob.status === 'FAILED') {
		return;
		}

		timeoutId = setTimeout(poll, 1000);
	} catch {
		if (!cancelled) {
		setPollError(
			'Impossible de récupérer le statut du traitement.'
		);
		}
	}
	};

	timeoutId = setTimeout(poll, 500);

	return () => {
	cancelled = true;
	clearTimeout(timeoutId);
	};
}, [job?.id, job?.status, onJobChange, onDone]);

if (!job) {
	return null;
}

const statusStyle =
	STATUS[job.status] ?? STATUS.FAILED;

const handleRetry = async () => {
	try {
	setRetrying(true);
	setPollError(null);

	const response = await api.post(
		`/jobs/${job.id}/retry`
	);

	onJobChange(response.data);
	} catch (error) {
	setPollError(
		error.response?.data?.message
		|| error.response?.data?.error
		|| 'Impossible de relancer le traitement.'
	);
	} finally {
	setRetrying(false);
	}
};

const running =
	job.status === 'QUEUED'
	|| job.status === 'PROCESSING';

return (
	<div
	style={{
		marginTop: '12px',
		padding: '12px 14px',
		border: `1px solid ${statusStyle.border}`,
		borderRadius: '8px',
		background: statusStyle.background,
	}}
	>
	<div
		style={{
		display: 'flex',
		alignItems: 'center',
		gap: '8px',
		}}
	>
		{running && (
		<Loader2
			size={15}
			style={{
			animation: 'spin 1s linear infinite',
			}}
		/>
		)}

		<span
		style={{
			color: statusStyle.color,
			fontWeight: 600,
			fontSize: '13.5px',
		}}
		>
		{statusStyle.label}
		</span>
	</div>

	<div
		style={{
		marginTop: '5px',
		fontSize: '12px',
		color: '#6b7280',
		}}
	>
		Tentative {job.attempts}/3
	</div>

	{job.error && (
		<div
		style={{
			marginTop: '8px',
			color: '#b91c1c',
			fontSize: '13px',
		}}
		>
		{job.error}
		</div>
	)}

	{pollError && (
		<div
		style={{
			marginTop: '8px',
			color: '#b91c1c',
			fontSize: '13px',
		}}
		>
		{pollError}
		</div>
	)}

	{job.status === 'FAILED'
		&& job.attempts < 3 && (
		<button
			type="button"
			onClick={handleRetry}
			disabled={retrying}
			style={{
			marginTop: '10px',
			padding: '7px 12px',
			border: 'none',
			borderRadius: '6px',
			background: '#1a2744',
			color: '#fff',
			cursor: retrying
				? 'not-allowed'
				: 'pointer',
			opacity: retrying ? 0.6 : 1,
			fontSize: '12.5px',
			fontWeight: 600,
			}}
		>
			{retrying
			? 'Relance...'
			: 'Réessayer'}
		</button>
	)}

	{job.status === 'FAILED'
		&& job.attempts >= 3 && (
		<div
			style={{
			marginTop: '8px',
			color: '#6b7280',
			fontSize: '12px',
			}}
		>
			Nombre maximal de tentatives atteint.
		</div>
	)}

	<style>
		{`
		@keyframes spin {
			from { transform: rotate(0deg); }
			to { transform: rotate(360deg); }
		}
		`}
	</style>
	</div>
);
}
