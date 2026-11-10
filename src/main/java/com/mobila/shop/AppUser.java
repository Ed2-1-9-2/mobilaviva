package com.mobila.shop;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "app_users")
public class AppUser {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public String name;
    @Column(unique = true)
    public String email;
    @JsonIgnore
    public String passwordHash;
    public String role = "USER";
    public LocalDateTime createdAt = LocalDateTime.now();
    public LocalDateTime termsAcceptedAt;     // momentul acceptării Termenilor și a Politicii de confidențialitate
    @JsonIgnore
    @Column(columnDefinition = "TEXT")
    public String cartData;                   // coșul salvat pe cont (id:cantitate,...): se păstrează după logout / pe alte dispozitive
    public LocalDateTime passwordChangedAt;   // setat când parola e schimbată de utilizator (reset prin email / Contul meu); SeedData nu mai suprascrie parola adminului

    public boolean isAdmin() { return "ADMIN".equals(role); }
}
