package com.mobila.shop;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class CustomerOrder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @ManyToOne
    public AppUser user;
    public String name;
    public String email;
    public String address;
    public double total;
    public double shipping;
    @Column(columnDefinition = "double precision default 0 not null")
    public double discount;       // reducere din codul promoțional (RON)
    public String couponCode;
    @Column(columnDefinition = "double precision default 0 not null")
    public double assembly;       // montaj la livrare (RON), opțional
    public String cardLast4;
    public String paymentMethod = "card";   // card | ramburs | transfer
    public String status = "Plătită";
    public String invoiceNumber;
    @Column(unique = true)
    public String stripeSessionId;
    public int deliveryDays;
    public LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    public List<OrderItem> items = new ArrayList<>();

    public String getDate() { return createdAt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")); }
    public String getEta() { return createdAt.plusDays(deliveryDays).format(DateTimeFormatter.ofPattern("dd.MM.yyyy")); }
}
