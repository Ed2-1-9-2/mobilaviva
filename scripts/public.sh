#!/usr/bin/env bash
# Link public TEMPORAR (Cloudflare Quick Tunnel) pentru PageSpeed și print screen-uri, până publici pe un domeniu.
#   ./scripts/public.sh               pornește tunelul + aplicația și afișează linkul public
#   ./scripts/public.sh screenshots   în plus rulează ./scripts/screenshots.sh pe linkul public (inclusiv PageSpeed 18/19)
# Linkul se schimbă la fiecare pornire și merge doar cât rulează acest script (Ctrl+C oprește tunelul).
cd "$(dirname "$0")/.." || exit 1
mkdir -p scripts/tools .runtime
port_busy() { (echo > "/dev/tcp/127.0.0.1/$1") >/dev/null 2>&1; }
LOCK=.runtime/public.lock
fail() { echo "!!! $1" >&2; rm -f "$LOCK" .runtime/tunnel.pid; exit 1; }

# ---- o singură instanță: ./scripts/public.sh stop oprește tot (tunel + aplicație); rămășițele unei rulări oprite brusc se curăță
if [ "$1" = "stop" ]; then
  [ -f "$LOCK" ] && { read -r OLD < "$LOCK"; kill "$OLD" 2>/dev/null; }
  [ -f .runtime/tunnel.pid ] && kill "$(cat .runtime/tunnel.pid)" 2>/dev/null
  docker compose down >/dev/null 2>&1
  rm -f "$LOCK" .runtime/tunnel.pid .runtime/public_url
  echo ">>> Oprit: tunel + aplicație."; exit 0
fi
if [ -f "$LOCK" ]; then
  read -r OLD < "$LOCK"
  if [ -n "$OLD" ] && kill -0 "$OLD" 2>/dev/null; then
    echo "!!! ./scripts/public.sh rulează deja (PID $OLD). Folosește linkul din terminalul acela sau oprește-l cu:  ./scripts/public.sh stop" >&2
    exit 1
  fi
  [ -f .runtime/tunnel.pid ] && kill "$(cat .runtime/tunnel.pid)" 2>/dev/null   # tunel rămas de la o rulare oprită brusc
  rm -f "$LOCK" .runtime/tunnel.pid
fi
echo $$ > "$LOCK"

# ---- cloudflared: din PATH sau descărcat o singură dată în tools/
CF="$(command -v cloudflared 2>/dev/null)"
if [ -z "$CF" ]; then
  case "$(uname -s)" in
    MINGW*|MSYS*|CYGWIN*) ASSET=cloudflared-windows-amd64.exe; CF=scripts/tools/cloudflared.exe ;;
    Linux)                ASSET=cloudflared-linux-amd64;       CF=scripts/tools/cloudflared ;;
    *) fail "Instalează cloudflared (macOS: brew install cloudflared) și rulează din nou." ;;
  esac
  if [ ! -s "$CF" ]; then
    echo ">>> Descarc cloudflared (o singură dată)..."
    curl -fL --progress-bar -o "$CF" "https://github.com/cloudflare/cloudflared/releases/latest/download/$ASSET" \
      || fail "Nu pot descărca cloudflared. Instalează-l manual: winget install --id Cloudflare.cloudflared"
    chmod +x "$CF"
  fi
fi

# ---- port liber, același pentru tunel și aplicație
P="${PORT:-9000}"; PORT=""
for p in $(seq "$P" $((P + 30))); do port_busy "$p" || { PORT="$p"; break; }; done
[ -n "$PORT" ] || fail "Nu am găsit niciun port liber."

# ---- tunelul
: > .runtime/tunnel.log
"$CF" tunnel --url "http://localhost:$PORT" --no-autoupdate > .runtime/tunnel.log 2>&1 &
TPID=$!
echo "$TPID" > .runtime/tunnel.pid
PUB=""
echo ">>> Pornesc tunelul Cloudflare..."
for _ in $(seq 1 60); do
  PUB="$(grep -o 'https://[a-z0-9-]*\.trycloudflare\.com' .runtime/tunnel.log | grep -v '^https://api\.' | head -1)"
  [ -n "$PUB" ] && break
  kill -0 "$TPID" 2>/dev/null || break
  sleep 1
done
[ -n "$PUB" ] || { kill "$TPID" 2>/dev/null; tail -5 .runtime/tunnel.log; fail "Tunelul nu a pornit (vezi .runtime/tunnel.log)."; }
echo "$PUB" > .runtime/public_url
echo ">>> Link public (temporar): $PUB"

# ---- aplicația, cu linkul public ca adresă de bază (Stripe se întoarce pe el)
SITE_HOST=localhost PORT="$PORT" PUBLIC_URL="$PUB" ./scripts/run.sh &
APID=$!
cleanup() { kill "$TPID" "$APID" 2>/dev/null; docker compose down >/dev/null 2>&1; rm -f "$LOCK" .runtime/tunnel.pid; }
trap 'cleanup; exit 0' INT TERM

echo ">>> Aștept aplicația prin tunel (prima pornire poate dura câteva minute)..."
OK=0
for _ in $(seq 1 360); do
  [ "$(curl -s -o /dev/null --max-time 5 -w '%{http_code}' "$PUB/")" = "200" ] && { OK=1; break; }
  kill -0 "$APID" 2>/dev/null || break
  sleep 2
done
[ "$OK" = 1 ] || { cleanup; fail "Aplicația nu răspunde pe $PUB."; }
echo; echo ">>> GATA. Site public temporar: $PUB"
echo ">>> PageSpeed manual: https://pagespeed.web.dev/  (lipești linkul de mai sus; Mobile și Desktop)"
XR="$(curl -sI --max-time 10 "$PUB/" 2>/dev/null | tr -d '\r' | grep -i '^x-robots-tag')"
[ -n "$XR" ] && echo ">>> Atenție SEO: tunelul trimite \"$XR\" → scorul SEO din PageSpeed scade (~66); pe un domeniu real dispare."

if [ "$1" = "screenshots" ]; then
  ./scripts/screenshots.sh "$PUB"
  echo ">>> Capturi gata în docs/screenshots/ și docs/PREZENTARE.html. Tunelul rămâne pornit; Ctrl+C îl oprește."
else
  echo ">>> Pentru capturi automate, în alt terminal: ./scripts/screenshots.sh $PUB"
fi
echo ">>> Ține terminalul deschis cât folosești linkul. Ctrl+C oprește tunelul și aplicația."
wait "$APID"
cleanup
