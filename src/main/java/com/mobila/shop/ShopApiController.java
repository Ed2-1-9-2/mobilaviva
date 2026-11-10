package com.mobila.shop;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpSession;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.time.YearMonth;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ShopApiController {
    private static final int MAX_QTY_PER_REQUEST = 1000;

    public record CartReq(Long productId, Integer qty) {}
    public record CouponReq(String code) {}
    public record AssemblyReq(Boolean on) {}
    public record QtyReq(Integer qty) {}
    public record CheckoutReq(String name, String email, String address, String card, String exp, String cvv) {}
    public record DeliveryReq(String name, String email, String address, String origin, String lang) {}
    public record OfflineReq(String name, String email, String address, String method) {}
    @org.springframework.beans.factory.annotation.Value("${shop.iban:RO49AAAA1B31007593840000}")
    private String iban;
    public record ConfirmReq(String sessionId) {}

    private final ProductRepository products;
    private final OrderRepository orders;
    private final UserRepository users;
    private final ShopService shop;
    private final StripeClient stripe;
    private final PaymentService payments;

    public ShopApiController(ProductRepository products, OrderRepository orders, UserRepository users, ShopService shop, StripeClient stripe, PaymentService payments) {
        this.stripe = stripe;
        this.payments = payments;
        this.products = products;
        this.orders = orders;
        this.users = users;
        this.shop = shop;
    }

    // ------------------------------------------------------------ helpers
    private AppUser me(HttpSession s) {
        Object id = s.getAttribute("uid");
        return id == null ? null : users.findById((Long) id).orElse(null);
    }

    @SuppressWarnings("unchecked")
    private static Map<Long, Integer> cartMap(HttpSession s) {
        Object o = s.getAttribute("cart");
        if (o == null) {
            o = new java.util.concurrent.ConcurrentSkipListMap<Long, Integer>();      // cereri paralele pe aceeași sesiune
            s.setAttribute("cart", o);
        }
        return (Map<Long, Integer>) o;
    }

    @SuppressWarnings("unchecked")
    private static List<Long> cmp(HttpSession s) {
        Object o = s.getAttribute("compare");
        if (o == null) {
            o = new java.util.concurrent.CopyOnWriteArrayList<Long>();
            s.setAttribute("compare", o);
        }
        return (List<Long>) o;
    }

    private static String nz(String s) { return s == null ? "" : s; }

    private static boolean luhn(String n) {
        int sum = 0;
        for (int i = 0; i < n.length(); i++) {
            int d = n.charAt(n.length() - 1 - i) - '0';
            if (i % 2 == 1) { d *= 2; if (d > 9) d -= 9; }
            sum += d;
        }
        return sum % 10 == 0;
    }

    // ------------------------------------------------------------ sesiune
    @GetMapping("/session")
    public Map<String, Object> session(HttpSession s) {
        AppUser u = me(s);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("user", u == null ? null : Util.userView(u));
        m.put("cartCount", cartMap(s).values().stream().mapToInt(i -> i).sum());
        m.put("compareCount", cmp(s).size());
        m.put("categories", products.categories());
        m.put("payment", stripe.enabled() ? "stripe" : "test");
        m.put("iban", iban);
        return m;
    }

    // ------------------------------------------------------------ coș
    @GetMapping("/cart")
    public Cart cart(HttpSession s) { return build(s); }

    /** Coșul cu codul promoțional și montajul alese în sesiune; un cod devenit invalid (folosit între timp, sub prag) se elimină. */
    private Cart build(HttpSession s) {
        AppUser u = me(s);
        String code = (String) s.getAttribute("coupon");
        Cart c = shop.buildCart(cartMap(s), code, Boolean.TRUE.equals(s.getAttribute("assembly")), u == null ? null : u.id);
        if (code != null && c.getCouponCode() == null && !c.getLines().isEmpty()) s.removeAttribute("coupon");
        return c;
    }

    /** Salvează coșul pe contul utilizatorului autentificat (invitații au coșul doar în sesiune). */
    private void saveCart(HttpSession s) {
        AppUser u = me(s);
        if (u == null) return;
        String d = cartMap(s).entrySet().stream().map(e -> e.getKey() + ":" + e.getValue()).collect(java.util.stream.Collectors.joining(","));
        if (!d.equals(u.cartData == null ? "" : u.cartData)) { u.cartData = d; users.save(u); }
    }

    private static void clearCart(HttpSession s) {
        cartMap(s).clear();
        s.removeAttribute("coupon");
        s.removeAttribute("assembly");
    }

    @PostMapping("/cart/coupon")
    public ResponseEntity<?> couponSet(@RequestBody CouponReq r, HttpSession s) {
        AppUser u = me(s);
        String code = nz(r.code()).trim().toUpperCase(Locale.ROOT);
        Cart c = shop.buildCart(cartMap(s));
        if (c.getLines().isEmpty()) return Util.err(400, "Coșul este gol.");
        String problem = shop.couponProblem(code, Math.round(c.getSubtotal() * 100), u == null ? null : u.id);
        if (problem != null) return Util.err(400, problem);
        s.setAttribute("coupon", code);
        return ResponseEntity.ok(build(s));
    }

    @DeleteMapping("/cart/coupon")
    public Cart couponDel(HttpSession s) {
        s.removeAttribute("coupon");
        return build(s);
    }

    @PutMapping("/cart/assembly")
    public Cart assemblySet(@RequestBody AssemblyReq r, HttpSession s) {
        s.setAttribute("assembly", Boolean.TRUE.equals(r.on()));
        return build(s);
    }

    @PostMapping("/cart/items")
    public Cart cartAdd(@RequestBody CartReq r, HttpSession s) {
        int q = r.qty() == null ? 1 : Math.min(Math.max(r.qty(), 1), MAX_QTY_PER_REQUEST);   // fără overflow la adunare
        if (r.productId() != null) {
            products.findById(r.productId()).ifPresent(p -> {
                if (p.stock > 0) {
                    Map<Long, Integer> c = cartMap(s);
                    c.merge(p.id, Math.min(q, p.stock), (a, b) -> Math.min(a + b, p.stock));      // atomic
                }
            });
        }
        saveCart(s);
        return cart(s);
    }

    @PutMapping("/cart/items/{id}")
    public Cart cartSet(@PathVariable Long id, @RequestBody QtyReq r, HttpSession s) {
        Map<Long, Integer> c = cartMap(s);
        Optional<Product> p = products.findById(id);
        int q = r.qty() == null ? 0 : r.qty();
        if (q <= 0 || p.isEmpty()) c.remove(id);
        else {
            int m = Math.min(q, p.get().stock);
            if (m <= 0) c.remove(id); else c.put(id, m);
        }
        saveCart(s);
        return cart(s);
    }

    @DeleteMapping("/cart/items/{id}")
    public Cart cartDel(@PathVariable Long id, HttpSession s) {
        cartMap(s).remove(id);
        saveCart(s);
        return cart(s);
    }

    // ------------------------------------------------------------ comparator (modul original)
    @GetMapping("/compare")
    public List<Product> compare(HttpSession s) {
        List<Product> items = new ArrayList<>();
        for (Long id : cmp(s)) products.findById(id).ifPresent(items::add);
        return items;
    }

    @PostMapping("/compare/{id}")
    public ResponseEntity<?> cmpAdd(@PathVariable Long id, HttpSession s) {
        List<Long> l = cmp(s);
        if (!l.contains(id)) {
            if (l.size() >= 3) return Util.err(400, "Comparatorul acceptă maxim 3 produse.");
            if (!products.existsById(id)) return Util.err(404, "Produs inexistent.");
            l.add(id);
        }
        return ResponseEntity.ok(compare(s));
    }

    @DeleteMapping("/compare/{id}")
    public List<Product> cmpRemove(@PathVariable Long id, HttpSession s) {
        cmp(s).remove(id);
        return compare(s);
    }

    @DeleteMapping("/compare")
    public List<Product> cmpClear(HttpSession s) {
        cmp(s).clear();
        return List.of();
    }

    // ------------------------------------------------------------ checkout (cont obligatoriu, card de test)
    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(@RequestBody CheckoutReq r, HttpSession s) {
        if (stripe.enabled()) return Util.err(400, "Plata se face prin Stripe.");   // altfel orice card Luhn valid ocolea plata reală
        AppUser u = me(s);
        if (u == null) return Util.err(401, "Autentifică-te pentru a plăti.");
        Cart cart = build(s);
        if (cart.getLines().isEmpty()) return Util.err(400, "Coșul este gol.");

        String name = nz(r.name()).trim(), email = nz(r.email()).trim(), address = nz(r.address()).trim();
        String digits = nz(r.card()).replaceAll("\\D", "");
        List<String> errors = new ArrayList<>();
        if (name.length() < 3 || name.length() > 120) errors.add("Introdu numele complet (max. 120 de caractere).");
        if (email.length() > 160) errors.add("Adresa de email este prea lungă.");
        if (!email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) errors.add("Adresa de email nu este validă.");
        if (address.length() < 8 || address.length() > 250) errors.add("Introdu adresa completă de livrare (max. 250 de caractere).");
        if (!(digits.length() >= 13 && digits.length() <= 19 && luhn(digits)))
            errors.add("Numărul cardului nu este valid (test: 4242 4242 4242 4242).");
        Matcher mt = Pattern.compile("(0[1-9]|1[0-2])\\s*/\\s*(\\d{2})").matcher(nz(r.exp()).trim());
        if (!mt.matches() || YearMonth.of(2000 + Integer.parseInt(mt.group(2)), Integer.parseInt(mt.group(1))).isBefore(YearMonth.now()))
            errors.add("Data de expirare este invalidă sau expirată (format LL/AA).");
        if (!nz(r.cvv()).trim().matches("\\d{3,4}")) errors.add("CVV invalid.");
        for (CartLine l : cart.getLines())
            if (l.getQty() > l.getProduct().stock) errors.add("Stoc insuficient pentru " + l.getProduct().name + ".");
        if (!errors.isEmpty()) return Util.err(400, errors.toArray(new String[0]));

        try {
            CustomerOrder o = shop.place(u, cart, name, email, address, digits.substring(digits.length() - 4), null);
            clearCart(s); saveCart(s);
            return ResponseEntity.ok(o);
        } catch (IllegalStateException ex) {
            return Util.err(400, ex.getMessage());
        }
    }

    // ------------------------------------------------------------ ramburs / transfer bancar (fără procesator de plăți)
    @PostMapping("/checkout/offline")
    public ResponseEntity<?> offline(@RequestBody OfflineReq r, HttpSession s) {
        AppUser u = me(s);
        if (u == null) return Util.err(401, "Autentifică-te pentru a plasa comanda.");
        String m = nz(r.method());
        if (!m.equals("ramburs") && !m.equals("transfer")) return Util.err(400, "Metodă de plată invalidă.");
        Cart cart = build(s);
        if (cart.getLines().isEmpty()) return Util.err(400, "Coșul este gol.");
        String name = nz(r.name()).trim(), email = nz(r.email()).trim(), address = nz(r.address()).trim();
        List<String> errors = new ArrayList<>();
        if (name.length() < 3 || name.length() > 120) errors.add("Introdu numele complet (max. 120 de caractere).");
        if (email.length() > 160 || !email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) errors.add("Adresa de email nu este validă.");
        if (address.length() < 8 || address.length() > 250) errors.add("Introdu adresa completă de livrare (max. 250 de caractere).");
        for (CartLine l : cart.getLines())
            if (l.getQty() > l.getProduct().stock) errors.add("Stoc insuficient pentru " + l.getProduct().name + ".");
        if (!errors.isEmpty()) return Util.err(400, errors.toArray(new String[0]));
        try {
            CustomerOrder o = shop.place(u, cart, name, email, address, null, null);
            o.paymentMethod = m;
            o.status = m.equals("ramburs") ? "Plată la livrare" : "Așteaptă plata";
            o = orders.save(o);
            clearCart(s); saveCart(s);
            return ResponseEntity.ok(o);
        } catch (IllegalStateException ex) {
            return Util.err(400, ex.getMessage());
        }
    }

    // ------------------------------------------------------------ plată cu Stripe Checkout (mod test)
    @PostMapping("/checkout/stripe")
    public ResponseEntity<?> stripeStart(@RequestBody DeliveryReq r, HttpSession s) {
        if (!stripe.enabled()) return Util.err(400, "Plata cu Stripe nu este configurată.");
        AppUser u = me(s);
        if (u == null) return Util.err(401, "Autentifică-te pentru a plăti.");
        Cart cart = build(s);
        if (cart.getLines().isEmpty()) return Util.err(400, "Coșul este gol.");
        String name = nz(r.name()).trim(), email = nz(r.email()).trim(), address = nz(r.address()).trim();
        List<String> errors = new ArrayList<>();
        if (name.length() < 3 || name.length() > 120) errors.add("Introdu numele complet (max. 120 de caractere).");
        if (email.length() > 160) errors.add("Adresa de email este prea lungă.");
        if (!email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) errors.add("Adresa de email nu este validă.");
        if (address.length() < 8 || address.length() > 250) errors.add("Introdu adresa completă de livrare (max. 250 de caractere).");
        for (CartLine l : cart.getLines())
            if (l.getQty() > l.getProduct().stock) errors.add("Stoc insuficient pentru " + l.getProduct().name + ".");
        if (!errors.isEmpty()) return Util.err(400, errors.toArray(new String[0]));
        try {
            String base = stripe.baseUrl();
            // Site-ul public e pe Vercel (alt domeniu decât backend-ul de pe Render): întoarcerea din Stripe trebuie să ajungă
            // pe domeniul din care a plătit clientul, altfel cookie-ul de sesiune lipsește. Origin-ul e verificat pe listă albă.
            String o = nz(r.origin()).trim();
            while (o.endsWith("/")) o = o.substring(0, o.length() - 1);
            java.util.Set<String> allowed = new java.util.HashSet<>(java.util.List.of(base, "https://mobilaviva.vercel.app"));
            String extra = System.getenv("EXTRA_ORIGINS");
            if (extra != null) for (String x : extra.split(",")) if (!x.isBlank()) allowed.add(x.strip());
            if (allowed.contains(o)) base = o;
            JsonNode sess = stripe.createCheckoutSession(cart, email, u.id, name, address,
                base + "/checkout/success?session_id={CHECKOUT_SESSION_ID}", base + "/checkout/cancel", r.lang());
            return ResponseEntity.ok(Map.of("url", sess.path("url").asText()));
        } catch (IllegalStateException ex) {
            return Util.err(502, ex.getMessage());
        }
    }

    @PostMapping("/checkout/stripe/confirm")
    public ResponseEntity<?> stripeConfirm(@RequestBody ConfirmReq r, HttpSession s) {
        AppUser u = me(s);
        if (u == null) return Util.err(401, "Autentificare necesară.");
        try {
            PaymentService.Result res = payments.complete(nz(r.sessionId()), u.id);     // idempotent la refresh / dublu-click / webhook paralel
            if (res.status() == PaymentService.Status.ORDER) { clearCart(s); saveCart(s); return ResponseEntity.ok(res.order()); }
            if (res.status() == PaymentService.Status.REFUNDED) return Util.err(409, res.message());
            if (res.status() == PaymentService.Status.NOT_PAID) return Util.err(402, res.message());
            return Util.err(400, res.message());
        } catch (IllegalStateException ex) {
            return Util.err(502, ex.getMessage());
        }
    }

    // ------------------------------------------------------------ comenzi
    @GetMapping("/orders")
    public ResponseEntity<?> myOrders(HttpSession s) {
        AppUser u = me(s);
        if (u == null) return Util.err(401, "Autentificare necesară.");
        return ResponseEntity.ok(orders.findByUserIdOrderByIdDesc(u.id));
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<?> order(@PathVariable Long id, HttpSession s) {
        AppUser u = me(s);
        if (u == null) return Util.err(401, "Autentificare necesară.");
        return orders.findById(id)
            .filter(o -> u.isAdmin() || (o.user != null && o.user.id.equals(u.id)))
            .<ResponseEntity<?>>map(ResponseEntity::ok)
            .orElseGet(() -> Util.err(404, "Comandă inexistentă."));
    }
}
