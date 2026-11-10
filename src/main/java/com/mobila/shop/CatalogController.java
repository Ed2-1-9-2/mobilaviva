package com.mobila.shop;

import jakarta.persistence.criteria.Predicate;
import java.time.Duration;
import java.util.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class CatalogController {
    static final int PER_PAGE = 12;

    // Valori din URL în orice limbă (mesas, tische, precio-ascendente...) -> valorile românești din baza de date și din API
    private static final Map<String, String> CAT_ALIAS = alias(new String[] {
        "armadi=Dulapuri",
        "armarios=Dulapuri",
        "armoires=Dulapuri",
        "bambini=Copii",
        "banken=Canapele",
        "bedden=Paturi",
        "beds=Paturi",
        "beleuchtung=Iluminat",
        "betten=Paturi",
        "birouri=Birouri",
        "bureaus=Birouri",
        "bureaux=Birouri",
        "cadeiras=Scaune",
        "camas=Paturi",
        "canapele=Canapele",
        "canapes=Canapele",
        "cassettiere=Comode",
        "chairs=Scaune",
        "chaises=Scaune",
        "commodes=Comode",
        "comodas=Comode",
        "comode=Comode",
        "copii=Copii",
        "criancas=Copii",
        "desks=Birouri",
        "divani=Canapele",
        "dressers=Comode",
        "dulapuri=Dulapuri",
        "enfants=Copii",
        "entree=Hol",
        "escritorios=Birouri",
        "flur=Hol",
        "hal=Hol",
        "hall=Hol",
        "hallway=Hol",
        "hol=Hol",
        "illuminazione=Iluminat",
        "iluminacao=Iluminat",
        "iluminacion=Iluminat",
        "iluminat=Iluminat",
        "infantil=Copii",
        "ingresso=Hol",
        "kasten=Dulapuri",
        "kids=Copii",
        "kinder=Copii",
        "kinderen=Copii",
        "kommoden=Comode",
        "ladekasten=Comode",
        "letti=Paturi",
        "lighting=Iluminat",
        "lits=Paturi",
        "luminaires=Iluminat",
        "mesas=Mese",
        "mese=Mese",
        "paturi=Paturi",
        "recibidor=Hol",
        "roupeiros=Dulapuri",
        "scaune=Scaune",
        "schranke=Dulapuri",
        "schreibtische=Birouri",
        "scrivanie=Birouri",
        "secretarias=Birouri",
        "sedie=Scaune",
        "sillas=Scaune",
        "sofas=Canapele",
        "stoelen=Scaune",
        "stuhle=Scaune",
        "tables=Mese",
        "tafels=Mese",
        "tavoli=Mese",
        "tische=Mese",
        "verlichting=Iluminat",
        "wardrobes=Dulapuri" });
    private static final Map<String, String> SORT_ALIAS = alias(new String[] {
        "consegna-rapida=delivery",
        "delivery=delivery",
        "entrega-rapida=delivery",
        "fastest-delivery=delivery",
        "livraison-rapide=delivery",
        "livrare-rapida=delivery",
        "pertinence=relevance",
        "precio-ascendente=price_asc",
        "precio-descendente=price_desc",
        "preco-crescente=price_asc",
        "preco-decrescente=price_desc",
        "preis-absteigend=price_desc",
        "preis-aufsteigend=price_asc",
        "pret-crescator=price_asc",
        "pret-descrescator=price_desc",
        "prezzo-crescente=price_asc",
        "prezzo-decrescente=price_desc",
        "price-high-low=price_desc",
        "price-low-high=price_asc",
        "price_asc=price_asc",
        "price_desc=price_desc",
        "prijs-aflopend=price_desc",
        "prijs-oplopend=price_asc",
        "prix-croissant=price_asc",
        "prix-decroissant=price_desc",
        "relevance=relevance",
        "relevancia=relevance",
        "relevanta=relevance",
        "relevantie=relevance",
        "relevanz=relevance",
        "rilevanza=relevance",
        "schnellste-lieferung=delivery",
        "snelle-levering=delivery" });
    private static Map<String, String> alias(String[] pairs) {
        Map<String, String> m = new HashMap<>();
        for (String x : pairs) { int k = x.indexOf('='); m.put(x.substring(0, k), x.substring(k + 1)); }
        return m;
    }
    static String fold(String x) {
        return java.text.Normalizer.normalize(x == null ? "" : x, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).strip();
    }
    static String catKey(String v) { String r = CAT_ALIAS.get(fold(v)); return r != null ? r : (v == null ? "" : v); }
    static String sortKey(String v) { String r = SORT_ALIAS.get(fold(v)); return r != null ? r : (v == null ? "" : v); }
    private static final String[][] PALETTE = {
        {"#a1887f", "#5d4037"}, {"#90a4ae", "#37474f"}, {"#a5d6a7", "#2e7d32"}, {"#ffcc80", "#e65100"},
        {"#b39ddb", "#4527a0"}, {"#80cbc4", "#00695c"}, {"#ef9a9a", "#b71c1c"}, {"#90caf9", "#0d47a1"}};

    private final ProductRepository products;

    public CatalogController(ProductRepository products) { this.products = products; }

    private static List<String> tokens(String q) {
        return Arrays.stream(Product.fold(q).split("[^\\p{L}\\p{N}]+")).filter(s -> !s.isBlank()).toList();
    }

    private static Double num(String s) {
        try { return s == null || s.isBlank() ? null : Double.valueOf(s.replace(',', '.')); }
        catch (NumberFormatException e) { return null; }
    }

    private static Specification<Product> search(List<String> toks, boolean all) {
        return (r, cq, cb) -> {
            if (toks.isEmpty()) return cb.conjunction();
            List<Predicate> ps = new ArrayList<>();
            for (String t : toks) {
                String like = "%" + t + "%";
                ps.add(cb.like(r.<String>get("searchText"), like));
            }
            Predicate[] arr = ps.toArray(new Predicate[0]);
            return all ? cb.and(arr) : cb.or(arr);
        };
    }

    private static Specification<Product> filters(String category, String brand, Double lo, Double hi, boolean inStock, boolean promo) {
        Specification<Product> s = (r, cq, cb) -> cb.conjunction();
        if (!category.isEmpty()) s = s.and((r, cq, cb) -> cb.equal(r.get("category"), category));
        if (!brand.isEmpty()) s = s.and((r, cq, cb) -> cb.equal(r.get("brand"), brand));
        if (lo != null) s = s.and((r, cq, cb) -> cb.greaterThanOrEqualTo(r.<Double>get("price"), lo));
        if (hi != null) s = s.and((r, cq, cb) -> cb.lessThanOrEqualTo(r.<Double>get("price"), hi));
        if (inStock) s = s.and((r, cq, cb) -> cb.greaterThan(r.<Integer>get("stock"), 0));
        if (promo) s = s.and((r, cq, cb) -> cb.greaterThan(r.<Double>get("oldPrice"), r.<Double>get("price")));
        return s;
    }

    @GetMapping("/api/products")
    public Map<String, Object> list(@RequestParam(defaultValue = "") String q,
                                    @RequestParam(defaultValue = "") String category,
                                    @RequestParam(defaultValue = "") String brand,
                                    @RequestParam(name = "min_price", defaultValue = "") String minPrice,
                                    @RequestParam(name = "max_price", defaultValue = "") String maxPrice,
                                    @RequestParam(name = "in_stock", defaultValue = "0") int inStock,
                                    @RequestParam(defaultValue = "0") int promo,
                                    @RequestParam(defaultValue = "relevance") String sort,
                                    @RequestParam(defaultValue = "1") int page) {
        category = catKey(category); sort = sortKey(sort);
        List<String> toks = tokens(q);
        Specification<Product> base = filters(category, brand, num(minPrice), num(maxPrice), inStock == 1, promo == 1);
        Sort sortBy = switch (sort) {
            case "price_asc" -> Sort.by("price");
            case "price_desc" -> Sort.by("price").descending();
            case "delivery" -> Sort.by("deliveryDays");
            default -> Sort.by("rating").descending();
        };
        Pageable pageable = PageRequest.of(Math.max(page, 1) - 1, PER_PAGE, sortBy.and(Sort.by("id")));
        Page<Product> pg = products.findAll(base.and(search(toks, true)), pageable);
        boolean relaxed = false;
        if (pg.isEmpty() && toks.size() > 1) {            // căutare relaxată: măcar un termen
            pg = products.findAll(base.and(search(toks, false)), pageable);
            relaxed = true;
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("items", pg.getContent());
        out.put("total", pg.getTotalElements());
        out.put("pages", Math.max(1, pg.getTotalPages()));
        out.put("page", Math.max(page, 1));
        out.put("relaxed", relaxed);
        return out;
    }

    @GetMapping("/api/products/{id}")
    public ResponseEntity<Product> one(@PathVariable Long id) {
        return products.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    private static List<Map<String, Object>> rows(List<Object[]> r) {
        return r.stream().map(x -> Map.<String, Object>of("name", x[0], "count", x[1])).toList();
    }

    @GetMapping("/api/facets")
    public Map<String, Object> facets() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("categories", rows(products.categoryCounts()));
        out.put("brands", rows(products.brandCounts()));
        return out;
    }

    @GetMapping("/api/suggest")
    public List<Map<String, Object>> suggest(@RequestParam(defaultValue = "") String q) {
        List<String> toks = tokens(q);
        if (toks.isEmpty()) return List.of();
        return products.findAll(search(toks, true), PageRequest.of(0, 6)).stream()
            .map(p -> Map.<String, Object>of("id", p.id, "name", p.name)).toList();
    }

    /** Ilustrație de mobilier generată pentru produs (ușoară, cache-uită, fără fișiere externe). Fotografiile reale se setează din Admin (URL imagine). */
    @GetMapping(value = "/img/{id}.svg", produces = "image/svg+xml")
    public ResponseEntity<String> img(@PathVariable Long id) {
        Product p = products.findById(id).orElse(null);
        if (p == null) return ResponseEntity.notFound().build();
        String svg = FurnitureArt.svg(p.name, p.category, p.material, p.color);
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic()).body(svg);
    }
}
