import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  ArrowLeft,
  Building2,
  UserRound,
  FileText,
  CalendarDays,
} from 'lucide-react';

import api from '../services/api';

const displayValue = (value) =>
  value === null || value === undefined || value === ''
    ? 'Non renseigné'
    : value;

const displayAmount = (value, currency) => {
  if (value === null || value === undefined) {
    return 'Non renseigné';
  }

  const number = Number(value);

  if (Number.isNaN(number)) {
    return value;
  }

  return new Intl.NumberFormat('fr-FR', {
    style: 'currency',
    currency: currency || 'EUR',
  }).format(number);
};

export default function InvoiceView() {
  const { id } = useParams();
  const navigate = useNavigate();

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
    return (
      <div style={{ padding: '32px 40px' }}>
        Chargement de la facture...
      </div>
    );
  }

  if (error) {
    const status = error.response?.status;

    let message = 'Impossible de charger la facture.';

    if (status === 403) {
      message = "Vous n'avez pas l'autorisation de consulter cette facture.";
    }

    if (status === 404) {
      message = 'Le document demandé est introuvable.';
    }

    if (status === 422) {
      message = "Ce document n'est pas un fichier XML de facture valide.";
    }

    return (
      <div
        style={{
          padding: '32px 40px',
          maxWidth: '1100px',
          margin: '0 auto',
        }}
      >
        <button onClick={() => navigate('/lecture-xml')} style={backButtonStyle}>
          <ArrowLeft size={15} />
          Retour à la lecture XML
        </button>

        <div
          style={{
            marginTop: '20px',
            background: '#fee2e2',
            color: '#991b1b',
            padding: '14px 16px',
            borderRadius: '8px',
          }}
        >
          {message}
        </div>
      </div>
    );
  }

  return (
    <div
      style={{
        padding: '32px 40px',
        maxWidth: '1100px',
        margin: '0 auto',
        background: '#f8f9fa',
        minHeight: '100vh',
      }}
    >
      <button onClick={() => navigate('/lecture-xml')} style={backButtonStyle}>
        <ArrowLeft size={15} />
        Retour aux factures XML
      </button>

      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'flex-start',
          gap: '20px',
          flexWrap: 'wrap',
          margin: '22px 0',
        }}
      >
        <div>
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '9px',
              marginBottom: '6px',
            }}
          >
            <FileText size={22} color="#2563eb" />

            <h1
              style={{
                margin: 0,
                fontSize: '26px',
                color: '#111827',
              }}
            >
              Facture {displayValue(invoice.invoiceNumber)}
            </h1>
          </div>

          <div
            style={{
              display: 'flex',
              gap: '14px',
              flexWrap: 'wrap',
              color: '#6b7280',
              fontSize: '13px',
            }}
          >
            <span
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '5px',
              }}
            >
              <CalendarDays size={14} />
              {displayValue(invoice.invoiceDate)}
            </span>

            <span>
              Devise : {displayValue(invoice.currency)}
            </span>
          </div>
        </div>
      </div>

      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
          gap: '16px',
          marginBottom: '20px',
        }}
      >
        <PartyCard
          title="Vendeur"
          icon={Building2}
          party={invoice.seller}
        />

        <PartyCard
          title="Acheteur"
          icon={UserRound}
          party={invoice.buyer}
        />
      </div>

      <div style={cardStyle}>
        <div style={cardHeaderStyle}>
          <h2 style={sectionTitleStyle}>Lignes de facture</h2>
        </div>

        <div style={{ overflowX: 'auto' }}>
          <table
            style={{
              width: '100%',
              borderCollapse: 'collapse',
              fontSize: '13.5px',
            }}
          >
            <thead>
              <tr style={{ background: '#f9fafb' }}>
                <th style={tableHeaderStyle}>Description</th>
                <th style={tableHeaderStyle}>Quantité</th>
                <th style={tableHeaderStyle}>Prix unitaire</th>
                <th style={tableHeaderStyle}>Total</th>
              </tr>
            </thead>

            <tbody>
              {invoice.lines?.length > 0 ? (
                invoice.lines.map((line, index) => (
                  <tr
                    key={index}
                    style={{ borderTop: '1px solid #f3f4f6' }}
                  >
                    <td style={tableCellStyle}>
                      {displayValue(line.description)}
                    </td>

                    <td style={tableCellStyle}>
                      {displayValue(line.quantity)}
                    </td>

                    <td style={tableCellStyle}>
                      {displayAmount(line.unitPrice, invoice.currency)}
                    </td>

                    <td
                      style={{
                        ...tableCellStyle,
                        fontWeight: 600,
                        color: '#111827',
                      }}
                    >
                      {displayAmount(line.lineTotal, invoice.currency)}
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td
                    colSpan="4"
                    style={{
                      padding: '28px',
                      textAlign: 'center',
                      color: '#9ca3af',
                    }}
                  >
                    Aucune ligne de facture disponible.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      <div
        style={{
          ...cardStyle,
          maxWidth: '430px',
          marginLeft: 'auto',
          marginTop: '20px',
        }}
      >
        <div style={cardHeaderStyle}>
          <h2 style={sectionTitleStyle}>Totaux</h2>
        </div>

        <div style={{ padding: '18px 20px' }}>
          <TotalRow
            label="Sous-total"
            value={displayAmount(invoice.subtotal, invoice.currency)}
          />

          <TotalRow
            label="TVA"
            value={displayAmount(invoice.vat, invoice.currency)}
          />

          <div
            style={{
              borderTop: '1px solid #e5e7eb',
              marginTop: '12px',
              paddingTop: '14px',
            }}
          >
            <TotalRow
              label="Total"
              value={displayAmount(invoice.total, invoice.currency)}
              strong
            />
          </div>
        </div>
      </div>
    </div>
  );
}

function PartyCard({ title, icon: Icon, party }) {
  return (
    <div style={cardStyle}>
      <div style={cardHeaderStyle}>
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
          }}
        >
          <Icon size={17} color="#2563eb" />

          <h2 style={sectionTitleStyle}>{title}</h2>
        </div>
      </div>

      <div style={{ padding: '18px 20px' }}>
        <div
          style={{
            fontSize: '15px',
            fontWeight: 600,
            color: '#111827',
            marginBottom: '8px',
          }}
        >
          {displayValue(party?.name)}
        </div>

        <div
          style={{
            fontSize: '13.5px',
            color: '#6b7280',
            lineHeight: 1.6,
          }}
        >
          <div>{displayValue(party?.address)}</div>
          <div>
            TVA : {displayValue(party?.vatId)}
          </div>
        </div>
      </div>
    </div>
  );
}

function TotalRow({ label, value, strong = false }) {
  return (
    <div
      style={{
        display: 'flex',
        justifyContent: 'space-between',
        gap: '20px',
        marginBottom: '10px',
        fontSize: strong ? '16px' : '13.5px',
        fontWeight: strong ? 700 : 400,
        color: strong ? '#111827' : '#374151',
      }}
    >
      <span>{label}</span>
      <span>{value}</span>
    </div>
  );
}

const cardStyle = {
  background: '#fff',
  border: '1px solid #e5e7eb',
  borderRadius: '10px',
  overflow: 'hidden',
};

const cardHeaderStyle = {
  padding: '13px 20px',
  borderBottom: '1px solid #e5e7eb',
  background: '#f9fafb',
};

const sectionTitleStyle = {
  margin: 0,
  fontSize: '14px',
  fontWeight: 600,
  color: '#374151',
};

const tableHeaderStyle = {
  textAlign: 'left',
  padding: '12px 20px',
  color: '#6b7280',
  fontWeight: 500,
  whiteSpace: 'nowrap',
};

const tableCellStyle = {
  padding: '14px 20px',
  color: '#374151',
};

const backButtonStyle = {
  display: 'flex',
  alignItems: 'center',
  gap: '6px',
  background: 'none',
  border: 'none',
  color: '#6b7280',
  cursor: 'pointer',
  padding: 0,
  fontSize: '13.5px',
};
