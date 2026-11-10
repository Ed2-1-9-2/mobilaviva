package com.mobila.shop;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

/**
 * Trimitere email cu mai multe metode încercate pe rând: emailul pleacă prin prima care reușește.
 * Ordinea: Gmail SMTP SSL 465 -> Gmail SMTP STARTTLS 587 (aceeași adresă/parolă) -> Brevo HTTPS -> releu Gmail prin Google Apps Script.
 * Fiecare metodă e activă doar dacă variabilele ei sunt completate; motivele de eșec sunt păstrate pentru pagina „Email” din Admin
 * (vezi /api/admin/email/status). Nu scrie niciodată în log conținutul emailului (linkuri, parole).
 */
@Service
public class MailService {
    private static final Logger LOG = LoggerFactory.getLogger(MailService.class);
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).followRedirects(HttpClient.Redirect.NORMAL).build();
    private final ObjectMapper om = new ObjectMapper();

    private static final String M_SMTP_465 = "smtp-465";
    private static final String M_SMTP_587 = "smtp-587";
    private static final String M_BREVO    = "brevo";
    private static final String M_RELAY    = "relay";
    private static final String M_SMTP2_465 = "smtp2-465";   // al doilea cont SMTP de rezervă (SMTP_USER2)
    private static final String M_SMTP2_587 = "smtp2-587";
    // Brevo întâi (expeditor monitorizat permanent, ajunge în Inbox); SMTP-ul personal (Gmail/ASE) doar ca rezervă, pentru că de acolo emailurile ajung în Spam
    private static final List<String> ORDER = List.of(M_BREVO, M_SMTP_465, M_SMTP_587, M_SMTP2_465, M_SMTP2_587, M_RELAY);
    private static String sfx(String methodId) { return methodId.startsWith("smtp2") ? "2" : ""; }

    private final Map<String, String> lastErrorByMethod = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lastOkByMethod = new ConcurrentHashMap<>();
    private volatile String last = "";

    private static String env(String k) {
        String v = System.getenv(k);
        return v == null ? "" : v.strip().replaceAll("^[\"']|[\"']$", "");
    }

    /** Ultimul motiv de eșec la trimitere (fără secrete): se poate afișa doar admin-ului. */
    public String lastError() { return last; }

    private static String shortMsg(String m) {
        m = m == null ? "" : m.replaceAll("\\s+", " ").trim();
        return m.length() > 200 ? m.substring(0, 200) : m;
    }

    private static boolean validEmail(String e) {
        return e != null && e.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");
    }

    // ---------- status per metodă ----------

    private boolean smtpConfigured() { return smtpConfigured(""); }
    private boolean smtpConfigured(String sf) {
        return validEmail(env("SMTP_USER" + sf)) && !env("SMTP_PASSWORD" + sf).replace(" ", "").isEmpty();
    }
    private boolean brevoConfigured() {
        return !env("BREVO_API_KEY").isEmpty() && validEmail(env("MAIL_FROM"));
    }
    private boolean relayConfigured() {
        return env("GMAIL_RELAY_URL").startsWith("https://") && !env("GMAIL_RELAY_TOKEN").isEmpty();
    }

    /** true dacă există cel puțin o metodă activă (configurată). */
    public boolean configured() {
        return smtpConfigured() || smtpConfigured("2") || brevoConfigured() || relayConfigured();
    }

    /** Raport pentru pagina „Email” din Admin: ce metodă e activă, ce a mers ultima dată, ce a eșuat. */
    public Map<String, Object> healthReport() {
        List<Map<String, Object>> methods = new ArrayList<>();
        for (String m : ORDER) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", m);
            row.put("label", switch (m) {
                case M_SMTP_465 -> "Gmail SMTP SSL (port 465)";
                case M_SMTP_587 -> "Gmail SMTP STARTTLS (port 587)";
                case M_SMTP2_465 -> "Cont 2 (you2@example.com) SMTP SSL (port 465)";
                case M_SMTP2_587 -> "Cont 2 (you2@example.com) SMTP STARTTLS (port 587)";
                case M_BREVO    -> "Brevo HTTPS API";
                case M_RELAY    -> "Releu Gmail (Google Apps Script)";
                default -> m;
            });
            row.put("from", switch (m) {
                case M_SMTP_465, M_SMTP_587 -> env("SMTP_USER");
                case M_SMTP2_465, M_SMTP2_587 -> env("SMTP_USER2");
                case M_BREVO                -> env("MAIL_FROM");
                case M_RELAY                -> "shopmobilaviva@gmail.com";
                default -> "";
            });
            boolean conf = switch (m) {
                case M_SMTP_465, M_SMTP_587 -> smtpConfigured();
                case M_SMTP2_465, M_SMTP2_587 -> smtpConfigured("2");
                case M_BREVO                -> brevoConfigured();
                case M_RELAY                -> relayConfigured();
                default -> false;
            };
            row.put("configured", conf);
            row.put("hint", switch (m) {
                case M_SMTP_465, M_SMTP_587 -> "Completează SMTP_USER și SMTP_PASSWORD (parolă de aplicație Google) în .env / Fly secrets.";
                case M_SMTP2_465, M_SMTP2_587 -> "Completează SMTP_USER2 și SMTP_PASSWORD2 (cont de rezervă) în .env / Fly secrets.";
                case M_BREVO                -> "Completează BREVO_API_KEY și verifică MAIL_FROM ca expeditor activ în Brevo.";
                case M_RELAY                -> "Publică docs/gmail_relay.gs ca aplicație web și pune URL-ul în GMAIL_RELAY_URL.";
                default -> "";
            });
            row.put("lastError", lastErrorByMethod.getOrDefault(m, ""));
            LocalDateTime t = lastOkByMethod.get(m);
            row.put("lastSuccessAt", t == null ? "" : t.toString());
            methods.add(row);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("configured", configured());
        out.put("methods", methods);
        out.put("note", "Emailurile încearcă pe rând metodele active, în ordinea din listă. Prima care reușește, trimite.");
        return out;
    }

    // ---------- startup check ----------

    @EventListener(ApplicationReadyEvent.class)
    public void bootReport() {
        if (!configured()) {
            LOG.warn("Email: NICIO metodă de trimitere nu e configurată. Completează SMTP_USER + SMTP_PASSWORD în .env (parolă de aplicație Google), apoi rulează setup_mobila_java_vue.sh. Până atunci „Am uitat parola” va răspunde neutru dar nu va trimite nimic.");
            return;
        }
        StringBuilder sb = new StringBuilder("Email: metode active ->");
        for (Map<String, Object> m : (List<Map<String, Object>>) healthReport().get("methods")) {
            if (Boolean.TRUE.equals(m.get("configured"))) sb.append(" ").append(m.get("id"));
        }
        LOG.info(sb.toString());
    }

    // ---------- SMTP (465 SSL și 587 STARTTLS, aceeași adresă/parolă) ----------

    private static String smtpHost(String sf) {
        String h = env("SMTP_HOST" + sf);
        if (!h.isEmpty()) return h;
        String u = env("SMTP_USER" + sf);
        int at = u.indexOf('@');
        String d = at < 0 ? "" : u.substring(at + 1).toLowerCase(Locale.ROOT);
        if (d.equals("gmail.com") || d.equals("googlemail.com")) return "smtp.gmail.com";
        if (d.matches("(outlook|hotmail|live|msn)\\..+")) return "smtp-mail.outlook.com";
        if (d.startsWith("yahoo.")) return "smtp.mail.yahoo.com";
        if (d.equals("icloud.com") || d.equals("me.com")) return "smtp.mail.me.com";
        if (!d.isEmpty()) return "smtp.gmail.com";   // ex. stud.ase.ro = Google Workspace
        return "";
    }

    private boolean sendSmtp(String methodId, int port, String toEmail, String subject, String text) {
        try {
            final String sf = sfx(methodId);
            final String user = env("SMTP_USER" + sf);
            String host = smtpHost(sf);
            if (host.isEmpty()) {
                String msg = "SMTP_HOST necunoscut pentru " + user;
                lastErrorByMethod.put(methodId, msg);
                LOG.warn("Email {}: {}", methodId, msg);
                return false;
            }
            int envPort = (sf.isEmpty() && env("SMTP_PORT").matches("\\d{2,5}")) ? Integer.parseInt(env("SMTP_PORT")) : 0;
            // SMTP_PORT din .env, dacă e setat, are prioritate pentru metoda 465 (altfel folosim portul fix din metodă)
            int usePort = (methodId.equals(M_SMTP_465) && envPort != 0) ? envPort : port;
            JavaMailSenderImpl ms = new JavaMailSenderImpl();
            ms.setHost(host);
            ms.setPort(usePort);
            ms.setUsername(user);
            ms.setPassword(env("SMTP_PASSWORD" + sf).replace(" ", ""));
            ms.setDefaultEncoding("UTF-8");
            Properties pr = ms.getJavaMailProperties();
            pr.put("mail.smtp.auth", "true");
            if (usePort == 465) {
                pr.put("mail.smtp.ssl.enable", "true");
            } else {
                pr.put("mail.smtp.starttls.enable", "true");
                pr.put("mail.smtp.starttls.required", "true");
            }
            pr.put("mail.smtp.connectiontimeout", "8000");
            pr.put("mail.smtp.timeout", "15000");
            pr.put("mail.smtp.writetimeout", "15000");
            SimpleMailMessage m = new SimpleMailMessage();
            m.setFrom(user);
            m.setTo(toEmail);
            m.setSubject(subject);
            m.setText(text);
            ms.send(m);
            lastErrorByMethod.remove(methodId);
            lastOkByMethod.put(methodId, LocalDateTime.now());
            LOG.info("Email {}: trimis ({} -> {})", methodId, user, toEmail);
            return true;
        } catch (Exception e) {
            String msg = shortMsg(e.getMessage());
            lastErrorByMethod.put(methodId, msg);
            LOG.warn("Email {} a eșuat: {}", methodId, msg);
            last = methodId + ": " + msg;
            return false;
        }
    }

    // ---------- Brevo HTTPS ----------

    private boolean sendBrevo(String toEmail, String toName, String subject, String text) {
        if (!brevoConfigured()) return false;
        try {
            Map<String, Object> to = new LinkedHashMap<>();
            to.put("email", toEmail);
            if (toName != null && !toName.isBlank()) to.put("name", toName);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("sender", Map.of("name", "MobilaViva", "email", env("MAIL_FROM")));
            body.put("to", List.of(to));
            body.put("subject", subject);
            body.put("textContent", text);
            HttpRequest rq = HttpRequest.newBuilder(URI.create("https://api.brevo.com/v3/smtp/email"))
                .timeout(Duration.ofSeconds(15))
                .header("api-key", env("BREVO_API_KEY"))
                .header("content-type", "application/json")
                .header("accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(om.writeValueAsString(body)))
                .build();
            HttpResponse<String> r = http.send(rq, HttpResponse.BodyHandlers.ofString());
            if (r.statusCode() / 100 != 2) {
                String msg = "HTTP " + r.statusCode() + " (expeditor " + env("MAIL_FROM") + " neverificat sau cheie greșită)";
                lastErrorByMethod.put(M_BREVO, msg);
                LOG.warn("Email brevo a refuzat: {}", msg);
                last = "brevo: " + msg;
                return false;
            }
            lastErrorByMethod.remove(M_BREVO);
            lastOkByMethod.put(M_BREVO, LocalDateTime.now());
            LOG.info("Email brevo: trimis ({} -> {})", env("MAIL_FROM"), toEmail);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            String msg = e.getClass().getSimpleName();
            lastErrorByMethod.put(M_BREVO, msg);
            LOG.warn("Email brevo a eșuat: {}", msg);
            last = "brevo: " + msg;
            return false;
        }
    }

    // ---------- Releu Gmail (Google Apps Script) ----------

    private boolean sendRelay(String toEmail, String subject, String text) {
        if (!relayConfigured()) return false;
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("token", env("GMAIL_RELAY_TOKEN"));
            body.put("to", toEmail);
            body.put("subject", subject);
            body.put("body", text);
            HttpRequest rq = HttpRequest.newBuilder(URI.create(env("GMAIL_RELAY_URL")))
                .timeout(Duration.ofSeconds(25))
                .header("content-type", "text/plain;charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(om.writeValueAsString(body)))
                .build();
            HttpResponse<String> r = http.send(rq, HttpResponse.BodyHandlers.ofString());
            String b = r.body() == null ? "" : r.body();
            if (r.statusCode() / 100 == 2 && b.contains("\"ok\":true")) {
                lastErrorByMethod.remove(M_RELAY);
                lastOkByMethod.put(M_RELAY, LocalDateTime.now());
                LOG.info("Email relay: trimis -> {}", toEmail);
                return true;
            }
            String msg = "HTTP " + r.statusCode() + " " + shortMsg(b);
            lastErrorByMethod.put(M_RELAY, msg);
            LOG.warn("Email relay a refuzat: {}", msg);
            last = "relay: " + msg;
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            String msg = e.getClass().getSimpleName();
            lastErrorByMethod.put(M_RELAY, msg);
            LOG.warn("Email relay a eșuat: {}", msg);
            last = "relay: " + msg;
            return false;
        }
    }

    // ---------- API public ----------

    /** Încearcă pe rând toate metodele active. Returnează true la prima care reușește. Nu aruncă excepții. */
    public boolean send(String toEmail, String toName, String subject, String text) {
        last = "";
        if (!validEmail(toEmail)) return false;
        if (!configured()) { last = "niciuna dintre metodele de trimitere nu e configurată"; return false; }
        List<String> errs = new ArrayList<>();
        for (String m : ORDER) {
            boolean ok = switch (m) {
                case M_SMTP_465 -> smtpConfigured() && sendSmtp(M_SMTP_465, 465, toEmail, subject, text);
                case M_SMTP_587 -> smtpConfigured() && sendSmtp(M_SMTP_587, 587, toEmail, subject, text);
                case M_SMTP2_465 -> smtpConfigured("2") && sendSmtp(M_SMTP2_465, 465, toEmail, subject, text);
                case M_SMTP2_587 -> smtpConfigured("2") && sendSmtp(M_SMTP2_587, 587, toEmail, subject, text);
                case M_BREVO    -> sendBrevo(toEmail, toName, subject, text);
                case M_RELAY    -> sendRelay(toEmail, subject, text);
                default -> false;
            };
            if (ok) return true;
            String e = lastErrorByMethod.get(m);
            if (e != null && !e.isEmpty()) errs.add(m + ": " + e);
        }
        last = String.join(" | ", errs);
        LOG.warn("Email: TOATE metodele au eșuat pentru {} -> {}", toEmail, last);
        return false;
    }

    /** Pentru pagina „Email” din Admin: trimite un email de test și întoarce rezultatul per metodă. */
    public Map<String, Object> sendTest(String toEmail) {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("to", toEmail);
        if (!validEmail(toEmail)) { res.put("ok", false); res.put("error", "Adresa de email nu e validă."); return res; }
        if (!configured()) { res.put("ok", false); res.put("error", "Nicio metodă de trimitere nu e configurată."); return res; }
        boolean ok = send(toEmail, "", "Test MobilaViva",
            "Bună!\n\nAcesta este un email de test din MobilaViva. Dacă îl primești, trimiterea emailurilor funcționează.\n\nEchipa MobilaViva");
        res.put("ok", ok);
        res.put("error", ok ? "" : last);
        res.put("methods", healthReport().get("methods"));
        return res;
    }
}
