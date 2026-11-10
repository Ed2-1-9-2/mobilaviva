#!/usr/bin/env bash
# Descarcă automat poze realiste pentru toate produsele (cele existente rămân neschimbate).
#   ./scripts/auto_photos.sh                 # Openverse, fără cheie
#   PEXELS_API_KEY=xxx ./scripts/auto_photos.sh   # recomandat (cheie gratuită: pexels.com/api); se poate pune și în .env
#   ./scripts/auto_photos.sh slug1 slug2     # doar anumite produse
# Credite: docs/photos-credits.txt (de pus la „Surse imagini” în documentație). Apoi: ./scripts/push.sh
cd "$(dirname "$0")/.." || exit 1
command -v node >/dev/null 2>&1 || { echo "Lipsește Node 18+" >&2; exit 1; }
if [ -z "${PEXELS_API_KEY:-}" ] && [ -f .env ]; then
  PEXELS_API_KEY="$(grep -E '^PEXELS_API_KEY=' .env | head -1 | cut -d= -f2- | tr -d '"'"'"' ')"; export PEXELS_API_KEY
fi
[ -n "${PEXELS_API_KEY:-}" ] && echo ">>> Pexels" || echo ">>> Fără PEXELS_API_KEY: folosesc Openverse (calitate mai slabă)"
node scripts/auto_photos.mjs "$@"
