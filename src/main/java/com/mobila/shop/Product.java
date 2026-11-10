package com.mobila.shop;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public String name = "";
    public String brand = "";
    public String category = "";
    public double price;
    public double oldPrice;          // 0 = fără promoție
    public int stock;
    public double rating = 4.0;
    public int deliveryDays = 5;
    @Column(length = 1000)
    public String description = "";
    public String material = "";
    public String dimensions = "";
    public String color = "";
    public String manufacturer = "";    // producătorul (denumire)
    public String originPlace = "";     // localitatea fabricii
    public String originCountry = "";   // țara fabricației, cod ISO 3166-1 (RO, PL ...)
    public String production = "";      // metoda de producție: cheie (prod_m_* în i18n) sau text liber
    public String icon = "🪑";
    @Column(length = 600)
    public String imageUrl = "";        // URL fotografie (opțional); gol = ilustrația generată

    @com.fasterxml.jackson.annotation.JsonIgnore
    @Column(length = 4000)
    public String searchText = "";      // text normalizat (fără diacritice, litere mici) pentru căutare

    @PrePersist @PreUpdate
    public void reindex() {
        searchText = fold(name + " " + brand + " " + category + " " + material + " " + color + " " + description);
    }

    public static String fold(String s) {
        return java.text.Normalizer.normalize(s == null ? "" : s, java.text.Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "").toLowerCase(java.util.Locale.ROOT);
    }

    public boolean isPromo() { return oldPrice > price; }
    public int getDiscount() { return isPromo() ? (int) Math.round((1 - price / oldPrice) * 100) : 0; }
    public String getStars() { return "★".repeat((int) Math.round(rating)); }
}
