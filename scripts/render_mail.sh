#!/usr/bin/env bash
# Trimite pe Render variabilele de email din .env (SMTP_*, GMAIL_RELAY_*), redeployează, apoi testează „Am uitat parola”.
# Rulare: ./scripts/render_mail.sh [email_cu_cont_in_magazin]     (RENDER_API_KEY trebuie să fie în .env)
cd "$(dirname "$0")/.."
getv() { local v="${!1:-}"; if [ -z "$v" ] && [ -f .env ]; then v="$(grep "^$1=" .env | tail -1 | cut -d= -f2- | tr -d '\r' | sed 's/^"\(.*\)"$/\1/')"; fi; printf '%s' "$v"; }
RK="$(getv RENDER_API_KEY)"; SVC="${RENDER_SERVICE_ID:-srv-dbbrhk59fdbs73f4q250}"; SITE="${SITE_URL:-https://mobilaviva.vercel.app}"
[ -z "$RK" ] && { echo "Lipsește RENDER_API_KEY din .env" >&2; exit 1; }
put() { python3 -c 'import json,sys;print(json.dumps({"value":sys.argv[1]}))' "$2" 2>/dev/null | curl -s -o /dev/null -w '%{http_code}' -X PUT "https://api.render.com/v1/services/$SVC/env-vars/$1" -H "Authorization: Bearer $RK" -H 'Content-Type: application/json' -d @-; }
for K in SMTP_USER SMTP_PASSWORD SMTP_HOST SMTP_PORT SMTP_USER2 SMTP_PASSWORD2 SMTP_HOST2 GMAIL_RELAY_URL GMAIL_RELAY_TOKEN BREVO_API_KEY MAIL_FROM; do
  V="$(getv $K)"; [ -z "$V" ] && continue
  c="$(put "$K" "$V")"; [ "$c" = "200" ] && echo ">>> $K setat pe Render" || { echo "Render a răspuns $c la $K (verifică RENDER_API_KEY / serviciul $SVC)" >&2; exit 1; }
done
[ -z "$(getv GMAIL_RELAY_URL)" ] && echo "!!! GMAIL_RELAY_URL e gol: pe Render gratuit SMTP e blocat, emailul NU va pleca. Fă pașii din docs/gmail_relay.gs." >&2
c="$(curl -s -o /dev/null -w '%{http_code}' -X POST "https://api.render.com/v1/services/$SVC/deploys" -H "Authorization: Bearer $RK" -H 'Content-Type: application/json' -d '{"clearCache":"do_not_clear"}')"
echo ">>> Redeploy pornit (cod $c). Aștept serverul (max ~6 min)..."
for i in $(seq 1 18); do sleep 20; curl -s --max-time 60 "$SITE/api/session" | grep -q '{' && [ "$i" -ge 3 ] && break; echo "    ($i/18)"; done
[ -n "${1:-}" ] && { echo ">>> Test „Am uitat parola” pentru $1:"; curl -s -X POST -H 'Content-Type: application/json' -d "{\"email\":\"$1\",\"origin\":\"$SITE\"}" "$SITE/api/auth/forgot"; echo; echo "    (fără „errors” = trimis; verifică Inbox)"; }
