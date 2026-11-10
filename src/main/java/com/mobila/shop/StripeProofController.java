package com.mobila.shop;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Dovadă plăți Stripe pe site-ul publicat (I.10). Sub /api/admin/** => doar ADMIN (AdminGuard); nu expune date clienților public. */
@RestController
@RequestMapping("/api/admin/stripe")
public class StripeProofController {
    private final StripeClient stripe;

    public StripeProofController(StripeClient stripe) { this.stripe = stripe; }

    @GetMapping("/payments")
    public ResponseEntity<?> payments() {
        if (!stripe.enabled()) return Util.err(400, "Stripe nu este configurat pe server (lipsește STRIPE_SECRET_KEY).");
        try {
            return ResponseEntity.ok(Map.of("payments", view(stripe.listPaymentIntents(20)), "mode", stripe.mode()));
        } catch (IllegalStateException ex) {
            return Util.err(502, ex.getMessage());
        }
    }

    /** Creează din server o plată de test (card 4242) în contul Stripe sandbox, ca să apară automat în dashboard și în tabel. */
    @PostMapping("/test-payment")
    public ResponseEntity<?> testPayment() {
        if (!stripe.enabled()) return Util.err(400, "Stripe nu este configurat pe server (lipsește STRIPE_SECRET_KEY).");
        try {
            stripe.createTestPayment(289900, "MobilaViva: plată de test (card 4242)");
            return ResponseEntity.ok(Map.of("payments", view(stripe.listPaymentIntents(20)), "mode", stripe.mode()));
        } catch (IllegalStateException ex) {
            return Util.err(502, ex.getMessage());
        }
    }

    private static List<Map<String, Object>> view(JsonNode j) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (JsonNode s : j.path("data")) {
            if (!"succeeded".equals(s.path("status").asText())) continue;
            JsonNode card = s.path("payment_method").path("card");
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("session", s.path("id").asText());
            m.put("paymentIntent", s.path("id").asText());
            m.put("amount", s.path("amount_received").asLong() / 100.0);
            m.put("currency", s.path("currency").asText().toUpperCase());
            m.put("card", card.path("brand").asText("card").toUpperCase() + " **** " + card.path("last4").asText("****"));
            m.put("live", s.path("livemode").asBoolean());
            m.put("created", s.path("created").asLong());
            m.put("description", s.path("description").asText(""));
            out.add(m);
        }
        return out;
    }
}
