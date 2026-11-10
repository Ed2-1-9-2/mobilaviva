package com.mobila.shop;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MatchingController {
    private static final long TTL_MS = 30_000;

    private final ProductRepository products;
    private volatile List<Product> snapshot = List.of();
    private volatile long snapshotAt = 0;

    public MatchingController(ProductRepository products) { this.products = products; }

    /** Catalogul se citește din baza de date cel mult o dată la 30 s (nu la fiecare vizualizare de produs). */
    private List<Product> catalog() {
        long now = System.currentTimeMillis();
        if (now - snapshotAt > TTL_MS) { snapshot = products.findAll(); snapshotAt = now; }
        return snapshot;
    }

    /** Set asortat pentru un produs: aceeași colecție + categorii complementare (max. 4 piese, în stoc). */
    @GetMapping("/api/products/{id}/matching")
    public ResponseEntity<?> matching(@PathVariable Long id) {
        Product p = products.findById(id).orElse(null);
        if (p == null) return Util.err(404, "Produs inexistent.");
        return ResponseEntity.ok(Matching.find(p, catalog()));
    }
}
