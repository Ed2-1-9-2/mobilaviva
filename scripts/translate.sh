#!/usr/bin/env bash
# Traduce produsele în EN/DE/FR/IT/PT/ES/NL (doar ce lipsește). Nu eșuează niciodată (cod 0), ca să nu blocheze build-ul sau deploy-ul.
#   ./scripts/translate.sh [--dry|--force]        TRANSLATE=0 = sare peste        TRANSLATE_FROM=https://site = include și produsele din Admin
cd "$(dirname "$0")/.." || exit 0
[ "${TRANSLATE:-1}" = 0 ] && exit 0
command -v node >/dev/null 2>&1 || { echo ">>> traduceri: lipsește Node, sar peste." >&2; exit 0; }
node scripts/translate_products.mjs "$@" || echo ">>> traduceri: a apărut o eroare; continui cu traducerile existente." >&2
exit 0
