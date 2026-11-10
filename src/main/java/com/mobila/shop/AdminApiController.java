package com.mobila.shop;

import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")   // protejat de AdminGuard
public class AdminApiController {
    static final List<String> STATUSES = List.of("Așteaptă plata", "Plată la livrare", "Plătită", "Expediată", "Livrată", "Anulată");
    public record StatusReq(String status) {}

    private final ProductRepository products;
    private final OrderRepository orders;
    private final UserRepository users;
    private final MailService mail;

    public AdminApiController(ProductRepository products, OrderRepository orders, UserRepository users, MailService mail) {
        this.products = products;
        this.orders = orders;
        this.users = users;
        this.mail = mail;
    }

    // ------- Email: pentru pagina „Email” din Admin -------
    @GetMapping("/email/status")
    public java.util.Map<String, Object> emailStatus() { return mail.healthReport(); }

    public record EmailTestReq(String email) {}
    @PostMapping("/email/test")
    public ResponseEntity<?> emailTest(@RequestBody EmailTestReq r) {
        String to = r == null || r.email() == null ? "" : r.email().trim().toLowerCase();
        if (to.isEmpty() || !to.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+"))
            return Util.err(400, "Adresa de email nu e validă.");
        return ResponseEntity.ok(mail.sendTest(to));
    }

    @GetMapping("/orders")
    public List<CustomerOrder> orders() { return orders.findAllByOrderByIdDesc(); }

    @PutMapping("/orders/{id}/status")
    public ResponseEntity<?> status(@PathVariable Long id, @RequestBody StatusReq r) {
        if (r.status() == null || !STATUSES.contains(r.status())) return Util.err(400, "Status invalid.");
        return orders.findById(id).<ResponseEntity<?>>map(o -> {
            if ("Anulată".equals(o.status) && !"Anulată".equals(r.status()))
                return Util.err(400, "O comandă anulată nu mai poate fi reactivată.");
            if ("Anulată".equals(r.status()) && !"Anulată".equals(o.status)) {
                for (OrderItem it : o.items)                       // produsele anulate se întorc în stoc
                    products.findById(it.productId).ifPresent(p -> { p.stock += it.qty; products.save(p); });
            }
            o.status = r.status();
            return ResponseEntity.ok(orders.save(o));
        }).orElseGet(() -> Util.err(404, "Comandă inexistentă."));
    }

    @GetMapping("/products")
    public List<Product> products() { return products.findAll(Sort.by("id")); }

    private static List<String> validate(Product in) {
        List<String> e = new ArrayList<>();
        if (in.name == null || in.name.isBlank()) e.add("Numele este obligatoriu.");
        if (in.brand == null || in.brand.isBlank()) e.add("Brandul este obligatoriu.");
        if (in.category == null || in.category.isBlank()) e.add("Categoria este obligatorie.");
        if (in.price < 0) e.add("Prețul nu poate fi negativ.");
        return e;
    }

    private static void apply(Product t, Product in) {
        t.name = in.name.trim(); t.brand = in.brand.trim(); t.category = in.category.trim();
        t.price = Math.max(in.price, 0); t.oldPrice = Math.max(in.oldPrice, 0); t.stock = Math.max(in.stock, 0);
        t.deliveryDays = Math.max(in.deliveryDays, 0); t.rating = Math.min(Math.max(in.rating, 0), 5);
        t.material = in.material == null ? "" : in.material; t.dimensions = in.dimensions == null ? "" : in.dimensions;
        t.color = in.color == null ? "" : in.color; t.description = in.description == null ? "" : in.description;
        if (in.manufacturer != null && !in.manufacturer.isBlank()) t.manufacturer = SeedData.clip(in.manufacturer, 200);
        if (in.originPlace != null && !in.originPlace.isBlank()) t.originPlace = SeedData.clip(in.originPlace, 120);
        if (in.originCountry != null && in.originCountry.strip().matches("[A-Za-z]{2}")) t.originCountry = in.originCountry.strip().toUpperCase(java.util.Locale.ROOT);
        if (in.production != null && !in.production.isBlank()) t.production = SeedData.clip(in.production, 200);
        SeedData.enrich(t);                                    // câmpurile rămase goale se completează după brand / material
        t.icon = SeedData.icon(t.category);
        String u = in.imageUrl == null ? "" : in.imageUrl.trim();
        t.imageUrl = (u.startsWith("https://") || u.startsWith("http://") || u.startsWith("/")) && u.length() <= 600 ? u : "";
    }

    @PostMapping("/products")
    public ResponseEntity<?> create(@RequestBody Product in) {
        List<String> e = validate(in);
        if (!e.isEmpty()) return Util.err(400, e.toArray(new String[0]));
        Product p = new Product();
        apply(p, in);
        return ResponseEntity.ok(products.save(p));
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Product in) {
        List<String> e = validate(in);
        if (!e.isEmpty()) return Util.err(400, e.toArray(new String[0]));
        return products.findById(id).<ResponseEntity<?>>map(p -> {
            apply(p, in);
            return ResponseEntity.ok(products.save(p));
        }).orElseGet(() -> Util.err(404, "Produs inexistent."));
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        products.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users")
    public List<AppUser> users() { return users.findAll(); }

    public record RoleReq(String role) {}

    @PutMapping("/users/{id}/role")
    public ResponseEntity<?> role(@PathVariable Long id, @RequestBody RoleReq r, HttpSession s) {
        if (r.role() == null || !List.of("USER", "ADMIN").contains(r.role())) return Util.err(400, "Rol invalid.");
        if (id.equals(s.getAttribute("uid"))) return Util.err(400, "Nu îți poți schimba propriul rol.");
        return users.findById(id).<ResponseEntity<?>>map(u -> {
            u.role = r.role();
            return ResponseEntity.ok(users.save(u));
        }).orElseGet(() -> Util.err(404, "Utilizator inexistent."));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id, HttpSession s) {
        if (id.equals(s.getAttribute("uid"))) return Util.err(400, "Nu îți poți șterge propriul cont.");
        if (!orders.findByUserIdOrderByIdDesc(id).isEmpty()) return Util.err(400, "Utilizatorul are comenzi și nu poate fi șters.");
        users.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
