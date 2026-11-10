# Publicare pe web (cerința obligatorie 1)

Aplicația citește `PORT`, `DATABASE_URL` (format `postgresql://...` sau JDBC), `ADMIN_PASSWORD`, `STRIPE_SECRET_KEY`
și își deduce singură adresa publică pe Render/Railway (`APP_BASE_URL`), deci merge pe orice host cu Docker.

## Webhook Stripe (confirmare automată a plății)
Dashboard Stripe (mod test) → Developers → Webhooks → Add endpoint: `https://LINK/api/stripe/webhook`, eveniment `checkout.session.completed`.
Copiezi „Signing secret” (`whsec_...`) în variabila `STRIPE_WEBHOOK_SECRET`. Local: `stripe listen --forward-to localhost:8000/api/stripe/webhook`.
Comanda și factura se creează la întoarcerea din Stripe SAU din webhook (idempotent, o singură comandă per sesiune).

## Varianta A: Render
1. Urcă proiectul pe GitHub (fără `target/`, `data/`, `node_modules/`: sunt deja în `.gitignore`).
2. render.com → New → Blueprint → alegi repo-ul (folosește `render.yaml`: serviciu web Docker; baza de date e Neon).
3. Baza de date: neon.tech → New project (gratuit, fără expirare) → copiezi connection string-ul `postgresql://...` în variabila `DATABASE_URL`.
   La variabile completezi și `ADMIN_PASSWORD` și (pentru Stripe test) `STRIPE_SECRET_KEY=sk_test_...`.
4. După deploy ai link-ul `https://mobilaviva.onrender.com` (sau similar). Planurile gratuite pot adormi după inactivitate:
   deschide site-ul cu 1-2 minute înainte de prezentare și de testul PageSpeed.
   Pe Fly.io backend-ul rămâne pornit permanent (`auto_stop_machines = false`), deci nu e nevoie de keep-alive sau de monitor extern.

## Varianta 0: Fly.io, FĂRĂ git, cu domeniul tău (o singură comandă)
`./scripts/deploy.sh magazin.exemplu.ro` construiește imaginea Docker direct din folderul proiectului, o publică cu HTTPS,
trimite cheile din `.env` ca secrete și atașează domeniul (îți afișează înregistrările DNS de adăugat). Fără argument: `https://NUME.fly.dev`.
Baza de date este PostgreSQL extern (`DATABASE_URL` din `.env`, de exemplu Neon), deci conturile și comenzile se păstrează la redeploy. Parola de admin nu e niciodată cea implicită: dacă `ADMIN_PASSWORD` din `.env` lipsește sau e `Admin123!`, se generează una aleatorie (`.runtime/admin-password`) și se afișează la final. Necesită cont Fly.io (cere card); flyctl se instalează singur dacă lipsește. După deploy, `vercel.json` arată automat spre Fly. Linkul final se salvează în `docs/link-public.txt` și intră în documentare.

## Varianta B: Railway
1. New Project → Deploy from GitHub repo (detectează `Dockerfile`).
2. Add → Database → PostgreSQL.
3. În serviciul web → Variables: `DATABASE_URL = ${{Postgres.DATABASE_URL}}`, `ADMIN_PASSWORD`, `STRIPE_SECRET_KEY`, `COOKIE_SECURE=true`.
4. Settings → Networking → Generate Domain. Acesta este link-ul public.

Aplicația rulează doar pe PostgreSQL: fără `DATABASE_URL` (sau fără baza locală pornită de `run.sh`) serverul nu pornește.

## Verificare după publicare
- `https://LINK/` afișează catalogul, iar `https://LINK/admin` cere autentificare (admin@mobilaviva.com).
- Plata: card `4242 4242 4242 4242`, orice dată viitoare, orice CVC (cu `STRIPE_SECRET_KEY` setat).
- Apoi rulezi https://pagespeed.web.dev pe link (Mobile și Desktop) și salvezi print screen-urile 18 și 19.

## Nume de domeniu
`mobilaviva.com` local (prin fișierul hosts) nu este un site publicat și nu poate fi testat pe pagespeed.web.dev.
Pentru testul PageSpeed folosești linkul public de pe Render/Railway sau un domeniu pe care îl deții
(Render: Settings → Custom Domains; Railway: Settings → Networking → Custom Domain).
Cheile Stripe se pun în Environment pe host, nu în cod.

## Link public temporar (fără hosting, pentru PageSpeed)
`./scripts/public.sh` pornește aplicația + un tunel Cloudflare gratuit (fără cont) și afișează un link `https://....trycloudflare.com`.
`./scripts/public.sh screenshots` rulează în plus toate print screen-urile pe acel link, inclusiv PageSpeed (18/19).
Linkul e temporar (se schimbă la fiecare pornire); pentru un link stabil folosești Render/Railway sau un domeniu propriu.

## Dacă nu publici
Regulamentul permite atașarea site-ului arhivat: rulează `./scripts/package.sh nume_prenume` și încarci `site_nume_prenume.zip`.
