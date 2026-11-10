#!/usr/bin/env bash
# Pornește MobilaViva pe http://mobilaviva.com (alias local în fișierul hosts), cu JDK 17+/Maven/Node sau cu Docker Compose (PostgreSQL).
#   SITE_HOST=mobila.test ./scripts/run.sh   alt nume local   |   SITE_HOST=localhost ./scripts/run.sh   fără alias
# Port: încearcă 80 (adresa fără port); dacă e ocupat sau interzis, primul port liber de la 8000 (PORT=9000 ./scripts/run.sh pentru alt început).
# Chei Stripe etc. se citesc din .env (nu intră în git, nici în arhiva de upload).
cd "$(dirname "$0")/.." || exit 1

fail() { echo; echo "!!! $1" >&2; echo "Apasă Enter pentru a închide..."; read -r _; exit 1; }
port_busy() { (echo > "/dev/tcp/127.0.0.1/$1") >/dev/null 2>&1; }
open_when_ready() {   # $1 = adresa de deschis în browser ; $2 = portul local de verificat
  for _ in $(seq 1 300); do
    if curl -s -o /dev/null --max-time 2 "http://127.0.0.1:$2/"; then
      echo; echo ">>> GATA: $1"
      [ "${NO_OPEN:-0}" = 1 ] || cmd.exe /c start "" "$1" >/dev/null 2>&1 || xdg-open "$1" >/dev/null 2>&1 || open "$1" >/dev/null 2>&1
      return
    fi
    sleep 2
  done
}

# ---- .env (variabilele deja exportate au prioritate)
if [ -f .env ]; then
  while IFS= read -r line || [ -n "$line" ]; do
    line="${line%$'\r'}"
    case "$line" in ''|\#*) continue ;; esac
    key="${line%%=*}"; val="${line#*=}"
    val="${val%\"}"; val="${val#\"}"
    [[ "$key" =~ ^[A-Za-z_][A-Za-z0-9_]*$ ]] || continue
    [ -n "${!key}" ] || export "$key=$val"
  done < .env
fi

# ---- nume de site (alias local): 127.0.0.1 -> mobilaviva.com
SITE_HOST="${SITE_HOST:-mobilaviva.com}"
HOSTS_FILE=/etc/hosts; [ -f /c/Windows/System32/drivers/etc/hosts ] && HOSTS_FILE=/c/Windows/System32/drivers/etc/hosts
host_mapped() {
  awk -v h="$SITE_HOST" '{gsub(/\r/,"")} tolower($1)=="127.0.0.1"{for(i=2;i<=NF;i++) if(tolower($i)==tolower(h)) f=1} END{exit !f}' "$HOSTS_FILE" 2>/dev/null
}
if [ "$SITE_HOST" != "localhost" ] && ! host_mapped; then
  echo ">>> Adaug $SITE_HOST în $HOSTS_FILE (127.0.0.1)..."
  { printf '\n127.0.0.1 %s www.%s\n' "$SITE_HOST" "$SITE_HOST" >> "$HOSTS_FILE"; } 2>/dev/null
  if host_mapped; then
    cmd.exe /c ipconfig /flushdns >/dev/null 2>&1
    echo ">>> OK: $SITE_HOST -> 127.0.0.1"
  else
    echo "!!! Nu pot scrie în $HOSTS_FILE. Deschide Git Bash cu „Run as administrator” și rulează ./scripts/run.sh o dată" >&2
    echo "!!! (Linux/macOS: sudo sh -c 'echo \"127.0.0.1 $SITE_HOST\" >> /etc/hosts'). Continui cu localhost." >&2
    SITE_HOST=localhost
  fi
fi

# ---- port liber
CAND=""
if [ -n "$PORT" ]; then CAND="$(seq "$PORT" $((PORT + 30)))"
elif [ "$SITE_HOST" != "localhost" ] && { [ "$(uname -s)" != "Linux" ] || [ "$(id -u)" = "0" ]; }; then CAND="80 $(seq 8000 8030)"
else CAND="$(seq 8000 8030)"; fi
PORT=""
for p in $CAND; do
  if ! port_busy "$p"; then PORT="$p"; break; fi
done
[ -n "$PORT" ] || fail "Nu am găsit niciun port liber."
if [ "$PORT" = "80" ]; then URL="http://$SITE_HOST"; else URL="http://$SITE_HOST:$PORT"; fi
[ "$PORT" = "80" ] || [ "$SITE_HOST" = "localhost" ] || echo ">>> Portul 80 nu e disponibil, adresa va fi $URL"
export HOST_PORT="$PORT"
export APP_BASE_URL="${PUBLIC_URL:-$URL}"
mkdir -p .runtime; echo "$URL" > .runtime/url

echo ">>> Admin: admin@mobilaviva.com / ${ADMIN_PASSWORD:-Admin123!}"
if [ -n "$STRIPE_SECRET_KEY" ]; then echo ">>> Plată: Stripe Checkout (mod test)"; else echo ">>> Plată: card simulat (pune STRIPE_SECRET_KEY=sk_test_... în .env pentru Stripe)"; fi
[ -n "$STRIPE_WEBHOOK_SECRET" ] && echo ">>> Webhook Stripe activ pe /api/stripe/webhook" || echo ">>> Webhook: local rulezi  stripe listen --forward-to 127.0.0.1:$PORT/api/stripe/webhook  și pui STRIPE_WEBHOOK_SECRET=whsec_... în .env"

HAVE_JAVA=0; HAVE_MVN=0; HAVE_NPM=0
command -v java >/dev/null 2>&1 && HAVE_JAVA=1
command -v mvn  >/dev/null 2>&1 && HAVE_MVN=1
command -v npm  >/dev/null 2>&1 && HAVE_NPM=1

if [ $HAVE_JAVA = 1 ] && [ $HAVE_MVN = 1 ] && [ $HAVE_NPM = 1 ]; then
  JV=$(java -version 2>&1 | head -1 | sed -E 's/.*"([0-9]+)(\.([0-9]+))?.*/\1/')
  [ "$JV" = "1" ] && JV=8
  echo ">>> Java: $(java -version 2>&1 | head -1)"
  [ "${JV:-0}" -ge 17 ] 2>/dev/null || fail "Ai Java $JV, dar proiectul cere Java 17+ (winget install EclipseAdoptium.Temurin.17.JDK, apoi redeschide terminalul)."

  JAR=target/mobila-shop.jar
  # rebuild DOAR dacă lipsește JAR-ul sau s-a modificat codul (REBUILD=1 forțează)
  needs_build() {
    [ "${REBUILD:-0}" = 1 ] && return 0
    [ -f "$JAR" ] || return 0
    [ -n "$(find src pom.xml frontend/src frontend/package.json frontend/index.html frontend/vite.config.js -type f -newer "$JAR" -not -path 'src/main/resources/static/*' 2>/dev/null | head -1)" ]
  }
  if needs_build; then
    ./scripts/translate.sh
    echo ">>> [1/2] Build frontend (Vue)..."
    (cd frontend && { [ -d node_modules ] || npm install --no-audit --no-fund; } && npm run build) || fail "Build-ul frontend a eșuat (vezi eroarea de mai sus)."
    [ -f src/main/resources/static/index.html ] || fail "Frontend-ul nu a generat src/main/resources/static/index.html."
    echo ">>> Împachetez aplicația (mvn package, prima dată 2-5 minute)..."
    mvn -q -DskipTests package || fail "Build-ul Maven a eșuat (vezi eroarea de mai sus)."
  else
    echo ">>> Fără modificări de cod: sar peste build, pornesc direct $JAR (REBUILD=1 ./scripts/run.sh forțează build-ul)."
  fi

  if [ -z "${DATABASE_URL:-}" ]; then
    # Aplicația rulează DOAR pe PostgreSQL: pornesc un container local (datele rămân în volumul Docker mobila_pgdata_local)
    command -v docker >/dev/null 2>&1 || fail "Aplicația folosește doar PostgreSQL. Instalează Docker Desktop (pornesc eu PostgreSQL) sau pune în .env DATABASE_URL=postgresql://utilizator:parola@gazdă:5432/baza (PostgreSQL local sau Neon)."
    docker info >/dev/null 2>&1 || fail "Docker este instalat, dar Docker Desktop nu rulează. Pornește-l și încearcă din nou."
    if docker ps -a --format '{{.Names}}' | grep -qx mobila-db; then
      docker start mobila-db >/dev/null 2>&1 || true
    else
      docker run -d --name mobila-db -p 127.0.0.1:5433:5432 -e POSTGRES_USER=mobila -e POSTGRES_PASSWORD=mobila -e POSTGRES_DB=mobila \
        -v mobila_pgdata_local:/var/lib/postgresql/data postgres:16-alpine >/dev/null || fail "Nu am putut porni PostgreSQL în Docker (portul 5433 e ocupat?)."
    fi
    echo ">>> PostgreSQL (Docker, container mobila-db, port 5433): aștept să pornească..."
    PGOK=0
    for _ in $(seq 1 60); do docker exec mobila-db pg_isready -U mobila -d mobila >/dev/null 2>&1 && { PGOK=1; break; }; sleep 1; done
    [ $PGOK = 1 ] || fail "PostgreSQL nu a pornit la timp (vezi: docker logs mobila-db)."
    export DATABASE_URL="jdbc:postgresql://localhost:5433/mobila" DB_USER=mobila DB_PASSWORD=mobila
    echo ">>> Print screen 02 (BD): Admin → Baza de date, sau: docker exec -it mobila-db psql -U mobila -d mobila -c '\\dt'"
  else
    echo ">>> PostgreSQL: folosesc DATABASE_URL din mediu/.env"
  fi
  echo ">>> [2/2] Pornesc Spring Boot pe $URL ..."
  java -jar "$JAR" --server.port="$PORT" &
  MVN_PID=$!
  trap 'kill $MVN_PID 2>/dev/null; exit 0' INT TERM
  open_when_ready "$URL" "$PORT" &
  wait $MVN_PID
  fail "Serverul s-a oprit (vezi eroarea de mai sus)."
elif command -v docker >/dev/null 2>&1; then
  docker info >/dev/null 2>&1 || fail "Docker este instalat, dar Docker Desktop nu rulează. Pornește-l și încearcă din nou."
  docker compose down >/dev/null 2>&1   # curăță containerele vechi ale acestui proiect
  echo ">>> $URL  (Docker + PostgreSQL)"
  echo ">>> Print screen 02 (BD): docker compose exec db psql -U mobila -d mobila -c '\dt'"
  open_when_ready "$URL" "$PORT" &
  docker compose up --build
else
  echo "Lipsește:" >&2
  [ $HAVE_JAVA = 1 ] || echo " - Java 17+   (winget install EclipseAdoptium.Temurin.17.JDK)" >&2
  [ $HAVE_MVN  = 1 ] || echo " - Maven      (winget install Apache.Maven)" >&2
  [ $HAVE_NPM  = 1 ] || echo " - Node 18+   (winget install OpenJS.NodeJS.LTS)" >&2
  fail "Instalează cele de mai sus (sau Docker Desktop), apoi redeschide Git Bash."
fi
