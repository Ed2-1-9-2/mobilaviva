#!/usr/bin/env bash
# TOT DINTR-O COMANDĂ, fiecare pas în terminalul lui:
#   terminal 1: git commit + push, apoi așteaptă ca versiunea nouă să apară pe site (Render redeployează)
#   terminal 2: aplicația locală (doar pentru captura 02, baza de date; log-urile se văd aici)
#   terminal 3: parola de admin -> așteaptă terminalele 1 și 2 -> print screen-uri + PageSpeed 18/19 pe site-ul public
#   ./scripts/full.sh [URL]        (implicit https://mobilaviva.vercel.app)      ./scripts/full.sh stop  oprește aplicația locală
cd "$(dirname "$0")/.." || exit 1
DIR="$(pwd)"
mkdir -p .runtime
[ "${1:-}" = "stop" ] && { [ -f .runtime/full-app.pid ] && kill "$(cat .runtime/full-app.pid)" 2>/dev/null; rm -f .runtime/full-app.pid; echo ">>> Oprit."; exit 0; }
URL="${1:-${LIVE_URL:-https://mobilaviva.vercel.app}}"; URL="${URL%/}"
echo "$URL" > .runtime/live-url
rm -f .runtime/deploy-ok .runtime/url

cat > .runtime/term-deploy.sh <<'T1'
#!/usr/bin/env bash
cd "$(dirname "$0")/.." || exit 1
URL="$(tr -d '\r\n' < .runtime/live-url)"
MARK="${DEPLOY_MARKER:-menubar}"
echo ">>> TERMINAL 1/3: git + deploy pe Render"
NO_CHECK=1 ./scripts/push.sh || { read -r -p "Enter pentru a închide..." _; exit 1; }
echo ">>> Aștept versiunea nouă pe $URL (de obicei 3-8 minute, maximum 20)..."
for i in $(seq 1 240); do
  if curl -s --max-time 60 "$URL/" | grep -q "$MARK"; then
    echo ">>> Versiunea nouă este live."
    for i in $(seq 1 60); do [ -s .runtime/admin-password ] || [ -n "${ADMIN_PASSWORD:-}" ] && break; [ "$i" = 1 ] && echo ">>> Aștept parola de admin din terminalul 3..."; sleep 5; done
    CHECK_WAIT=300 ./scripts/check_vercel.sh "$URL"; touch .runtime/deploy-ok; read -r -p "Gata. Enter pentru a închide..." _; exit 0
  fi
  [ $((i % 6)) -eq 0 ] && echo "    ... încă se publică ($((i * 5 / 60)) min)"
  sleep 5
done
echo "!!! După 20 de minute nu apare versiunea nouă. Verifică deploy-ul pe Render." >&2
read -r -p "Enter pentru a închide..." _
T1

cat > .runtime/term-app.sh <<'T2'
#!/usr/bin/env bash
cd "$(dirname "$0")/.." || exit 1
echo $$ > .runtime/full-app.pid
echo ">>> TERMINAL 2/3: aplicația locală (log-uri; Ctrl+C oprește). Folosită doar pentru captura 02."
./scripts/run.sh
T2

cat > .runtime/term-shots.sh <<'T3'
#!/usr/bin/env bash
cd "$(dirname "$0")/.." || exit 1
URL="$(tr -d '\r\n' < .runtime/live-url)"
code() { curl -s -o /dev/null --max-time 60 -w '%{http_code}' "$1"; }
echo ">>> TERMINAL 3/3: print screen-uri + PageSpeed pe $URL"
if [ -z "${ADMIN_PASSWORD:-}" ] && [ ! -s .runtime/admin-password ]; then
  read -r -s -p "Parola de admin de pe Render (Enter = Admin123!): " AP; echo
  export ADMIN_PASSWORD="${AP:-Admin123!}"
  # o salvez local (nu intră în git/arhivă) ca terminalul 1 să verifice login-ul cu aceeași parolă
  ( umask 077; printf '%s' "$ADMIN_PASSWORD" > .runtime/admin-password )
fi
echo ">>> Aștept terminalul 1 (deploy)..."
until [ -f .runtime/deploy-ok ]; do sleep 5; done
echo ">>> Trezesc site-ul (2 răspunsuri 200 la rând)..."
OK=0
for _ in $(seq 1 36); do
  if curl -s --max-time 60 "$URL/" | grep -q 'id="app"'; then OK=$((OK + 1)); else OK=0; fi
  [ "$OK" -ge 2 ] && break
  sleep 5
done
[ "$OK" -ge 2 ] || { echo "!!! $URL nu răspunde 200." >&2; read -r -p "Enter pentru a închide..." _; exit 1; }
mkdir -p docs; echo "$URL" > docs/link-public.txt   # linkul verificat intră în documentare
echo ">>> Aștept aplicația locală din terminalul 2 (prima rulare: câteva minute)..."
for _ in $(seq 1 180); do
  L="$(cat .runtime/url 2>/dev/null | tr -d '\r\n')"
  if [ -n "$L" ] && [ "$(code "$L/")" = "200" ]; then break; fi
  sleep 5
done
./scripts/screenshots.sh "$URL"
echo; echo ">>> Gata: docs/screenshots/, docs/PREZENTARE.html, docs/documentare/. Poți opri terminalul 2 cu Ctrl+C."
read -r -p "Apasă Enter pentru a închide..." _
T3
chmod +x .runtime/term-deploy.sh .runtime/term-app.sh .runtime/term-shots.sh

open_term() {   # $1 = titlu, $2 = script din directorul proiectului
  case "$(uname -s)" in
    MINGW*|MSYS*|CYGWIN*)
      cmd.exe /c start "$1" "$(cygpath -w "$(command -v bash)")" --login -i "$(cygpath -m "$DIR/$2")" >/dev/null 2>&1 ;;
    Darwin)
      osascript -e "tell application \"Terminal\" to do script \"bash '$DIR/$2'\"" -e 'tell application "Terminal" to activate' >/dev/null 2>&1 ;;
    *)
      if   command -v gnome-terminal      >/dev/null 2>&1; then gnome-terminal --title="$1" -- bash "$DIR/$2" >/dev/null 2>&1
      elif command -v konsole             >/dev/null 2>&1; then konsole -e bash "$DIR/$2" >/dev/null 2>&1 &
      elif command -v xfce4-terminal      >/dev/null 2>&1; then xfce4-terminal --title="$1" -e "bash '$DIR/$2'" >/dev/null 2>&1 &
      elif command -v x-terminal-emulator >/dev/null 2>&1; then x-terminal-emulator -e bash "$DIR/$2" >/dev/null 2>&1 &
      elif command -v xterm               >/dev/null 2>&1; then xterm -T "$1" -e bash "$DIR/$2" >/dev/null 2>&1 &
      else return 1; fi ;;
  esac
}

if open_term "MobilaViva 1 - git + deploy" .runtime/term-deploy.sh && sleep 2 \
   && open_term "MobilaViva 2 - aplicatie locala" .runtime/term-app.sh && sleep 2 \
   && open_term "MobilaViva 3 - capturi + PageSpeed" .runtime/term-shots.sh; then
  echo ">>> Am deschis 3 terminale: (1) git + deploy, (2) aplicația locală, (3) capturi + PageSpeed. Tastează parola de admin în terminalul 3."
else
  echo "!!! Nu pot deschide terminale noi automat. Deschide manual 3 terminale în $DIR și rulează, pe rând:" >&2
  echo "    bash .runtime/term-deploy.sh   |   bash .runtime/term-app.sh   |   bash .runtime/term-shots.sh" >&2
  exit 1
fi
