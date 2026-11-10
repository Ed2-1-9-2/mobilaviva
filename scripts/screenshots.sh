#!/usr/bin/env bash
# Print screen automat pentru cerințele proiectului + PREZENTARE.html cu galerie.
#   ./scripts/screenshots.sh                 folosește linkul public (.runtime/public_url) dacă tunelul merge, altfel .url / localhost
#   ./scripts/screenshots.sh https://...     URL explicit (PageSpeed 18/19 doar pe https public)
# Poze salvate în docs/screenshots/. Prima rulare descarcă Chromium (~150 MB).
cd "$(dirname "$0")/tools" || exit 1
# cont de test EXISTENT din .env (nu intră în git și nici în arhiva de upload):  TEST_EMAIL=...  TEST_PASSWORD=...
for K in TEST_EMAIL TEST_PASSWORD; do
  eval "CUR=\${$K:-}"
  if [ -z "$CUR" ]; then
    V="$(grep "^$K=" ../../.env 2>/dev/null | head -1 | cut -d= -f2- | tr -d '\r')"
    V="${V%\"}"; V="${V#\"}"; V="${V%\'}"; V="${V#\'}"
    [ -n "$V" ] && export "$K=$V"
  fi
done
alive() { [ -n "$1" ] && [ "$(curl -s -o /dev/null --max-time 8 -w '%{http_code}' "$1/")" = "200" ]; }
URL="$1"
if [ -z "$URL" ]; then
  PUB="$(cat ../../.runtime/public_url 2>/dev/null | tr -d '\r\n')"
  if alive "$PUB"; then URL="$PUB"
  elif [ -z "${NO_LIVE:-}" ] && [ -x ../../scripts/live.sh ]; then
    # fără tunel activ: site-ul PUBLICAT (Vercel/Render, trezit automat) + aplicația locală doar pentru captura 02 -> scripts/live.sh
    echo ">>> Nu există tunel activ: folosesc site-ul publicat (scripts/live.sh). Pentru localhost: NO_LIVE=1 ./scripts/screenshots.sh" >&2
    cd ../.. && exec ./scripts/live.sh
  else URL="$(cat ../../.runtime/url 2>/dev/null | tr -d '\r\n')"; fi
  [ -n "$URL" ] || URL=http://localhost:8000
fi
echo ">>> Folosesc: $URL"
command -v node >/dev/null 2>&1 || { echo "Lipsește Node 18+ (winget install OpenJS.NodeJS.LTS)" >&2; exit 1; }
curl -s -o /dev/null --max-time 8 "$URL/" || { echo "Aplicația nu răspunde la $URL. Pornește-o întâi (./scripts/run.sh sau ./scripts/public.sh) sau dă alt URL." >&2; exit 1; }
[ -d node_modules/playwright ] || npm install --no-audit --no-fund || exit 1
npx playwright install chromium || exit 1
# pe site-ul public (deploy.sh) parola de admin e cea generată; local rămâne ADMIN_PASSWORD / implicita
if [ -z "${ADMIN_PASSWORD:-}" ] && [ -s ../../.runtime/admin-password ]; then
  case "$URL" in *localhost*|*127.0.0.1*) ;; *) export ADMIN_PASSWORD="$(tr -d '\r\n' < ../../.runtime/admin-password)" ;; esac
fi
node screenshots.mjs "$URL"
cd ../.. && ./scripts/docs.sh || echo "(documentarea .docx/.pptx nu s-a generat; rulează ./scripts/docs.sh)" >&2
