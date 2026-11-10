package com.mobila.shop;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.springframework.http.ResponseEntity;

public final class Util {
    private Util() {}

    public static ResponseEntity<Map<String, Object>> err(int status, String... messages) {
        return ResponseEntity.status(status).body(Map.<String, Object>of("errors", List.of(messages)));
    }

    public static Map<String, Object> userView(AppUser u) {
        return Map.<String, Object>of("id", u.id, "name", u.name, "email", u.email, "admin", u.isAdmin());
    }

    /** Iterații PBKDF2 pentru parolele noi (OWASP 2023: 600.000). Numărul se păstrează în hash, deci poate fi crescut ulterior. */
    static final int ITERATIONS = 210_000;
    /** Hash-urile vechi (format „salt$hash”) foloseau 120.000 de iterații. */
    static final int LEGACY_ITERATIONS = 120_000;
    private static final byte[] DUMMY_SALT = new byte[16];

    public static String hash(String password) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return "pbkdf2$" + ITERATIONS + "$" + Base64.getEncoder().encodeToString(salt) + "$"
            + Base64.getEncoder().encodeToString(pbkdf2(password, salt, ITERATIONS));
    }

    public static boolean verify(String password, String stored) {
        try {
            String[] p = stored.split("\\$");
            int iter; byte[] salt; byte[] expected;
            if (p.length == 4 && p[0].equals("pbkdf2")) {
                iter = Integer.parseInt(p[1]); salt = Base64.getDecoder().decode(p[2]); expected = Base64.getDecoder().decode(p[3]);
                if (iter < LEGACY_ITERATIONS || iter > 5_000_000) return false;
            } else if (p.length == 2) {               // format vechi
                iter = LEGACY_ITERATIONS; salt = Base64.getDecoder().decode(p[0]); expected = Base64.getDecoder().decode(p[1]);
            } else return false;
            return MessageDigest.isEqual(expected, pbkdf2(password, salt, iter));
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** true dacă hash-ul e în format vechi sau cu mai puține iterații: se recalculează la prima autentificare reușită. */
    public static boolean needsRehash(String stored) {
        return stored == null || !stored.startsWith("pbkdf2$" + ITERATIONS + "$");
    }

    /** Consumă același timp ca o verificare reală (email inexistent nu trebuie să se vadă după timpul de răspuns). */
    public static void burn(String password) {
        pbkdf2(password == null ? "" : password, DUMMY_SALT, ITERATIONS);
    }

    private static byte[] pbkdf2(String password, byte[] salt, int iterations) {
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(new PBEKeySpec(password.toCharArray(), salt, iterations, 256)).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
