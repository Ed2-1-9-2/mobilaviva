#!/usr/bin/env bash
# ./scripts/lansare.sh          -> pornește tot (DB + backend Fly) și verifică Vercel
# ./scripts/lansare.sh front    -> + republică frontend-ul pe Vercel (modificări de limbă/interfață)
# ./scripts/lansare.sh tot      -> + rebuild backend pe Fly + frontend pe Vercel (modificări Java)
cd "$(dirname "$0")/.." || exit 1
FLY="$HOME/.fly/bin/flyctl.exe"; [ -x "$FLY" ] || FLY="$(ls /c/Users/*/.fly/bin/flyctl.exe 2>/dev/null | head -1)"; [ -x "$FLY" ] || FLY="$(command -v fly flyctl | head -1)"
[ "${1:-}" = "tot" ] && { ./scripts/deploy.sh || exit 1; }
[ -f .runtime/fly-app ] || { echo "Nu există .runtime/fly-app. Rulează întâi ./scripts/deploy.sh (sau ./scripts/lansare.sh tot)"; exit 1; }
A="$(cat .runtime/fly-app)"; DB="$(cat .runtime/fly-db 2>/dev/null || echo "${A}-db")"
ids() { "$FLY" machines list -a "$1" --json 2>/dev/null | node -e 'let s="";process.stdin.on("data",d=>s+=d).on("end",()=>{try{JSON.parse(s).forEach(m=>console.log(m.id))}catch(e){}})' | tr -d '\r'; }

echo ">>> Pornesc baza de date și backend-ul (dacă erau oprite)..."
for m in $(ids "$DB"); do "$FLY" machine start "$m" -a "$DB" >/dev/null 2>&1; done
sleep 15
for m in $(ids "$A"); do "$FLY" machine start "$m" -a "$A" >/dev/null 2>&1; done

echo ">>> Aștept backend-ul..."
r=""
for i in $(seq 1 30); do
  r=$(curl -s -m 8 "https://$A.fly.dev/api/facets" | head -c 40)
  [ -n "$r" ] && { echo "Backend OK: $r"; break; }; echo "  încă pornește... $i"; sleep 10
done
[ -z "$r" ] && { echo "Backend-ul nu răspunde. Vezi: $FLY logs -a $A --no-tail | tail -40"; exit 1; }

case "${1:-}" in front|tot) ./scripts/vercel_deploy.sh ;; esac

echo ">>> Verificare Vercel:"
curl -s https://mobilaviva.vercel.app/api/facets | head -c 80; echo
echo ">>> Gata: https://mobilaviva.vercel.app"
