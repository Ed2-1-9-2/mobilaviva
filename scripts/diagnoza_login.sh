#!/usr/bin/env bash
# Test site vs. script: se loghează (sau înregistrează) cu curl, fără browser, și verifică dacă sesiunea rămâne.
#   bash diagnoza_login.sh                       folosește TEST_EMAIL/TEST_PASSWORD din .env (folderul curent) sau creează un cont nou
#   bash diagnoza_login.sh email parola          cont explicit
# Rulează pe ambele adrese: Vercel (cum intră clienții) și Render direct (backend-ul).
EMAIL="${1:-${TEST_EMAIL:-}}"; PASS="${2:-${TEST_PASSWORD:-}}"
if [ -z "$EMAIL" ] && [ -f .env ]; then
  EMAIL="$(grep '^TEST_EMAIL=' .env | head -1 | cut -d= -f2- | tr -d '\r"')"
  PASS="$(grep '^TEST_PASSWORD=' .env | head -1 | cut -d= -f2- | tr -d '\r"')"
fi
REG=0
if [ -z "$EMAIL" ] || [ -z "$PASS" ]; then EMAIL="diag$(date +%s)@test.ro"; PASS='Parola123!'; REG=1; fi
BASES="${BASES:-https://mobilaviva.vercel.app https://mobilaviva.onrender.com}"
esc() { printf '%s' "$1" | sed 's/\\/\\\\/g; s/"/\\"/g'; }
echo "Cont: $EMAIL ($([ $REG = 1 ] && echo 'NOU, se creează' || echo 'existent'))"
for BASE in $BASES; do
  echo; echo "=================== $BASE"
  JAR="$(mktemp)"
  curl -sS -m 150 -c "$JAR" -b "$JAR" -o /dev/null -w "1) trezire GET /api/session: HTTP %{http_code} în %{time_total}s\n" "$BASE/api/session" || { echo "   nu răspunde"; continue; }
  if [ $REG = 1 ]; then
    URL="$BASE/api/auth/register"; BODY="{\"name\":\"Diag Test\",\"email\":\"$(esc "$EMAIL")\",\"password\":\"$(esc "$PASS")\",\"acceptTerms\":true}"
  else
    URL="$BASE/api/auth/login";    BODY="{\"email\":\"$(esc "$EMAIL")\",\"password\":\"$(esc "$PASS")\"}"
  fi
  H="$(mktemp)"
  CODE="$(curl -sS -m 150 -c "$JAR" -b "$JAR" -D "$H" -o /dev/null -w '%{http_code}' -H 'Content-Type: application/json' -d "$BODY" "$URL")"
  echo "2) POST ${URL#$BASE}: HTTP $CODE"
  echo "   set-cookie primit: $(grep -i '^set-cookie' "$H" | sed -E 's/=[^;]*/=***/' | tr -d '\r' | tr '\n' ' ' | sed 's/^$//')"
  [ -n "$(grep -i '^set-cookie' "$H")" ] || echo "   (niciun Set-Cookie în răspunsul de autentificare)"
  S="$(curl -sS -m 60 -c "$JAR" -b "$JAR" "$BASE/api/session")"
  if echo "$S" | grep -q '"user": *null'; then echo "3) GET /api/session după login: user = NULL  -> sesiunea NU a păstrat utilizatorul"
  elif echo "$S" | grep -q '"user": *{'; then echo "3) GET /api/session după login: utilizator logat OK"
  else echo "3) răspuns neașteptat: $(echo "$S" | head -c 160)"; fi
  S2="$(curl -sS -m 60 -c "$JAR" -b "$JAR" "$BASE/api/session")"
  echo "$S2" | grep -q '"user": *{' && echo "4) a doua cerere: încă logat" || echo "4) a doua cerere: user = NULL (sesiunea s-a pierdut între cereri)"
  rm -f "$JAR" "$H"
done
echo; echo "Interpretare: Render direct OK + Vercel NULL = problema e redirecționarea Vercel (cookie). Ambele NULL = backend-ul (repornește / sesiune pierdută). Ambele OK = problema e doar în scriptul de capturi."
