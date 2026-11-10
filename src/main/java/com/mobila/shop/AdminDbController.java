package com.mobila.shop;

import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Vizualizare a bazei de date (doar citire) pentru print screen 02, pe site-ul publicat.
 * Sub /api/admin/** => AdminGuard cere sesiune ADMIN (altfel 403). Fără SQL de la client: tabelele sunt o listă fixă,
 * iar interogările sunt constante (doar COUNT și un eșantion de 3 rânduri). Coloanele secrete sunt mascate.
 */
@RestController
@RequestMapping("/api/admin/db")
public class AdminDbController {
    private static final List<String> TABLES = List.of("product", "app_users", "orders", "order_items", "wish_items", "invoice_seq");
    private static final Set<String> SECRET = Set.of("password_hash");
    private static final int SAMPLE = 3, MAX_CELL = 48;

    private final JdbcTemplate jdbc;

    public AdminDbController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @GetMapping
    public Map<String, Object> overview() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("engine", jdbc.execute((ConnectionCallback<String>) c ->
            c.getMetaData().getDatabaseProductName() + " " + c.getMetaData().getDatabaseProductVersion()));
        List<Map<String, Object>> tables = new ArrayList<>();
        for (String t : TABLES) {
            try {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("name", t);
                m.put("rows", jdbc.queryForObject("SELECT COUNT(*) FROM " + t, Long.class));
                List<String> cols = new ArrayList<>();
                List<List<String>> sample = jdbc.query("SELECT * FROM " + t + " ORDER BY 1 DESC FETCH FIRST " + SAMPLE + " ROWS ONLY", rs -> {
                    ResultSetMetaData md = rs.getMetaData();
                    int n = md.getColumnCount();
                    for (int i = 1; i <= n; i++) cols.add(md.getColumnLabel(i).toLowerCase());
                    List<List<String>> rows = new ArrayList<>();
                    while (rs.next()) {
                        List<String> r = new ArrayList<>();
                        for (int i = 1; i <= n; i++) {
                            Object v = rs.getObject(i);
                            String x = SECRET.contains(cols.get(i - 1)) ? "•••• (hash, ascuns)" : v == null ? "NULL" : String.valueOf(v);
                            r.add(x.length() > MAX_CELL ? x.substring(0, MAX_CELL) + "…" : x);
                        }
                        rows.add(r);
                    }
                    return rows;
                });
                m.put("columns", cols);
                m.put("sample", sample);
                tables.add(m);
            } catch (RuntimeException e) { /* tabel încă inexistent: se omite */ }
        }
        out.put("tables", tables);
        return out;
    }
}
