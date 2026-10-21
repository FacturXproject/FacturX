package com.facturx.app.extraction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * F12 : ventilation de la TVA d'un brouillon corrige, une ligne par taux
 * (base HT, montant de TVA). Toujours recalculee par le serveur a partir des
 * lignes - jamais saisie par l'utilisateur. C'est ce qu'utilisera F13 pour les
 * blocs ApplicableTradeTax du XML CII.
 */
@Entity
@Table(name = "draft_taxes")
public class DraftTax {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "draft_invoice_id", nullable = false)
    private DraftInvoice draftInvoice;

    @Column(name = "position", nullable = false)
    private int position;

    @Column(name = "rate", nullable = false, precision = 7, scale = 4)
    private BigDecimal rate;

    @Column(name = "taxable_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal taxableAmount;

    @Column(name = "tax_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal taxAmount;

    public Long getId() { return id; }
    public DraftInvoice getDraftInvoice() { return draftInvoice; }
    public int getPosition() { return position; }
    public BigDecimal getRate() { return rate; }
    public BigDecimal getTaxableAmount() { return taxableAmount; }
    public BigDecimal getTaxAmount() { return taxAmount; }

    public void setDraftInvoice(DraftInvoice draftInvoice) { this.draftInvoice = draftInvoice; }
    public void setPosition(int position) { this.position = position; }
    public void setRate(BigDecimal rate) { this.rate = rate; }
    public void setTaxableAmount(BigDecimal taxableAmount) { this.taxableAmount = taxableAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }
}
