package com.mobila.shop;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Recenzie reală a unui client (personalizare): stele 1-5 + text, o singură recenzie pe (utilizator, produs), doar după comandă. */
@Entity
@Table(name = "reviews", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "product_id"}))
public class Review {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @JsonIgnore
    @Column(name = "user_id")
    public Long userId;
    @Column(name = "product_id")
    public Long productId;
    public String author = "";
    public int stars = 5;
    @Column(name = "body", length = 1000)
    public String text = "";
    public LocalDateTime createdAt = LocalDateTime.now();

    public String getDate() { return createdAt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")); }
}
