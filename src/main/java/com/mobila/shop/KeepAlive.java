package com.mobila.shop;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Atinge baza de date la 4 minute: Neon nu mai suspendă compute-ul (limita lui e 5 minute), iar pool-ul are mereu o conexiune caldă. */
@Component
@EnableScheduling
public class KeepAlive {
    private final JdbcTemplate jdbc;
    public KeepAlive(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Scheduled(initialDelay = 60_000, fixedDelay = 240_000)
    public void ping() {
        try { jdbc.queryForObject("SELECT 1", Integer.class); } catch (Exception ignored) { }
    }
}
