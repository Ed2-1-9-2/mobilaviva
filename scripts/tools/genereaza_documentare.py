#!/usr/bin/env python3
"""Generează documentarea (.docx) și prezentarea (.pptx) din docs/screenshots/.
Textul se editează în docs/continut-documentare.json (creat la prima rulare, nu se suprascrie).
Documentele nu conțin date personale."""
import argparse, json, re
from pathlib import Path
from docx import Document
from docx.shared import Inches, Pt
from docx.enum.text import WD_ALIGN_PARAGRAPH
from pptx import Presentation
from pptx.util import Inches as PI, Pt as PP
from PIL import Image

DEFAULT = {
    "tip": "B2C (business to consumer): magazin online care vinde direct persoanelor fizice.",
    "tip_detalii": [
        "Clientul își creează cont, adaugă produse în coș, plătește online și primește factură.",
        "Plata se face electronic, cu card, în modul de test (Stripe, card de test 4242 4242 4242 4242).",
        "Livrarea este gratuită peste 1500 RON, altfel 49,99 RON."],
    "produse": "Mobilier pentru locuință (canapele, paturi, mese, scaune, dulapuri).",
    "produse_motiv": [
        "Cumpărătură cu valoare medie ridicată și cerere constantă.",
        "Produsele se pretează la prezentare vizuală, comparare și planificare a camerei.",
        "Permit personalizări utile: comparator de produse și planificator de cameră."],
    "site_model": "Din cele 10 magazine analizate în Studiul de caz 1 (5 din România, 5 din străinătate) am folosit ca model două site-uri: Mobexpert (mobexpert.ro), locul 1 în topul vânzătorilor de mobilă din România, și home24.de (Germania), cel mai bun scor de viteză mobilă dintre cele 10 site-uri.",
    "site_model_concluzie": [
        "Mobexpert (model pentru România): cel mai larg set de metode de plată (card, rate fără dobândă până la 48 de rate, ramburs, link de plată, ordin de plată), mega-meniu pe camere și rețea de magazine pentru vizionare. Punctul slab observat: termenul de livrare apare doar pe pagina produsului, iar LCP pe mobil este 11,3 s. În MobilaViva afișez termenul și costul total de livrare încă din pagina produsului, cu o politică unică de livrare.",
        "home24.de (model pentru străinătate): cel mai bun scor mobil dintre cele 10 site-uri (69, LCP 4,1 s) și cel mai mic LCP pe desktop (0,8 s), recenzii Trustpilot (4,5/5) pentru încredere, plată pe factură și retur în 30 de zile. Preiau paginile ușoare, încrederea prin recenzii, plata pe factură și termenul de livrare clar; țintă LCP sub 2,5 s.",
        "Din auditul de funcționalitate (1.1) am reținut trei criterii decisive pentru mobilă: termenul și costul total afișate din pagina produsului, dimensiunile exacte și o politică de retur ușor de găsit. Datele ECDB (aprox. 88,5–89% abandon al coșului) arată că transparența costurilor și metodele de plată contează direct pentru vânzări."],
    "platforma": "Dezvoltare proprie (realizare manuală a site-ului, varianta a treia din cerință), fără platformă de comerț electronic gata făcută. PrestaShop (freeware) a fost ales ca model funcțional de referință, iar aplicația este implementată în Java Spring Boot (backend REST) și Vue 3 (frontend SPA).",
    "platforma_concluzie": [
        "Din cele 9 produse analizate în Studiul de caz 2 (comerciale: Shopify, BigCommerce, X-Cart; freeware: WooCommerce, PrestaShop, OpenCart; românești: Gomag, MerchantPro, Extended) am testat Shopify (1.1) și PrestaShop (1.2: instalare, produs cu combinații, TVA pentru România, comandă și factură PDF) și am ales PrestaShop ca model.",
        "De ce nu o platformă comercială: abonamente lunare (Shopify și BigCommerce 39–399 USD/lună, Gomag 7–97 €/lună), comision de tranzacție la Shopify (2,5–2,9% + 0,30 USD) și control redus asupra codului; X-Cart cere server propriu, iar la X-Cart și Extended prețul este doar la cerere.",
        "De ce PrestaShop ca model: gratuit, cu combinații de atribute (culoare, dimensiune, material) cu stoc și preț propriu, TVA pe țări, monede multiple și factură PDF native, adică exact funcțiile unui magazin de mobilă; WooCommerce și OpenCart le obțin prin plugin-uri sau extensii.",
        "De ce aplicație proprie, nu PrestaShop instalat: PrestaShop este un monolit PHP și nu acoperă cerința de backend REST în Java și frontend SPA în Vue 3. Preiau modelul funcțional (variante, stocuri, TVA, comenzi, factură) și îl implementez cu entități JPA, cu control complet asupra codului și cost zero de licență."],
    "limbaj": "Java 17 (Spring Boot) pentru backend; JavaScript (Vue 3) pentru frontend.",
    "tehnologii": [
        "Backend: Java, Spring Boot, Spring Data JPA, Maven.",
        "Frontend: Vue 3 (aplicație SPA), Vue Router.",
        "Plăți: Stripe Checkout (mod test) și card simulat local.",
        "Rulare: Docker sau Maven local; proiectul se predă ca arhivă zip."],
    "baza_date": [
        "Sistem de baze de date: PostgreSQL (local în Docker, online pe Render/Neon).",
        "Tabele principale: product, app_users, orders, order_items, wish_items, invoice_seq."],
    "arhitectura": [
        "Straturi separate: controller, service, repository, entity.",
        "API REST consumat de frontend-ul Vue.",
        "Parole stocate cu PBKDF2 și salt.",
        "Facturile se generează după confirmarea plății."],
    "personalizari": [
        ["Planificator de cameră „Încape la mine?” (modulul reprezentativ)",
         "Introduci dimensiunile camerei (cm), adaugi piese din catalog și le tragi pe un plan 2D la scară. Piesele care nu încap sau se suprapun devin roșii, iar aplicația arată cât din podea e ocupat și dă un verdict. Piesele se pot rota și adăuga în coș.",
         "Implementat"],
        ["Recenzii reale pe produs",
         "Pe pagina produsului apar stelele și textul clienților. Doar cine a comandat produsul (comandă neanulată) poate scrie o recenzie, una singură pe produs, pe care o poate modifica sau șterge. Media devine ratingul produsului (catalog, sortare, asistent AI), iar în datele structurate JSON-LD apar AggregateRating și Review, doar din recenzii reale.",
         "Implementat"],
        ["Planificator → coș într-un click",
         "Din planul camerei, butonul „Adaugă toate piesele în coș” grupează piesele (inclusiv copiile) pe produs, limitează cantitatea la stoc, omite produsele epuizate, afișează totalul și avertizează dacă unele piese nu încap în cameră.",
         "Implementat"],
        ["Cod de reducere la checkout",
         "În coș se poate introduce un cod promoțional (BUNVENIT10: -10%, o singură dată pe cont; SALON100: -100 RON la comenzi de minimum 1000 RON). Reducerea se calculează pe server, apare ca linie separată în coș, în proforma, în comandă și în factura cu TVA și se transmite la Stripe ca reducere, astfel încât totalul plătit este cel din coș.",
         "Implementat"],
        ["Estimare de livrare și montaj opțional",
         "Coșul și checkout-ul arată data estimată a livrării (cel mai lent produs din coș). Clientul poate adăuga montaj la livrare (tarif pe bucată, după categorie), ca linie separată în factură și în plata Stripe; montajul adaugă o zi la estimare.",
         "Implementat"],
        ["Văzute recent și „Clienții au cumpărat și…”",
         "Pe pagina produsului apar ultimele produse deschise (doar în browser, localStorage) și produsele cumpărate cel mai des împreună cu cel curent, calculate din istoricul comenzilor.",
         "Implementat"],
        ["Comparator de produse", "Până la 3 produse într-un tabel side-by-side (preț, rating, material, dimensiuni, culoare, stoc, livrare), cu adăugare în coș.", "Implementat"],
        ["Căutare cu autocomplete", "Sugestii în timp real din denumiri, brand și material; rezultate parțiale când nu există potriviri complete.", "Implementat"],
        ["Estimare livrare pe coș", "Termenul de livrare se calculează după cel mai lent produs din coș; transport gratuit peste 1500 RON.", "Implementat"],
        ["Ilustrații de mobilier generate", "Fiecare produs are o imagine vectorială potrivită tipului și culorii; fotografiile reale se pot seta din Admin.", "Implementat"],
        ["Set asortat „completează camera”", "Pe pagina produsului apar piese din aceeași colecție (ex. pat Luna + noptieră Luna) și din categorii complementare, în stoc; setul complet se adaugă în coș dintr-un click, cu totalul afișat.", "Implementat"],
        ["Listă de dorințe", "Produse favorite salvate pe cont (♡). Pagina /wishlist arată prețul de la salvare și o alertă 🔔 când prețul a scăzut; contorul apare în meniu.", "Implementat"],
        ["Asistent AI pentru clienți", "Chatbot pe toate paginile: recomandă produse din catalog după buget, categorie și nevoi, răspunde despre livrare, retur și plată. Folosește Claude (Anthropic API) cu catalogul la zi, iar produsele afișate sunt validate în baza de date; fără cheie API rulează o rezervă locală de căutare în catalog. Panoul se încarcă la primul click (fără impact asupra PageSpeed).", "Implementat"]],
    "modul_reprezentativ": [
        "Flux: pagina produsului, butonul „Încape în camera ta?”, pagina /planner, dimensiunile camerei, tragerea pieselor pe plan, verdictul.",
        "Algoritm: amprenta la sol se extrage din dimensiunile produsului; se verifică încadrarea în cameră și suprapunerea fiecărei piese; se calculează procentul de podea ocupat (peste 50% se avertizează).",
        "Implementare: Vue 3 (SVG interactiv cu tragere), Planner.vue și footprint.js."],
    "instalare": [
        "1. Necesită JDK 17+, Maven și Node 18+ (sau Docker).",
        "2. ./scripts/run.sh pornește aplicația; deschide http://localhost:PORT.",
        "3. Pentru plata Stripe de test: export STRIPE_SECRET_KEY=sk_test_... înainte de run.sh.",
        "4. Link public: ./scripts/public.sh; print screen-uri: ./scripts/screenshots.sh."],
    "concluzii": [
        "Proiectul acoperă fluxul complet: catalog, cont, coș, plată electronică de test, comandă, factură.",
        "Plata electronică a fost testată cu cont și card de test.",
        "Scorurile PageSpeed (Mobile și Desktop) sunt în capturile 18 și 19."],
}

def cap(name):
    s = re.sub(r"^\d+[a-z]?-", "", Path(name).stem).replace("-", " ").strip()
    return s[:1].upper() + s[1:]

def load_desc(shots):
    d = {}
    f = shots / "LISTA.txt"
    if f.exists():
        for ln in f.read_text(encoding="utf-8").splitlines():
            m = re.match(r"^(\S+\.png)\s+(.*)$", ln)
            if m: d[m.group(1)] = m.group(2).strip()
    return d

REQS = [
    ("Obligatoriu 2", "Sistem de baze de date", "PostgreSQL: product, app_users, orders, order_items, wish_items, invoice_seq", ["02"]),
    ("Obligatoriu 3", "Script-uri pe server", "Java Spring Boot: controllere REST, servicii, entități JPA", ["03"]),
    ("Obligatoriu 4", "Sistem de plată electronic", "Stripe Checkout, mod test (card 4242 4242 4242 4242)", ["04"]),
    ("I.1", "Navigare cu meniuri", "Bara de categorii + Oferte (Vue Router)", ["05"]),
    ("I.2", "Catalog de produse", "Filtre, sortare, căutare cu autocomplete, paginare", ["06"]),
    ("I.3", "Grupare pe categorii", "Filtrare după categorie (/?category=...)", ["07"]),
    ("I.4", "Detalii produs", "Imagine, preț, caracteristici, stoc, livrare", ["08"]),
    ("I.5", "Coș + actualizare", "/cart: modificare cantitate, ștergere, transport", ["09"]),
    ("I.6", "Înregistrarea comenzii pe cont", "Înregistrare/login, /orders", ["10"]),
    ("I.7", "Stocuri", "Stoc epuizat, scade după comandă, editare în Admin", ["11"]),
    ("I.8", "Promoții", "Insignă -X%, preț vechi tăiat, filtrul Doar oferte", ["12"]),
    ("I.9", "Emiterea facturii", "Articole, preț unitar, valori, TVA 21%", ["13"]),
    ("I.10", "Plata facturii (electronică, testată)", "Stripe în mod test, comandă confirmată", ["14"]),
    ("I.11", "Interfață de administrare", "Comenzi, produse (CRUD, stoc, promoții), utilizatori", ["15"]),
    ("II", "Personalizări + modul reprezentativ", "Planificator de cameră (reprezentativ), comparator, set asortat, listă de dorințe, asistent AI (chatbot), autocomplete, estimare livrare, ilustrații", ["16-", "16b", "16c", "16d", "20"]),
    ("III", "Test PageSpeed", "Rezultat Mobile și Desktop", ["18", "19"]),
]

def matrix(files):
    names = [f.name for f in files]
    rows = []
    for cod, cer, impl, pref in REQS:
        hit = [next((n for n in names if n.startswith(x)), None) for x in pref]
        rows.append((cod, cer, impl, "✔ " + ", ".join(h for h in hit if h) if all(hit) else "✘ lipsește print screen"))
    return rows

def has_todo(x):
    if isinstance(x, str): return "[COMPLETEAZĂ" in x
    if isinstance(x, dict): return any(has_todo(v) for v in x.values())
    if isinstance(x, list): return any(has_todo(v) for v in x)
    return False

def _fmt(d):
    """Format compact A4: margini mici, fonturi și spații reduse (documentația trebuie să rămână la ~20-21 pagini)."""
    from docx.shared import Cm, RGBColor
    sec = d.sections[0]
    sec.page_width, sec.page_height = Cm(21.0), Cm(29.7)
    sec.top_margin = sec.bottom_margin = Cm(1.6); sec.left_margin = sec.right_margin = Cm(2.0)
    st = d.styles
    st["Normal"].font.name = "Calibri"; st["Normal"].font.size = Pt(10)
    st["Normal"].paragraph_format.space_after = Pt(3); st["Normal"].paragraph_format.space_before = Pt(0)
    st["Normal"].paragraph_format.line_spacing = 1.0
    st["List Bullet"].paragraph_format.space_after = Pt(1)
    for name, size, before, after in (("Title", 20, 0, 4), ("Heading 1", 14, 8, 3), ("Heading 2", 11, 5, 2)):
        s = st[name]; s.font.size = Pt(size); s.font.name = "Calibri"; s.font.color.rgb = RGBColor(0x1F, 0x3A, 0x5F)
        s.paragraph_format.space_before = Pt(before); s.paragraph_format.space_after = Pt(after)
        s.paragraph_format.keep_with_next = True

USABLE_W_CM = 16.8   # 21 - 2 x 2.0 margini

def build_docx(path, C, rest, pers, speed, desc, link, hcap=9.0):
    """hcap = înălțimea maximă (cm) a unei capturi; se alege automat de main() ca documentul să încapă în limita de pagini."""
    from docx.shared import Cm
    d = Document(); _fmt(d)
    h = lambda t, l=1: d.add_heading(t, level=l)
    p = lambda t: d.add_paragraph(t)
    def bl(items):
        for i in items: d.add_paragraph(i, style="List Bullet")
    fig = [0]
    def img(f):
        w, hh = Image.open(f).size
        wc = min(USABLE_W_CM, hcap * w / hh)          # încape în lățimea paginii ȘI în înălțimea maximă
        d.add_picture(str(f), width=Cm(wc))
        ip = d.paragraphs[-1]; ip.alignment = WD_ALIGN_PARAGRAPH.CENTER
        ip.paragraph_format.keep_with_next = True; ip.paragraph_format.space_after = Pt(1)
        fig[0] += 1
        c = d.add_paragraph(f"Fig. {fig[0]}. " + (desc.get(f.name) or cap(f.name))); c.alignment = WD_ALIGN_PARAGRAPH.CENTER
        c.paragraph_format.space_after = Pt(6)
        for r in c.runs: r.italic = True; r.font.size = Pt(9)
    d.add_heading("Documentația proiectului de comerț electronic", 0); p("Magazin online de mobilier.")
    h("1. Tipul de comerț electronic"); p(C["tip"]); bl(C["tip_detalii"])
    h("2. Produse comercializate"); p(C["produse"]); bl(C["produse_motiv"])
    h("3. Site folosit ca model (Studiul de caz 1)"); p(C["site_model"]); bl(C["site_model_concluzie"])
    h("4. Platforma software (Studiul de caz 2)"); p(C["platforma"]); bl(C["platforma_concluzie"])
    h("5. Tehnologii folosite"); p(C["limbaj"]); bl(C["tehnologii"])
    h("6. Baza de date"); bl(C["baza_date"])
    h("7. Arhitectură și securitate"); bl(C["arhitectura"])
    h("8. Publicare")
    p("Site publicat la: " + link if link else "Site-ul este atașat în arhiva zip (folderul site/).")
    h("9. Conformitate cu cerințele proiectului")
    t = d.add_table(rows=1, cols=4); t.style = "Table Grid"
    for i, x in enumerate(("Cerință", "Denumire", "Implementare", "Print screen")): t.rows[0].cells[i].text = x
    for row in matrix(rest + pers + speed):
        cells = t.add_row().cells
        for i, x in enumerate(row): cells[i].text = x
    for row in t.rows:                                  # tabel compact: font 8.5 pt, fără spații între paragrafe
        for c in row.cells:
            for para in c.paragraphs:
                para.paragraph_format.space_after = Pt(0)
                for r in para.runs: r.font.size = Pt(8.5)
    for c in t.rows[0].cells:
        for r in c.paragraphs[0].runs: r.bold = True
    h("10. Cerințele proiectului, cu print screen")
    if not rest: p("(Nu s-au găsit capturi în docs/screenshots/. Rulează ./scripts/screenshots.sh.)")
    for f in rest: img(f)
    h("11. Personalizări")
    p("Elemente de originalitate identificate, proiectate și implementate (toate cele 7 sunt funcționale; modulul reprezentativ este detaliat mai jos):")
    for it in C["personalizari"]:                       # un singur paragraf pe personalizare (titlu îngroșat + descriere)
        para = d.add_paragraph(style="List Bullet")
        r = para.add_run(it[0] + (f" [{it[2]}]" if len(it) > 2 else "") + ". "); r.bold = True
        para.add_run(it[1])
    h("Modulul reprezentativ implementat: planificatorul de cameră", 2); bl(C["modul_reprezentativ"])
    for f in pers: img(f)
    h("12. Testare PageSpeed"); p("Rezultatul testării cu https://pagespeed.web.dev/ (Mobile și Desktop):")
    if speed:
        for f in speed: img(f)
    else: p("[COMPLETEAZĂ: lipsesc capturile 18/19-pagespeed; rulează ./scripts/public.sh screenshots]")
    h("13. Instalare și rulare"); bl(C["instalare"])
    h("14. Concluzii"); bl(C["concluzii"])
    d.save(path)

def count_pages(docx_path):
    """Numără paginile randate cu LibreOffice (dacă există); None dacă nu se poate măsura."""
    import shutil, subprocess, tempfile
    so = shutil.which("soffice") or shutil.which("libreoffice")
    if not so: return None
    with tempfile.TemporaryDirectory() as td:
        try:
            subprocess.run([so, "--headless", "--convert-to", "pdf", "--outdir", td, str(docx_path)],
                           stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=180, check=True)
            pdf = Path(td) / (Path(docx_path).stem + ".pdf")
            return len(re.findall(rb"/Type\s*/Page(?![a-zA-Z])", pdf.read_bytes()))
        except Exception:
            return None

def fit_docx(path, C, rest, pers, speed, desc, link, max_pages, hmax=11.0, hmin=4.0):
    """Alege cea mai mare înălțime de captură (hmin..hmax cm) pentru care documentul are cel mult max_pages pagini."""
    import tempfile
    if max_pages <= 0:
        build_docx(path, C, rest, pers, speed, desc, link, hmax); return None, hmax
    with tempfile.TemporaryDirectory() as td:
        probe = Path(td) / "probe.docx"
        def pages(hc):
            build_docx(probe, C, rest, pers, speed, desc, link, hc); return count_pages(probe)
        n = pages(hmax)
        if n is None:                                   # fără LibreOffice: estimare din suprafața capturilor
            total = 0.0
            for f in rest + pers + speed:
                w, hh = Image.open(f).size; total += min(hmax, USABLE_W_CM * hh / w) + 0.9
            hc = hmax
            text_pages = 5.0; avail = (max_pages - 1 - text_pages) * 25.0 * 0.9
            if total > avail: hc = max(hmin, hmax * avail / total)
            build_docx(path, C, rest, pers, speed, desc, link, hc); return None, hc
        if n <= max_pages:
            build_docx(path, C, rest, pers, speed, desc, link, hmax); return n, hmax
        lo, hi, best = hmin, hmax, hmin                 # căutare binară pe înălțime
        for _ in range(7):
            mid = (lo + hi) / 2; n = pages(mid)
            if n is not None and n <= max_pages: best, lo = mid, mid
            else: hi = mid
        build_docx(path, C, rest, pers, speed, desc, link, best)
        return pages(best), best

def build_pptx(path, C, rest, pers, speed, desc, link):
    pr = Presentation(); pr.slide_width = PI(13.333); pr.slide_height = PI(7.5); blank = pr.slide_layouts[6]
    def title(s, t):
        tf = s.shapes.add_textbox(PI(0.6), PI(0.3), PI(12.1), PI(0.9)).text_frame; tf.text = t
        tf.paragraphs[0].runs[0].font.size = PP(30); tf.paragraphs[0].runs[0].font.bold = True
    def text(t, lines):
        s = pr.slides.add_slide(blank); title(s, t)
        tf = s.shapes.add_textbox(PI(0.8), PI(1.5), PI(11.7), PI(5.3)).text_frame; tf.word_wrap = True
        for i, ln in enumerate(lines):
            para = tf.paragraphs[0] if i == 0 else tf.add_paragraph()
            para.text = "• " + ln; para.runs[0].font.size = PP(22); para.space_after = PP(10)
    def pic(t, f):
        s = pr.slides.add_slide(blank); title(s, t)
        w, h = Image.open(f).size; bw, bh = 12.1, 5.7; k = min(bw / w, bh / h)
        s.shapes.add_picture(str(f), PI(0.6 + (bw - w * k) / 2), PI(1.3 + (bh - h * k) / 2), PI(w * k), PI(h * k))
    text("Magazin online de mobilier", ["Prezentarea proiectului de comerț electronic"])
    text("Tipul de comerț electronic", [C["tip"]] + C["tip_detalii"])
    text("Produse comercializate", [C["produse"]] + C["produse_motiv"])
    text("Site model (Studiul de caz 1)", [C["site_model"]] + C["site_model_concluzie"])
    text("Platforma software (Studiul de caz 2)", [C["platforma"]] + C["platforma_concluzie"])
    text("Tehnologii", [C["limbaj"]] + C["tehnologii"])
    text("Baza de date", C["baza_date"])
    text("Publicare", ["Site publicat la: " + link] if link else ["Site-ul este atașat în arhiva zip (folderul site/)."])
    s = pr.slides.add_slide(blank); title(s, "Conformitate cu cerințele")
    mx = matrix(rest + pers + speed)
    tbl = s.shapes.add_table(len(mx) + 1, 3, PI(0.4), PI(1.2), PI(12.5), PI(5.9)).table
    for i, x in enumerate(("Cerință", "Denumire", "Print screen")): tbl.cell(0, i).text = x
    for r_, row in enumerate(mx, 1):
        for c_, x in enumerate((row[0], row[1], row[3])): tbl.cell(r_, c_).text = x
    for r_ in range(len(mx) + 1):
        for c_ in range(3):
            for para in tbl.cell(r_, c_).text_frame.paragraphs:
                for run in para.runs: run.font.size = PP(11)
    for f in rest: pic(cap(f.name), f)
    text("Personalizări (module)", [it[0] + (f" [{it[2]}]" if len(it) > 2 else "") for it in C["personalizari"]])
    text("Modulul reprezentativ: planificatorul de cameră", C["modul_reprezentativ"])
    for f in pers: pic(cap(f.name), f)
    if speed:
        for f in speed: pic("Rezultat PageSpeed", f)
    else: text("Rezultat PageSpeed", ["[COMPLETEAZĂ: lipsesc capturile 18/19-pagespeed]"])
    text("Concluzii", C["concluzii"])
    pr.save(path)

def main():
    a = argparse.ArgumentParser()
    a.add_argument("--shots", default="docs/screenshots"); a.add_argument("--out", default="docs/documentare")
    a.add_argument("--continut", default="docs/continut-documentare.json"); a.add_argument("--link", default=None)
    a.add_argument("--ps-extra", type=int, default=2, help="câte ecrane suplimentare din raportul PageSpeed intră în documentare, pe dispozitiv (implicit 2)")
    a.add_argument("--pagini", type=int, default=21, help="număr maxim de pagini pentru documentatie.docx (0 = fără limită)")
    a = a.parse_args()
    cf = Path(a.continut)
    if not cf.exists():
        cf.parent.mkdir(parents=True, exist_ok=True)
        cf.write_text(json.dumps(DEFAULT, ensure_ascii=False, indent=2), encoding="utf-8")
        print(f"Creat {cf} (editează-l: textele din documentare, mai ales zonele [COMPLETEAZĂ ...])")
    C = {**DEFAULT, **json.loads(cf.read_text(encoding="utf-8"))}
    migrat = [k for k in DEFAULT if has_todo(C.get(k)) and not has_todo(DEFAULT[k])]
    for k in migrat: C[k] = DEFAULT[k]      # zonele [COMPLETEAZĂ] din JSON-ul existent se înlocuiesc cu textele completate (restul rămâne cum l-ai editat)
    if migrat:
        cf.write_text(json.dumps(C, ensure_ascii=False, indent=2), encoding="utf-8"); print("Completate automat în", cf, ":", ", ".join(migrat))
    shots = Path(a.shots); desc = load_desc(shots)
    psf = shots / "PAGESPEED.txt"
    if psf.exists():     # scoruri citite automat din raportul PageSpeed (Performanță / Accesibilitate / Bune practici / SEO)
        sc = "; ".join(l.strip() for l in psf.read_text(encoding="utf-8").splitlines() if l.strip())
        if sc: C["concluzii"] = [c for c in C["concluzii"] if not c.startswith("Scor")] + ["Scoruri PageSpeed (Performanță / Accesibilitate / Bune practici / SEO): " + sc + "."]
    files = sorted(shots.glob("*.png")) if shots.is_dir() else []
    pers = [f for f in files if f.name.startswith("16")]
    speed_all = [f for f in files if re.match(r"^(18|19)-", f.name)]
    # PageSpeed: ecranul cu scorurile (18-pagespeed-mobile.png / 19-...desktop.png) + cel mult --ps-extra ecrane din raport pe dispozitiv
    # (restul de zeci de ecrane rămân în docs/screenshots/ și în PREZENTARE.html, dar nu umflă documentația)
    speed, _cnt = [], {}
    for f in speed_all:
        if not re.search(r"-\d\d\.png$", f.name): speed.append(f); continue
        k = f.name[:2]; _cnt[k] = _cnt.get(k, 0) + 1
        if _cnt[k] <= a.ps_extra: speed.append(f)
    rest = [f for f in files if f not in pers and f not in speed_all]
    out = Path(a.out); out.mkdir(parents=True, exist_ok=True)
    npag, hc = fit_docx(out / "documentatie.docx", C, rest, pers, speed, desc, a.link, a.pagini)
    build_pptx(out / "prezentare.pptx", C, rest, pers, speed, desc, a.link)
    print(f"Documentare generată în {out}/ : documentatie.docx, prezentare.pptx "
          f"({len(rest)} capturi cerințe, {len(pers)} personalizări, {len(speed)} din {len(speed_all)} PageSpeed)")
    if a.pagini > 0:
        print(f"documentatie.docx: {npag if npag else '~'+str(a.pagini)} pagini (limită {a.pagini}; capturi de cel mult {hc:.1f} cm înălțime)"
              + ("" if npag else " [estimare: instalează LibreOffice pentru numărătoare exactă; în Word verifică numărul de pagini]"))
    if has_todo(C): print(f"ATENȚIE: completează zonele [COMPLETEAZĂ ...] în {cf} și rulează ./scripts/docs.sh din nou.")
    if not speed: print("ATENȚIE: lipsesc capturile PageSpeed (18/19); se fac cu ./scripts/public.sh screenshots")

if __name__ == "__main__":
    main()
