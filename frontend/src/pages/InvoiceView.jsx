import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import api from '../services/api';

export default function InvoiceView() {
	const { id } = useParams();

	const [invoice, setInvoice] = useState(null);
	const [loading, setLoading] = useState(true);
	const [error, setError] = useState(null);

	useEffect(() => {
	async function loadInvoice() {
		try {
		const response = await api.get(`/documents/${id}/invoice-view`);
		setInvoice(response.data);
		} catch (err) {
		setError(err);
		} finally {
		setLoading(false);
		}
	}

	loadInvoice();
	}, [id]);

	if (loading) {
		return <p>Chargement de la facture...</p>;
		}

	if (error) {
		const status = error.response?.status;

		if (status === 403) {
			return <p>Vous n'avez pas l'autorisation de consulter cette facture.</p>;
		}

		if (status === 404) {
			return <p>Le document demandé est introuvable.</p>;
		}

		if (status === 422) {
			return <p>Ce document n'est pas un fichier XML de facture valide.</p>;
		}

		return <p>Impossible de charger la facture.</p>;
	}

	if (!invoice) {
	return null;
	}

	return (
		<div>
			<h1>Facture {invoice.invoiceNumber}</h1>

			<p>Date : {invoice.invoiceDate ?? '-'}</p>
			<p>Devise : {invoice.currency ?? '-'}</p>

			<h2>Vendeur</h2>
			<p>{invoice.seller?.name ?? '-'}</p>
			<p>{invoice.seller?.address ?? '-'}</p>
			<p>TVA : {invoice.seller?.vatId ?? '-'}</p>

			<h2>Acheteur</h2>
			<p>{invoice.buyer?.name ?? '-'}</p>
			<p>{invoice.buyer?.address ?? '-'}</p>
			<p>TVA : {invoice.buyer?.vatId ?? '-'}</p>

			<h2>Lignes de facture</h2>

			<table>
			<thead>
				<tr>
				<th>Description</th>
				<th>Quantité</th>
				<th>Prix unitaire</th>
				<th>Total</th>
				</tr>
			</thead>

			<tbody>
				{invoice.lines?.length > 0 ? (
				invoice.lines.map((line, index) => (
					<tr key={index}>
					<td>{line.description ?? '-'}</td>
					<td>{line.quantity ?? '-'}</td>
					<td>{line.unitPrice ?? '-'}</td>
					<td>{line.lineTotal ?? '-'}</td>
					</tr>
				))
				) : (
				<tr>
					<td colSpan="4">Aucune ligne de facture disponible.</td>
				</tr>
				)}
			</tbody>
			</table>

			<h2>Totaux</h2>
			<p>Sous-total : {invoice.subtotal ?? '-'}</p>
			<p>TVA : {invoice.vat ?? '-'}</p>
			<p>Total : {invoice.total ?? '-'}</p>
		</div>
	);
}
