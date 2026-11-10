#!/usr/bin/env bash
# Verifică automat respectarea cerințelor proiectului (obligatorii, I.1-I.11, II, III).
#   ./scripts/verifica.sh [URL|offline]    (implicit: docs/link-public.txt, apoi linkul tunelului, apoi localhost)
# ✔ = îndeplinit, ! = de verificat/recomandat, ✘ = nu respectă cerința. Cod de ieșire 1 dacă există ✘.
cd "$(dirname "$0")/.." || exit 1
OFFLINE=0; if [ "${1:-}" = "offline" ]; then OFFLINE=1; shift; fi   # offline = doar fișierele, fără apeluri către site
URL="${1:-}"
for f in docs/link-public.txt .runtime/public_url .runtime/url; do
  [ -z "$URL" ] && [ -f "$f" ] && URL="$(tr -d '\r\n' < "$f")"
done
[ -n "$URL" ] || URL=http://localhost:8000
URL="${URL%/}"
OKN=0; BADN=0; WARNN=0
ok()   { echo "  ✔ $1"; OKN=$((OKN+1)); }
bad()  { echo "  ✘ $1"; BADN=$((BADN+1)); }
warn() { echo "  ! $1"; WARNN=$((WARNN+1)); }
get()  { curl -s --max-time 15 "$URL$1"; }
code() { curl -s -o /dev/null --max-time 15 -w '%{http_code}' "$URL$1"; }
shot() { ls docs/screenshots/$2*.png >/dev/null 2>&1 && ok "$1: print screen $2" || bad "$1: lipsește print screen $2 (./scripts/screenshots.sh)"; }
PUBLIC=0; case "$URL" in https://*) case "$URL" in *localhost*|*127.0.0.1*) ;; *) PUBLIC=1 ;; esac ;; esac

echo ">>> Verific $URL"
LIVE=0
if [ $OFFLINE = 1 ]; then echo "  (mod offline: doar fișierele proiectului)"
elif [ "$(code /)" = "200" ]; then LIVE=1; ok "site-ul răspunde"; else bad "site-ul nu răspunde la $URL (pornește ./scripts/run.sh sau ./scripts/deploy.sh)"; fi

echo; echo "Cerințe obligatorii"
ok "1 publicare: nu se cere; proiectul se predă ca site_nume_prenume.zip (./scripts/package.sh)"
grep -q 'spring.datasource' src/main/resources/application.properties 2>/dev/null && ok "2 bază de date (JPA + PostgreSQL)" || bad "2 configurația bazei de date lipsește"
shot "2 bază de date" 02
ls src/main/java/com/mobila/shop/*Controller.java >/dev/null 2>&1 && ok "3 script-uri pe server (Java Spring Boot)" || bad "3 controllerele Java lipsesc"
shot "3 script-uri server" 03
if [ $LIVE = 1 ]; then
  if get /api/session | grep -q '"payment":"stripe"'; then ok "4 plată electronică: Stripe Checkout ACTIV (mod test)"
  else warn "4 plata: Stripe inactiv (cheie lipsă/invalidă în .env) => se folosește cardul simulat; pentru „cont de test, card de test” pune sk_test_... în .env"; fi
fi
shot "4 plată Stripe" 04

echo; echo "I. Elemente clasice"
shot "I.1 meniu" 05
if [ $LIVE = 1 ]; then
  get "/api/products" | grep -q '"items":\[{' && ok "I.2 catalog: produse returnate de API" || bad "I.2 catalogul e gol"
  get "/api/facets" | grep -q '"categories":\[{' && ok "I.3 produse grupate pe categorii" || bad "I.3 nu există categorii"
  ID="$(get /api/products | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)"
  [ -n "$ID" ] && get "/api/products/$ID" | grep -q '"price"' && ok "I.4 detalii produs (preț, caracteristici)" || bad "I.4 detalii produs"
  get /api/cart | grep -q '"lines"' && ok "I.5 coș de cumpărături (API)" || bad "I.5 coșul nu răspunde"
  [ "$(code /api/orders)" = "401" ] && ok "I.6 comenzile cer cont de utilizator" || warn "I.6 /api/orders nu cere autentificare?"
  get /api/products | grep -q '"stock":' && ok "I.7 stocuri în catalog" || bad "I.7 stocuri"
  get "/api/products?promo=1" | grep -q '"items":\[{' && ok "I.8 promoții (produse cu preț redus)" || bad "I.8 nu există produse în promoție"
  c="$(code /api/admin/orders)"; { [ "$c" = "401" ] || [ "$c" = "403" ]; } && ok "I.11 administrare există și e protejată" || bad "I.11 /api/admin/orders răspunde $c fără autentificare"
fi
shot "I.2 catalog" 06; shot "I.3 categorii" 07; shot "I.4 detalii" 08; shot "I.5 coș" 09
shot "I.6 comandă pe cont" 10; shot "I.7 stocuri" 11; shot "I.8 promoții" 12
grep -rq 'invoiceNumber' src/main/java 2>/dev/null && ok "I.9 factură (număr, articole, TVA)" || bad "I.9 factura lipsește"
shot "I.9 factură" 13
if [ $LIVE = 1 ]; then
  if get /api/session | grep -q '"payment":"stripe"'; then ok "I.10 plata facturii: plată electronică TESTATĂ (Stripe, mod test, card 4242 4242 4242 4242)"
  else warn "I.10 plata facturii: card de test simulat (4242...); pentru „cont de test” real pune sk_test_... în .env și refă print screen-ul 04"; fi
  for ep in checkout checkout/offline; do
    [ "$(curl -s -o /dev/null -X POST -H 'Content-Type: application/json' -d '{}' --max-time 15 -w '%{http_code}' "$URL/api/$ep")" = "401" ] && ok "I.10 /api/$ep există și cere cont de utilizator" || bad "I.10 /api/$ep nu răspunde 401 fără autentificare"
  done
fi
shot "I.10 plata facturii (testată)" 14
shot "I.10 metode de plată (card, ramburs, transfer)" 14b
shot "I.11 administrare" 15

echo; echo "II. Personalizări"
[ -f frontend/src/views/Compare.vue ] && ok "comparator de produse implementat" || bad "Compare.vue lipsește"
[ -f frontend/src/views/Planner.vue ] && ok "modul reprezentativ: planificator de cameră implementat" || bad "Planner.vue lipsește"
[ -f src/main/java/com/mobila/shop/Matching.java ] && ok "set asortat „completează camera” implementat" || bad "Matching.java lipsește"
[ -f frontend/src/views/Wishlist.vue ] && ok "listă de dorințe implementată" || bad "Wishlist.vue lipsește"
if [ $LIVE = 1 ]; then
  [ -n "$ID" ] && get "/api/products/$ID/matching" | grep -q '"items"' && ok "set asortat: API răspunde" || bad "set asortat: /api/products/ID/matching nu răspunde"
  [ "$(code /api/wishlist)" = "401" ] && ok "listă de dorințe: cere cont (API)" || bad "listă de dorințe: /api/wishlist nu răspunde corect"
fi
[ -f src/main/java/com/mobila/shop/RelatedController.java ] && ok "„Văzute recent” și „Clienții au cumpărat și…” implementate" || bad "RelatedController.java lipsește"
if [ $LIVE = 1 ]; then
  [ -n "$ID" ] && get "/api/products/$ID/also-bought" | grep -q '^\[' && ok "„Clienții au cumpărat și…”: API răspunde" || bad "also-bought nu răspunde"
  [ "$(code -X POST -H 'Content-Type: application/json' -d '{"code":"NUEXISTA"}' /api/cart/coupon)" = "400" ] && ok "cod promoțional: un cod invalid este respins" || warn "cod promoțional: /api/cart/coupon nu a răspuns 400 la un cod invalid"
fi
[ -f src/main/java/com/mobila/shop/ReviewController.java ] && ok "recenzii reale pe produs implementate" || bad "ReviewController.java lipsește"
if [ $LIVE = 1 ]; then
  [ -n "$ID" ] && get "/api/products/$ID/reviews" | grep -q '"count":' && ok "recenzii: API răspunde" || bad "recenzii: /api/products/ID/reviews nu răspunde"
fi
[ -f src/main/java/com/mobila/shop/ChatService.java ] && [ -f frontend/src/components/ChatWidget.vue ] && ok "asistent AI (chatbot) implementat" || bad "asistentul AI lipsește (ChatService.java / ChatWidget.vue)"
if [ $LIVE = 1 ]; then
  CHR="$(curl -s -m 40 -H 'Content-Type: application/json' -d '{"messages":[{"role":"user","content":"Caut o canapea sub 3000 RON"}]}' "$URL/api/chat")"
  echo "$CHR" | grep -q '"reply":"' && ok "asistent AI: API răspunde ($(echo "$CHR" | grep -o '"mode":"[a-z]*"' | head -1))" || bad "asistent AI: /api/chat nu răspunde corect"
  echo "$CHR" | grep -q '"mode":"ai"' || warn "asistent AI: rulează în mod local; pune ANTHROPIC_API_KEY în .env pentru răspunsuri generate de Claude"
fi
shot "personalizări: comparator" 16-
shot "personalizări: planificator" 16b
shot "personalizări: set asortat" 16c
shot "personalizări: listă de dorințe" 16d
shot "personalizări: asistent AI" 20-

echo; echo "III. Documentare și prezentare"
shot "PageSpeed Mobile" 18
shot "PageSpeed Desktop" 19
[ -f docs/documentare/documentatie.docx ] && [ -f docs/documentare/prezentare.pptx ] && ok "documentare .docx și prezentare .pptx generate" || bad "lipsește documentarea (./scripts/docs.sh)"
if grep -q 'COMPLETEAZĂ' docs/continut-documentare.json 2>/dev/null; then bad "documentarea are zone [COMPLETEAZĂ ...] (studiile de caz 1 și 2): docs/continut-documentare.json"; else ok "textele din documentare sunt completate"; fi
[ -s docs/link-public.txt ] && ok "link de publicare: $(tr -d '\r\n' < docs/link-public.txt)" || ok "predare ca arhivă zip (./scripts/package.sh nume_prenume)"

echo; echo "Traduceri listă mobilier"
if OUTN=$(node scripts/verifica-nume.mjs 2>&1); then ok "$OUTN"; else bad "traduceri incomplete:"; echo "$OUTN" | head -20 | sed "s/^/      /"; fi

echo; echo "Securitate"
if [ $LIVE = 1 ] && [ $PUBLIC = 1 ]; then
  c="$(curl -s -o /dev/null -m 20 -w '%{http_code}' -H 'Content-Type: application/json' -d '{"email":"admin@mobilaviva.com","password":"Admin123!"}' "$URL/api/auth/login")"
  [ "$c" = "200" ] && bad "admin cu parola IMPLICITĂ pe site-ul public! Rulează ./scripts/deploy.sh (generează altă parolă)" || ok "parola implicită de admin nu funcționează pe site-ul public"
fi
grep -qE '^STRIPE_SECRET_KEY=sk_(test|live)_' .env 2>/dev/null && ok "cheia Stripe e în .env (nu intră în arhivă)" || warn "STRIPE_SECRET_KEY lipsește din .env"

echo; echo ">>> Rezultat: $OKN îndeplinite, $WARNN de verificat, $BADN neîndeplinite"
[ $BADN -eq 0 ] || exit 1
