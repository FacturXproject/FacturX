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
@Table(name = "extracted_fields")
public class ExtractedField {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "draft_invoice_id", nullable = false)
    private DraftInvoice draftInvoice;

    @Column(name = "field_name", nullable = false, length = 50)
    private String fieldName;

    @Column(name = "value", length = 500)
    private String value;

    @Column(name = "confidence")
    private Double confidence;

    @Column(name = "source", length = 50)
    private String source;

    public Long getId() { return id; }
    public DraftInvoice getDraftInvoice() { return draftInvoice; }
    public String getFieldName() { return fieldName; }
    public String getValue() { return value; }
    public Double getConfidence() { return confidence; }
    public String getSource() { return source; }

    public void setDraftInvoice(DraftInvoice draftInvoice) { this.draftInvoice = draftInvoice; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }
    public void setValue(String value) { this.value = value; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }
    public void setSource(String source) { this.source = source; }
}
