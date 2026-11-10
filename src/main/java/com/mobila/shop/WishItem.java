package com.mobila.shop;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Produs salvat în lista de dorințe a unui utilizator, cu prețul de referință (pentru alerta de scădere a prețului). Un singur rând pe (utilizator, produs). */
@Entity
@Table(name = "wish_items", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "product_id"}))
public class WishItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(name = "user_id")
    public Long userId;
    @Column(name = "product_id")
    public Long productId;
    public double priceAtAdd;
    public LocalDateTime addedAt = LocalDateTime.now();
}
