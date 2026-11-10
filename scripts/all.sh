#!/usr/bin/env bash
# TOT DINTR-O COMANDĂ: commit + push (Render face redeploy) -> așteaptă să apară versiunea nouă pe site -> ./scripts/live.sh (capturi + PageSpeed).
#   ./scripts/all.sh [URL]        (implicit https://mobilaviva.vercel.app)
# Versiunea nouă e recunoscută după markerul „menubar” din HTML-ul site-ului (CSS inline). Dacă e deja publicat, trece direct mai departe.
cd "$(dirname "$0")/.." || exit 1
URL="${1:-${LIVE_URL:-https://mobilaviva.vercel.app}}"; URL="${URL%/}"
MARK="${DEPLOY_MARKER:-menubar}"

NO_CHECK=1 ./scripts/push.sh || exit 1

echo ">>> Aștept versiunea nouă pe $URL (Render redeployează: de obicei 3-8 minute, maximum 20)..."
for i in $(seq 1 240); do
  if curl -s --max-time 60 "$URL/" | grep -q "$MARK"; then echo ">>> Versiunea nouă este live."; FOUND=1; break; fi
  [ $((i % 6)) -eq 0 ] && echo "    ... încă se publică ($((i * 5 / 60)) min)"
  sleep 5
done
[ -n "${FOUND:-}" ] || { echo "!!! După 20 de minute nu apare versiunea nouă. Verifică deploy-ul pe Render, apoi rulează ./scripts/live.sh" >&2; exit 1; }

CHECK_WAIT=300 ./scripts/check_vercel.sh "$URL" || echo "!!! Verificarea login/API a eșuat (vezi mai sus); continui cu capturile." >&2
exec ./scripts/live.sh "$URL"
