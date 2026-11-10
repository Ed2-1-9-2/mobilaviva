package com.mobila.shop;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Pagina HTML (rutele Vue Router, history mode) + fișiere pentru motoare de căutare/agenți.
 * PageSpeed: sesiunea, produsele și facetele vin DEJA în HTML (window.__BOOT__) -> fără cereri API pe calea critică;
 * CSS-ul se inserează inline (fără resursă care blochează randarea); primele imagini au preload cu prioritate mare.
 */
@Controller
public class PageController {
    private static final Pattern CSS_LINK = Pattern.compile("<link rel=\"stylesheet\"[^>]*href=\"(/assets/[^\"]+\\.css)\"[^>]*>");
    private static final MediaType HTML = new MediaType("text", "html", StandardCharsets.UTF_8);

    private final ShopApiController api;
    private final CatalogController catalog;
    private final ProductRepository products;
    private final ObjectMapper json;
    private volatile String shell;

    public PageController(ShopApiController api, CatalogController catalog, ProductRepository products, ObjectMapper json) {
        this.api = api;
        this.catalog = catalog;
        this.products = products;
        this.json = json;
    }

    private String shell() throws IOException {
        String s = shell;
        if (s == null) {
            s = new String(new ClassPathResource("static/index.html").getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            Matcher m = CSS_LINK.matcher(s);
            if (m.find()) {
                try {
                    String css = new String(new ClassPathResource("static" + m.group(1)).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                    s = s.substring(0, m.start()) + "<style>" + css.replace("</style", "<\\/style") + "</style>" + s.substring(m.end());
                } catch (IOException e) { /* rămâne linkul către fișierul CSS */ }
            }
            shell = s;
        }
        return s;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void warmUp() {
        try { shell(); catalog.list("", "", "", "", "", 0, 0, "relevance", 1); catalog.facets(); }
        catch (Exception e) { /* încălzirea nu trebuie să oprească pornirea */ }
    }

    private static String base(HttpServletRequest req) {
        return ServletUriComponentsBuilder.fromContextPath(req).build().toUriString();
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static String param(HttpServletRequest r, String k) {
        String v = r.getParameter(k);
        return v == null ? "" : v;
    }

    private static int intParam(HttpServletRequest r, String k, int d) {
        try { return Integer.parseInt(r.getParameter(k)); } catch (Exception e) { return d; }
    }

    /** Aceeași cheie pe care o calculează clientul: '/products?' + new URLSearchParams(route.query). */
    private static String catalogKey(HttpServletRequest req) {
        StringBuilder sb = new StringBuilder("/products?");
        boolean first = true;
        for (Map.Entry<String, String[]> e : req.getParameterMap().entrySet()) {
            String[] v = e.getValue();
            if (v == null || v.length == 0) continue;
            String val = v[0];
            if (e.getKey().equals("category")) val = CatalogController.catKey(val);       // aceeași normalizare ca pe client (canonicalQuery)
            if (e.getKey().equals("sort")) { val = CatalogController.sortKey(val); if (val.equals("relevance")) continue; }
            if (!first) sb.append('&');
            sb.append(enc(e.getKey())).append('=').append(enc(val));
            first = false;
        }
        return sb.toString();
    }

    // Căi traduse (/catalogo, /produs/12, /warenkorb ...) -> căile interne; canonical, sitemap și llms.txt folosesc varianta în română
    private static final Map<String, String> SEG_ALIAS = pairs(new String[] {
        "abbruch=cancel",
        "accedi=login",
        "account=account",
        "afrekenen=checkout",
        "agb=termeni",
        "anmelden=login",
        "annulation=cancel",
        "annullato=cancel",
        "anulare=cancel",
        "autentificare=login",
        "bestelling=order",
        "bestellingen=orders",
        "bestellung=order",
        "bestellungen=orders",
        "cancel=cancel",
        "cancelado=cancel",
        "carrello=cart",
        "carrinho=cart",
        "carrito=cart",
        "cart=cart",
        "cassa=checkout",
        "catalog=catalog",
        "catalogo=catalog",
        "catalogue=catalog",
        "catalogus=catalog",
        "checkout=checkout",
        "comanda=order",
        "comenzi=orders",
        "commande=order",
        "commandes=orders",
        "comparador=compare",
        "comparateur=compare",
        "comparator=compare",
        "compare=compare",
        "compte=account",
        "conditions=termeni",
        "confidentialitate=confidentialitate",
        "confidentialite=confidentialitate",
        "confronto=compare",
        "connexion=login",
        "cont=account",
        "cont-nou=register",
        "conta=account",
        "contrasena-olvidada=forgot",
        "cos=cart",
        "cuenta=account",
        "datenschutz=confidentialitate",
        "deseos=wishlist",
        "desideri=wishlist",
        "devoluciones=retur",
        "devolucoes=retur",
        "dorinte=wishlist",
        "encomenda=order",
        "encomendas=orders",
        "entrar=login",
        "erfolg=success",
        "exito=success",
        "factura=invoice",
        "facture=invoice",
        "factuur=invoice",
        "fattura=invoice",
        "fatura=invoice",
        "favoris=wishlist",
        "favoritos=wishlist",
        "finalizare=checkout",
        "forgot=forgot",
        "forgot-password=forgot",
        "geannuleerd=cancel",
        "gelukt=success",
        "iniciar-sesion=login",
        "inloggen=login",
        "inscription=register",
        "invoice=invoice",
        "kasse=checkout",
        "katalog=catalog",
        "konto=account",
        "login=login",
        "mot-de-passe-oublie=forgot",
        "order=order",
        "orders=orders",
        "ordine=order",
        "ordini=orders",
        "pagamento=checkout",
        "pago=checkout",
        "paiement=checkout",
        "panier=cart",
        "parola-uitata=forgot",
        "password-dimenticata=forgot",
        "passwort-vergessen=forgot",
        "passwort-zuruecksetzen=reset",
        "pedido=order",
        "pedidos=orders",
        "planeador=planner",
        "planificador=planner",
        "planificateur=planner",
        "planificator=planner",
        "planificatore=planner",
        "planner=planner",
        "privacidad=confidentialitate",
        "privacidade=confidentialitate",
        "privacy=confidentialitate",
        "prodotto=product",
        "product=product",
        "producto=product",
        "produit=product",
        "produkt=product",
        "produs=product",
        "produto=product",
        "raumplaner=planner",
        "rechnung=invoice",
        "redefinir-senha=reset",
        "registar=register",
        "register=register",
        "registrati=register",
        "registreren=register",
        "registrieren=register",
        "registro=register",
        "reimposta-password=reset",
        "reinitialiser-mot-de-passe=reset",
        "reset=reset",
        "reset-password=reset",
        "resetare-parola=reset",
        "resi=retur",
        "restablecer-contrasena=reset",
        "retour=retur",
        "retours=retur",
        "retur=retur",
        "returns=retur",
        "rueckgabe=retur",
        "senha-esquecida=forgot",
        "succes=success",
        "success=success",
        "successo=success",
        "sucesso=success",
        "termeni=termeni",
        "termini=termeni",
        "terminos=termeni",
        "termos=termeni",
        "terms=termeni",
        "vergelijken=compare",
        "vergleich=compare",
        "verlanglijst=wishlist",
        "voorwaarden=termeni",
        "wachtwoord-resetten=reset",
        "wachtwoord-vergeten=forgot",
        "warenkorb=cart",
        "winkelwagen=cart",
        "wishlist=wishlist",
        "wunschliste=wishlist" });
    private static final Map<String, String> SEG_RO = pairs(new String[] {
        "account=cont",
        "cancel=anulare",
        "cart=cos",
        "catalog=catalog",
        "checkout=finalizare",
        "compare=comparator",
        "confidentialitate=confidentialitate",
        "forgot=parola-uitata",
        "invoice=factura",
        "login=autentificare",
        "order=comanda",
        "orders=comenzi",
        "planner=planificator",
        "product=produs",
        "register=cont-nou",
        "reset=resetare-parola",
        "retur=retur",
        "success=succes",
        "termeni=termeni",
        "wishlist=dorinte" });
    private static final Map<String, String> CAT_RO = pairs(new String[] {
        "Birouri=birouri",
        "Canapele=canapele",
        "Comode=comode",
        "Copii=copii",
        "Dulapuri=dulapuri",
        "Hol=hol",
        "Iluminat=iluminat",
        "Mese=mese",
        "Paturi=paturi",
        "Scaune=scaune" });
    private static Map<String, String> pairs(String[] a) {
        Map<String, String> m = new java.util.HashMap<>();
        for (String x : a) { int k = x.indexOf('='); m.put(x.substring(0, k), x.substring(k + 1)); }
        return m;
    }
    static String internalPath(String path) {
        String[] p = path.split("/", -1);
        for (int i = 0; i < p.length; i++) { String r = SEG_ALIAS.get(CatalogController.fold(p[i])); if (r != null) p[i] = r; }
        return String.join("/", p);
    }
    static String roPath(String path) {
        String[] p = path.split("/", -1);
        for (int i = 0; i < p.length; i++) { String r = SEG_RO.get(p[i]); if (r != null) p[i] = r; }
        return String.join("/", p);
    }
    static String roCat(String cat) { String r = CAT_RO.get(cat); return r != null ? r : cat; }

    private static String titleFor(String path) {
        return switch (path) {
            case "/cart" -> "Coș de cumpărături";
            case "/login" -> "Autentificare";
            case "/register" -> "Cont nou";
            case "/forgot" -> "Am uitat parola";
            case "/reset" -> "Parolă nouă";
            case "/account" -> "Contul meu";
            case "/compare" -> "Comparator de produse";
            case "/planner" -> "Planificator de cameră";
            case "/termeni" -> "Termeni și condiții";
            case "/confidentialitate" -> "Politica de confidențialitate";
            case "/retur" -> "Livrare și retur";
            default -> null;
        };
    }

    private static boolean isPrivate(String path) {
        return path.equals("/cart") || path.startsWith("/checkout") || path.equals("/login") || path.equals("/register")
            || path.equals("/forgot") || path.equals("/reset") || path.equals("/account") || path.equals("/orders") || path.startsWith("/order/") || path.startsWith("/invoice/") || path.equals("/admin") || path.equals("/dovada-plata");
    }

    private static String preload(Product p) {
        if (p == null || p.id == null) return "";
        if (p.imageUrl != null && !p.imageUrl.isBlank())
            return p.imageUrl.startsWith("/") ? "<link rel=\"preload\" as=\"image\" href=\"" + esc(p.imageUrl) + "\" fetchpriority=\"high\">" : "";
        return "<link rel=\"preload\" as=\"image\" href=\"/img/" + p.id + ".svg?v=4\" fetchpriority=\"high\">";
    }

    @GetMapping({"/", "/catalog", "/katalog", "/catalogue", "/catalogo", "/catalogus",
                 "/product/**", "/produs/**", "/produkt/**", "/produit/**", "/prodotto/**", "/produto/**",
                 "/producto/**", "/cart", "/cos", "/warenkorb", "/panier", "/carrello",
                 "/carrinho", "/carrito", "/winkelwagen", "/checkout", "/finalizare", "/kasse",
                 "/paiement", "/cassa", "/pagamento", "/pago", "/afrekenen", "/checkout/success",
                 "/finalizare/succes", "/kasse/erfolg", "/paiement/succes", "/cassa/successo", "/pagamento/sucesso", "/pago/exito",
                 "/afrekenen/gelukt", "/checkout/cancel", "/finalizare/anulare", "/kasse/abbruch", "/paiement/annulation", "/cassa/annullato",
                 "/pagamento/cancelado", "/pago/cancelado", "/afrekenen/geannuleerd", "/login", "/autentificare", "/anmelden",
                 "/connexion", "/accedi", "/entrar", "/iniciar-sesion", "/inloggen", "/register",
                 "/cont-nou", "/registrieren", "/inscription", "/registrati", "/registar", "/registro",
                 "/registreren", "/forgot", "/parola-uitata", "/forgot-password", "/passwort-vergessen", "/mot-de-passe-oublie",
                 "/password-dimenticata", "/senha-esquecida", "/contrasena-olvidada", "/wachtwoord-vergeten", "/reset", "/resetare-parola",
                 "/reset-password", "/passwort-zuruecksetzen", "/reinitialiser-mot-de-passe", "/reimposta-password", "/redefinir-senha", "/restablecer-contrasena",
                 "/wachtwoord-resetten", "/account", "/cont", "/konto", "/compte", "/conta",
                 "/cuenta", "/compare", "/comparator", "/vergleich", "/comparateur", "/confronto",
                 "/comparador", "/vergelijken", "/planner", "/planificator", "/raumplaner", "/planificateur",
                 "/planificatore", "/planeador", "/planificador", "/orders", "/comenzi", "/bestellungen",
                 "/commandes", "/ordini", "/encomendas", "/pedidos", "/bestellingen", "/wishlist",
                 "/dorinte", "/wunschliste", "/favoris", "/desideri", "/favoritos", "/deseos",
                 "/verlanglijst", "/order/**", "/comanda/**", "/bestellung/**", "/commande/**", "/ordine/**",
                 "/encomenda/**", "/pedido/**", "/bestelling/**", "/invoice/**", "/factura/**", "/rechnung/**",
                 "/facture/**", "/fattura/**", "/fatura/**", "/factuur/**", "/admin", "/dovada-plata",
                 "/termeni", "/terms", "/agb", "/conditions", "/termini", "/termos",
                 "/terminos", "/voorwaarden", "/confidentialitate", "/privacy", "/datenschutz", "/confidentialite",
                 "/privacidade", "/privacidad", "/retur", "/returns", "/rueckgabe", "/retours",
                 "/resi", "/devolucoes", "/devoluciones", "/retour"})
    @SuppressWarnings("unchecked")
    public ResponseEntity<String> page(HttpServletRequest req, HttpSession session) throws IOException {
        String path = internalPath(req.getRequestURI());
        String base = base(req);
        String title = null, desc = null, canonical = base + roPath(path);
        StringBuilder head = new StringBuilder(1024);
        Map<String, Object> boot = new LinkedHashMap<>();
        try {
            boot.put("/session", api.session(session));
            if (path.equals("/") || path.equals("/catalog")) {
                String sort = param(req, "sort");
                Map<String, Object> list = catalog.list(param(req, "q"), param(req, "category"), param(req, "brand"),
                    param(req, "min_price"), param(req, "max_price"), intParam(req, "in_stock", 0), intParam(req, "promo", 0),
                    sort.isEmpty() ? "relevance" : sort, intParam(req, "page", 1));
                boot.put(catalogKey(req), list);
                boot.put("/facets", catalog.facets());
                List<Product> items = (List<Product>) list.get("items");
                // preload doar pe pagina de catalog: pe start primul ecran e hero-ul (text)
                if (path.equals("/catalog")) for (int i = 0; i < Math.min(3, items.size()); i++) head.append(preload(items.get(i)));
                String cat = CatalogController.catKey(param(req, "category"));
                if (!cat.isEmpty()) { title = cat + " - MobilaViva"; desc = "Mobilă " + cat.toLowerCase(Locale.ROOT) + " online: filtre, oferte, livrare estimată și plată cu cardul."; canonical = base + roPath("/catalog") + "?category=" + enc(roCat(cat)).replace("+", "%20"); }
                else if (intParam(req, "promo", 0) == 1) { title = "Oferte - MobilaViva"; }
                else if (path.equals("/catalog")) { title = "Catalog mobilier - MobilaViva"; }
                // pagini de filtrare/sortare/căutare: nu se indexează (conținut duplicat), dar linkurile se urmăresc
                if (!param(req, "q").isEmpty() || !param(req, "brand").isEmpty() || !param(req, "min_price").isEmpty() || !param(req, "max_price").isEmpty()
                        || intParam(req, "in_stock", 0) == 1 || !param(req, "sort").isEmpty()) head.append("<meta name=\"robots\" content=\"noindex,follow\">");
            } else if (path.startsWith("/product/")) {
                try {
                    Product p = catalog.one(Long.valueOf(path.substring("/product/".length()).replaceAll("/.*$", ""))).getBody();
                    if (p != null) {
                        boot.put("/products/" + p.id, p);
                        head.append(preload(p));
                        title = p.name + " - MobilaViva";
                        String d = p.description == null ? "" : p.description;
                        desc = d.length() > 150 ? d.substring(0, 150) + "…" : d;
                        canonical = base + roPath("/product/" + p.id);
                    }
                } catch (NumberFormatException e) { /* id invalid: clientul redirecționează */ }
            } else if (titleFor(path) != null) {
                title = titleFor(path) + " - MobilaViva";
            }
        } catch (Exception e) { boot.clear(); head.setLength(0); }   // în caz de eroare, clientul încarcă datele prin API

        String html = shell();
        if (title != null) html = html.replaceFirst("<title>[^<]*</title>", Matcher.quoteReplacement("<title>" + esc(title) + "</title>"));
        if (desc != null && !desc.isBlank()) html = html.replaceFirst("<meta name=\"description\" content=\"[^\"]*\">",
            Matcher.quoteReplacement("<meta name=\"description\" content=\"" + esc(desc) + "\">"));
        head.append("<link rel=\"canonical\" href=\"").append(esc(canonical)).append("\">");
        if (isPrivate(path)) head.append("<meta name=\"robots\" content=\"noindex\">");
        if (!boot.isEmpty()) {
            String data = json.writeValueAsString(boot).replace("<", "\\u003c");
            head.append("<script>window.__BOOT__=").append(data).append("</script>");
        }
        html = html.replace("</head>", head + "</head>");
        return ResponseEntity.ok().contentType(HTML).header("Cache-Control", "private, no-cache").body(html);
    }

    @GetMapping(value = "/robots.txt", produces = "text/plain;charset=UTF-8")
    @ResponseBody
    public String robots(HttpServletRequest req) {
        return "User-agent: *\nAllow: /\nDisallow: /api/\nDisallow: /admin\n\nSitemap: " + base(req) + "/sitemap.xml\n";
    }

    @GetMapping(value = "/sitemap.xml", produces = "application/xml;charset=UTF-8")
    @ResponseBody
    public String sitemap(HttpServletRequest req) {
        String b = base(req);
        StringBuilder x = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        x.append("<url><loc>").append(b).append("/</loc></url>\n");
        x.append("<url><loc>").append(b).append(roPath("/catalog")).append("</loc></url>\n");
        for (String c : products.categories())
            x.append("<url><loc>").append(esc(b + roPath("/catalog") + "?category=" + enc(roCat(c)).replace("+", "%20"))).append("</loc></url>\n");
        for (Product p : products.findAll())
            x.append("<url><loc>").append(b).append(roPath("/product/" + p.id)).append("</loc></url>\n");
        for (String l : new String[] {"/termeni", "/confidentialitate", "/retur"})
            x.append("<url><loc>").append(b).append(roPath(l)).append("</loc></url>\n");
        return x.append("</urlset>\n").toString();
    }

    /** llms.txt (Lighthouse „Agentic Browsing”): descriere scurtă + linkuri către paginile importante. */
    @GetMapping(value = "/llms.txt", produces = "text/plain;charset=UTF-8")
    @ResponseBody
    public String llms(HttpServletRequest req) {
        String b = base(req);
        StringBuilder t = new StringBuilder();
        t.append("# MobilaViva\n\n");
        t.append("> Magazin online de mobilier (B2C): canapele, paturi, dulapuri, mese, scaune, birouri și iluminat. ");
        t.append("Catalog cu filtre, promoții, stocuri, conturi, comenzi, factură cu TVA și plată cu cardul (mod test).\n\n");
        t.append("## Catalog\n\n");
        t.append("- [Toate produsele](").append(b).append("/): catalogul complet, cu căutare, filtre și sortare\n");
        for (String c : products.categories())
            t.append("- [").append(c).append("](").append(b).append(roPath("/catalog")).append("?category=").append(enc(roCat(c)).replace("+", "%20")).append("): produse din categoria ").append(c).append('\n');
        t.append("- [Oferte](").append(b).append("/catalog?promo=1): produse la preț redus\n\n");
        t.append("## Informații\n\n");
        t.append("- [Termeni și condiții](").append(b).append("/termeni): condițiile de vânzare\n");
        t.append("- [Politica de confidențialitate](").append(b).append("/confidentialitate): prelucrarea datelor personale\n");
        t.append("- [Livrare și retur](").append(b).append("/retur): livrare, retur în 14 zile\n");
        t.append("- [Sitemap](").append(b).append("/sitemap.xml): lista tuturor paginilor\n");
        return t.toString();
    }
}
