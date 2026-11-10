package com.mobila.shop;

import jakarta.servlet.http.HttpSession;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Recenzii pe produs (personalizare). Citire publică; scriere doar pentru clienții care au comandat produsul (comenzi neanulate și plătite/la livrare). */
@RestController
@RequestMapping("/api/products/{productId}/reviews")
public class ReviewController {
    public record ReviewReq(Integer stars, String text) {}

    private final ReviewRepository reviews;
    private final ProductRepository products;
    private final UserRepository users;
    private final OrderRepository orders;

    public ReviewController(ReviewRepository reviews, ProductRepository products, UserRepository users, OrderRepository orders) {
        this.reviews = reviews;
        this.products = products;
        this.users = users;
        this.orders = orders;
    }

    private Long uid(HttpSession s) {
        Object id = s.getAttribute("uid");
        return (id instanceof Long l && users.existsById(l)) ? l : null;
    }

    private boolean bought(Long uid, Long productId) {
        for (CustomerOrder o : orders.findByUserIdOrderByIdDesc(uid)) {
            if ("Anulată".equals(o.status) || "Așteaptă plata".equals(o.status)) continue;
            for (OrderItem it : o.items) if (productId.equals(it.productId)) return true;
        }
        return false;
    }

    private static String firstName(String name) {
        if (name == null || name.isBlank()) return "Client";
        String f = name.trim().split("\\s+")[0];
        return f.length() > 30 ? f.substring(0, 30) : f;
    }

    private Map<String, Object> view(Long productId, Long uid) {
        List<Review> all = reviews.findByProductIdOrderByIdDesc(productId);
        double avg = all.stream().mapToInt(r -> r.stars).average().orElse(0);
        Review mine = null;
        if (uid != null) for (Review r : all) if (uid.equals(r.userId)) { mine = r; break; }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("items", all);
        m.put("count", all.size());
        m.put("avg", Math.round(avg * 10) / 10.0);
        m.put("canReview", uid != null && bought(uid, productId));
        m.put("mine", mine);
        return m;
    }

    /** Media recenziilor reale devine ratingul produsului (catalog, sortare, asistent AI). */
    private void syncRating(Long productId) {
        Double avg = reviews.avgStars(productId);
        Product p = products.findById(productId).orElse(null);
        if (p != null && avg != null) { p.rating = Math.round(avg * 10) / 10.0; products.save(p); }
    }

    @GetMapping
    public ResponseEntity<?> list(@PathVariable Long productId, HttpSession s) {
        if (!products.existsById(productId)) return Util.err(404, "Produs inexistent.");
        return ResponseEntity.ok(view(productId, uid(s)));
    }

    @PostMapping
    public ResponseEntity<?> save(@PathVariable Long productId, @RequestBody ReviewReq r, HttpSession s) {
        Long uid = uid(s);
        if (uid == null) return Util.err(401, "Autentificare necesară.");
        if (!products.existsById(productId)) return Util.err(404, "Produs inexistent.");
        if (!bought(uid, productId)) return Util.err(403, "Poți evalua doar produsele pe care le-ai comandat.");
        int stars = r.stars() == null ? 0 : r.stars();
        if (stars < 1 || stars > 5) return Util.err(400, "Alege între 1 și 5 stele.");
        String text = r.text() == null ? "" : r.text().trim();
        if (text.length() > 1000) text = text.substring(0, 1000);
        List<Review> ex = reviews.findByProductIdAndUserId(productId, uid);
        Review rv = ex.isEmpty() ? new Review() : ex.get(0);
        AppUser u = users.findById(uid).orElse(null);
        rv.userId = uid;
        rv.productId = productId;
        rv.stars = stars;
        rv.text = text;
        rv.author = firstName(u == null ? null : u.name);
        try { reviews.save(rv); }
        catch (DataIntegrityViolationException dup) { return Util.err(409, "Recenzia există deja."); }   // cerere paralelă
        syncRating(productId);
        return ResponseEntity.ok(view(productId, uid));
    }

    @DeleteMapping
    public ResponseEntity<?> remove(@PathVariable Long productId, HttpSession s) {
        Long uid = uid(s);
        if (uid == null) return Util.err(401, "Autentificare necesară.");
        reviews.deleteAll(reviews.findByProductIdAndUserId(productId, uid));
        syncRating(productId);
        return ResponseEntity.ok(view(productId, uid));
    }
}
