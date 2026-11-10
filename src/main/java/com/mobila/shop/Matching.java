package com.mobila.shop;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Set asortat / „completează camera” (personalizare).
 * 1) piese din aceeași colecție: cuvântul-colecție este un cuvânt cu majusculă din numele produsului (nu primul), ex. „Luna”, „Oslo”;
 * 2) apoi, din categoriile complementare (ex. pat -> noptieră, dulap), cel mai bine cotat produs aflat în stoc.
 */
public final class Matching {
    private Matching() {}

    public static final int MAX = 4;

    public record Match(Product product, String reason) {}

    public record Result(String collection, List<Match> items) {}

    private static final Map<String, List<String>> COMPLEMENT = Map.of(
        "Canapele", List.of("Mese", "Comode", "Iluminat"),
        "Paturi", List.of("Comode", "Dulapuri", "Iluminat"),
        "Dulapuri", List.of("Paturi", "Comode"),
        "Comode", List.of("Paturi", "Canapele", "Iluminat"),
        "Mese", List.of("Scaune", "Iluminat"),
        "Scaune", List.of("Mese", "Birouri"),
        "Birouri", List.of("Scaune", "Iluminat"),
        "Iluminat", List.of("Canapele", "Paturi"));

    static Set<String> collectionTokens(String name) {
        Set<String> out = new LinkedHashSet<>();
        String[] w = (name == null ? "" : name).split("[^\\p{L}\\p{N}]+");
        for (int i = 1; i < w.length; i++) {
            String t = w[i];
            if (t.length() >= 3 && Character.isUpperCase(t.charAt(0)) && t.chars().allMatch(Character::isLetter)) out.add(t);
        }
        return out;
    }

    public static Result find(Product p, List<Product> all) {
        List<Match> out = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        seen.add(p.id);
        String collection = null;
        for (String t : collectionTokens(p.name)) {
            List<Product> same = new ArrayList<>();
            for (Product o : all)
                if (!seen.contains(o.id) && o.stock > 0 && collectionTokens(o.name).contains(t)) same.add(o);
            if (!same.isEmpty()) {
                collection = t;
                for (Product o : same) {
                    if (out.size() >= MAX) break;
                    seen.add(o.id);
                    out.add(new Match(o, "Aceeași colecție: " + t));
                }
                break;
            }
        }
        for (String c : COMPLEMENT.getOrDefault(p.category, List.of())) {
            if (out.size() >= MAX) break;
            Product best = null;
            for (Product o : all)
                if (c.equals(o.category) && o.stock > 0 && !seen.contains(o.id) && (best == null || o.rating > best.rating)) best = o;
            if (best != null) {
                seen.add(best.id);
                out.add(new Match(best, "Se asortează: " + c.toLowerCase(Locale.ROOT)));
            }
        }
        return new Result(collection, out);
    }
}
