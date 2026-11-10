#!/usr/bin/env bash
# Verifică în detaliu Stripe pe site-ul publicat: stare, mod (TEST/LIVE) și plățile reale din contul Stripe.
#   ./scripts/verifica_stripe.sh [URL]     (parola admin: ADMIN_PASSWORD sau Admin123!)
cd "$(dirname "$0")/.." || exit 1
URL="${1:-https://mobilaviva.vercel.app}"; URL="${URL%/}"
EMAIL="admin@mobilaviva.com"; PASS="${ADMIN_PASSWORD:-Admin123!}"
JAR=$(mktemp)
echo ">>> 1. Stare Stripe pe $URL"
curl -s --max-time 60 "$URL/api/session" | grep -o '"payment":"[a-z]*"' || echo "!!! /api/session nu răspunde"
echo ">>> 2. Login admin"
CODE=$(curl -s --max-time 60 -c "$JAR" -b "$JAR" -o /dev/null -w '%{http_code}' -X POST -H 'Content-Type: application/json' \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASS\"}" "$URL/api/auth/login")
echo "    login: HTTP $CODE"; [ "$CODE" = 200 ] || { echo "!!! Login eșuat (parolă? ADMIN_PASSWORD=... ./scripts/verifica_stripe.sh)"; rm -f "$JAR"; exit 1; }
echo ">>> 3. Plăți citite direct din API-ul Stripe"
curl -s --max-time 60 -b "$JAR" "$URL/api/admin/stripe/payments" | node -e '
let d="";process.stdin.on("data",c=>d+=c).on("end",()=>{
  let j;try{j=JSON.parse(d)}catch{console.log("Răspuns invalid:",d.slice(0,300));process.exit(1)}
  if(j.errors){console.log("!!!",j.errors.join(" "));process.exit(1)}
  console.log("    Mod cont Stripe:",j.mode);
  if(!j.payments.length){console.log("    Nicio plată reușită încă (fă o comandă cu cardul 4242 și rulează din nou).");return}
  console.log("    "+"Data".padEnd(18)+"Payment Intent".padEnd(30)+"Sumă".padEnd(16)+"Card".padEnd(18)+"Mod");
  for(const p of j.payments){
    const dt=new Date(p.created*1000).toLocaleString("ro-RO",{dateStyle:"short",timeStyle:"short"});
    console.log("    "+dt.padEnd(18)+p.paymentIntent.padEnd(30)+(p.amount.toFixed(2)+" "+p.currency).padEnd(16)+p.card.padEnd(18)+(p.live?"LIVE":"TEST"));
  }
  console.log("    Total plăți reușite:",j.payments.length);
})'
rm -f "$JAR"
