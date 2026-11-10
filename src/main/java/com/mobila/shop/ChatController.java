package com.mobila.shop;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * API-ul asistentului: POST /api/chat {messages:[{role,content}]} -> {reply, products, mode}.
 * Protecții (endpoint public care poate costa bani): lungime limitată, ultimele 10 mesaje, 15 cereri / 5 minute pe sesiune.
 */
@RestController
public class ChatController {
    private static final int MAX_MSGS = 10, MAX_CHARS = 500, PER_WINDOW = 15, LIVE_PER_WINDOW = 150;
    private static final long WINDOW_MS = 5 * 60_000L;
    private final ChatService chat;

    public ChatController(ChatService chat) { this.chat = chat; }

    public record ChatMsg(String role, String content) {}
    public record ChatRequest(List<ChatMsg> messages, String lang) {}

    @GetMapping("/api/chat/info")
    public Map<String, Object> info() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("mode", chat.aiEnabled() ? "ai" : "local");
        if (chat.aiEnabled()) m.put("model", chat.model());
        return m;
    }

    @GetMapping("/api/chat/live")
    public Map<String, Object> live(@RequestParam(name = "q", defaultValue = "") String q, HttpSession s) {
        String t = q.strip();
        Map<String, Object> out = new LinkedHashMap<>();
        if (t.length() < 3 || !allowed(s, "chatLiveTs", LIVE_PER_WINDOW)) { out.put("reply", ""); out.put("products", List.of()); return out; }
        ChatService.Result r = chat.live(t.length() > MAX_CHARS ? t.substring(0, MAX_CHARS) : t);
        out.put("reply", r.reply());
        out.put("products", r.products());
        return out;
    }

    @PostMapping("/api/chat")
    public ResponseEntity<?> send(@RequestBody ChatRequest req, HttpSession s, HttpServletRequest http) {
        if (req == null || req.messages() == null || req.messages().isEmpty()) return Util.err(400, "Scrie un mesaj.");
        if (!allowed(s, "chatTs", PER_WINDOW)) return Util.err(429, "Prea multe mesaje într-un timp scurt. Încearcă din nou în câteva minute.");
        List<ChatMsg> in = req.messages();
        List<ChatService.Msg> hist = new ArrayList<>();
        for (ChatMsg m : in.subList(Math.max(0, in.size() - MAX_MSGS), in.size())) {
            if (m == null || m.content() == null) continue;
            String c = m.content().strip();
            if (c.isEmpty()) continue;
            hist.add(new ChatService.Msg("assistant".equals(m.role()) ? "assistant" : "user", c.length() > MAX_CHARS ? c.substring(0, MAX_CHARS) : c));
        }
        if (hist.isEmpty() || !"user".equals(hist.get(hist.size() - 1).role())) return Util.err(400, "Scrie un mesaj.");
        ChatService.Result r = chat.answer(hist, req.lang());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("reply", r.reply());
        out.put("products", r.products());
        out.put("mode", r.mode());
        return ResponseEntity.ok(out);
    }

    @SuppressWarnings("unchecked")
    private static boolean allowed(HttpSession s, String key, int max) {
        long now = System.currentTimeMillis();
        synchronized (s) {
            List<Long> ts = (List<Long>) s.getAttribute(key);
            if (ts == null) ts = new ArrayList<>();
            ts.removeIf(t -> now - t > WINDOW_MS);
            if (ts.size() >= max) { s.setAttribute(key, ts); return false; }
            ts.add(now);
            s.setAttribute(key, ts);
            return true;
        }
    }
}
