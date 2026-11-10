package com.mobila.shop;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "order_items")
public class OrderItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @JsonIgnore
    @ManyToOne
    public CustomerOrder order;
    public Long productId;
    public String name;
    public double price;
    public int qty;
}
