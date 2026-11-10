#!/usr/bin/env bash
# Descarcă pozele din docs/photos-urls.txt în frontend/public/photos/ (sar peste cele existente și peste liniile fără URL).
#   ./scripts/fetch_photos.sh
# Apoi: ./scripts/run.sh (local) sau ./scripts/push.sh (Render + Vercel). Serverul leagă pozele de produse la pornire.
cd "$(dirname "$0")/.." || exit 1
F=docs/photos-urls.txt
[ -f "$F" ] || ./scripts/photos_list.sh >/dev/null
mkdir -p frontend/public/photos
N=0; BAD=0
while read -r slug url; do
  case "$slug" in ''|\#*) continue ;; esac
  [ -n "$url" ] || continue
  ext=jpg; case "${url%%\?*}" in *.webp) ext=webp ;; *.png) ext=png ;; esac
  out="frontend/public/photos/$slug.$ext"
  if [ -s "$out" ]; then echo "  = $slug (există deja)"; continue; fi
  ct="$(curl -fsSL --max-time 90 -A 'Mozilla/5.0' -o "$out" -w '%{content_type}' "$url" 2>/dev/null)"
  case "$ct" in
    image/*) echo "  ✔ $slug"; N=$((N+1)) ;;
    *) echo "  ✘ $slug: răspunsul nu e o imagine ($url)"; rm -f "$out"; BAD=$((BAD+1)) ;;
  esac
done < "$F"
echo ">>> $N poze noi, $BAD eșuate. Verifică: ./scripts/photos_list.sh"
