#!/usr/bin/env bash
# Activează Stripe pe serviciul Render FĂRĂ dashboard: trimite STRIPE_SECRET_KEY din .env prin API-ul Render și redeployează.
# O singură dată: Render -> Account Settings -> API Keys -> Create API Key; pune-o în .env ca RENDER_API_KEY=rnd_...
# În .env trebuie și STRIPE_SECRET_KEY=sk_test_...   Rulare: ./scripts/render_env.sh
cd "$(dirname "$0")/.."
getv() { local v="${!1:-}"; if [ -z "$v" ] && [ -f .env ]; then v="$(grep "^$1=" .env | tail -1 | cut -d= -f2- | tr -d '\r' | sed 's/^"\(.*\)"$/\1/')"; fi; printf '%s' "$v"; }
RK="$(getv RENDER_API_KEY)"; SK="$(getv STRIPE_SECRET_KEY)"; PK="$(getv STRIPE_PUBLISHABLE_KEY)"
SVC="${RENDER_SERVICE_ID:-srv-dbbrhk59fdbs73f4q250}"; SITE="${SITE_URL:-https://mobilaviva.vercel.app}"
[ -z "$RK" ] && { echo "Lipsește RENDER_API_KEY. Render -> Account Settings -> API Keys -> Create API Key, apoi pune în .env:  RENDER_API_KEY=rnd_..." >&2; exit 1; }
case "$SK" in sk_test_*) ;; "") echo "Lipsește STRIPE_SECRET_KEY din .env (pune STRIPE_SECRET_KEY=sk_test_...)." >&2; exit 1 ;;
  *) echo "STRIPE_SECRET_KEY nu începe cu sk_test_ (din siguranță trimit doar chei de test)." >&2; exit 1 ;; esac
put() {   # $1=cheie $2=valoare
  curl -s -o /dev/null -w '%{http_code}' -X PUT "https://api.render.com/v1/services/$SVC/env-vars/$1" \
    -H "Authorization: Bearer $RK" -H 'Content-Type: application/json' -d "{\"value\":\"$2\"}"
}
c="$(put STRIPE_SECRET_KEY "$SK")"; [ "$c" = "200" ] || { echo "Render a răspuns $c la STRIPE_SECRET_KEY (verifică RENDER_API_KEY și ID-ul serviciului $SVC)." >&2; exit 1; }
echo ">>> STRIPE_SECRET_KEY setată pe serviciul Render"
case "$PK" in pk_test_*) [ "$(put STRIPE_PUBLISHABLE_KEY "$PK")" = "200" ] && echo ">>> STRIPE_PUBLISHABLE_KEY setată" ;; esac
c="$(curl -s -o /dev/null -w '%{http_code}' -X POST "https://api.render.com/v1/services/$SVC/deploys" -H "Authorization: Bearer $RK" -H 'Content-Type: application/json' -d '{"clearCache":"do_not_clear"}')"
echo ">>> Redeploy pornit (cod $c). Aștept ca site-ul să raporteze Stripe activ (max. ~6 min)..."
for i in $(seq 1 18); do
  sleep 20
  if curl -s --max-time 60 "$SITE/api/session" | grep -q '"payment":"stripe"'; then
    echo ">>> STRIPE ACTIV pe $SITE (payment=stripe). Fă o comandă nouă și plătește cu 4242 4242 4242 4242."; exit 0
  fi
  echo "    încă se pornește ($i/18)..."
done
echo "!!! După ~6 minute payment nu e încă 'stripe'. Verifică Render -> Logs, linia 'Stripe:'." >&2; exit 1
