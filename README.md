# MobilaViva

Site web de comerț electronic (B2C) pentru mobilier. „Mobila” înseamnă mobilier, nu aplicație mobilă: este un site în browser, cu layout responsive.

**Demo:** https://mobilaviva.vercel.app

## Tehnologii

- **Backend:** Java 17, Spring Boot 3, JPA (API REST, sesiune cu cookie), PostgreSQL
- **Frontend:** Vue 3 + Vue Router, servit de Spring Boot după build (un singur port)
- **Publicare:** Docker, Fly.io (backend), Vercel (frontend)

## Funcții

- meniu de categorii, catalog, căutare cu autocomplete și filtre
- pagină de produs cu imagine, comparator de produse, listă de dorințe
- coș (salvat pe cont după autentificare), conturi de utilizator, comenzi pe cont
- stocuri, promoții, factură cu TVA
- plată cu card de test (Stripe) și administrare (comenzi, produse, utilizatori)
- asistent AI pentru clienți (Claude), cu rezervă locală dacă nu există cheie API
- „Am uitat parola” prin email (Brevo sau SMTP)
- optimizări PageSpeed (date inițiale în HTML, CSS inline, preload imagini, `llms.txt`, sitemap)

## Cerințe

Una dintre variante:

- **JDK 17+**, **Maven** și **Node 18+**
- **Docker** (Docker Desktop)

Aplicația folosește doar PostgreSQL. Cu Docker, baza de date pornește automat; fără Docker ai nevoie de un PostgreSQL propriu (de exemplu Neon).

## Instalare și pornire

1. Clonează repo-ul:

   ```bash
   git clone https://github.com/Ed-2-1-9-2/mobilaviva.git
   cd mobilaviva
   ```

2. Creează fișierul de configurare:

   ```bash
   cp .env.example .env
   ```

   Completează valorile în `.env` (vezi tabelul de mai jos). **Nu urca niciodată `.env` pe git.**

3. Pornește aplicația:

   ```bash
   ./scripts/run.sh          # local, cu JDK/Maven/Node sau Docker
   # sau, doar cu Docker:
   docker compose up --build
   ```

   Implicit aplicația rulează pe portul 8000 (`HOST_PORT` pentru alt port). Dacă portul e ocupat, `run.sh` alege automat următorul liber.

4. Deschide `http://localhost:8000` (sau adresa afișată de script).

## Configurare (`.env`)

| Variabilă | Descriere |
|---|---|
| `SITE_HOST` | Numele site-ului (`localhost` pentru dezvoltare) |
| `DATABASE_URL` | Adresa PostgreSQL (din Docker, Neon etc.) |
| `ADMIN_PASSWORD` | Parola contului de admin. Schimb-o, nu folosi valoarea implicită |
| `STRIPE_SECRET_KEY`, `STRIPE_PUBLISHABLE_KEY`, `STRIPE_WEBHOOK_SECRET` | Plăți Stripe (chei de test) |
| `ANTHROPIC_API_KEY` | Opțional: asistentul AI. Fără ea, asistentul caută local în catalog |
| `BREVO_API_KEY`, `MAIL_FROM` | Email „Am uitat parola” prin Brevo |
| `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASSWORD` | Rezervă prin SMTP (parolă de aplicație pentru Gmail/Yahoo/iCloud) |

Contul de admin este `admin@mobilaviva.com`, cu parola din `ADMIN_PASSWORD`.

## Structura proiectului

```
src/            cod Java (backend)
frontend/       cod Vue 3
scripts/        run, deploy, push, verifica, screenshots, measure_speed, package
docs/           prezentare, documentare, instrucțiuni de publicare
.runtime/       fișiere generate la rulare (ignorat de git)
```

În rădăcină rămân fișierele cerute de unelte: `pom.xml`, `Dockerfile`, `docker-compose.yml`, `fly.toml`, `render.yaml`, `.env.example`.

## Verificare

```bash
./scripts/verifica.sh      # verifică cerințele proiectului
./scripts/check_vercel.sh  # verifică site-ul publicat (pagină, API, login de test)
```

## Publicare

Pe scurt: backendul se publică pe Fly.io (`./scripts/deploy.sh`), iar frontendul pe Vercel (`./scripts/push.sh` sau `./scripts/vercel_deploy.sh`). Pașii completi sunt în [docs/DEPLOY.md](docs/DEPLOY.md).

## Securitate

- `.env` este în `.gitignore`. Păstrează secretele doar acolo sau în variabilele de mediu ale hostingului.
- Dacă o cheie sau o parolă a ajuns vreodată într-un repo public, regenereaz-o din dashboard-ul serviciului respectiv.
- Pe un site public nu lăsa parola de admin implicită.

## Licență

Proiectul este distribuit sub licența [MIT](LICENSE).

Imaginile, fonturile, textele și bibliotecile terțe pot avea licențe proprii. Verifică-le înainte de publicare.

## Autor

Ed-2-1-9-2 · https://github.com/Ed-2-1-9-2