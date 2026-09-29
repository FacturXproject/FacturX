package com.facturx.app.extraction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "draft_lines")
public class DraftLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "draft_invoice_id", nullable = false)
    private DraftInvoice draftInvoice;

    @Column(name = "position")
    private int position;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "quantity", length = 500)
    private String quantity;

    @Column(name = "unit_price", length = 500)
    private String unitPrice;

    @Column(name = "total", length = 500)
    private String total;

    @Column(name = "confidence")
    private Double confidence;

    public Long getId() { return id; }
    public DraftInvoice getDraftInvoice() { return draftInvoice; }
    public int getPosition() { return position; }
    public String getDescription() { return description; }
    public String getQuantity() { return quantity; }
    public String getUnitPrice() { return unitPrice; }
    public String getTotal() { return total; }
    public Double getConfidence() { return confidence; }

    public void setDraftInvoice(DraftInvoice draftInvoice) { this.draftInvoice = draftInvoice; }
    public void setPosition(int position) { this.position = position; }
    public void setDescription(String description) { this.description = description; }
    public void setQuantity(String quantity) { this.quantity = quantity; }
    public void setUnitPrice(String unitPrice) { this.unitPrice = unitPrice; }
    public void setTotal(String total) { this.total = total; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }
}
