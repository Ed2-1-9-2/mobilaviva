package com.mobila.shop;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class SeedData implements CommandLineRunner {
    private final ProductRepository repo;
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(SeedData.class);
    private static final String DEFAULT_ADMIN_PASSWORD = "Admin123!";

    private final UserRepository users;
    private final OrderRepository orders;
    private final InvoiceSeqRepository invoices;

    public SeedData(ProductRepository repo, UserRepository users, OrderRepository orders, InvoiceSeqRepository invoices) {
        this.repo = repo;
        this.users = users;
        this.orders = orders;
        this.invoices = invoices;
    }

    private static final Map<String, String> ICONS = Map.of(
        "Canapele", "🛋️", "Paturi", "🛏️", "Dulapuri", "🚪", "Comode", "🗄️",
        "Mese", "🍽️", "Scaune", "🪑", "Birouri", "🖥️", "Iluminat", "💡", "Hol", "🧥", "Copii", "🧸");

    public static String icon(String category) { return ICONS.getOrDefault(category, "🪑"); }

    private static Product p(String name, String brand, String cat, double price, double old, int stock, double rating,
                             int days, String material, String dims, String color, String desc) {
        Product x = new Product();
        x.name = name; x.brand = brand; x.category = cat; x.price = price; x.oldPrice = old; x.stock = stock;
        x.rating = rating; x.deliveryDays = days; x.material = material; x.dimensions = dims; x.color = color;
        x.description = desc; x.icon = icon(cat);
        enrich(x);
        return x;
    }

    // Producător, locul fabricației și metoda de producție. ATENȚIE: date demo; înlocuiește-le cu datele reale ale furnizorilor
    // (în mod normal, și adresa poștală a producătorului, cerută de reglementările UE privind siguranța produselor).
    private static final java.util.Map<String, String[]> MAKERS = java.util.Map.of(
        "Casa Lemn", new String[]{"Casa Lemn Producție SRL", "Brașov", "RO"},
        "DormiBine", new String[]{"DormiBine Industries SRL", "Timișoara", "RO"},
        "Living Pro", new String[]{"Living Pro Furniture SRL", "Cluj-Napoca", "RO"},
        "LumiCasă", new String[]{"LumiCasă Lighting SRL", "Sibiu", "RO"},
        "ErgoWork", new String[]{"ErgoWork Manufacturing Sp. z o.o.", "Poznań", "PL"},
        "Nordic Home", new String[]{"Nordic Home Meble Sp. z o.o.", "Gdańsk", "PL"});

    private static boolean missing(String s) { return s == null || s.isBlank(); }

    public static String clip(String s, int n) { s = s == null ? "" : s.strip(); return s.length() > n ? s.substring(0, n) : s; }

    // Cheia metodei de producție, dedusă din categorie și material (traducerea: prod_m_<cheie> în i18n-extra.js)
    public static String productionKey(String cat, String material) {
        String m = Product.fold(material);
        if ("Iluminat".equals(cat)) return "lighting";
        if (m.contains("spuma")) return "mattress";
        if (m.matches(".*(stofa|piele|catifea|textil|tapitat).*")) return "upholstered";
        if (m.contains("ratan")) return "rattan";
        if (m.matches(".*(plasa|nylon|polipropilena).*")) return "mesh";
        if (m.contains("lemn")) return "solid_wood";
        if (m.matches(".*(^|[^a-z])(mdf|pal)([^a-z]|$).*")) return "panel";
        if (m.contains("metal")) return "metal";
        if (m.matches(".*(sticla|ceramic|oglinda).*")) return "glass";
        return "assembled";
    }

    // Completează doar câmpurile goale; întoarce true dacă a modificat ceva
    public static boolean enrich(Product x) {
        boolean ch = false;
        String[] m = MAKERS.get(x.brand);
        if (m != null) {
            if (missing(x.manufacturer)) { x.manufacturer = m[0]; ch = true; }
            if (missing(x.originPlace)) { x.originPlace = m[1]; ch = true; }
            if (missing(x.originCountry)) { x.originCountry = m[2]; ch = true; }
        }
        if (missing(x.production)) { x.production = productionKey(x.category, x.material); ch = true; }
        return ch;
    }

    @Override
    public void run(String... args) {
        String env = System.getenv("ADMIN_PASSWORD");
        if (env != null) env = env.strip().replaceAll("^[\"']|[\"']$", "");   // spații / ghilimele lipite în dashboard-ul Render
        boolean reset = "true".equalsIgnoreCase(System.getenv("ADMIN_RESET"));   // ADMIN_RESET=true: parola adminului se aplică la pornire
        boolean explicit = env != null && !env.isBlank();
        // Demo pe Render: parola adminului e mereu Admin123! (indiferent de ce e în dashboard), ca să meargă login-ul cu parola dată.
        // Pentru parolă proprie pe Render: ADMIN_PASSWORD_STRICT=true + ADMIN_PASSWORD=... (Render setează singur variabila RENDER).
        boolean strict = "true".equalsIgnoreCase(System.getenv("ADMIN_PASSWORD_STRICT"));
        boolean demo = System.getenv("RENDER") != null && !strict;
        if (demo) reset = true;
        String adminPass = (explicit && !demo) ? env : DEFAULT_ADMIN_PASSWORD;
        // admin@ și admin2@: dacă ADMIN_PASSWORD e setat (sau ADMIN_RESET=true), parola din .env/host se aplică MEREU la pornire,
        // chiar dacă a fost schimbată din site. Conturile lipsă se creează cu rol ADMIN.
        for (String em : new String[]{"admin@mobilaviva.com", "admin2@mobilaviva.com"}) {
            java.util.Optional<AppUser> adm = users.findByEmail(em);
            if (adm.isEmpty()) {
                AppUser a = new AppUser();
                a.name = em.startsWith("admin2") ? "Administrator 2" : "Administrator"; a.email = em; a.role = "ADMIN"; a.passwordHash = Util.hash(adminPass);
                users.save(a);
                LOG.info("Cont administrator creat: {}", em);
            } else if ((explicit || reset) && (!Util.verify(adminPass, adm.get().passwordHash) || !"ADMIN".equals(adm.get().role))) {
                adm.get().passwordHash = Util.hash(adminPass);
                adm.get().passwordChangedAt = null;
                adm.get().role = "ADMIN";
                users.save(adm.get());
                LOG.info("Parola contului {} a fost actualizată din ADMIN_PASSWORD.", em);
            }
        }
        String sk = System.getenv("STRIPE_SECRET_KEY");   // doar diagnostic: cheia nu se afișează niciodată
        LOG.info("Stripe: {}", sk == null || sk.isBlank() ? "INACTIV (STRIPE_SECRET_KEY lipsește în serviciu => card simulat)"
            : sk.strip().startsWith("sk_test_") ? "ACTIV, mod TEST" : sk.strip().startsWith("sk_live_") ? "ACTIV, mod LIVE (!)" : "cheie invalidă (trebuie să înceapă cu sk_test_)");
        if (DEFAULT_ADMIN_PASSWORD.equals(adminPass))
            LOG.warn("ATENȚIE: parola implicită de administrator este activă. Setează ADMIN_PASSWORD înainte de a publica site-ul.");
        if (invoices.findById(1L).isEmpty()) {                 // contor de facturi fără goluri; pornește după cele existente
            InvoiceSeq q = new InvoiceSeq();
            q.id = 1L;
            q.last = orders.findAll().stream().mapToLong(o -> o.id == null ? 0 : o.id).max().orElse(0);
            invoices.save(q);
        }
        for (Product x : repo.findAll())                       // baze create înainte de coloana searchText
            if (x.searchText == null || x.searchText.isBlank()) { x.reindex(); repo.save(x); }
        for (Product x : repo.findAll())                       // baze create înainte de câmpurile producător / proveniență
            if (enrich(x)) repo.save(x);
        if (repo.count() > 0) { addExtras(); attachPhotos(); return; }
        List<Product> l = new ArrayList<>();
        l.add(p("Canapea extensibilă 3 locuri Milano", "Nordic Home", "Canapele", 2899, 3499, 8, 4.6, 7, "Stofă + lemn masiv", "220 x 95 x 88 cm", "Gri", "Canapea extensibilă cu ladă de depozitare și saltea confortabilă."));
        l.add(p("Colțar living Oslo, stânga", "Nordic Home", "Canapele", 4599, 0, 5, 4.7, 10, "Stofă catifelată", "290 x 170 x 85 cm", "Albastru petrol", "Colțar modular, extensibil, cu ladă de depozitare."));
        l.add(p("Canapea 2 locuri Verona", "Living Pro", "Canapele", 1899, 2299, 12, 4.3, 5, "Piele ecologică", "160 x 88 x 82 cm", "Maro", "Canapea compactă, potrivită pentru apartamente mici."));
        l.add(p("Fotoliu Relax Bergen", "Living Pro", "Canapele", 1199, 0, 15, 4.4, 4, "Stofă bouclé", "78 x 82 x 90 cm", "Crem", "Fotoliu confortabil cu picioare din lemn."));
        l.add(p("Pat dublu tapițat Luna 160x200", "Casa Lemn", "Paturi", 2299, 2799, 9, 4.7, 7, "Stofă + MDF", "175 x 215 x 110 cm", "Bej", "Pat tapițat cu tăblie înaltă și ladă de depozitare."));
        l.add(p("Pat lemn masiv Rustic 140x200", "Casa Lemn", "Paturi", 1799, 0, 6, 4.5, 9, "Lemn masiv de stejar", "150 x 210 x 95 cm", "Stejar natural", "Pat robust din stejar masiv, fără ladă."));
        l.add(p("Saltea ortopedică Confort 160x200", "DormiBine", "Paturi", 1099, 1399, 25, 4.6, 3, "Spumă + arcuri", "160 x 200 x 24 cm", "Alb", "Saltea ortopedică cu 7 zone de confort."));
        l.add(p("Pat metalic Loft 90x200", "Living Pro", "Paturi", 699, 0, 20, 4.1, 3, "Metal", "95 x 205 x 100 cm", "Negru", "Pat simplu cu structură metalică."));
        l.add(p("Dulap 4 uși glisante Modena", "Casa Lemn", "Dulapuri", 2599, 3099, 7, 4.4, 10, "PAL melaminat", "240 x 60 x 220 cm", "Alb + stejar", "Dulap cu oglindă și uși glisante."));
        l.add(p("Dulap haine 2 uși Nordic", "Nordic Home", "Dulapuri", 999, 0, 18, 4.2, 5, "PAL melaminat", "100 x 52 x 200 cm", "Alb", "Dulap compact cu bară și rafturi."));
        l.add(p("Bibliotecă Cube 5 rafturi", "Living Pro", "Dulapuri", 549, 649, 22, 4.3, 3, "MDF", "80 x 30 x 180 cm", "Nuc", "Bibliotecă deschisă, ușor de asamblat."));
        l.add(p("Comodă TV Bergen 180 cm", "Nordic Home", "Comode", 1099, 0, 14, 4.5, 5, "MDF + furnir", "180 x 40 x 50 cm", "Nuc", "Comodă TV cu 3 sertare și compartimente deschise."));
        l.add(p("Comodă 6 sertare Lyon", "Casa Lemn", "Comode", 1299, 1599, 10, 4.4, 7, "Lemn de pin", "120 x 45 x 85 cm", "Alb", "Comodă spațioasă pentru dormitor."));
        l.add(p("Noptieră Luna", "Casa Lemn", "Comode", 299, 0, 40, 4.3, 3, "MDF tapițat", "45 x 38 x 45 cm", "Bej", "Noptieră asortată cu patul Luna."));
        l.add(p("Masă extensibilă Oslo 140-180", "Nordic Home", "Mese", 1499, 1799, 11, 4.6, 6, "MDF + metal", "140-180 x 80 x 75 cm", "Alb + negru", "Masă de living extensibilă pentru 4-6 persoane."));
        l.add(p("Măsuță living rotundă Marmo", "Living Pro", "Mese", 899, 0, 9, 4.4, 5, "Sticlă + metal", "80 x 80 x 45 cm", "Auriu", "Măsuță de cafea cu blat din sticlă."));
        l.add(p("Masă lemn masiv Rustic 200", "Casa Lemn", "Mese", 2799, 0, 4, 4.8, 12, "Lemn masiv de stejar", "200 x 90 x 76 cm", "Stejar natural", "Masă de sufragerie pentru 8 persoane."));
        l.add(p("Set 4 scaune tapițate Milano", "Nordic Home", "Scaune", 799, 999, 16, 4.5, 5, "Stofă + lemn", "45 x 52 x 90 cm", "Gri", "Set de 4 scaune confortabile pentru sufragerie."));
        l.add(p("Scaune bar Loft (set 2)", "Living Pro", "Scaune", 599, 0, 0, 4.2, 6, "Metal + piele ecologică", "40 x 42 x 105 cm", "Negru", "Set de 2 scaune de bar cu înălțime reglabilă."));
        l.add(p("Scaun birou ergonomic Pro", "ErgoWork", "Scaune", 899, 1099, 20, 4.6, 4, "Plasă + nylon", "65 x 65 x 115 cm", "Negru", "Scaun ergonomic cu suport lombar și brațe reglabile."));
        l.add(p("Birou calculator Simple 140", "ErgoWork", "Birouri", 649, 0, 17, 4.3, 4, "PAL melaminat", "140 x 60 x 75 cm", "Alb", "Birou simplu cu loc pentru unitate și monitor."));
        l.add(p("Birou reglabil electric 160", "ErgoWork", "Birouri", 1999, 2399, 6, 4.7, 6, "Blat MDF + cadru metalic", "160 x 80 x 65-130 cm", "Nuc + negru", "Birou cu înălțime reglabilă electric și memorie de poziții."));
        l.add(p("Lustră modernă Halo", "LumiCasă", "Iluminat", 549, 0, 13, 4.2, 4, "Metal", "Ø 60 cm", "Auriu", "Lustră cu 3 becuri, potrivită pentru living."));
        l.add(p("Lampadar arcuit Nordic", "LumiCasă", "Iluminat", 399, 499, 19, 4.4, 4, "Metal + textil", "35 x 35 x 190 cm", "Negru", "Lampadar cu braț arcuit și abajur textil."));
        repo.saveAll(l);
        addExtras();
        attachPhotos();
    }

    /** Catalog extins (Hol, Copii și piese noi în categoriile existente). Se adaugă după nume, deci și pe baze de date deja create; nu se dublează. */
    private void addExtras() {
        java.util.Set<String> have = new java.util.HashSet<>();
        for (Product x : repo.findAll()) have.add(x.name);
        List<Product> e = new ArrayList<>();
        e.add(p("Canapea 3 locuri Chester", "Living Pro", "Canapele", 3299, 3799, 6, 4.5, 9, "Piele ecologică capitonată", "210 x 90 x 78 cm", "Maro", "Canapea clasică capitonată, cu brațe înalte și picioare din lemn."));
        e.add(p("Pat boxspring Royal 160x200", "DormiBine", "Paturi", 3499, 3999, 5, 4.8, 12, "Stofă + arcuri", "170 x 215 x 120 cm", "Gri", "Pat boxspring cu saltea cu arcuri, topper și ladă de depozitare."));
        e.add(p("Vitrină sufragerie Venezia", "Casa Lemn", "Dulapuri", 1899, 2199, 5, 4.4, 9, "MDF + sticlă", "90 x 40 x 190 cm", "Nuc", "Vitrină cu uși din sticlă și iluminare LED pentru obiecte decorative."));
        e.add(p("Comodă 3 sertare Aria", "Nordic Home", "Comode", 799, 0, 15, 4.3, 5, "MDF", "80 x 40 x 80 cm", "Alb", "Comodă compactă cu sertare cu închidere lentă."));
        e.add(p("Masă bucătărie rotundă Terra", "Nordic Home", "Mese", 1099, 1299, 8, 4.5, 6, "Lemn de fag", "Ø 110 x 75 cm", "Stejar natural", "Masă rotundă pentru 4 persoane, cu picior central."));
        e.add(p("Set 2 scaune dining Scandi", "Nordic Home", "Scaune", 499, 0, 24, 4.4, 4, "Lemn de fag + stofă", "44 x 50 x 82 cm", "Bej", "Scaune în stil scandinav, cu șezut tapițat."));
        e.add(p("Fotoliu gaming Racer", "ErgoWork", "Scaune", 1299, 1599, 10, 4.3, 5, "Piele ecologică", "70 x 70 x 125 cm", "Negru", "Fotoliu gaming cu suport lombar, brațe reglabile și rotile silențioase."));
        e.add(p("Birou colț L Studio", "ErgoWork", "Birouri", 1199, 0, 8, 4.4, 6, "PAL melaminat", "160 x 120 x 75 cm", "Stejar natural", "Birou de colț pentru home office, cu loc pentru două monitoare."));
        e.add(p("Veioză ceramică Luna", "LumiCasă", "Iluminat", 199, 249, 30, 4.3, 3, "Ceramică + textil", "Ø 25 x 40 cm", "Crem", "Veioză asortată cu patul și noptiera Luna."));
        e.add(p("Aplică perete Arc", "LumiCasă", "Iluminat", 249, 0, 25, 4.1, 3, "Metal", "30 x 12 x 35 cm", "Negru", "Aplică minimalistă pentru dormitor sau hol."));
        e.add(p("Pantofar Slim 3 clapete", "Nordic Home", "Hol", 649, 0, 14, 4.2, 4, "MDF", "80 x 24 x 120 cm", "Alb", "Pantofar îngust pentru hol, cu 3 clapete basculante."));
        e.add(p("Bancă hol cu depozitare Oslo", "Nordic Home", "Hol", 549, 699, 12, 4.4, 5, "MDF + stofă", "100 x 38 x 45 cm", "Gri", "Bancă tapițată cu spațiu de depozitare sub șezut."));
        e.add(p("Cuier perete Wood", "Casa Lemn", "Hol", 199, 0, 30, 4.0, 3, "Lemn de pin", "80 x 10 x 25 cm", "Stejar natural", "Cuier de perete cu 5 agățători."));
        e.add(p("Oglindă hol Halo", "LumiCasă", "Hol", 349, 0, 16, 4.5, 4, "Metal + sticlă", "60 x 3 x 160 cm", "Negru", "Oglindă înaltă cu ramă subțire din metal."));
        e.add(p("Pat copii Casuță 90x200", "Casa Lemn", "Copii", 1299, 1599, 7, 4.6, 8, "Lemn de pin", "100 x 205 x 160 cm", "Alb", "Pat în formă de căsuță, din lemn masiv de pin."));
        e.add(p("Birou copii Mic Artist", "ErgoWork", "Copii", 549, 0, 12, 4.4, 5, "PAL melaminat", "100 x 55 x 75 cm", "Alb", "Birou pentru copii, cu înălțime reglabilă și sertar."));
        // --- catalog extins la 100 de produse (variat: stiluri, dimensiuni, materiale, prețuri)
        e.add(p("Canapea 2 locuri Scandi Lund", "Nordic Home", "Canapele", 1999, 2399, 9, 4.5, 6, "Stofă țesută + lemn de fag", "165 x 85 x 80 cm", "Gri", "Canapea compactă în stil scandinav, cu picioare conice din lemn."));
        e.add(p("Canapea 3 locuri Velvet Aurora", "Living Pro", "Canapele", 3199, 3799, 5, 4.7, 10, "Catifea", "215 x 92 x 80 cm", "Verde", "Canapea din catifea cu cusături capitonate și picioare aurii."));
        e.add(p("Colțar U Panorama", "Nordic Home", "Canapele", 6499, 7499, 3, 4.6, 14, "Stofă + lemn masiv", "340 x 210 x 85 cm", "Bej", "Colțar în formă de U pentru living mare, cu două lăzi de depozitare."));
        e.add(p("Canapea extensibilă click-clack Tokyo", "Living Pro", "Canapele", 1599, 0, 14, 4.2, 5, "Stofă + metal", "190 x 85 x 82 cm", "Antracit", "Canapea cu mecanism click-clack, se transformă rapid în pat."));
        e.add(p("Canapea 3 locuri Cognac Premium", "Casa Lemn", "Canapele", 4299, 4999, 4, 4.8, 12, "Piele naturală", "225 x 95 x 82 cm", "Maro", "Canapea din piele naturală cu cadru din lemn masiv."));
        e.add(p("Fotoliu Wing clasic Bergère", "Casa Lemn", "Canapele", 1499, 1799, 7, 4.5, 8, "Stofă + lemn masiv", "80 x 78 x 105 cm", "Albastru", "Fotoliu clasic cu spătar înalt și picioare din lemn masiv."));
        e.add(p("Fotoliu balansoar Nordic Rocky", "Nordic Home", "Canapele", 899, 0, 11, 4.3, 5, "Lemn de fag + textil", "68 x 90 x 95 cm", "Crem", "Fotoliu balansoar cu pernă detașabilă."));
        e.add(p("Pat dublu hotel Grand 180x200", "DormiBine", "Paturi", 3199, 3699, 6, 4.7, 10, "Stofă + MDF", "195 x 215 x 125 cm", "Gri", "Pat tapițat cu tăblie înaltă matlasată, stil hotel."));
        e.add(p("Pat lemn Nordic Fjord 160x200", "Nordic Home", "Paturi", 2099, 0, 8, 4.5, 8, "Lemn de fag", "170 x 210 x 85 cm", "Stejar natural", "Pat minimalist din fag, cu tăblie joasă."));
        e.add(p("Pat cu ladă Storage 140x200", "Casa Lemn", "Paturi", 1899, 2299, 10, 4.4, 7, "PAL melaminat + stofă", "150 x 210 x 100 cm", "Antracit", "Pat cu ladă de depozitare și somieră ridicabilă."));
        e.add(p("Pat simplu Studio 90x200", "Living Pro", "Paturi", 899, 0, 19, 4.1, 4, "PAL melaminat", "95 x 205 x 80 cm", "Alb", "Pat simplu pentru garsonieră sau cameră de oaspeți."));
        e.add(p("Pat rustic Cabana 180x200", "Casa Lemn", "Paturi", 2899, 3299, 4, 4.8, 12, "Lemn masiv de pin", "190 x 210 x 100 cm", "Nuc", "Pat masiv, stil cabană, cu tăblie din scânduri."));
        e.add(p("Saltea memory Cloud 140x200", "DormiBine", "Paturi", 1399, 1699, 17, 4.6, 3, "Spumă memory + arcuri", "140 x 200 x 26 cm", "Alb", "Saltea cu strat memory și 5 zone de confort."));
        e.add(p("Dulap 3 uși Torino", "Casa Lemn", "Dulapuri", 1799, 2099, 9, 4.3, 8, "PAL melaminat", "150 x 55 x 210 cm", "Alb + stejar", "Dulap cu 3 uși, bară și rafturi interioare."));
        e.add(p("Dulap 5 uși glisante Madrid", "Nordic Home", "Dulapuri", 3299, 3899, 4, 4.5, 12, "PAL + oglindă", "250 x 62 x 230 cm", "Gri", "Dulap mare cu uși glisante și oglindă centrală."));
        e.add(p("Dulap tineret Studio 2 uși", "Living Pro", "Dulapuri", 849, 0, 16, 4.1, 5, "MDF", "90 x 50 x 190 cm", "Alb", "Dulap compact pentru cameră de tineret."));
        e.add(p("Dulap rustic Brașov 2 uși", "Casa Lemn", "Dulapuri", 2399, 0, 5, 4.6, 11, "Lemn masiv de pin", "110 x 55 x 200 cm", "Nuc", "Dulap din lemn masiv cu uși panelate."));
        e.add(p("Bibliotecă scară Ladder 5 trepte", "Living Pro", "Dulapuri", 449, 549, 21, 4.2, 3, "Metal + MDF", "65 x 38 x 180 cm", "Negru", "Bibliotecă tip scară, se reazemă de perete."));
        e.add(p("Vitrină Modern Cristal 2 uși", "Casa Lemn", "Dulapuri", 1599, 1899, 6, 4.4, 9, "MDF + sticlă", "80 x 38 x 170 cm", "Alb", "Vitrină cu uși din sticlă și 4 rafturi reglabile."));
        e.add(p("Comodă TV Loft 160", "Living Pro", "Comode", 899, 0, 13, 4.3, 5, "Metal + MDF", "160 x 40 x 52 cm", "Negru", "Comodă TV industrială cu cadru metalic."));
        e.add(p("Comodă TV suspendată Float 200", "Nordic Home", "Comode", 1299, 1499, 9, 4.5, 6, "MDF lăcuit", "200 x 35 x 30 cm", "Alb", "Comodă TV suspendată, cu uși push-open."));
        e.add(p("Comodă TV Rustic 150", "Casa Lemn", "Comode", 1199, 0, 7, 4.4, 7, "Lemn masiv de stejar", "150 x 40 x 55 cm", "Stejar natural", "Comodă TV din lemn masiv cu două sertare."));
        e.add(p("Noptieră suspendată Float", "Nordic Home", "Comode", 249, 0, 35, 4.2, 3, "MDF lăcuit", "40 x 32 x 20 cm", "Alb", "Noptieră suspendată cu sertar."));
        e.add(p("Noptieră Rustic cu sertar", "Casa Lemn", "Comode", 349, 0, 28, 4.4, 4, "Lemn masiv de pin", "45 x 38 x 55 cm", "Nuc", "Noptieră din lemn masiv, cu sertar și raft."));
        e.add(p("Comodă 5 sertare Scandi", "Nordic Home", "Comode", 1499, 1799, 8, 4.5, 6, "Lemn de fag + MDF", "80 x 40 x 120 cm", "Stejar natural", "Comodă înaltă cu 5 sertare, ideală pentru dormitor."));
        e.add(p("Masă extensibilă Berlin 160-200", "Nordic Home", "Mese", 1899, 2199, 8, 4.5, 7, "MDF + metal", "160-200 x 90 x 76 cm", "Antracit", "Masă de dining extensibilă pentru 6-8 persoane."));
        e.add(p("Masă pătrată Cube 90", "Living Pro", "Mese", 799, 0, 14, 4.2, 5, "PAL melaminat", "90 x 90 x 75 cm", "Alb", "Masă pătrată pentru bucătărie mică, 4 persoane."));
        e.add(p("Măsuță cafea Pallet", "Living Pro", "Mese", 449, 0, 20, 4.1, 4, "Lemn de pin", "100 x 60 x 38 cm", "Stejar natural", "Măsuță de cafea cu raft inferior, stil industrial."));
        e.add(p("Măsuță laterală rotundă Duo", "LumiCasă", "Mese", 329, 399, 24, 4.3, 3, "Metal + sticlă", "Ø 45 x 55 cm", "Auriu", "Măsuță laterală pentru canapea sau fotoliu."));
        e.add(p("Masă bar înaltă Pub 120", "Living Pro", "Mese", 1099, 0, 6, 4.3, 6, "Lemn + metal", "120 x 60 x 105 cm", "Negru", "Masă înaltă pentru bar sau open space."));
        e.add(p("Masă rotundă Marmo 120", "Nordic Home", "Mese", 2299, 2699, 5, 4.7, 10, "Blat ceramic imitație marmură", "Ø 120 x 75 cm", "Alb", "Masă rotundă cu picior central, 4-5 persoane."));
        e.add(p("Set 4 scaune Velvet Aurora", "Living Pro", "Scaune", 999, 1199, 12, 4.5, 5, "Catifea + metal", "46 x 55 x 88 cm", "Verde", "Set de 4 scaune din catifea cu picioare aurii."));
        e.add(p("Set 6 scaune Loft metal", "Living Pro", "Scaune", 1199, 0, 10, 4.3, 6, "Metal", "42 x 46 x 85 cm", "Negru", "Set de 6 scaune industriale, stivuibile."));
        e.add(p("Scaune bar Wood (set 2)", "Casa Lemn", "Scaune", 649, 799, 13, 4.4, 5, "Lemn masiv + piele ecologică", "40 x 42 x 100 cm", "Nuc", "Set de 2 scaune de bar din lemn masiv."));
        e.add(p("Scaun birou ergonomic Mesh Basic", "ErgoWork", "Scaune", 549, 0, 25, 4.2, 4, "Plasă + plastic", "60 x 60 x 105 cm", "Gri", "Scaun de birou cu spătar din plasă și brațe fixe."));
        e.add(p("Set 4 scaune Windsor lemn", "Casa Lemn", "Scaune", 1099, 0, 9, 4.5, 8, "Lemn masiv de fag", "45 x 50 x 90 cm", "Stejar natural", "Set de 4 scaune clasice cu spătar din bare."));
        e.add(p("Scaun conferință Executive piele", "ErgoWork", "Scaune", 1499, 1799, 6, 4.6, 6, "Piele ecologică", "68 x 68 x 118 cm", "Maro", "Scaun de birou executiv cu tetieră."));
        e.add(p("Taburet rotund Cub", "Nordic Home", "Scaune", 199, 0, 30, 4.0, 3, "Stofă bouclé", "Ø 40 x 45 cm", "Crem", "Taburet rotund, util ca scaun suplimentar."));
        e.add(p("Birou compact Mini 100", "ErgoWork", "Birouri", 449, 0, 22, 4.1, 4, "PAL melaminat", "100 x 50 x 75 cm", "Alb", "Birou mic pentru spații restrânse."));
        e.add(p("Birou rustic lemn masiv Atelier 150", "Casa Lemn", "Birouri", 1899, 2199, 5, 4.7, 10, "Lemn masiv de stejar", "150 x 70 x 76 cm", "Stejar natural", "Birou masiv cu două sertare."));
        e.add(p("Birou gaming Dragon 140", "ErgoWork", "Birouri", 999, 1199, 11, 4.4, 5, "MDF + metal", "140 x 65 x 75 cm", "Negru", "Birou gaming cu suport pentru căști și cablu."));
        e.add(p("Birou cu rafturi Studio 120", "Living Pro", "Birouri", 799, 0, 15, 4.3, 5, "PAL melaminat", "120 x 55 x 150 cm", "Nuc", "Birou cu etajeră pentru cărți și documente."));
        e.add(p("Birou secretar Wall 80", "Nordic Home", "Birouri", 699, 849, 8, 4.2, 6, "MDF", "80 x 40 x 110 cm", "Alb", "Birou secretar pliabil, se montează pe perete."));
        e.add(p("Lampadar trepied Tripod", "LumiCasă", "Iluminat", 449, 0, 15, 4.3, 4, "Lemn + textil", "50 x 50 x 150 cm", "Stejar natural", "Lampadar pe trepied cu abajur textil."));
        e.add(p("Lampadar LED Torch Slim", "LumiCasă", "Iluminat", 349, 429, 20, 4.2, 3, "Metal", "25 x 25 x 180 cm", "Negru", "Lampadar LED cu lumină reglabilă."));
        e.add(p("Veioză birou LED Flex", "LumiCasă", "Iluminat", 169, 0, 40, 4.4, 3, "Metal + plastic", "18 x 18 x 45 cm", "Alb", "Veioză de birou cu braț flexibil și 3 trepte de lumină."));
        e.add(p("Veioză sticlă Opal", "LumiCasă", "Iluminat", 229, 279, 26, 4.3, 3, "Sticlă opal + metal", "Ø 22 x 38 cm", "Alb", "Veioză cu abajur din sticlă opală."));
        e.add(p("Aplică dublă Twin", "LumiCasă", "Iluminat", 299, 0, 18, 4.2, 4, "Metal", "40 x 14 x 20 cm", "Auriu", "Aplică cu două brațe orientabile."));
        e.add(p("Lustră cubică Cube", "LumiCasă", "Iluminat", 649, 799, 9, 4.3, 5, "Metal + sticlă", "50 x 50 x 40 cm", "Negru", "Lustră geometrică cu 4 becuri."));
        e.add(p("Cuier stativ Tree", "Casa Lemn", "Hol", 299, 0, 22, 4.1, 4, "Lemn de pin", "45 x 45 x 175 cm", "Nuc", "Cuier stativ cu 8 agățători."));
        e.add(p("Consolă hol Slim 100", "Nordic Home", "Hol", 549, 649, 13, 4.3, 5, "MDF + metal", "100 x 28 x 80 cm", "Stejar natural", "Consolă îngustă pentru hol, cu sertar."));
        e.add(p("Dulap pantofi Step 2 uși", "Living Pro", "Hol", 799, 0, 12, 4.2, 5, "PAL melaminat", "70 x 30 x 110 cm", "Alb", "Dulap de pantofi cu uși basculante."));
        e.add(p("Bancă pantofi Nordic", "Nordic Home", "Hol", 399, 0, 18, 4.2, 4, "Lemn de fag + metal", "90 x 32 x 48 cm", "Stejar natural", "Bancă cu raft deschis pentru încălțăminte."));
        e.add(p("Cuier cu oglindă Entry", "Casa Lemn", "Hol", 499, 599, 10, 4.3, 5, "MDF + oglindă", "60 x 20 x 180 cm", "Alb", "Cuier de perete cu oglindă și raft."));
        e.add(p("Pat etajat Bunk 90x200", "Casa Lemn", "Copii", 2199, 2599, 6, 4.6, 9, "Lemn masiv de pin", "100 x 210 x 165 cm", "Alb", "Pat etajat cu scară laterală și balustradă."));
        e.add(p("Pat copii Montessori 90x190", "Nordic Home", "Copii", 1099, 0, 9, 4.5, 7, "Lemn de pin", "100 x 200 x 70 cm", "Stejar natural", "Pat jos Montessori, cu protecție laterală."));
        e.add(p("Birou copii Rocket", "ErgoWork", "Copii", 649, 799, 13, 4.4, 5, "PAL melaminat", "110 x 55 x 76 cm", "Albastru", "Birou colorat, cu raft și sertar."));
        e.add(p("Dulap copii Zoo", "Living Pro", "Copii", 1199, 0, 9, 4.3, 6, "MDF", "100 x 50 x 180 cm", "Verde", "Dulap cu 2 uși și motive de animale."));
        e.add(p("Comodă copii Colors", "Living Pro", "Copii", 749, 0, 14, 4.2, 5, "MDF", "80 x 40 x 85 cm", "Galben", "Comodă cu 4 sertare colorate."));
        e.add(p("Bibliotecă copii Mini", "Nordic Home", "Copii", 399, 0, 17, 4.4, 4, "MDF", "80 x 30 x 100 cm", "Alb", "Bibliotecă joasă, la înălțimea copilului."));
        // --- scaune individuale
        e.add(p("Scaun dining Windsor lemn", "Casa Lemn", "Scaune", 289, 349, 16, 4.5, 6, "Lemn masiv de fag", "45 x 50 x 90 cm", "Stejar natural", "Scaun clasic cu spătar din bare și șezut din lemn masiv."));
        e.add(p("Scaun dining Velvet Aurora", "Living Pro", "Scaune", 349, 0, 14, 4.6, 5, "Catifea + metal auriu", "46 x 55 x 88 cm", "Verde", "Scaun din catifea cu spătar canelat și picioare aurii."));
        e.add(p("Scaun dining Fjord curbat", "Nordic Home", "Scaune", 259, 319, 20, 4.4, 5, "Lemn de fag", "45 x 52 x 82 cm", "Stejar natural", "Scaun scandinav cu spătar curbat din lemn de fag."));
        e.add(p("Scaun dining bouclé Cloud", "Nordic Home", "Scaune", 449, 0, 10, 4.5, 6, "Stofă bouclé + lemn", "52 x 56 x 80 cm", "Crem", "Scaun cu șezut și spătar din bouclé, picioare din lemn."));
        e.add(p("Scaun dining piele Cognac", "Casa Lemn", "Scaune", 379, 449, 12, 4.5, 6, "Piele ecologică + metal", "47 x 54 x 88 cm", "Maro", "Scaun tapițat cu piele ecologică și picioare metalice."));
        e.add(p("Scaun rattan Bali", "Living Pro", "Scaune", 429, 0, 8, 4.6, 8, "Ratan + lemn", "50 x 54 x 85 cm", "Stejar natural", "Scaun cu spătar din ratan împletit."));
        e.add(p("Scaun shell Nordic", "Nordic Home", "Scaune", 229, 0, 22, 4.3, 4, "Polipropilenă + lemn", "46 x 50 x 82 cm", "Alb", "Scaun cu carcasă ergonomică și picioare din lemn."));
        e.add(p("Scaun metalic Loft Factory", "Living Pro", "Scaune", 199, 0, 26, 4.2, 4, "Metal", "42 x 46 x 85 cm", "Negru", "Scaun industrial din metal, rezistent și stivuibil."));
        e.add(p("Scaun pliabil Compact", "Living Pro", "Scaune", 129, 159, 30, 4.1, 3, "Metal + plastic", "44 x 46 x 80 cm", "Gri", "Scaun pliabil, ideal pentru oaspeți sau balcon."));
        e.add(p("Scaun bar Wood", "Casa Lemn", "Scaune", 259, 0, 13, 4.4, 5, "Lemn + piele ecologică", "40 x 42 x 100 cm", "Nuc", "Scaun de bar cu spătar jos și suport pentru picioare."));
        e.add(p("Scaun bar înalt Industrial", "Living Pro", "Scaune", 279, 329, 11, 4.3, 5, "Metal + șezut tapițat", "40 x 42 x 105 cm", "Negru", "Scaun de bar înalt cu cadru metalic."));
        e.add(p("Scaun gaming Racer Pro", "ErgoWork", "Scaune", 1199, 1499, 9, 4.5, 5, "Piele ecologică", "68 x 68 x 130 cm", "Negru + roșu", "Scaun gaming cu tetieră, suport lombar și brațe reglabile."));
        e.add(p("Scaun ergonomic Posture cu tetieră", "ErgoWork", "Scaune", 1099, 0, 8, 4.7, 6, "Plasă + nylon", "64 x 64 x 125 cm", "Negru", "Scaun ergonomic cu tetieră, suport lombar și înclinare sincron."));
        e.add(p("Scaun birou Basic Studio", "ErgoWork", "Scaune", 399, 0, 24, 4.1, 4, "Plasă + plastic", "58 x 58 x 100 cm", "Gri", "Scaun de birou simplu, cu spătar din plasă."));
        List<Product> fresh = new ArrayList<>();
        for (Product x : e) if (!have.contains(x.name)) fresh.add(x);
        if (!fresh.isEmpty()) repo.saveAll(fresh);
    }

    static String slug(String name) {
        return Product.fold(name).replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
    }

    /** Dacă în static/photos/ există poza produsului (nume = slug al denumirii), o folosește; URL-ul setat din Admin are prioritate. */
    private void attachPhotos() {
        for (Product x : repo.findAll()) {
            if (x.imageUrl != null && !x.imageUrl.isBlank()) continue;
            String s = slug(x.name);
            for (String ext : new String[] {"webp", "jpg", "jpeg", "png"}) {
                if (new org.springframework.core.io.ClassPathResource("static/photos/" + s + "." + ext).exists()) {
                    x.imageUrl = "/photos/" + s + "." + ext;
                    repo.save(x);
                    break;
                }
            }
        }
    }
}
