package com.mobila.shop;

import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Lista de dorințe pe cont (personalizare): produse salvate cu prețul de la salvare; alertă când prețul a scăzut. */
@RestController
@RequestMapping("/api/wishlist")
public class WishlistController {
    private final WishRepository wishes;
    private final ProductRepository products;
    private final UserRepository users;

    public WishlistController(WishRepository wishes, ProductRepository products, UserRepository users) {
        this.wishes = wishes;
        this.products = products;
        this.users = users;
    }

    private Long uid(HttpSession s) {
        Object id = s.getAttribute("uid");
        return (id instanceof Long l && users.existsById(l)) ? l : null;
    }

    @GetMapping
    public ResponseEntity<?> list(HttpSession s) {
        Long uid = uid(s);
        if (uid == null) return Util.err(401, "Autentificare necesară.");
        List<Map<String, Object>> items = new ArrayList<>();
        Set<Long> ids = new LinkedHashSet<>();
        for (WishItem w : wishes.findByUserIdOrderByIdDesc(uid)) {
            if (ids.contains(w.productId)) continue;                 // rânduri duplicate (dublu-click) se ignoră
            Optional<Product> op = products.findById(w.productId);
            if (op.isEmpty()) continue;                              // produs șters din catalog
            Product p = op.get();
            ids.add(w.productId);
            int drop = (w.priceAtAdd > p.price && w.priceAtAdd > 0) ? (int) Math.round((1 - p.price / w.priceAtAdd) * 100) : 0;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("product", p);
            m.put("priceAtAdd", w.priceAtAdd);
            m.put("dropped", drop > 0);
            m.put("dropPct", drop);
            items.add(m);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("items", items);
        out.put("ids", new ArrayList<>(ids));
        return ResponseEntity.ok(out);
    }

    @PutMapping("/{productId}")
    public ResponseEntity<?> add(@PathVariable Long productId, HttpSession s) {
        Long uid = uid(s);
        if (uid == null) return Util.err(401, "Autentificare necesară.");
        Product p = products.findById(productId).orElse(null);
        if (p == null) return Util.err(404, "Produs inexistent.");
        if (wishes.findByUserIdAndProductId(uid, productId).isEmpty()) {
            WishItem w = new WishItem();
            w.userId = uid;
            w.productId = productId;
            w.priceAtAdd = p.price;
            try { wishes.save(w); }
            catch (DataIntegrityViolationException dup) { /* dublu-click / cerere paralelă: există deja */ }
        }
        return ResponseEntity.ok(Map.of("wished", true));
    }

    /** „Am văzut”: prețul curent devine prețul de referință, iar alerta de scădere dispare până la o nouă reducere. */
    @PostMapping("/{productId}/ack")
    public ResponseEntity<?> ack(@PathVariable Long productId, HttpSession s) {
        Long uid = uid(s);
        if (uid == null) return Util.err(401, "Autentificare necesară.");
        Product p = products.findById(productId).orElse(null);
        if (p == null) return Util.err(404, "Produs inexistent.");
        for (WishItem w : wishes.findByUserIdAndProductId(uid, productId)) { w.priceAtAdd = p.price; wishes.save(w); }
        return ResponseEntity.ok(Map.of("acknowledged", true));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<?> remove(@PathVariable Long productId, HttpSession s) {
        Long uid = uid(s);
        if (uid == null) return Util.err(401, "Autentificare necesară.");
        wishes.deleteAll(wishes.findByUserIdAndProductId(uid, productId));
        return ResponseEntity.ok(Map.of("wished", false));
    }
}
