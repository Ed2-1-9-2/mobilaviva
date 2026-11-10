#!/usr/bin/env bash
# Instalează comanda `mobilaviva` (merge din ORICE folder/terminal):  mobilaviva | mobilaviva front | mobilaviva tot
# Rulat de generator la final; îl poți rula și singur: ./scripts/comanda.sh
cd "$(dirname "$0")/.." || exit 1
P="$(pwd)"
mkdir -p "$HOME/bin"
printf '#!/usr/bin/env bash\ncd "%s" || { echo "Folderul proiectului nu mai există: %s" >&2; exit 1; }\nexec ./scripts/lansare.sh "$@"\n' "$P" "$P" > "$HOME/bin/mobilaviva"
chmod +x "$HOME/bin/mobilaviva"
for RC in "$HOME/.bashrc" "$HOME/.bash_profile"; do
  [ -f "$RC" ] || [ "$RC" = "$HOME/.bashrc" ] || continue
  grep -q 'HOME/bin' "$RC" 2>/dev/null || echo 'export PATH="$HOME/bin:$PATH"' >> "$RC"
done
case ":$PATH:" in *":$HOME/bin:"*) echo ">>> Comanda 'mobilaviva' e gata (proiect: $P)";; *) echo ">>> Comanda 'mobilaviva' instalată. Redeschide terminalul sau rulează: source ~/.bashrc";; esac
