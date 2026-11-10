#!/usr/bin/env bash
# Publică frontend-ul Vue 3 pe Vercel conectat la backend.
# Utilizare: ./scripts/vercel.sh <URL_BACKEND>
# Exemplu:   ./scripts/vercel.sh https://mobilaviva-backend.onrender.com

set -e

BACKEND_URL="$1"

if [ -z "$BACKEND_URL" ]; then
  echo "!!! Eroare: Trebuie să furnizezi URL-ul de producție al backend-ului."
  echo ">>> Utilizare: $0 https://nume-backend.onrender.com"
  exit 1
fi

# Verificare Vercel CLI
command -v vercel >/dev/null || {
  echo "!!! Vercel CLI nu este instalat."
  echo ">>> Instalează-l rulând în terminal: npm install -g vercel"
  exit 1
}

# Navigare strict către folderul frontend al proiectului local
DIR="$(cd "$(dirname "$0")/../frontend" && pwd)"
cd "$DIR" || { echo "!!! Folderul 'frontend' nu a fost găsit la nivelul superior!"; exit 1; }

echo ">>> Se pregătește publicarea frontend-ului pe Vercel..."
echo ">>> Backend URL setat pentru conectare: $BACKEND_URL"

# Trimiterea către Vercel (injectând URL-ul API-ului pentru build-ul de producție)
# Notă: Dacă în fișierele tale Vue (ex. api.js) variabila se numește altfel (ex. VITE_BACKEND_URL),
# înlocuiește VITE_API_URL mai jos cu numele corect folosit de tine.
vercel deploy --prod --build-env VITE_API_URL="$BACKEND_URL" --env VITE_API_URL="$BACKEND_URL"

echo ""
echo ">>> Comanda a fost trimisă către Vercel!"