package com.mobila.shop;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShopService {
    public static final double FREE_SHIPPING = 1500.0;
    public static final double SHIPPING_COST = 49.99;

    /** Cod promoțional: procent sau sumă fixă, cu prag minim și, opțional, o singură folosire pe cont. */
    public record Coupon(String code, int percent, long fixedCents, long minCents, boolean oncePerUser) {}
    static final Map<String, Coupon> COUPONS = Map.of(
        "BUNVENIT10", new Coupon("BUNVENIT10", 10, 0, 0, true),         // -10%, o dată pe cont
        "SALON100", new Coupon("SALON100", 0, 10000, 100000, false));   // -100 RON la comenzi de minimum 1000 RON

    /** Montaj la livrare: tarif pe bucată, în RON, după categorie (0 = nu se montează). */
    static final Map<String, Integer> ASSEMBLY_RON = Map.of(
        "Canapele", 79, "Paturi", 89, "Dulapuri", 129, "Comode", 59, "Mese", 49, "Scaune", 19, "Birouri", 69, "Hol", 49, "Copii", 59);

    /** Reducere/montaj suplimentare comenzii (în cenți); extraDays = zile adăugate livrării (montaj). */
    public record Extras(long discountCents, String couponCode, long assemblyCents, int extraDays) {
        public static final Extras NONE = new Extras(0, null, 0, 0);
    }

    /** null = codul e valid pentru acest coș și cont; altfel mesajul de eroare. */
    public String couponProblem(String raw, long subtotalCents, Long userId) {
        String code = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        Coupon c = COUPONS.get(code);
        if (c == null) return "Cod de reducere invalid.";
        if (subtotalCents < c.minCents()) return "Codul " + c.code() + " se aplică la comenzi de minimum " + (c.minCents() / 100) + " RON.";
        if (c.oncePerUser()) {
            if (userId == null) return "Autentifică-te pentru a folosi acest cod.";
            if (orders.existsByUserIdAndCouponCodeAndStatusNot(userId, c.code(), "Anulată")) return "Ai folosit deja acest cod.";
        }
        return null;
    }

    static long assemblyCents(List<CartLine> lines) {
        long s = 0;
        for (CartLine l : lines) s += ASSEMBLY_RON.getOrDefault(l.getProduct().category, 0) * 100L * l.getQty();
        return s;
    }

    /** Stoc insuficient (sau produs șters) în momentul creării comenzii. */
    public static class OutOfStockException extends IllegalStateException {
        public OutOfStockException(String message) { super(message); }
    }

    /** Linie de comandă cu prețul fixat în bani (cenți): exact prețul care a fost plătit. */
    public record OrderLine(Long productId, int qty, long unitCents) {}

    private final ProductRepository products;
    private final OrderRepository orders;
    private final InvoiceSeqRepository invoices;

    public ShopService(ProductRepository products, OrderRepository orders, InvoiceSeqRepository invoices) {
        this.products = products;
        this.orders = orders;
        this.invoices = invoices;
    }

    public Cart buildCart(Map<Long, Integer> raw) { return buildCart(raw, null, false, null); }

    public Cart buildCart(Map<Long, Integer> raw, String coupon, boolean wantAssembly, Long userId) {
        List<CartLine> lines = new ArrayList<>();
        for (Map.Entry<Long, Integer> e : raw.entrySet()) {
            products.findById(e.getKey()).ifPresent(p -> lines.add(new CartLine(p, e.getValue())));
        }
        double subtotal = lines.stream().mapToDouble(CartLine::getSub).sum();
        double shipping = (lines.isEmpty() || subtotal >= FREE_SHIPPING) ? 0 : SHIPPING_COST;
        int days = lines.stream().mapToInt(l -> l.getProduct().deliveryDays).max().orElse(0);
        long subC = 0;
        for (CartLine l : lines) subC += Math.round(l.getProduct().price * 100) * l.getQty();
        long offerC = assemblyCents(lines);
        boolean asmOn = wantAssembly && offerC > 0;
        String code = null;
        long discC = 0;
        if (coupon != null && !lines.isEmpty() && couponProblem(coupon, subC, userId) == null) {
            Coupon c = COUPONS.get(coupon.trim().toUpperCase(Locale.ROOT));
            code = c.code();
            discC = c.percent() > 0 ? Math.round(subC * c.percent() / 100.0) : Math.min(c.fixedCents(), subC);
        }
        return new Cart(lines, subtotal, shipping, days, discC / 100.0, code, asmOn ? offerC / 100.0 : 0, offerC / 100.0, asmOn);
    }

    /** Comandă din coșul curent (card simulat); prețurile sunt cele de acum. */
    @Transactional
    public CustomerOrder place(AppUser user, Cart cart, String name, String email, String address, String last4, String stripeSessionId) {
        List<OrderLine> lines = new ArrayList<>();
        for (CartLine l : cart.getLines())
            lines.add(new OrderLine(l.getProduct().id, l.getQty(), Math.round(l.getProduct().price * 100)));
        Extras x = new Extras(Math.round(cart.getDiscount() * 100), cart.getCouponCode(), Math.round(cart.getAssembly() * 100), cart.isAssemblyOn() ? 1 : 0);
        return placeOrder(user, lines, Math.round(cart.getShipping() * 100), name, email, address, last4, stripeSessionId, x);
    }

    /**
     * Creează comanda cu prețurile date (cenți), blocând rândurile produselor (în ordinea id-ului: fără deadlock între comenzi concurente).
     * Dacă stocul nu ajunge, nimic nu se salvează și numărul de factură nu se consumă (fără goluri în numerotare).
     */
    @Transactional
    public CustomerOrder placeOrder(AppUser user, List<OrderLine> lines, long shippingCents, String name, String email,
                                    String address, String last4, String stripeSessionId) {
        return placeOrder(user, lines, shippingCents, name, email, address, last4, stripeSessionId, Extras.NONE);
    }

    @Transactional
    public CustomerOrder placeOrder(AppUser user, List<OrderLine> lines, long shippingCents, String name, String email,
                                    String address, String last4, String stripeSessionId, Extras x) {
        Coupon cp = x.couponCode() == null ? null : COUPONS.get(x.couponCode());
        if (cp != null && cp.oncePerUser() && user != null && orders.existsByUserIdAndCouponCodeAndStatusNot(user.id, cp.code(), "Anulată"))
            throw new OutOfStockException("Codul de reducere a fost deja folosit.");   // cerere paralelă: comanda nu se creează (la Stripe: rambursare automată)
        CustomerOrder o = new CustomerOrder();
        o.stripeSessionId = stripeSessionId;
        o.user = user; o.name = name; o.email = email; o.address = address; o.cardLast4 = last4;
        List<OrderLine> sorted = new ArrayList<>(lines);
        sorted.sort(Comparator.comparing(OrderLine::productId));
        long total = shippingCents + x.assemblyCents() - x.discountCents();
        int days = 0;
        for (OrderLine l : sorted) {
            Product p = products.lockById(l.productId()).orElseThrow(() -> new OutOfStockException("Produs indisponibil."));
            if (l.qty() <= 0 || p.stock < l.qty()) throw new OutOfStockException("Stoc insuficient pentru " + p.name + ".");
            p.stock -= l.qty();
            products.save(p);
            OrderItem it = new OrderItem();
            it.order = o; it.productId = p.id; it.name = p.name; it.price = l.unitCents() / 100.0; it.qty = l.qty();
            o.items.add(it);
            total += l.unitCents() * l.qty();
            days = Math.max(days, p.deliveryDays);
        }
        o.shipping = shippingCents / 100.0; o.total = total / 100.0; o.deliveryDays = days + x.extraDays();
        o.discount = x.discountCents() / 100.0; o.couponCode = x.couponCode(); o.assembly = x.assemblyCents() / 100.0;
        InvoiceSeq seq = invoices.lockNext().orElseThrow(() -> new IllegalStateException("Secvența de facturi nu este inițializată."));
        seq.last += 1;
        invoices.save(seq);
        o.invoiceNumber = String.format("MV-%d-%06d", LocalDate.now().getYear(), seq.last);
        return orders.save(o);
    }
}
