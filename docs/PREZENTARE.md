# Prezentare proiect — MobilaViva (site_nume_prenume.zip)

**Student:** *(nume prenume, grupa)*

## 1. Date generale
| Cerință | Răspuns |
|---|---|
| Tip comerț electronic | B2C (magazin online către persoane fizice) |
| Produse comercializate și de ce | Mobilă și decorațiuni pentru casă: canapele, paturi, dulapuri, comode, mese, scaune, birouri, iluminat. *(completează motivul: cerere constantă, produse cu caracteristici clare de comparat, valoare mare a coșului, livrare estimată diferită pe produs)* |
| Site model (studiul de caz 1) | *(completează: site-ul ales, de ex. un retailer de mobilă, și ce ai preluat de la el: filtre, promoții, estimare livrare)* |
| Platformă software (studiul de caz 2) | Dezvoltare proprie, fără platformă gata făcută. *(completează motivul și comparația cu platformele studiate)* |
| Realizat în | Java 17 + Spring Boot 3 (server, API REST), JavaScript: Vue 3 + Vue Router + Vite (client) |
| Baze de date | H2 (dezvoltare) / PostgreSQL (producție) prin JPA/Hibernate: tabelele `product`, `app_users`, `orders`, `order_items` |
| Plată electronică | Stripe Checkout în mod test (card `4242 4242 4242 4242`); fără cheie, card de test simulat |
| Publicat la | *(link-ul public, vezi DEPLOY.md; altfel se atașează acest zip)* |
| Cont administrator | `admin@mobilaviva.com` (parola din `ADMIN_PASSWORD`) |

## 2. Cerințe obligatorii și elemente clasice (print screen pentru fiecare)
Fișierele se salvează în `docs/screenshots/` cu numele din `docs/screenshots/LISTA.txt`.

| Cerință | Unde în aplicație | Print screen |
|---|---|---|
| Obligatoriu 1: publicat pe web | URL-ul public în browser | 01 |
| Obligatoriu 2: bază de date | Consola H2 / `psql` cu tabelele `product`, `app_users`, `orders`, `order_items` | 02 |
| Obligatoriu 3: script-uri server | `src/main/java/com/mobila/shop/*` (controllere REST, servicii, entități JPA) | 03 |
| Obligatoriu 4: plată electronică | Checkout → pagina Stripe în mod test → pagina comenzii | 04, 04b |
| I.1 Navigare cu meniuri | Bara de categorii + Oferte din antet (Vue Router, fără reîncărcare de pagină) | 05 |
| I.2 Catalog | Pagina principală cu filtre (categorie, brand, preț, stoc, oferte), sortare, căutare, paginare | 06 |
| I.3 Categorii | `/?category=Canapele` | 07 |
| I.4 Detalii produs | `/product/{id}` (imagine, preț, caracteristici, stoc, livrare) | 08 |
| I.5 Coș + actualizare | `/cart` (modifici cantitatea, ștergi, vezi transportul) | 09 |
| I.6 Comandă pe cont | Înregistrare/login, apoi `/orders` | 10 |
| I.7 Stocuri | „Stoc epuizat” pe produs; stocul scade după comandă; editare în Admin → Produse | 11 |
| I.8 Promoții | Insigna `-X%`, prețul vechi tăiat, filtrul „Doar oferte” | 12 |
| I.9 Factură | Proformă la checkout (înainte de plată), apoi factura fiscală `/invoice/{id}` (articole, preț unitar, valori, TVA 21%) | 13 |
| I.10 Plata facturii | Plata electronică testată: card de test (Stripe test sau card simulat 4242 4242 4242 4242). Ramburs și transfer bancar sunt opțiuni suplimentare și nu înlocuiesc plata cu cardul | 14, 14b; `./scripts/dovada_plata.sh` afișează plățile reușite din Stripe (mod test) |
| I.11 Administrare | `/admin`: comenzi (status), produse (CRUD, stoc, promoții), utilizatori (rol, ștergere) | 15a, 15b, 15c |
| Termeni și condiții / GDPR | `/termeni`, `/confidentialitate`, `/retur`; acceptare la înregistrare și la checkout | 17 |

## 3. Personalizări (elemente de originalitate)
Modulele proiectate pentru un magazin de mobilier și starea lor:

| Modul | Ce face | Stare |
|---|---|---|
| **Planificator de cameră „Încape la mine?”** (modulul reprezentativ) | Introduci dimensiunile camerei (cm), adaugi piese din catalog și le tragi pe un plan 2D la scară; piesele care nu încap sau se suprapun devin roșii, iar aplicația arată cât din podea e ocupat și dă un verdict; piesele se pot adăuga în coș | **Implementat** (`/planner`, `Planner.vue`, `footprint.js`; buton „📐 Încape în camera ta?” pe pagina produsului) |
| **Comparator de produse** | Adaugi până la 3 produse din catalog și le vezi într-un tabel side-by-side (preț, rating, material, dimensiuni, culoare, stoc, livrare); poți adăuga în coș direct din comparator | **Implementat** (`/compare`, `CatalogController`/`ShopApiController`, `Compare.vue`) |
| Căutare cu autocomplete | Sugestii în timp real din denumiri/brand/material; dacă nu există produse cu toți termenii, se afișează rezultate parțiale | Implementat |
| Estimare livrare pe coș | Termenul de livrare se calculează după cel mai lent produs din coș; transport gratuit peste 1500 RON | Implementat |
| Ilustrații de mobilier generate | Fiecare produs are o imagine vectorială potrivită tipului și culorii; fotografiile reale se pot seta din Admin (URL imagine) | Implementat |
| **Set asortat „completează camera”** | Pe pagina produsului apar piese din aceeași colecție (ex. pat Luna + noptieră Luna) și din categorii complementare (în stoc); „Adaugă setul în coș” cu totalul setului | **Implementat** (`/api/products/{id}/matching`, `Matching.java`, secțiunea din `Product.vue`) |
| **Listă de dorințe** | Salvarea produselor favorite pe cont (♡ pe card și pe pagina produsului); pagina `/wishlist` arată prețul de la salvare și alertă 🔔 când prețul a scăzut | **Implementat** (`/api/wishlist`, `WishlistController`, `Wishlist.vue`) |

Print screen: 16 (comparator), 16b (planificator), 16c (set asortat), 16d (listă de dorințe). Flux modul reprezentativ: pagina produsului → „📐 Încape în camera ta?” → `/planner` → dimensiunile camerei → tragi piesele pe plan → verdict.

## 4. PageSpeed
Rezultatul de la https://pagespeed.web.dev pe URL-ul public: print screen 18 (Mobile) și 19 (Desktop).
Optimizări aplicate: compresie gzip, cache de 1 an pe resursele cu hash și 30 de zile pe imagini, code splitting pe rute (JS încărcat la cerere),
datele inițiale (sesiune, produse, facete) incluse direct în HTML (fără lanț HTML → JS → API), CSS inline (fără resurse care blochează randarea),
`preload` + `fetchpriority="high"` pe primele imagini, imagini SVG ușoare cu `width`/`height` declarate, `loading="lazy"` sub pliu,
meta description/titlu/canonical per pagină, `robots.txt` + `sitemap.xml` + `llms.txt`, linkuri crawlabile, JVM cu SerialGC și încălzire la pornire.
Local: `./scripts/measure_speed.sh` generează rapoarte Lighthouse în `docs/speed-reports/`.

## 5. Conținutul arhivei
Cod sursă (`src/`, `frontend/`), scripturi (`scripts/`, cu `scripts/tools/` pentru print screen automat), documentație (`docs/`: `PREZENTARE.md`, `DEPLOY.md`, `screenshots/`), iar în rădăcină `pom.xml`, `Dockerfile`, `docker-compose.yml`, `render.yaml`. Fișierele generate la rulare stau în `.runtime/` (ignorat de git).
Rulare locală: `./scripts/run.sh` (necesită JDK 17+, Maven, Node 18+, sau Docker). Print screen automat: `./scripts/screenshots.sh [URL]` (generează și `PREZENTARE.html`). Totul dintr-o comandă (aplicație + link public + print screen-uri): `./scripts/auto.sh` sau `./scripts/auto.sh multi`. Documentare .docx/.pptx: `./scripts/docs.sh` (se rulează automat după print screen-uri și la arhivare). Arhivarea: `./scripts/package.sh nume_prenume`.
