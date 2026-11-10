package com.mobila.shop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Webhook Stripe: checkout.session.completed -> comandă + factură (idempotent, semnătură verificată, rambursare dacă stocul a dispărut). */
@RestController
@RequestMapping("/api/stripe")
public class StripeWebhookController {
    private static final Logger LOG = LoggerFactory.getLogger(StripeWebhookController.class);
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final int MAX_BODY = 1_000_000;

    private final StripeClient stripe;
    private final PaymentService payments;

    public StripeWebhookController(StripeClient stripe, PaymentService payments) {
        this.stripe = stripe; this.payments = payments;
    }

    @PostMapping("/webhook")
    public ResponseEntity<?> webhook(HttpServletRequest req) throws IOException {
        byte[] body = req.getInputStream().readNBytes(MAX_BODY + 1);     // octeți bruți: semnătura se calculează pe payload-ul exact
        if (body.length > MAX_BODY) return Util.err(413, "Cerere prea mare.");
        if (!stripe.verifySignature(body, req.getHeader("Stripe-Signature")))
            return Util.err(400, "Semnătură Stripe invalidă sau webhook neconfigurat.");
        JsonNode ev = JSON.readTree(body);
        String type = ev.path("type").asText();
        if (type.equals("checkout.session.completed") || type.equals("checkout.session.async_payment_succeeded")) {
            String sid = ev.path("data").path("object").path("id").asText("");
            if (!sid.isBlank()) {
                try {
                    PaymentService.Result r = payments.complete(sid, null);
                    LOG.info("Webhook Stripe {}: {}", sid, r.status());
                } catch (RuntimeException ex) {
                    LOG.error("Webhook Stripe {}: eroare temporară, Stripe va reîncerca", sid, ex);
                    return Util.err(500, "Eroare temporară.");          // non-2xx => Stripe reîncearcă; operația e idempotentă
                }
            }
        }
        return ResponseEntity.ok(Map.of("received", true));
    }
}
