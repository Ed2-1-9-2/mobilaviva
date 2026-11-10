package com.mobila.shop;

import java.util.ArrayList;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** „Văzute recent” (id-urile vin din browser) și „Clienții au cumpărat și…” (din istoricul comenzilor). */
@RestController
public class RelatedController {
    private final ProductRepository products;
    private final OrderRepository orders;

    public RelatedController(ProductRepository products, OrderRepository orders) {
        this.products = products;
        this.orders = orders;
    }

    @GetMapping("/api/products/by-ids")
    public List<Product> byIds(@RequestParam(defaultValue = "") String ids) {
        List<Product> out = new ArrayList<>();
        String in = ids.length() > 200 ? ids.substring(0, 200) : ids;
        for (String part : in.split(",")) {
            try { products.findById(Long.parseLong(part.trim())).ifPresent(out::add); }
            catch (NumberFormatException ignore) { /* id invalid: se sare */ }
            if (out.size() >= 8) break;
        }
        return out;
    }

    @GetMapping("/api/products/{id}/also-bought")
    public List<Product> alsoBought(@PathVariable Long id) {
        List<Product> out = new ArrayList<>();
        for (Object[] row : orders.alsoBought(id)) {
            products.findById(((Number) row[0]).longValue()).filter(p -> p.stock > 0).ifPresent(out::add);
            if (out.size() >= 4) break;
        }
        return out;
    }
}
