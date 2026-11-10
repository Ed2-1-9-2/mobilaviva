#!/usr/bin/env bash
# Test email, într-o singură comandă:  ./scripts/test_email.sh [email_cu_cont_in_magazin]
#  1) trimite un email de test prin SMTP (arată dacă parola e bună)
#  2) dacă dai un email: cere „Am uitat parola” pe site-ul public și arată ce scrie serverul în log
cd "$(dirname "$0")/.."
rd() { grep "^$1=" .env 2>/dev/null | tail -1 | cut -d= -f2- | tr -d '\r'; }
U="$(rd SMTP_USER)"; P="$(rd SMTP_PASSWORD | tr -d ' ')"
[ "${1:-}" != auto ] && [ -z "$U" ] && read -r -p "Adresa de email expeditor (SMTP_USER): " U
if [ -z "$P" ] && [ "${1:-}" != auto ]; then read -r -s -p "Parola de aplicație pentru $U (nu se afișează): " P; echo; P="$(printf %s "$P" | tr -d ' \r\n')"; fi
[ -z "$U" ] || [ -z "$P" ] && { echo ">>> Email: SMTP necompletat, emailurile de resetare NU pleacă. Pune SMTP_USER_DEFAULT și SMTP_PASSWORD_DEFAULT în setup_mobila_java_vue.sh și rulează din nou."; [ "${1:-}" = auto ] && exit 0; exit 1; }
HOST="smtp.gmail.com"; case "${U#*@}" in outlook.*|hotmail.*|live.*|msn.*) HOST="smtp-mail.outlook.com:587";; yahoo.*) HOST="smtp.mail.yahoo.com";; icloud.com|me.com) HOST="smtp.mail.me.com";; esac
URL="smtps://$HOST"; case "$HOST" in *:587) URL="smtp://$HOST";; *) URL="smtps://$HOST:465";; esac
echo ">>> 1/2  Trimit email de test de la $U către $U ..."
if OUT="$(printf 'From: %s\nTo: %s\nSubject: Test MobilaViva\n\nDacă vezi acest mesaj, parola SMTP este bună.\n' "$U" "$U" | curl -sS --ssl-reqd "$URL" --user "$U:$P" --mail-from "$U" --mail-rcpt "$U" -T - 2>&1)"; then
  echo "    OK: parola e bună. Verifică Inbox / Spam / Promoții la $U."
  grep -q "^SMTP_USER=" .env 2>/dev/null || { echo "SMTP_USER=$U" >> .env; echo "SMTP_PASSWORD=$P" >> .env; }
else
  echo "    EȘEC: $OUT"; echo "    -> parola nu e o parolă de aplicație validă (Gmail cere verificarea în 2 pași) sau adresa e greșită."; exit 1
fi
case "${1:-}" in auto) exit 0;; esac
[ -z "${1:-}" ] && { echo ">>> Pentru testul 2: ./scripts/test_email.sh adresa_cu_cont@..."; exit 0; }
BASE="https://mobilaviva.vercel.app"
echo ">>> 2/2  „Am uitat parola” pe $BASE pentru $1 ..."
echo "    HTTP $(curl -s -o /dev/null -w '%{http_code}' -X POST -H 'Content-Type: application/json' -d "{\"email\":\"$1\",\"origin\":\"$BASE\"}" "$BASE/api/auth/forgot")  (200 = cerere acceptată)"
sleep 8
FLY="$(command -v flyctl || echo "$HOME/.fly/bin/flyctl")"; APP="$(grep -m1 '^app *=' fly.toml | sed 's/.*"\(.*\)".*/\1/')"
echo ">>> Log server (SMTP / Brevo):"
"$FLY" logs -a "$APP" --no-tail 2>/dev/null | grep -i "smtp\|brevo" | tail -5
echo "    'SMTP: email trimis' = a plecat. Nicio linie = adresa NU are cont în magazin (creeaz-o la „Cont nou”) sau serverul nu are încă SMTP_USER/SMTP_PASSWORD."
