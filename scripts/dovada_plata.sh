#!/usr/bin/env bash
# Dovadă pentru cerința I.10 (plata facturii, plată electronică TESTATĂ).
# Citește din API-ul Stripe (cont de test) ultimele plăți reușite și le afișează curat, pentru print screen.
# Cheia secretă nu se afișează și nu se scrie nicăieri. Rezultatul se salvează în docs/dovada-I10.txt.
#   ./scripts/dovada_plata.sh        ultimele 5 plăți        ./scripts/dovada_plata.sh 10   ultimele 10
cd "$(dirname "$0")/.."
KEY="${STRIPE_SECRET_KEY:-}"
[ -z "$KEY" ] && [ -f .env ] && KEY="$(grep '^STRIPE_SECRET_KEY=' .env | head -1 | cut -d= -f2- | tr -d '\r"')"
case "$KEY" in
  sk_test_*) ;;
  "") echo "Nu găsesc STRIPE_SECRET_KEY (în .env sau exportată). Pune sk_test_... și rulează din nou." >&2; exit 1 ;;
  *) echo "Cheia nu este de test (sk_test_...). Din siguranță nu rulez pe un cont live." >&2; exit 1 ;;
esac
command -v node >/dev/null || { echo "Lipsește node (necesar pentru formatarea rezultatului)." >&2; exit 1; }
N="${1:-5}"
mkdir -p docs
curl -sg --max-time 30 "https://api.stripe.com/v1/checkout/sessions?limit=$N&expand[]=data.payment_intent.payment_method" -u "$KEY:" \
  | node scripts/tools/dovada-plata.js | tee docs/dovada-I10.txt
