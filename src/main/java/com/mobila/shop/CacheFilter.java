package com.mobila.shop;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * PageSpeed: resursele Vite (cu hash în nume) se cache-uiesc 1 an; HTML-ul nu.
 * API-ul personal (sesiune, coș, comparator, dorințe, comenzi, administrare, autentificare) nu se cache-uiește niciodată (no-store).
 */
@Component
public class CacheFilter extends OncePerRequestFilter {
    private static boolean publicApi(String p) {
        return (p.startsWith("/api/products") && !p.endsWith("/reviews")) || p.equals("/api/facets") || p.equals("/api/suggest");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String p = req.getRequestURI();
        if (p.startsWith("/assets/")) res.setHeader("Cache-Control", "public, max-age=31536000, immutable");
        else if (p.startsWith("/api/")) { if (!publicApi(p)) res.setHeader("Cache-Control", "no-store"); }
        else if (p.startsWith("/photos/")) res.setHeader("Cache-Control", "public, max-age=2592000");
        else if (!p.startsWith("/img/")) res.setHeader("Cache-Control", "no-cache");
        chain.doFilter(req, res);
    }
}
