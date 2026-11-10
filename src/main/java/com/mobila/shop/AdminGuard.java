package com.mobila.shop;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** Permite accesul la /api/admin/** doar utilizatorilor cu rol ADMIN. */
@Component
public class AdminGuard implements HandlerInterceptor {
    private final UserRepository users;

    public AdminGuard(UserRepository users) { this.users = users; }

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws Exception {
        HttpSession s = req.getSession(false);
        Object id = s == null ? null : s.getAttribute("uid");
        boolean ok = id != null && users.findById((Long) id).map(AppUser::isAdmin).orElse(false);
        if (!ok) {
            res.setStatus(403);
            res.setContentType("application/json;charset=UTF-8");
            res.getWriter().write("{\"errors\":[\"Acces interzis.\"]}");
        }
        return ok;
    }
}
