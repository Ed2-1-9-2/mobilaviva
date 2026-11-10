#!/usr/bin/env bash
# sed -i portabil (GNU/Linux și BSD/macOS)
sed_i() { if sed --version >/dev/null 2>&1; then sed -i "$@"; else sed -i '' "$@"; fi; }
# Deploy FĂRĂ git, direct din folderul proiectului (folosește Dockerfile), pe Fly.io, cu domeniu propriu și HTTPS.
#   ./scripts/deploy.sh                      -> https://NUME.fly.dev
#   ./scripts/deploy.sh magazin.exemplu.ro   -> în plus atașează domeniul tău și îți spune ce înregistrări DNS pui
# Necesită: flyctl (https://fly.io/docs/flyctl/install/), cont Fly.io (cere card, cost mic) și un domeniu pe care îl deții (opțional).
# Cheile din .env (STRIPE_SECRET_KEY, ADMIN_PASSWORD, STRIPE_WEBHOOK_SECRET) se trimit ca secrete, nu intră în imagine.
# Rulat din nou, redeployează aplicația existentă. Baza de date: dacă .env are DATABASE_URL (ex. Neon) se folosește aceea; altfel se creează automat PostgreSQL pe Fly.
set -e
cd "$(dirname "$0")/.."
./scripts/translate.sh
find_fly() {   # caută flyctl în PATH și în locurile unde îl pune instalatorul (Git Bash nu reîncarcă PATH-ul)
  local c d n
  for c in flyctl fly flyctl.exe fly.exe; do command -v "$c" 2>/dev/null && return 0; done
  for d in "$HOME/.fly/bin" "$(cygpath -u "${USERPROFILE:-}" 2>/dev/null)/.fly/bin" "$(cygpath -u "${LOCALAPPDATA:-}" 2>/dev/null)/fly" /usr/local/bin; do
    for n in flyctl.exe fly.exe flyctl fly; do [ -f "$d/$n" ] && { echo "$d/$n"; return 0; }; done
  done
  return 1
}
FLY="$(find_fly || true)"
if [ -z "$FLY" ]; then
  echo ">>> flyctl nu e instalat. Îl instalez acum..."
  case "$(uname -s)" in
    MINGW*|MSYS*|CYGWIN*) powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "iwr https://fly.io/install.ps1 -useb | iex" || true ;;
    *) curl -fsSL https://fly.io/install.sh | sh || true ;;
  esac
  export PATH="$HOME/.fly/bin:$PATH"
  FLY="$(find_fly || true)"
fi
[ -n "$FLY" ] || { echo "Nu am putut instala flyctl. Instalează manual (PowerShell: iwr https://fly.io/install.ps1 -useb | iex), redeschide terminalul și rulează din nou ./scripts/deploy.sh" >&2; exit 1; }
echo ">>> flyctl: $FLY"
"$FLY" auth whoami >/dev/null 2>&1 || "$FLY" auth login
DOMAIN="${1:-}"
mkdir -p .runtime
[ -f .runtime/fly-app ] || echo "mobilaviva-$RANDOM$RANDOM" > .runtime/fly-app
APP="$(tr -d '\r\n' < .runtime/fly-app)"
BASE="https://$APP.fly.dev"
FLYBASE="$BASE"
if [ -n "$DOMAIN" ]; then
  # adresa de bază (întoarcerea din Stripe) devine domeniul abia când răspunde; altfel plata ar redirecționa spre o adresă care nu merge încă
  if curl -s -o /dev/null -m 10 "https://$DOMAIN/" 2>/dev/null; then BASE="https://$DOMAIN"
  else echo ">>> Domeniul $DOMAIN nu răspunde încă (DNS/certificat). Folosesc $BASE; după ce adaugi DNS-ul rulează din nou ./scripts/deploy.sh $DOMAIN"; fi
fi
# .env se citește ca date (NU se execută): parolele cu ( ) spații & ; $ ? nu mai strică scriptul. DATABASE_URL se citește separat.
if [ -f .env ]; then
  while IFS= read -r _L || [ -n "$_L" ]; do
    _L="${_L%$'\r'}"
    case "$_L" in ''|'#'*|DATABASE_URL=*) continue;; esac
    _K="${_L%%=*}"; _V="${_L#*=}"
    case "$_K" in ''|*[!A-Za-z0-9_]*) continue;; esac
    export "$_K=$_V"
  done < .env
  unset _L _K _V
fi

# Parola de admin: pe site-ul public NICIODATĂ cea implicită. Dacă în .env e lipsă sau Admin123!, se generează una aleatorie (păstrată în .runtime/admin-password).
PWFILE=.runtime/admin-password
if [ -z "${ADMIN_PASSWORD:-}" ] || [ "$ADMIN_PASSWORD" = "Admin123!" ]; then
  if [ ! -s "$PWFILE" ]; then ( umask 077; LC_ALL=C tr -dc 'A-Za-z0-9' < /dev/urandom | head -c 16 > "$PWFILE" ) 2>/dev/null || true; fi
  [ -s "$PWFILE" ] || { echo "Nu am putut genera parola de admin. Pune ADMIN_PASSWORD=... (nu Admin123!) în .env și rulează din nou." >&2; exit 1; }
  ADMIN_PASSWORD="$(tr -d '\r\n' < "$PWFILE")"
fi

DBLINE="$(grep -E '^DATABASE_URL=' .env 2>/dev/null | tail -1 | cut -d= -f2- | tr -d '\r' | sed -E "s/^[[:space:]'\"]+//; s/[[:space:]'\"]+\$//" || true)"
# Baza de date implicită: Neon (serverless, gratuit, mereu disponibil). Suprascrie cu DATABASE_URL în .env sau cu NEON_DATABASE_URL.
[ -z "$DBLINE" ] && [ "${DB:-}" != flypg ] && DBLINE="${NEON_DATABASE_URL:-}"
[ -n "$DBLINE" ] && DATABASE_URL="$DBLINE"
db_bad() { case "${1:-}" in postgresql://*|postgres://*) case "$1" in *USER:PAROLA@HOST*|*@HOST/*|*@host/*|*"..."*) return 0;; esac; return 1;; esac; return 0; }
USE_FLYPG=0
if db_bad "${DATABASE_URL:-}" && [ "${DB:-}" != neon ]; then
  # fără DATABASE_URL în .env: baza de date PostgreSQL se creează automat pe Fly (fără Neon, fără lipit nimic)
  USE_FLYPG=1
  sed_i '/^DATABASE_URL=/d' .env 2>/dev/null || true
  DATABASE_URL=""
  echo ">>> Fără DATABASE_URL în .env: folosesc PostgreSQL pe Fly, creat automat. (Pentru Neon: lasă implicit sau DB=neon ./scripts/deploy.sh)"
elif db_bad "${DATABASE_URL:-}"; then
  echo ">>> Lipsește DATABASE_URL real (în .env e gol sau exemplul USER:PAROLA@HOST)."
  echo "    Neon: neon.tech -> proiectul tău -> butonul Connect -> copiezi șirul postgresql://..."
  DBIN=""
  # încercare automată (fără copy-paste): Neon CLI, autentificare o dată în browser
  # Deschide linkul de autentificare Neon DOAR în Google Chrome, în profilul conectat cu contul școlii (NEON_GOOGLE_EMAIL); niciodată în Edge.
  open_neon_chrome() {   # $1 = URL
    local url="$1" chrome="" ud="" prof="" c lad
    local email="${NEON_GOOGLE_EMAIL:-you@example.com}"
    lad="$(cygpath -u "${LOCALAPPDATA:-}" 2>/dev/null)"
    for c in "/c/Program Files/Google/Chrome/Application/chrome.exe" "/c/Program Files (x86)/Google/Chrome/Application/chrome.exe" "$lad/Google/Chrome/Application/chrome.exe"; do
      [ -f "$c" ] && { chrome="$c"; break; }
    done
    if [ -z "$chrome" ] && [ "$(uname -s)" = "Darwin" ]; then
      open -a "Google Chrome" "$url" >/dev/null 2>&1 && return 0
    elif [ -z "$chrome" ]; then
      for c in google-chrome google-chrome-stable chromium; do command -v "$c" >/dev/null 2>&1 && { chrome="$(command -v "$c")"; break; }; done
    fi
    if [ -z "$chrome" ]; then
      printf '%s' "$url" | clip.exe 2>/dev/null
      echo "    !!! Nu găsesc Google Chrome. Linkul e copiat în clipboard: lipește-l în Chrome (profilul $email)."
      echo "    $url"
      return 0
    fi
    ud="$lad/Google/Chrome/User Data"
    [ -d "$ud" ] || ud="$HOME/.config/google-chrome"
    [ -d "$ud" ] || ud="$HOME/Library/Application Support/Google/Chrome"
    prof="$(node -e 'const fs=require("fs");try{const j=JSON.parse(fs.readFileSync(process.argv[1],"utf8").replace(/^\uFEFF/,""));const c=j.profile.info_cache;for(const k in c){if((c[k].user_name||"").toLowerCase()===process.argv[2].toLowerCase()){console.log(k);break}}}catch(e){}' "$ud/Local State" "$email" 2>/dev/null | tr -d '\r')"
    local args=()
    if [ -n "$prof" ]; then
      args+=("--profile-directory=$prof")
      echo "    (Chrome, profilul „$prof” = $email)"
    else
      echo "    !!! Nu există în Chrome un profil conectat cu $email; deschid profilul implicit. Conectează-te în Chrome cu acel cont (Google), apoi rulează din nou."
    fi
    "$chrome" "${args[@]}" "$url" >/dev/null 2>&1 &
  }
  neon_auto() {
    command -v node >/dev/null 2>&1 && command -v npx >/dev/null 2>&1 || return 1
    local pj u i log url apid
    # dacă ești deja autentificat în Neon CLI nu se mai deschide browserul
    if ! npx -y neonctl@latest projects list --output json >/dev/null 2>&1; then
      for i in 1 2 3; do
        echo ">>> Autentificare Neon ($i/3): se deschide browserul. Apasă Authorize/Allow în maximum 60 de secunde."
        log="$(mktemp)"
        npx -y neonctl@latest auth >"$log" 2>&1 &
        apid=$!
        url=""
        for _ in $(seq 1 30); do url="$(grep -o 'https://oauth2\.neon\.tech[^ ]*' "$log" 2>/dev/null | head -1)"; [ -n "$url" ] && break; sleep 1; done
        if [ -n "$url" ]; then
          open_neon_chrome "$url"
        fi
        if wait "$apid"; then rm -f "$log"; break; fi
        rm -f "$log"
        [ "$i" = 3 ] && return 1
      done
    fi
    pj="$(npx -y neonctl@latest projects list --output json 2>/dev/null | node -e 'let d="";process.stdin.on("data",c=>d+=c).on("end",()=>{try{const j=JSON.parse(d);const a=Array.isArray(j)?j:(j.projects||[]);const p=a.find(x=>x.name==="mobilaviva")||a[0];if(p)console.log(p.id)}catch(e){}})' | tr -d '\r')"
    [ -n "$pj" ] || return 1
    u="$(npx -y neonctl@latest connection-string --project-id "$pj" --database-name neondb --role-name neondb_owner 2>/dev/null | tr -d '\r' | tail -1)"
    [ -n "$u" ] || return 1
    DBIN="$u"
  }
  neon_auto || echo ">>> Automat nu a mers; continui cu lipire manuală."
  if db_bad "$DBIN"; then DBIN=""
  for _try in 1 2 3 4 5 6; do
    read -r -p ">>> Lipește aici connection string-ul Neon și apasă Enter: " DBIN || break
    DBIN="$(printf '%s' "$DBIN" | tr -d '\r' | sed -E "s/^[[:space:]]*psql[[:space:]]+//; s/^DATABASE_URL=//; s/^[[:space:]'\"]+//; s/[[:space:]'\"]+\$//")"
    case "$DBIN" in ''|\#*|DATABASE_URL_POOLED=*) DBIN=""; continue ;; esac   # la lipirea blocului .env din Neon se sar comentariul și linia „pooled”
    if db_bad "$DBIN"; then echo "!!! Nu e valid: trebuie să înceapă cu postgresql:// și să nu fie exemplul." >&2; DBIN=""; continue; fi
    break
  done
  fi
  [ -n "$DBIN" ] || { echo "Fără DATABASE_URL valid nu pot publica. Rulează din nou ./scripts/deploy.sh" >&2; exit 1; }
  DATABASE_URL="$DBIN"
  { grep -v '^DATABASE_URL=' .env 2>/dev/null || true; echo "DATABASE_URL=$DATABASE_URL"; } > .env.tmp && mv .env.tmp .env
  echo ">>> DATABASE_URL salvat în .env"
fi
# linia din .env fără ghilimele/CRLF, ca run.sh, docker compose și deploy să o citească la fel
if [ "$USE_FLYPG" != 1 ]; then
  { grep -v '^DATABASE_URL=' .env 2>/dev/null || true; echo "DATABASE_URL=$DATABASE_URL"; } > .env.tmp && mv .env.tmp .env
fi
"$FLY" status --app "$APP" >/dev/null 2>&1 || "$FLY" apps create "$APP"
if [ "$USE_FLYPG" = 1 ] && [ ! -f .runtime/fly-db ]; then
  DBAPP="$APP-db"
  echo ">>> Creez PostgreSQL pe Fly ($DBAPP, 1 nod, volum 1 GB, regiunea fra). Durează 1-3 minute..."
  "$FLY" status --app "$DBAPP" >/dev/null 2>&1 || "$FLY" postgres create --name "$DBAPP" --region fra --initial-cluster-size 1 --vm-size shared-cpu-1x --volume-size 1
  ATTACH_OK=0
  "$FLY" postgres attach "$DBAPP" --app "$APP" --database-name mobila --yes && ATTACH_OK=1
  # atașarea reușită sau secretul DATABASE_URL vizibil în listă (fără ancore: formatul tabelului diferă între versiuni flyctl)
  if [ "$ATTACH_OK" = 1 ] || "$FLY" secrets list --app "$APP" 2>/dev/null | grep -q 'DATABASE_URL'; then echo "$DBAPP" > .runtime/fly-db; echo ">>> Baza de date PostgreSQL pe Fly: gata ($DBAPP)"
  else echo "Nu am putut crea/atașa baza de date pe Fly. Vezi eroarea de mai sus; după ce o rezolvi, rulează din nou ./scripts/deploy.sh" >&2; exit 1; fi
fi
cat > fly.toml <<TOML
app = "$APP"
primary_region = "fra"

[env]
  PORT = "8000"
  COOKIE_SECURE = "true"
  APP_BASE_URL = "$BASE"

[http_service]
  internal_port = 8000
  force_https = true
  auto_stop_machines = false
  auto_start_machines = true
  min_machines_running = 1

swap_size_mb = 512

[[restart]]
  policy = "always"

[[vm]]
  memory = "1gb"
  cpu_kind = "shared"
  cpus = 1
TOML

SECRETS=()
[ -n "${STRIPE_SECRET_KEY:-}" ] && SECRETS+=("STRIPE_SECRET_KEY=$STRIPE_SECRET_KEY")
[ -n "${STRIPE_PUBLISHABLE_KEY:-}" ] && SECRETS+=("STRIPE_PUBLISHABLE_KEY=$STRIPE_PUBLISHABLE_KEY")
[ -n "${STRIPE_WEBHOOK_SECRET:-}" ] && SECRETS+=("STRIPE_WEBHOOK_SECRET=$STRIPE_WEBHOOK_SECRET")
[ -n "${ANTHROPIC_API_KEY:-}" ] && SECRETS+=("ANTHROPIC_API_KEY=$ANTHROPIC_API_KEY")
[ -n "${BREVO_API_KEY:-}" ] && SECRETS+=("BREVO_API_KEY=$BREVO_API_KEY")
[ -n "${MAIL_FROM:-}" ] && SECRETS+=("MAIL_FROM=$MAIL_FROM")
_GU="$(grep '^SMTP_USER=' .env 2>/dev/null | tail -1 | cut -d= -f2- | tr -d '\r')"; _GP="$(grep '^SMTP_PASSWORD=' .env 2>/dev/null | tail -1 | cut -d= -f2- | tr -d '\r')"
[ -n "${SMTP_USER:-$_GU}" ] && SECRETS+=("SMTP_USER=${SMTP_USER:-$_GU}")
[ -n "${SMTP_PASSWORD:-$_GP}" ] && SECRETS+=("SMTP_PASSWORD=${SMTP_PASSWORD:-$_GP}")
echo ">>> Secrete email: SMTP_USER=${SMTP_USER:-$_GU} (parolă ${#_GP} caractere) | SMTP_USER2=$(grep '^SMTP_USER2=' .env 2>/dev/null | tail -1 | cut -d= -f2- | tr -d '\r')"
for _K in SMTP_HOST SMTP_PORT SMTP_USER2 SMTP_PASSWORD2 SMTP_HOST2 GMAIL_RELAY_URL GMAIL_RELAY_TOKEN; do _V="${!_K:-}"; [ -z "$_V" ] && _V="$(grep "^$_K=" .env 2>/dev/null | tail -1 | cut -d= -f2- | tr -d '\r')"; [ -n "$_V" ] && SECRETS+=("$_K=$_V"); done
SECRETS+=("ADMIN_PASSWORD=$ADMIN_PASSWORD")
[ "$USE_FLYPG" = 1 ] || SECRETS+=("DATABASE_URL=$DATABASE_URL")
[ ${#SECRETS[@]} -gt 0 ] && "$FLY" secrets set --app "$APP" --stage "${SECRETS[@]}"

echo ">>> Construiesc și public (prima dată durează câteva minute)..."
if ! "$FLY" deploy --app "$APP" --remote-only; then
  # Depot (builderul implicit) răspunde uneori cu 401/403 la push; builderul clasic Fly nu depinde de el
  echo ">>> Deploy eșuat cu builderul Depot (401/403). Reîncerc cu builderul clasic Fly (--depot=false)..." >&2
  if ! "$FLY" deploy --app "$APP" --remote-only --depot=false; then
    echo "Deploy eșuat. Dacă eroarea e 401/403: $FLY auth logout && $FLY auth login, apoi rulează din nou ./scripts/deploy.sh. Verifică și că aplicația $APP apare în $FLY apps list (contul/organizația cu care ești logat)." >&2
    exit 1
  fi
fi

# ---- să nu se oprească: aplicația și baza de date repornesc mereu singure, DB-ul primește mai mult RAM (256 MB cădea cu OOM)
_ids() { "$FLY" machines list -a "$1" --json 2>/dev/null | node -e 'let s="";process.stdin.on("data",d=>s+=d).on("end",()=>{try{JSON.parse(s).forEach(m=>console.log(m.id))}catch(e){}})' | tr -d '\r'; }
_DBA="$(cat .runtime/fly-db 2>/dev/null || true)"
if [ -n "$_DBA" ]; then
  "$FLY" scale memory 512 -a "$_DBA" --yes >/dev/null 2>&1 || "$FLY" scale memory 512 -a "$_DBA" >/dev/null 2>&1 || true
  for _m in $(_ids "$_DBA"); do "$FLY" machine update "$_m" -a "$_DBA" --restart always --yes >/dev/null 2>&1 || true; "$FLY" machine start "$_m" -a "$_DBA" >/dev/null 2>&1 || true; done
fi
for _m in $(_ids "$APP"); do "$FLY" machine update "$_m" -a "$APP" --restart always --yes >/dev/null 2>&1 || true; "$FLY" machine start "$_m" -a "$APP" >/dev/null 2>&1 || true; done
echo ">>> Fly: aplicația și baza de date au restart=always (nu se mai opresc singure)."

if [ -n "$DOMAIN" ]; then
  "$FLY" certs add "$DOMAIN" --app "$APP" || true
  echo
  echo ">>> DOMENIU: la furnizorul domeniului tău adaugi înregistrările DNS (valorile exacte: $FLY ips list --app $APP):"
  "$FLY" ips list --app "$APP" || true
  echo "    A     $DOMAIN -> adresa IPv4 de mai sus       AAAA  $DOMAIN -> adresa IPv6 de mai sus"
  echo "    (sau, pentru un subdomeniu: CNAME $DOMAIN -> $APP.fly.dev)"
  echo ">>> Verifici certificatul HTTPS cu:  $FLY certs show $DOMAIN --app $APP   (după propagarea DNS, de obicei minute)"
fi
mkdir -p docs
echo "$BASE" > docs/link-backend.txt
# backend-ul e acum pe Fly: /api, /img etc. din Vercel arată spre el (și regenerarea scriptului păstrează adresa)
echo "$FLYBASE" > .runtime/backend-url
for VJ in vercel.json frontend/vercel.json; do
  [ -f "$VJ" ] && sed_i -E "s#\"destination\": \"https://[^\"/]+/(api|img|robots\.txt|sitemap\.xml|llms\.txt)#\"destination\": \"$FLYBASE/\1#" "$VJ"
done
echo ">>> Vercel: /api -> $FLYBASE (vercel.json actualizat). Publici cu ./scripts/push.sh sau ./scripts/vercel_deploy.sh"
echo ">>> ADMIN: admin@mobilaviva.com  parola: $ADMIN_PASSWORD   (pune-le în documentare pentru profesor; fișier: .runtime/admin-password, nu intră în arhivă)"
echo ">>> Pentru print screen-uri pe site-ul public parola de admin se citește automat din .runtime/admin-password"
echo
echo ">>> PUBLICAT: $BASE   (backend; linkul public rămâne cel de pe Vercel)"
# Fără email de test automat: ./scripts/test_email.sh se rulează doar manual, când vrei.
echo ">>> Stripe webhook (opțional): endpoint $BASE/api/stripe/webhook, eveniment checkout.session.completed; whsec_... în .env, apoi rulezi din nou ./scripts/deploy.sh"
echo ">>> Apoi: ./scripts/screenshots.sh $BASE   (print screen-uri + PageSpeed pe linkul stabil)"
