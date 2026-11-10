package com.mobila.shop;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Contor pentru numerele de factură: fără goluri (creșterea se anulează odată cu tranzacția dacă comanda eșuează). */
@Entity
@Table(name = "invoice_seq")
public class InvoiceSeq {
    @Id
    public Long id;
    public long last;
}
