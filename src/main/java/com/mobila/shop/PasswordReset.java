package com.mobila.shop;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Cerere de resetare a parolei. Se păstrează doar hash-ul SHA-256 al token-ului (niciodată token-ul în clar). */
@Entity
@Table(name = "password_resets", indexes = @Index(columnList = "tokenHash"))
public class PasswordReset {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public Long userId;
    @Column(unique = true)
    public String tokenHash;
    public LocalDateTime createdAt = LocalDateTime.now();
    public LocalDateTime expiresAt;
}
