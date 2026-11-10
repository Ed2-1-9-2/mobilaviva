package com.mobila.shop;

import java.net.URI;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        adaptHostingEnv();
        SpringApplication.run(Application.class, args);
    }

    /**
     * Hosting-urile (Render, Railway, Heroku) dau DATABASE_URL ca postgresql://user:parola@host:port/db,
     * iar JDBC cere jdbc:postgresql://host:port/db. Tot aici se deduce adresa publică pentru întoarcerea de la Stripe.
     */
    static void adaptHostingEnv() {
        String db = System.getenv("DATABASE_URL");
        if (db != null && (db.startsWith("postgres://") || db.startsWith("postgresql://"))) {
            try {
                URI u = URI.create(db);
                int port = u.getPort() > 0 ? u.getPort() : 5432;
                String rq = u.getRawQuery() == null ? "" : u.getRawQuery();
                // Neon dă și channel_binding=require (parametru libpq, necunoscut pentru JDBC): îl scoatem
                rq = java.util.Arrays.stream(rq.split("&")).filter(x -> !x.isEmpty() && !x.startsWith("channel_binding=")).collect(java.util.stream.Collectors.joining("&"));
                String q = rq.isEmpty() ? "" : "?" + rq;
                System.setProperty("DATABASE_URL", "jdbc:postgresql://" + u.getHost() + ":" + port + u.getPath() + q);
                if (u.getUserInfo() != null) {
                    String[] ui = u.getUserInfo().split(":", 2);
                    System.setProperty("DB_USER", ui[0]);
                    if (ui.length > 1) System.setProperty("DB_PASSWORD", ui[1]);
                }
            } catch (IllegalArgumentException ignored) { /* rămâne valoarea originală */ }
        }
        String base = System.getenv("APP_BASE_URL");
        if (base == null || base.isBlank()) {
            String render = System.getenv("RENDER_EXTERNAL_URL");
            String railway = System.getenv("RAILWAY_PUBLIC_DOMAIN");
            if (render != null && !render.isBlank()) System.setProperty("APP_BASE_URL", render);
            else if (railway != null && !railway.isBlank()) System.setProperty("APP_BASE_URL", "https://" + railway);
        }
    }
}
