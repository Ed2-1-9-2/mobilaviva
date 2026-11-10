#!/usr/bin/env bash
# Test funcțional cap-coadă, cu carduri de test (modul fără Stripe): cont nou, coș, plată refuzată, plată reușită,
# factură, comenzi, ramburs, transfer, recenzii și căutări speciale. Raportul se salvează în docs/test-flux.txt.
#   ./scripts/test_flux.sh [URL]        (implicit: aplicația locală; creează cont și comenzi de test, deci NU se rulează pe site-ul public)
cd "$(dirname "$0")/.." || exit 1
URL="${1:-}"
for f in .runtime/url; do [ -z "$URL" ] && [ -f "$f" ] && URL="$(tr -d '\r\n' < "$f")"; done
[ -n "$URL" ] || URL=http://localhost:8000
URL="${URL%/}"
mkdir -p docs
{
OK=0; BAD=0
ok()  { echo "  ✔ $1"; OK=$((OK+1)); }
bad() { echo "  ✘ $1"; BAD=$((BAD+1)); }
JAR="$(mktemp)"; BODY="$(mktemp)"
trap 'rm -f "$JAR" "$BODY"' EXIT
call() {   # call METODĂ CALE [JSON] -> codul HTTP; corpul rămâne în $BODY
  if [ -n "$3" ]; then curl -s -o "$BODY" -w '%{http_code}' --max-time 20 -b "$JAR" -c "$JAR" -X "$1" -H 'Content-Type: application/json' -d "$3" "$URL$2"
  else curl -s -o "$BODY" -w '%{http_code}' --max-time 20 -b "$JAR" -c "$JAR" -X "$1" "$URL$2"; fi
}
has() { grep -q "$1" "$BODY"; }

echo ">>> Test funcțional pe $URL ($(date '+%d.%m.%Y %H:%M'))"
[ "$(call GET /api/session)" = "200" ] || { echo "  ✘ site-ul nu răspunde la $URL"; exit 1; }
if has '"payment":"stripe"'; then
  echo "  ! Stripe e activ: plata se face pe pagina Stripe (card 4242 4242 4242 4242); testele cu card simulat se sar."
  STRIPE=1
else STRIPE=0; fi

MAIL="test$(date +%s)@example.com"
[ "$(call POST /api/auth/register "{\"name\":\"Client Test\",\"email\":\"$MAIL\",\"password\":\"Parola123!\",\"acceptTerms\":true}")" = "200" ] && ok "cont nou creat și autentificat ($MAIL)" || bad "înregistrarea a eșuat"
[ "$(call GET /api/orders)" = "200" ] && ok "zona „comenzile mele” e accesibilă după autentificare" || bad "comenzile nu se încarcă după autentificare"

call GET "/api/products?in_stock=1" >/dev/null
ID="$(grep -o '"id":[0-9]*' "$BODY" | head -1 | cut -d: -f2)"
[ -n "$ID" ] && ok "catalog: produs în stoc găsit (id $ID)" || { bad "nu există produse în stoc"; exit 1; }
call GET "/api/products?q=canapea" >/dev/null; has '"items":\[{' && ok "căutare text: „canapea” returnează produse" || bad "căutarea „canapea” nu returnează nimic"
call GET "/api/products?max_price=3000&in_stock=1&sort=price_asc" >/dev/null; has '"items":\[{' && ok "căutare cu filtre: preț maxim 3000, în stoc, cele mai ieftine întâi" || bad "căutarea cu filtre nu returnează nimic"
call GET "/api/suggest?q=can" >/dev/null; has '"name"' && ok "autocomplete: sugestii pentru „can”" || bad "autocomplete fără rezultate"

if [ $STRIPE = 0 ]; then
  CARD() { echo "{\"name\":\"Client Test\",\"email\":\"$MAIL\",\"address\":\"Str. Exemplu 1, București\",\"card\":\"$1\",\"exp\":\"12/30\",\"cvv\":\"123\"}"; }
  [ "$(call POST /api/cart/items "{\"productId\":$ID,\"qty\":1}")" = "200" ] && ok "produs adăugat în coș" || bad "adăugarea în coș a eșuat"
  [ "$(call POST /api/checkout "$(CARD 1234567812345678)")" = "400" ] && ok "card cu număr invalid (Luhn): plata respinsă" || bad "un card invalid a fost acceptat"
  [ "$(call POST /api/checkout "$(CARD 4000000000000002)")" = "400" ] && has 'refuzat' && ok "card de test 4000 0000 0000 0002: plată refuzată de bancă" || bad "cardul de test „refuzat” nu a fost respins"
  [ "$(call POST /api/checkout "$(CARD 4000000000009995)")" = "400" ] && has 'insuficiente' && ok "card de test 4000 0000 0000 9995: fonduri insuficiente" || bad "cardul „fonduri insuficiente” nu a fost respins"
  call GET /api/cart >/dev/null; has "\"id\":$ID" && ok "după plata refuzată coșul rămâne intact" || bad "coșul a fost golit de o plată refuzată"
  if [ "$(call POST /api/checkout "$(CARD 4242424242424242)")" = "200" ] && has 'invoiceNumber'; then
    ok "card de test 4242 4242 4242 4242: plată reușită, comandă și factură create ($(grep -o '"invoiceNumber":"[^"]*"' "$BODY" | head -1 | cut -d'"' -f4))"
    ORD="$(grep -o '"id":[0-9]*' "$BODY" | head -1 | cut -d: -f2)"
  else bad "plata cu cardul de test 4242 a eșuat"; fi
  call GET /api/cart >/dev/null; has '"lines":\[\]' && ok "după plată coșul e gol" || bad "coșul nu s-a golit după plată"
  [ -n "$ORD" ] && [ "$(call GET "/api/orders/$ORD")" = "200" ] && has 'invoiceNumber' && ok "factura comenzii se poate deschide (număr, articole, TVA)" || bad "factura comenzii nu se încarcă"
  call GET /api/orders >/dev/null; has 'invoiceNumber' && ok "comanda apare în istoricul contului" || bad "comanda lipsește din istoric"
  for M in ramburs transfer; do
    call POST /api/cart/items "{\"productId\":$ID,\"qty\":1}" >/dev/null
    [ "$(call POST /api/checkout/offline "{\"name\":\"Client Test\",\"email\":\"$MAIL\",\"address\":\"Str. Exemplu 1, București\",\"method\":\"$M\"}")" = "200" ] && ok "metodă de plată „$M”: comandă creată" || bad "metoda „$M” a eșuat"
  done
fi

[ "$(call POST "/api/products/$ID/reviews" '{"stars":9,"text":"x"}')" = "400" ] && ok "recenzie cu notă invalidă (9): respinsă" || bad "o notă invalidă a fost acceptată"
[ "$(call POST "/api/products/$ID/reviews" '{"stars":5,"text":"Produs foarte bun, livrare rapidă."}')" = "200" ] && ok "recenzie trimisă (5 stele)" || bad "trimiterea recenziei a eșuat"
[ $STRIPE = 0 ] && { has '"verified":true' && ok "recenzia e marcată „cumpărător verificat” (produsul a fost comandat)" || bad "recenzia nu e marcată verificat deși produsul a fost comandat"; }
[ "$(call POST "/api/products/$ID/reviews" '{"stars":4}')" = "409" ] && ok "a doua recenzie la același produs: respinsă" || bad "recenzie dublă acceptată"
call GET "/api/products/$ID/reviews" >/dev/null; has '"count":1' && ok "recenziile se citesc public, cu media notelor" || bad "lista de recenzii nu se încarcă"
call PUT "/api/wishlist/$ID" >/dev/null; call GET /api/wishlist >/dev/null; has "\"ids\":\[$ID\]" && ok "listă de dorințe: produs salvat" || bad "lista de dorințe nu funcționează"
[ "$(call GET "/api/products/$ID/matching")" = "200" ] && ok "set asortat „completează camera” răspunde" || bad "setul asortat nu răspunde"

echo; echo ">>> Rezultat: $OK trecute, $BAD eșuate"
[ $BAD -eq 0 ]
} 2>&1 | tee docs/test-flux.txt
exit "${PIPESTATUS[0]}"
