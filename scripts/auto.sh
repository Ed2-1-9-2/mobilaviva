#!/usr/bin/env bash
# Pornește tot dintr-o singură comandă (aplicație + link public Cloudflare + print screen-uri + PageSpeed 18/19):
#   ./scripts/auto.sh           (sau ./scripts/auto.sh single)  UN terminal, totul automat
#   ./scripts/auto.sh multi     terminale separate: 1) aplicație + tunel (log-uri)   2) print screen-uri (așteaptă site-ul)
#   ./scripts/auto.sh stop      oprește tunelul și aplicația
# Rezultat: docs/screenshots/*.png + docs/PREZENTARE.html. Tunelul rămâne pornit până la Ctrl+C (terminalul 1).
cd "$(dirname "$0")/.." || exit 1
DIR="$(pwd)"
MODE="${1:-single}"
mkdir -p .runtime

case "$MODE" in
  stop) exec ./scripts/public.sh stop ;;
  single|auto) rm -f .runtime/public_url; exec ./scripts/public.sh screenshots ;;
  multi) ;;
  *) echo "Folosire: ./scripts/auto.sh [single|multi|stop]" >&2; exit 1 ;;
esac

# ---------------- varianta cu terminale multiple
cat > .runtime/term-app.sh <<'T1'
#!/usr/bin/env bash
cd "$(dirname "$0")/.." || exit 1
echo ">>> TERMINAL 1/2: aplicație + tunel public (aici vezi log-urile; Ctrl+C oprește totul)"
./scripts/public.sh
echo; read -r -p "Apasă Enter pentru a închide..." _
T1
cat > .runtime/term-shots.sh <<'T2'
#!/usr/bin/env bash
cd "$(dirname "$0")/.." || exit 1
echo ">>> TERMINAL 2/2: print screen-uri. Aștept site-ul public pornit în terminalul 1 (prima pornire: câteva minute)..."
PUB=""
for _ in $(seq 1 720); do
  PUB=""; [ -f .runtime/public_url ] && PUB="$(tr -d '\r\n' < .runtime/public_url)"
  [ -n "$PUB" ] && [ "$(curl -s -o /dev/null --max-time 8 -w '%{http_code}' "$PUB/")" = "200" ] && break
  PUB=""; sleep 5
done
if [ -z "$PUB" ]; then echo "!!! Site-ul public nu a pornit (verifică terminalul 1)." >&2; read -r -p "Enter pentru a închide..." _; exit 1; fi
echo ">>> Site public: $PUB"
./scripts/screenshots.sh "$PUB"
echo; echo ">>> Gata: docs/screenshots/ și docs/PREZENTARE.html"
read -r -p "Apasă Enter pentru a închide..." _
T2
chmod +x .runtime/term-app.sh .runtime/term-shots.sh

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

rm -f .runtime/public_url
if open_term "MobilaViva 1 - aplicatie + tunel" .runtime/term-app.sh && sleep 2 && open_term "MobilaViva 2 - print screen" .runtime/term-shots.sh; then
  echo ">>> Am deschis 2 terminale noi: (1) aplicație + tunel public, (2) print screen-uri automate."
  echo ">>> Rezultatul apare în docs/screenshots/ și docs/PREZENTARE.html. Oprire: ./scripts/auto.sh stop"
else
  echo "!!! Nu pot deschide terminale noi automat. Deschide manual 2 terminale în $DIR și rulează:" >&2
  echo "    terminal 1:  bash .runtime/term-app.sh" >&2
  echo "    terminal 2:  bash .runtime/term-shots.sh" >&2
  exit 1
fi
