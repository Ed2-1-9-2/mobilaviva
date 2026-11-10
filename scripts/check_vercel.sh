#!/usr/bin/env bash
# Verifică site-ul publicat pe Vercel: pagina, API-ul prin rewrite către Render, catalogul și LOGIN-ul de test (admin).
#   ./scripts/check_vercel.sh [URL]     (implicit LIVE_URL sau https://mobilaviva.vercel.app)
#   CHECK_WAIT=600 = câte secunde aștept deploy-ul (implicit 600); CHECK_WAIT=0 = o singură încercare
# Parola admin: ADMIN_PASSWORD, apoi .runtime/admin-password, apoi .env, apoi Admin123!. Cod ieșire 0 = totul OK.
cd "$(dirname "$0")/.." || exit 1
URL="${1:-${LIVE_URL:-https://mobilaviva.vercel.app}}"; URL="${URL%/}"
WAIT="${CHECK_WAIT:-600}"
EMAIL="${ADMIN_EMAIL:-admin@mobilaviva.com}"
PASS="${ADMIN_PASSWORD:-}"
[ -z "$PASS" ] && [ -s .runtime/admin-password ] && PASS="$(tr -d '\r\n' < .runtime/admin-password)"
[ -z "$PASS" ] && [ -f .env ] && PASS="$(grep '^ADMIN_PASSWORD=' .env | tail -1 | cut -d= -f2- | tr -d '\r' | sed 's/^"\(.*\)"$/\1/')"
PASS="${PASS:-Admin123!}"
mkdir -p .runtime
JAR=.runtime/check-cookies.txt
REPORT=.runtime/check-vercel.txt
jesc() { printf '%s' "$1" | sed 's/\\/\\\\/g; s/"/\\"/g'; }

run_checks() {   # completează R_* ; întoarce 0 doar dacă toate trec
  rm -f "$JAR"; FAILS=0; FINAL=0; LINES=()
  add() { LINES+=("  $1 $2"); [ "$1" = "✘" ] && FAILS=$((FAILS+1)); return 0; }

  # 1) pagina principală = build-ul Vite (nu varianta veche cu vue3-sfc-loader)
  local home; home="$(curl -s --max-time 60 "$URL/")"
  if ! printf '%s' "$home" | grep -q 'id="app"'; then add ✘ "pagina $URL/ nu răspunde cu aplicația"
  elif printf '%s' "$home" | grep -q 'vue3-sfc-loader\|vue.global'; then add ✘ "pagina e versiunea VECHE (vue3-sfc-loader); rulează scriptul de setup + ./scripts/push.sh"
  else add ✔ "pagina principală (build Vite)"; fi

  # 2) API prin rewrite: JSON, nu HTML
  local ct body
  body="$(curl -s --max-time 60 -c "$JAR" -b "$JAR" -w '\n%{content_type}' "$URL/api/session")"; ct="${body##*$'\n'}"
  case "$ct" in application/json*) add ✔ "/api/session ajunge la Render (JSON)" ;;
    *) add ✘ "/api/session întoarce '$ct' în loc de JSON: lipsesc rewrite-urile din vercel.json sau Render doarme" ;; esac

  # 3) catalog: categorii + produse
  if curl -s --max-time 60 "$URL/api/facets" | grep -q '"categories":\[{'; then add ✔ "/api/facets are categorii"
  else add ✘ "/api/facets nu întoarce categorii"; fi
  if curl -s --max-time 60 "$URL/api/products?" | grep -q '"items":\[{'; then add ✔ "/api/products are produse"
  else add ✘ "/api/products nu întoarce produse"; fi

  # 4) login de test (POST) + sesiune păstrată prin cookie
  local code
  code="$(curl -s --max-time 60 -c "$JAR" -b "$JAR" -o .runtime/check-login.json -w '%{http_code}' -X POST \
          -H 'Content-Type: application/json' -d "{\"email\":\"$(jesc "$EMAIL")\",\"password\":\"$(jesc "$PASS")\"}" "$URL/api/auth/login")"
  if [ "$code" = "401" ] && [ "$PASS" != "Admin123!" ]; then     # pe Render (demo) parola adminului e Admin123!
    PASS="Admin123!"
    code="$(curl -s --max-time 60 -c "$JAR" -b "$JAR" -o .runtime/check-login.json -w '%{http_code}' -X POST \
            -H 'Content-Type: application/json' -d "{\"email\":\"$(jesc "$EMAIL")\",\"password\":\"$(jesc "$PASS")\"}" "$URL/api/auth/login")"
  fi
  case "$code" in
    200) add ✔ "login $EMAIL (POST /api/auth/login → 200)"
         if curl -s --max-time 60 -c "$JAR" -b "$JAR" "$URL/api/session" | grep -q "\"email\":\"$EMAIL\""; then add ✔ "sesiunea rămâne activă după login (cookie prin Vercel)"
         else add ✘ "login OK, dar sesiunea nu se păstrează (cookie-ul nu trece prin Vercel)"; fi ;;
    401) add ✘ "API-ul merge, dar parola admin e greșită (pune ADMIN_PASSWORD=... sau .runtime/admin-password)"; FINAL=1 ;;
    429) add ✘ "prea multe încercări de login eșuate; reîncearcă peste câteva minute"; FINAL=1 ;;
    405) add ✘ "POST /api/auth/login → 405: Vercel nu trimite /api către Render (vercel.json lipsă/ignorat)" ;;
    *)   add ✘ "POST /api/auth/login → ${code:-fără răspuns}" ;;
  esac
  rm -f "$JAR" .runtime/check-login.json
  [ "$FAILS" = 0 ]
}

echo ">>> Verific deploy-ul pe $URL (login de test cu $EMAIL)"
START=$(date +%s); TRY=0
while :; do
  TRY=$((TRY+1))
  if run_checks; then STATUS=OK; break; fi
  # parolă greșită / blocare: NU reîncerc (altfel contul admin se blochează pe Render)
  if [ "$FINAL" = 1 ] || [ $(( $(date +%s) - START )) -ge "$WAIT" ]; then STATUS=EȘUAT; break; fi
  echo "    încercarea $TRY: $FAILS verificări eșuate (Vercel/Render încă publică sau Render se trezește); reîncerc în 20 s..."
  sleep 20
done
{
  echo "Verificare deploy $URL — $(date '+%Y-%m-%d %H:%M:%S') — $STATUS"
  printf '%s\n' "${LINES[@]}"
} | tee "$REPORT"
if [ "$STATUS" = OK ]; then echo ">>> DEPLOY OK: site-ul, catalogul și login-ul merg pe $URL (raport: $REPORT)"; exit 0; fi
echo "!!! DEPLOY CU PROBLEME pe $URL (raport: $REPORT). Verifică Vercel → Deployments și Render → Logs." >&2
if grep -q 'versiunea VECHE\|405' "$REPORT"; then
  echo "!!! Rezolvare automată (o comandă): ./scripts/vercel_deploy.sh $URL" >&2
  cat >&2 <<HELP
!!! $URL arată un deploy VECHI. Cauza obișnuită: adresa e fixată pe un deploy vechi (Instant Rollback / alias manual),
!!! așa că deploy-urile noi din GitHub nu mai ajung pe ea. Vercel → proiect → Deployments → ultimul „Ready” → ⋯ → Promote to Production.
!!! Sau, dacă adresa e la ALT proiect Vercel, mut-o:
    1) vercel.com → proiectul VECHI (cel care are domeniul ${URL#https://}) → Settings → Domains → Remove ${URL#https://}
       (sau Settings → General → Delete Project, dacă nu-l mai folosești)
    2) proiectul NOU (legat la GitHub, ex. mobilaviva-mobila-viva) → Settings → Domains → Add → ${URL#https://} → Save
       (dacă proiectul vechi a fost șters: Settings → General → Project Name = $(H=${URL#https://}; echo ${H%.vercel.app}); adresa ${URL#https://} devine automat a lui)
    3) apoi rulează din nou: ./scripts/check_vercel.sh $URL
    Verificare rapidă pe adresa proiectului nou: ./scripts/check_vercel.sh https://NUME-PROIECT-NOU.vercel.app
HELP
fi
exit 1
