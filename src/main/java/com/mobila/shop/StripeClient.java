package com.mobila.shop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Client minimal pentru Stripe Checkout (API REST, fără SDK).
 * Activ doar dacă STRIPE_SECRET_KEY este setat (sk_test_... pentru mod test); altfel aplicația folosește cardul simulat.
 */
@Component
public class StripeClient {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Pattern SESSION_ID = Pattern.compile("cs_[A-Za-z0-9_]+");
    private static final String API = "https://api.stripe.com/v1/checkout/sessions";
    private final HttpClient http = HttpClient.newHttpClient();

    @Value("${stripe.secret-key:}")
    private String secretKey;

    @Value("${stripe.webhook-secret:}")
    private String webhookSecret;

    @Value("${app.base-url:http://localhost:8000}")
    private String baseUrl;

    public boolean enabled() { return secretKey != null && secretKey.startsWith("sk_"); }

    public String baseUrl() { return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl; }

    /** Suma totală a coșului în bani (cenți), calculată identic cu liniile trimise către Stripe. */
    public static long cents(Cart cart) {
        long total = 0;
        for (CartLine l : cart.getLines()) total += Math.round(l.getProduct().price * 100) * l.getQty();
        if (cart.getShipping() > 0) total += Math.round(cart.getShipping() * 100);
        total += Math.round(cart.getAssembly() * 100) - Math.round(cart.getDiscount() * 100);
        return total;
    }

    private static String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }

    private JsonNode send(HttpRequest.Builder b) {
        try {
            HttpResponse<String> res = http.send(
                b.header("Authorization", "Bearer " + secretKey).timeout(Duration.ofSeconds(20)).build(),
                HttpResponse.BodyHandlers.ofString());
            JsonNode body = JSON.readTree(res.body());
            if (res.statusCode() >= 400)
                throw new IllegalStateException("Stripe: " + body.path("error").path("message").asText("eroare necunoscută"));
            return body;
        } catch (IOException e) {
            throw new IllegalStateException("Stripe indisponibil: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Cerere Stripe întreruptă.");
        }
    }

    private static void addLine(Map<String, String> f, int i, String name, long unitAmount, int qty) {
        String k = "line_items[" + i + "]";
        f.put(k + "[quantity]", String.valueOf(qty));
        f.put(k + "[price_data][currency]", "ron");
        f.put(k + "[price_data][unit_amount]", String.valueOf(unitAmount));
        f.put(k + "[price_data][product_data][name]", name);
    }

    public JsonNode createCheckoutSession(Cart cart, String email, Long userId, String name, String address, String successUrl, String cancelUrl) {
        return createCheckoutSession(cart, email, userId, name, address, successUrl, cancelUrl, "ro");
    }

    private static final java.util.Map<String, String> SHIP_LABEL = java.util.Map.of("ro", "Transport", "en", "Shipping", "de", "Versand", "fr", "Livraison",
        "it", "Spedizione", "pt", "Envio", "es", "Envío", "nl", "Verzending");

    public JsonNode createCheckoutSession(Cart cart, String email, Long userId, String name, String address, String successUrl, String cancelUrl, String lang) {
        String loc = lang == null ? "ro" : lang.toLowerCase();
        if (!SHIP_LABEL.containsKey(loc)) loc = "ro";      // limbi acceptate de Stripe Checkout și de site
        Map<String, String> f = new LinkedHashMap<>();
        f.put("mode", "payment");
        f.put("locale", loc);
        f.put("payment_method_types[0]", "card");
        f.put("success_url", successUrl);
        f.put("cancel_url", cancelUrl);
        f.put("customer_email", email);
        f.put("client_reference_id", String.valueOf(userId));
        // metadata: permit webhook-ului să creeze comanda fără sesiunea HTTP a clientului
        // coșul se salvează în metadata cu prețul unitar în cenți (id:cantitate:preț); comanda se creează exact pe aceste valori
        StringBuilder cartMeta = new StringBuilder();
        for (CartLine l : cart.getLines())
            cartMeta.append(cartMeta.length() > 0 ? "," : "").append(l.getProduct().id).append(':').append(l.getQty())
                .append(':').append(Math.round(l.getProduct().price * 100));
        if (cartMeta.length() > 5 * 450) throw new IllegalStateException("Coșul are prea multe produse diferite pentru plata online.");
        for (int c = 0; c * 450 < cartMeta.length(); c++)           // Stripe: max. 500 de caractere / valoare de metadata
            f.put("metadata[cart" + c + "]", cartMeta.substring(c * 450, Math.min(cartMeta.length(), (c + 1) * 450)));
        f.put("metadata[ship]", String.valueOf(Math.round(cart.getShipping() * 100)));
        long discC = Math.round(cart.getDiscount() * 100), asmC = Math.round(cart.getAssembly() * 100);
        f.put("metadata[disc]", String.valueOf(discC));
        f.put("metadata[asm]", String.valueOf(asmC));
        f.put("metadata[xd]", cart.isAssemblyOn() ? "1" : "0");
        if (cart.getCouponCode() != null) f.put("metadata[coupon]", cart.getCouponCode());
        f.put("metadata[name]", name.length() > 400 ? name.substring(0, 400) : name);
        f.put("metadata[address]", address.length() > 400 ? address.substring(0, 400) : address);
        int i = 0;
        for (CartLine l : cart.getLines()) addLine(f, i++, l.getProduct().name, Math.round(l.getProduct().price * 100), l.getQty());
        if (cart.getShipping() > 0) addLine(f, i++, SHIP_LABEL.get(loc), Math.round(cart.getShipping() * 100), 1);
        if (asmC > 0) addLine(f, i++, ASM_LABEL.get(loc), asmC, 1);
        if (discC > 0) f.put("discounts[0][coupon]", createCoupon(discC, cart.getCouponCode()));   // reducerea apare în Stripe Checkout, totalul plătit = totalul coșului
        String body = f.entrySet().stream().map(e -> enc(e.getKey()) + "=" + enc(e.getValue())).collect(Collectors.joining("&"));
        return send(HttpRequest.newBuilder(URI.create(API))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body)));
    }

    private static final java.util.Map<String, String> ASM_LABEL = java.util.Map.of("ro", "Montaj", "en", "Assembly", "de", "Montage", "fr", "Montage",
        "it", "Montaggio", "pt", "Montagem", "es", "Montaje", "nl", "Montage");

    /** Cupon Stripe de unică folosință (sumă fixă în RON) pentru reducerea din coș. */
    private String createCoupon(long amountOffCents, String name) {
        String body = "amount_off=" + amountOffCents + "&currency=ron&duration=once&max_redemptions=1&name=" + enc(name == null ? "Reducere" : name);
        return send(HttpRequest.newBuilder(URI.create("https://api.stripe.com/v1/coupons"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body))).path("id").asText();
    }

    /** Verifică antetul Stripe-Signature (HMAC-SHA256 pe "t.payload", toleranță 5 minute). */
    public boolean verifySignature(byte[] payload, String header) {
        if (webhookSecret == null || webhookSecret.isBlank() || header == null) return false;
        String t = null;
        List<String> sigs = new ArrayList<>();
        for (String part : header.split(",")) {
            String[] kv = part.trim().split("=", 2);
            if (kv.length != 2) continue;
            if (kv[0].equals("t")) t = kv[1]; else if (kv[0].equals("v1")) sigs.add(kv[1]);
        }
        if (t == null || sigs.isEmpty()) return false;
        try {
            if (Math.abs(System.currentTimeMillis() / 1000 - Long.parseLong(t)) > 300) return false;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            mac.update((t + ".").getBytes(StandardCharsets.UTF_8));
            byte[] expected = HexFormat.of().formatHex(mac.doFinal(payload)).getBytes(StandardCharsets.UTF_8);
            for (String sig : sigs) if (MessageDigest.isEqual(expected, sig.getBytes(StandardCharsets.UTF_8))) return true;
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    /** Rambursare totală a plății (cheie de idempotență pe sesiune: apelurile repetate returnează aceeași rambursare). */
    public JsonNode refund(String paymentIntentId, String sessionId) {
        String body = "payment_intent=" + enc(paymentIntentId) + "&reason=requested_by_customer&metadata%5Bsession%5D=" + enc(sessionId);
        return send(HttpRequest.newBuilder(URI.create("https://api.stripe.com/v1/refunds"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Idempotency-Key", "refund-" + sessionId)
            .POST(HttpRequest.BodyPublishers.ofString(body)));
    }

    /** Ultimele sesiuni de plată din contul Stripe (pentru pagina de dovadă, doar admin). */
    public JsonNode listSessions(int n) {
        return send(HttpRequest.newBuilder(URI.create(API + "?limit=" + Math.max(1, Math.min(n, 50)) + "&expand%5B%5D=data.payment_intent.payment_method")).GET());
    }

    /** Plăți (PaymentIntent) din contul Stripe: acoperă și Checkout, și plățile de test create din cod. */
    public JsonNode listPaymentIntents(int n) {
        return send(HttpRequest.newBuilder(URI.create("https://api.stripe.com/v1/payment_intents?limit=" + Math.max(1, Math.min(n, 50)) + "&expand%5B%5D=data.payment_method")).GET());
    }

    /** Plată de TEST creată direct din server (card de test pm_card_visa = 4242): apare automat în Stripe sandbox. Doar cu sk_test_. */
    public JsonNode createTestPayment(long cents, String description) {
        if (!"TEST".equals(mode())) throw new IllegalStateException("Plata de test se poate crea doar cu o cheie sk_test_.");
        String body = "amount=" + cents + "&currency=ron&payment_method=pm_card_visa&confirm=true"
            + "&automatic_payment_methods%5Benabled%5D=true&automatic_payment_methods%5Ballow_redirects%5D=never"
            + "&description=" + enc(description) + "&expand%5B%5D=payment_method";
        return send(HttpRequest.newBuilder(URI.create("https://api.stripe.com/v1/payment_intents"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body)));
    }

    public String mode() { return secretKey != null && secretKey.startsWith("sk_test_") ? "TEST" : "LIVE"; }

    public JsonNode retrieveSession(String id) {
        if (id == null || !SESSION_ID.matcher(id).matches()) throw new IllegalStateException("ID de sesiune invalid.");
        return send(HttpRequest.newBuilder(URI.create(API + "/" + id + "?expand%5B%5D=payment_intent.payment_method")).GET());
    }
}
