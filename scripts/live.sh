#!/usr/bin/env bash
# O SINGURĂ COMANDĂ: print screen-uri + PageSpeed (18/19) pe site-ul PUBLICAT (implicit https://mobilaviva.vercel.app).
#   ./scripts/live.sh [URL]          (sau LIVE_URL=https://... ./scripts/live.sh)
# Pașii: pornește aplicația locală (doar pentru captura 02, baza de date) -> trezește site-ul de pe Render (planul free adoarme)
#        -> cere parola de admin de pe Render -> rulează ./scripts/screenshots.sh pe URL -> oprește aplicația locală.
# Atenție: pe site-ul live se creează un cont de test și comenzi (stocul unor produse scade). PageSpeed rulează într-o fereastră Chrome vizibilă: nu o închide.
cd "$(dirname "$0")/.." || exit 1
EXPLICIT="${1:-${LIVE_URL:-}}"
SAVED="$(tr -d '\r\n' < docs/link-public.txt 2>/dev/null)"
case "$SAVED" in *fly.dev*|*onrender.com*) SAVED="" ;; esac   # backend-ul (Fly/Render) nu e linkul public: se analizează site-ul de pe Vercel
URL="${EXPLICIT:-${SAVED:-https://mobilaviva.vercel.app}}"; URL="${URL%/}"
mkdir -p .runtime
code() { curl -s -o /dev/null --max-time 60 -w '%{http_code}' "$1"; }
LPID=""
cleanup() { [ -n "$LPID" ] && kill "$LPID" 2>/dev/null; }
trap cleanup EXIT INT TERM

# 1) aplicația locală, doar pentru captura 02 (baza de date)
LOCAL="$(cat .runtime/url 2>/dev/null | tr -d '\r\n')"
if [ "${START_LOCAL:-0}" = 1 ] && { [ -z "$LOCAL" ] || [ "$(code "$LOCAL/")" != "200" ]; }; then
  echo ">>> Pornesc aplicația locală (pentru captura 02, baza de date)..."
  rm -f .runtime/url
  ./scripts/run.sh > .runtime/live-local.log 2>&1 < /dev/null &
  LPID=$!
fi

# 2) trezește site-ul public: 2 răspunsuri 200 la rând
wake() {   # $1 = URL, $2 = număr maxim de încercări (câte 5 s); 0 dacă site-ul real răspunde de 2 ori la rând
  OK=0; C=""
  for _ in $(seq 1 "$2"); do
    # „treaz” = HTML-ul real al site-ului (nu pagina „SERVICE WAKING UP” a Render-ului, care poate avea tot cod 200)
    if curl -s --max-time 60 "$1/" | grep -q 'id="app"'; then C=200; OK=$((OK + 1)); else C=asteptare; OK=0; fi
    [ "$OK" -ge 2 ] && return 0
    sleep 5
  done
  return 1
}
echo ">>> Trezesc $URL (Render free adoarme; poate dura ~1-2 minute)..."
wake "$URL" 36 || true
[ "${OK:-0}" -ge 2 ] || { echo "!!! $URL nu răspunde (ultimul cod: ${C:-?}). Verifică Render (Logs) și Vercel; sau dă linkul: ./scripts/live.sh https://..." >&2; exit 1; }
echo ">>> $URL răspunde."
[ "${NO_DIAG:-0}" = 1 ] || { echo ">>> Test autentificare (site vs. script; NO_DIAG=1 îl sare):"; bash scripts/diagnoza_login.sh || true; echo; }
mkdir -p docs; echo "$URL" > docs/link-public.txt   # linkul verificat intră în documentare (docs.sh) și în verifica.sh

# 3) parola de admin de pe site-ul public (cea din ADMIN_PASSWORD setată pe Render)
if [ -z "${ADMIN_PASSWORD:-}" ] && [ ! -s .runtime/admin-password ] && [ -t 0 ]; then
  read -r -s -p "Parola de admin de pe Render (Enter = Admin123!): " AP; echo
  ADMIN_PASSWORD="${AP:-Admin123!}"; export ADMIN_PASSWORD
fi

# 4) aplicația locală trebuie să fie gata înainte de capturi
if [ -n "$LPID" ]; then
  echo ">>> Aștept aplicația locală (prima rulare: câteva minute, Maven descarcă dependențe)..."
  for _ in $(seq 1 180); do
    L="$(cat .runtime/url 2>/dev/null | tr -d '\r\n')"
    if [ -n "$L" ] && [ "$(code "$L/")" = "200" ]; then break; fi
    if ! kill -0 "$LPID" 2>/dev/null; then
      echo "!!! Aplicația locală s-a oprit (vezi .runtime/live-local.log). Captura 02 va lipsi, restul se face." >&2
      LPID=""; break
    fi
    sleep 5
  done
fi

# 5) capturi + PageSpeed + documentare
./scripts/screenshots.sh "$URL"
echo ">>> Gata: docs/screenshots/ și docs/PREZENTARE.html (documentare: docs/documentare/)"
