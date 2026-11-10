package com.mobila.shop;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Finalizarea unei plăți Stripe, comună pentru întoarcerea clientului din Stripe și pentru webhook.
 * Comanda se creează din datele sesiunii Stripe (prețuri fixate la inițierea plății), nu din coșul curent.
 * Dacă plata e încasată dar comanda nu se poate onora (stoc epuizat), plata se rambursează automat.
 */
@Service
public class PaymentService {
    private static final Logger LOG = LoggerFactory.getLogger(PaymentService.class);

    public enum Status { ORDER, REFUNDED, NOT_PAID, INVALID }

    public record Result(Status status, CustomerOrder order, String message) {}

    private final StripeClient stripe;
    private final ShopService shop;
    private final OrderRepository orders;
    private final UserRepository users;

    public PaymentService(StripeClient stripe, ShopService shop, OrderRepository orders, UserRepository users) {
        this.stripe = stripe; this.shop = shop; this.orders = orders; this.users = users;
    }

    /**
     * @param expectedUserId utilizatorul autentificat (la întoarcerea din Stripe) sau null (webhook).
     * @throws IllegalStateException la erori Stripe tranzitorii: apelantul poate reîncerca în siguranță (operația e idempotentă).
     */
    public Result complete(String sessionId, Long expectedUserId) {
        Optional<CustomerOrder> existing = orders.findByStripeSessionId(sessionId);
        if (existing.isPresent()) {
            CustomerOrder o = existing.get();
            if (expectedUserId == null || (o.user != null && o.user.id.equals(expectedUserId))) return new Result(Status.ORDER, o, null);
            return new Result(Status.INVALID, null, "Comandă inexistentă.");
        }
        JsonNode sess = stripe.retrieveSession(sessionId);
        if (!"paid".equals(sess.path("payment_status").asText())) return new Result(Status.NOT_PAID, null, "Plata nu a fost finalizată.");
        JsonNode md = sess.path("metadata");
        if (md.path("cart0").isMissingNode()) return new Result(Status.INVALID, null, "Sesiune de plată necunoscută.");   // nu e a magazinului: nu o atingem

        Long uid;
        try { uid = Long.valueOf(sess.path("client_reference_id").asText("")); }
        catch (NumberFormatException e) { return new Result(Status.INVALID, null, "Sesiune de plată necunoscută."); }
        if (expectedUserId != null && !expectedUserId.equals(uid)) return new Result(Status.INVALID, null, "Sesiune de plată necunoscută.");

        long paid = sess.path("amount_total").asLong();
        long shipping = md.path("ship").asLong(0);
        long disc = md.path("disc").asLong(0), asm = md.path("asm").asLong(0);
        int xd = md.path("xd").asInt(0);
        String coupon = md.path("coupon").asText("");
        if (coupon.isBlank()) coupon = null;
        List<ShopService.OrderLine> lines = new ArrayList<>();
        long total = shipping + asm - disc;
        try {
            StringBuilder cart = new StringBuilder();
            for (int i = 0; i < 5 && !md.path("cart" + i).isMissingNode(); i++) cart.append(md.path("cart" + i).asText(""));
            for (String part : cart.toString().split(",")) {
                String[] kv = part.split(":");
                if (kv.length != 3) throw new NumberFormatException(part);
                long id = Long.parseLong(kv[0].trim());
                int qty = Integer.parseInt(kv[1].trim());
                long unit = Long.parseLong(kv[2].trim());
                if (qty <= 0 || unit < 0) throw new NumberFormatException(part);
                lines.add(new ShopService.OrderLine(id, qty, unit));
                total += unit * qty;
            }
        } catch (NumberFormatException e) {
            LOG.error("Plata {}: metadata coș invalidă, rambursez", sessionId);
            return refund(sess, sessionId, "Datele comenzii sunt invalide.");
        }
        AppUser user = users.findById(uid).orElse(null);
        if (user == null || lines.isEmpty() || total != paid) {
            LOG.error("Plata {}: utilizator lipsă sau sumă diferită (plătit {}, comandă {}), rambursez", sessionId, paid, total);
            return refund(sess, sessionId, "Comanda nu a putut fi validată.");
        }

        String email = sess.path("customer_email").asText(sess.path("customer_details").path("email").asText(""));
        String last4 = sess.path("payment_intent").path("payment_method").path("card").path("last4").asText("****");
        try {
            CustomerOrder o = shop.placeOrder(user, lines, shipping, md.path("name").asText(""), email, md.path("address").asText(""), last4, sessionId,
                new ShopService.Extras(disc, coupon, asm, xd));
            LOG.info("Plata {}: comandă {} creată", sessionId, o.invoiceNumber);
            return new Result(Status.ORDER, o, null);
        } catch (DataIntegrityViolationException dup) {                   // cerere paralelă (webhook + întoarcere) pentru aceeași sesiune
            return orders.findByStripeSessionId(sessionId).map(o -> new Result(Status.ORDER, o, null))
                .orElseGet(() -> new Result(Status.INVALID, null, "Comanda se procesează, reîncarcă pagina."));
        } catch (ShopService.OutOfStockException ex) {
            LOG.warn("Plata {}: {} Rambursez automat.", sessionId, ex.getMessage());
            return refund(sess, sessionId, ex.getMessage());
        }
    }

    private Result refund(JsonNode sess, String sessionId, String reason) {
        JsonNode pi = sess.path("payment_intent");
        String id = pi.path("id").asText(pi.asText(""));
        if (id.isBlank()) {
            LOG.error("Plata {}: nu găsesc payment_intent pentru rambursare", sessionId);
            return new Result(Status.INVALID, null, "Plata a fost încasată, dar comanda nu s-a putut crea. Contactează suportul cu referința " + sessionId + ".");
        }
        stripe.refund(id, sessionId);       // idempotent; la eroare Stripe aruncă IllegalStateException => reîncercare
        return new Result(Status.REFUNDED, null, reason + " Plata a fost rambursată automat.");
    }
}
