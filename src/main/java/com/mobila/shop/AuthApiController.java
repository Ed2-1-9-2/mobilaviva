package com.mobila.shop;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {
    public record LoginReq(String email, String password) {}
    public record RegisterReq(String name, String email, String password, Boolean acceptTerms) {}
    public record ForgotReq(String email, String origin) {}
    public record ResetReq(String token, String password) {}
    public record ChangeReq(String current, String password) {}

    static final int MAX_PASSWORD = 128;
    static final long TOKEN_MINUTES = 30;
    static final long TEN_MIN = 600_000L, ONE_HOUR = 3_600_000L;
    private static final SecureRandom RND = new SecureRandom();

    private final UserRepository users;
    private final PasswordResetRepository resets;
    private final MailService mail;
    private final String baseUrl;

    public AuthApiController(UserRepository users, PasswordResetRepository resets, MailService mail,
                             @Value("${app.base-url:http://localhost:8000}") String baseUrl) {
        this.users = users;
        this.resets = resets;
        this.mail = mail;
        this.baseUrl = baseUrl;
    }

    /** Coșul din sesiune (invitat) + coșul salvat pe cont = un singur coș, salvat înapoi pe cont. */
    @SuppressWarnings("unchecked")
    private void mergeSavedCart(HttpSession s, AppUser u) {
        Object o = s.getAttribute("cart");
        if (o == null) { o = new java.util.concurrent.ConcurrentSkipListMap<Long, Integer>(); s.setAttribute("cart", o); }
        Map<Long, Integer> cart = (Map<Long, Integer>) o;
        if (u.cartData != null && !u.cartData.isBlank()) {
            for (String part : u.cartData.split(",")) {
                String[] kv = part.split(":");
                if (kv.length != 2) continue;
                try {
                    long id = Long.parseLong(kv[0].trim());
                    int q = Math.min(Math.max(Integer.parseInt(kv[1].trim()), 1), 99);
                    cart.merge(id, q, (a, b) -> Math.min(Math.max(a, b), 99));
                } catch (NumberFormatException ignored) { }
            }
        }
        String d = cart.entrySet().stream().map(e -> e.getKey() + ":" + e.getValue()).collect(java.util.stream.Collectors.joining(","));
        if (!d.equals(u.cartData == null ? "" : u.cartData)) { u.cartData = d; users.save(u); }
    }

    private ResponseEntity<?> loginAs(HttpServletRequest req, AppUser u) {
        HttpSession s = req.getSession();
        req.changeSessionId();               // previne session fixation
        s.setAttribute("uid", u.id);
        mergeSavedCart(s, u);
        return ResponseEntity.ok(Util.userView(u));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginReq r, HttpServletRequest req) {
        String mail = r.email() == null ? "" : r.email().trim().toLowerCase();
        String pass = r.password() == null ? "" : r.password();
        if (mail.length() > 160 || pass.length() > MAX_PASSWORD) return Util.err(401, "Email sau parolă incorectă.");
        Optional<AppUser> u = users.findByEmail(mail);
        if (u.isPresent() && !pass.isEmpty() && Util.verify(pass, u.get().passwordHash)) {
            AppUser user = u.get();
            if (Util.needsRehash(user.passwordHash)) {          // migrare la numărul curent de iterații
                user.passwordHash = Util.hash(pass);
                users.save(user);
            }
            return loginAs(req, user);
        }
        if (u.isEmpty()) Util.burn(pass);                       // același timp de răspuns ca pentru un email existent
        return Util.err(401, "Email sau parolă incorectă.");
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterReq r, HttpServletRequest req) {
        String name = r.name() == null ? "" : r.name().trim();
        String mail = r.email() == null ? "" : r.email().trim().toLowerCase();
        String pass = r.password() == null ? "" : r.password();
        List<String> errors = new ArrayList<>();
        if (name.length() < 2 || name.length() > 120) errors.add("Introdu numele (2-120 de caractere).");
        if (mail.length() > 160 || !mail.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) errors.add("Adresa de email nu este validă.");
        if (pass.length() < 8) errors.add("Parola trebuie să aibă minim 8 caractere.");
        if (pass.length() > MAX_PASSWORD) errors.add("Parola poate avea cel mult " + MAX_PASSWORD + " de caractere.");
        if (!Boolean.TRUE.equals(r.acceptTerms())) errors.add("Trebuie să accepți Termenii și condițiile și Politica de confidențialitate.");
        if (errors.isEmpty() && users.findByEmail(mail).isPresent()) errors.add("Există deja un cont cu acest email.");
        if (!errors.isEmpty()) return Util.err(400, errors.toArray(new String[0]));
        AppUser u = new AppUser();
        u.name = name; u.email = mail; u.passwordHash = Util.hash(pass);
        u.termsAcceptedAt = java.time.LocalDateTime.now();
        users.save(u);
        return loginAs(req, u);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpSession s) {
        s.invalidate();
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------------ parola uitată (link prin email)

    private static String sha256(String x) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(x.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static List<String> passwordErrors(String pass) {
        List<String> e = new ArrayList<>();
        if (pass.length() < 8) e.add("Parola trebuie să aibă minim 8 caractere.");
        if (pass.length() > MAX_PASSWORD) e.add("Parola poate avea cel mult " + MAX_PASSWORD + " de caractere.");
        return e;
    }

    /** Linkul din email trebuie să ducă pe domeniul din care a cerut utilizatorul (Vercel), nu pe fly.dev; origin-ul e verificat pe listă albă. */
    private String linkBase(String origin) {
        String base = baseUrl == null ? "" : baseUrl.strip();
        while (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        String o = origin == null ? "" : origin.trim();
        while (o.endsWith("/")) o = o.substring(0, o.length() - 1);
        Set<String> allowed = new HashSet<>(List.of(base, "https://mobilaviva.vercel.app"));
        String extra = System.getenv("EXTRA_ORIGINS");
        if (extra != null) for (String x : extra.split(",")) if (!x.isBlank()) allowed.add(x.strip());
        return allowed.contains(o) ? o : base;
    }

    private void sendAsync(AppUser u, String subject, String text) {
        final String to = u.email, name = u.name;
        CompletableFuture.runAsync(() -> mail.send(to, name, subject, text));   // răspunsul HTTP nu depinde de viteza Brevo
    }

    @PostMapping("/forgot")
    public ResponseEntity<?> forgot(@RequestBody ForgotReq r, HttpServletRequest req) {
        // Răspuns neutru: utilizatorul primește mereu același mesaj, indiferent dacă adresa are cont,
        // dacă rate-limit s-a atins sau dacă trimiterea a eșuat. Erorile tehnice rămân doar în logul serverului.
        ResponseEntity<?> neutral = ResponseEntity.ok(Map.of("ok", true));
        String email = r.email() == null ? "" : r.email().trim().toLowerCase();
        if (email.length() > 160 || !email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+"))
            return Util.err(400, "Adresa de email nu este validă.");
        String sk = "forgot-s:" + req.getSession().getId(), ek = "forgot-e:" + email, gk = "forgot-all";
        if (RateLimiter.count(sk, ONE_HOUR) >= 3 || RateLimiter.count(ek, ONE_HOUR) >= 3 || RateLimiter.count(gk, ONE_HOUR) >= 60) {
            org.slf4j.LoggerFactory.getLogger(AuthApiController.class).warn("forgot: rate-limit atins pentru {} (răspuns neutru).", email);
            return neutral;
        }
        RateLimiter.hit(sk, ONE_HOUR); RateLimiter.hit(ek, ONE_HOUR); RateLimiter.hit(gk, ONE_HOUR);
        Optional<AppUser> u = users.findByEmail(email);
        if (u.isEmpty()) {
            org.slf4j.LoggerFactory.getLogger(AuthApiController.class).info("forgot: nicio adresă {} înregistrată (răspuns neutru).", email);
            return neutral;
        }
        if (!mail.configured()) {
            org.slf4j.LoggerFactory.getLogger(AuthApiController.class).error("forgot: nicio metodă de trimitere email nu e configurată; completează SMTP_USER/SMTP_PASSWORD. (răspuns neutru)");
            return neutral;
        }
        byte[] raw = new byte[32];
        RND.nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        resets.deleteByUserId(u.get().id);                    // o cerere nouă o anulează pe cea veche
        PasswordReset pr = new PasswordReset();
        pr.userId = u.get().id;
        pr.tokenHash = sha256(token);
        pr.expiresAt = LocalDateTime.now().plusMinutes(TOKEN_MINUTES);
        resets.save(pr);
        final String to = u.get().email, name = u.get().name;
        final String body = "Bună, " + name + "!\n\nAm primit o cerere de resetare a parolei pentru contul tău MobilaViva.\n"
                + "Alege o parolă nouă din linkul de mai jos (valabil " + TOKEN_MINUTES + " de minute, se poate folosi o singură dată):\n\n"
                + linkBase(r.origin()) + "/reset?token=" + token + "\n\n"
                + "Dacă nu ai cerut tu resetarea, ignoră acest mesaj: parola ta rămâne neschimbată.\n\nEchipa MobilaViva";
        // Trimitere asincronă: răspunsul HTTP nu depinde de SMTP; dacă toate metodele eșuează, invalidăm tokenul tăcut.
        final Long userId = u.get().id;
        CompletableFuture.runAsync(() -> {
            boolean sent = mail.send(to, name, "Resetare parolă MobilaViva", body);
            if (!sent) {
                resets.deleteByUserId(userId);
                org.slf4j.LoggerFactory.getLogger(AuthApiController.class).error("forgot: trimiterea emailului a eșuat pentru {} -> {} (răspuns neutru deja întors)", to, mail.lastError());
            }
        });
        return neutral;
    }

    @PostMapping("/reset")
    public ResponseEntity<?> reset(@RequestBody ResetReq r, HttpServletRequest req) {
        String token = r.token() == null ? "" : r.token().trim();
        String pass = r.password() == null ? "" : r.password();
        String key = "reset:" + req.getSession().getId();
        if (RateLimiter.count(key, TEN_MIN) >= 5) return Util.err(429, "Prea multe încercări. Încearcă din nou peste 10 minute.");
        List<String> errors = passwordErrors(pass);
        if (!errors.isEmpty()) return Util.err(400, errors.toArray(new String[0]));
        String invalid = "Linkul de resetare nu este valid sau a expirat. Cere unul nou.";
        if (token.length() < 20 || token.length() > 100) { RateLimiter.hit(key, TEN_MIN); return Util.err(400, invalid); }
        String h = sha256(token);
        Optional<PasswordReset> pr = resets.findByTokenHash(h);
        if (pr.isEmpty() || pr.get().expiresAt == null || pr.get().expiresAt.isBefore(LocalDateTime.now())) {
            RateLimiter.hit(key, TEN_MIN);
            return Util.err(400, invalid);
        }
        Optional<AppUser> u = users.findById(pr.get().userId);
        if (u.isEmpty() || resets.deleteByTokenHash(h) != 1) {     // token folosit o singură dată, chiar și la cereri paralele
            RateLimiter.hit(key, TEN_MIN);
            return Util.err(400, invalid);
        }
        AppUser user = u.get();
        user.passwordHash = Util.hash(pass);
        user.passwordChangedAt = LocalDateTime.now();
        users.save(user);
        resets.deleteByUserId(user.id);
        sendAsync(user, "Parola ți-a fost schimbată",
            "Bună, " + user.name + "!\n\nParola contului tău MobilaViva tocmai a fost schimbată.\n"
            + "Dacă nu ai făcut tu această schimbare, cere imediat o resetare a parolei sau scrie-ne la contact@mobilaviva.com.\n\nEchipa MobilaViva");
        return loginAs(req, user);                                  // autentificare automată: linkul din email dovedește deținerea adresei
    }

    // ------------------------------------------------------------------ schimbare parolă cu contul deschis

    @PostMapping("/password")
    public ResponseEntity<?> change(@RequestBody ChangeReq r, HttpServletRequest req) {
        Object id = req.getSession().getAttribute("uid");
        Optional<AppUser> u = id == null ? Optional.empty() : users.findById((Long) id);
        if (u.isEmpty()) return Util.err(401, "Autentificare necesară.");
        AppUser user = u.get();
        String key = "chg:" + user.id;                             // pe cont, nu pe sesiune: o sesiune nouă nu resetează contorul
        if (RateLimiter.count(key, TEN_MIN) >= 5) return Util.err(429, "Prea multe încercări greșite. Încearcă din nou peste 10 minute.");
        String cur = r.current() == null ? "" : r.current();
        String pass = r.password() == null ? "" : r.password();
        if (cur.isEmpty() || cur.length() > MAX_PASSWORD || !Util.verify(cur, user.passwordHash)) {
            RateLimiter.hit(key, TEN_MIN);
            return Util.err(400, "Parola actuală este incorectă.");
        }
        List<String> errors = passwordErrors(pass);
        if (errors.isEmpty() && pass.equals(cur)) errors.add("Parola nouă trebuie să fie diferită de cea actuală.");
        if (!errors.isEmpty()) return Util.err(400, errors.toArray(new String[0]));
        user.passwordHash = Util.hash(pass);
        user.passwordChangedAt = LocalDateTime.now();
        users.save(user);
        resets.deleteByUserId(user.id);                            // linkurile de resetare vechi nu mai sunt valabile
        RateLimiter.clear(key);
        req.changeSessionId();
        if (mail.configured())
            sendAsync(user, "Parola ți-a fost schimbată",
                "Bună, " + user.name + "!\n\nParola contului tău MobilaViva tocmai a fost schimbată.\n"
                + "Dacă nu ai făcut tu această schimbare, cere imediat o resetare a parolei sau scrie-ne la contact@mobilaviva.com.\n\nEchipa MobilaViva");
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
