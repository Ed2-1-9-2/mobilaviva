# Standarde de design aplicate în MobilaViva

Direcția vizuală: catalog de mobilier cu aer de plan de arhitectură. Elementul memorabil este planul de cameră la scară din pagina de start
(același aspect ca modulul „Planificator de cameră”); restul paginii rămâne liniștit, ca produsele să rămână în prim-plan.

## Identitate vizuală
| Element | Alegere | De ce |
|---|---|---|
| Culori | ipsos rece (#f3f4f1), petrol (#1d5560), stejar (#b97a2e) doar ca accent, roșu de ofertă (#b3261e) | petrolul vine din canapeaua „Albastru petrol” din catalog; fără gradient și fără umbre decorative |
| Font | Familjen Grotesk (variabil, găzduit local, ~34 KB pentru latin + latin-ext) | caracter propriu, diacritice românești complete, fără cereri către terți (GDPR, PageSpeed) |
| Imagini | ilustrații SVG generate, pe fundal neutru; fotografiile reale se pun din Admin | încărcare foarte rapidă, aspect uniform |
| Iconițe | set SVG liniar (`components/Icon.vue`), fără emoji | aspect identic pe orice sistem, culoarea vine din temă |
| Raze de colț | 4 px imagini, 6 px controale, 10 px panouri | ierarhie, nu o singură valoare peste tot |

## Standarde și unde se văd
| Standard / practică | Unde se vede |
|---|---|
| WCAG 2.2 AA: contrast de minimum 4.5:1 pe text (tokeni verificați, și în modul întunecat) | `style.css`, variabilele `:root` |
| WCAG 2.4.1 / 2.4.7: link „Sari la conținut”, focus vizibil pe orice control, repere (header, nav, main, footer) | `App.vue`, `:focus-visible` |
| WCAG 2.5.8: ținte tactile de minimum 44 px (butoane, câmpuri, meniu) | `button`, `input`, `.act` |
| WCAG 2.3.3: `prefers-reduced-motion` dezactivează orice animație | sfârșitul `style.css` |
| Un singur moment de mișcare (la încărcarea paginii de start), restul răspunde acțiunilor utilizatorului (confirmarea „Adăugat în coș”) | planul din hero, `.toast` |
| Core Web Vitals: `aspect-ratio` și `width`/`height` pe imagini (CLS), font cu `font-display: swap` și preîncărcat, text ca LCP | `.plate`, `index.html` |
| Mobile-first / m-commerce: filtre pliabile, 2 coloane pe telefon, meniu derulabil, căutare pe rând separat | `@media` din `style.css` |
| Card de produs: imagine, nume, preț (cu preț vechi și reducere), stoc real, termen de livrare, acțiune principală unică | `ProductCard.vue` |
| Pagina de produs: breadcrumb, preț și stoc lângă butonul de cumpărare, specificații, retur și garanție vizibile | `Product.vue` |
| Checkout în pași, metode de plată explicate, preț final cu TVA și transport înainte de plată, semnale de încredere lângă buton | `Checkout.vue`, `.steps`, `.pm`, `.trust` |
| Termeni, confidențialitate, retur, ANPC SAL și Consumer Redress (UE) accesibile din subsol; fără cookie-uri de marketing | `App.vue` (subsol), `Legal.vue` |
| Arhitectura informației: pagina de start (brand, categorii, oferte) separată de pagina de catalog (filtre, sortare, paginare), cu breadcrumb | `Home.vue`, `Catalog.vue` |
| Căutare cu sugestii, filtre, sortare, mesaj clar la zero rezultate | `Catalog.vue` |

## SEO (după ghidul „SEO pentru magazine online”, Netlogiq)
| Recomandare din ghid | Cum este aplicată |
|---|---|
| Structură clară, maximum 3 clicuri până la produs (Acasă, categorie, produs) | meniu cu categorii pe toate paginile, plăci de categorie pe start, breadcrumb pe catalog și produs |
| Pagini de filtrare și sortare fără indexare, canonical către categorie | `meta robots noindex,follow` pe căutări, brand, preț, stoc și sortare; canonical către pagina categoriei |
| Conținut unic pe paginile de categorie | text introductiv pe fiecare categorie, plus linkuri către celelalte categorii (link building intern, anchor text descriptiv) |
| Title și meta description unice, cu cuvinte-cheie | generate pe server pentru categorii, produse și pagini statice |
| Un singur H1 pe pagină, H2 pentru produsele din listă | H1 pe start, catalog și produs; titlurile cardurilor sunt H2 în catalog și H3 în secțiunile de pe start |
| Date structurate: Organization, WebSite cu căutare, Product cu ofertă, BreadcrumbList | JSON-LD în `jsonld.js`; AggregateRating și Review apar doar din recenziile reale ale clienților (când există) |
| Sitemap.xml cu start, catalog, categorii, produse și pagini statice; robots.txt cu linkul către sitemap | `/sitemap.xml` și `/robots.txt` generate de backend |
| HTTPS, PageSpeed și Core Web Vitals, mobile-first | HTTPS la publicare, fonturi preîncărcate, imagini cu dimensiuni fixe, același conținut pe mobil și desktop |
| URL-uri simple, cu litere mici | rutele statice sunt corecte; produsele folosesc `/product/ID` (varianta cu nume în URL nu este implementată) |
