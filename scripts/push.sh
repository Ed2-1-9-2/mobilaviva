#!/usr/bin/env bash
# Trimite proiectul pe GitHub (repo-ul din care publică Render). Repo-ul poate fi privat.
#   ./scripts/push.sh                          REPO_URL=https://github.com/... ./scripts/push.sh  pentru alt repo
# Dacă folderul curent e repo git, face commit + push direct. Altfel clonează REPO_URL în .runtime/repo, copiază proiectul peste el
# (fără target, data, node_modules, .env, .runtime), face commit + push. Prima dată GitHub poate cere autentificare în browser.
cd "$(dirname "$0")/.." || exit 1
./scripts/translate.sh
./scripts/snapshot.sh || echo "!!! snapshot catalog nereușit: site-ul merge, dar fără catalog static cât doarme backend-ul." >&2
REPO_URL="${REPO_URL:-https://github.com/Ed-2-1-9-2/mobilaviva.git}"
if git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  R="$(pwd)"
else
  R=.runtime/repo
  if [ -d "$R/.git" ]; then
    git -C "$R" pull --ff-only || { echo "!!! git pull a eșuat în $R. Șterge folderul $R și rulează din nou." >&2; exit 1; }
  else
    echo ">>> Clonez $REPO_URL (repo privat: la prima rulare poate cere autentificare GitHub)..."
    rm -rf "$R"
    git clone "$REPO_URL" "$R" || { echo "!!! git clone a eșuat (link greșit sau fără acces)." >&2; exit 1; }
  fi
  # siguranță: dacă repo-ul are conținut dar nu are Dockerfile în rădăcină, structura e alta și nu copiez nimic
  if [ -n "$(git -C "$R" ls-files | head -1)" ] && [ ! -f "$R/Dockerfile" ]; then
    echo "!!! Repo-ul nu are Dockerfile în rădăcină (structură diferită). Nu am copiat nimic." >&2
    echo "!!! Vezi structura:  git -C .runtime/repo ls-files | head -30" >&2
    exit 1
  fi
  tar --exclude=./target --exclude=./data --exclude=./frontend/node_modules --exclude=./scripts/tools/node_modules \
      --exclude=./.git --exclude=./.env --exclude=./.env.local --exclude=./.runtime --exclude=./frontend/.vercel --exclude=./frontend/dist \
      --exclude=./scripts/tools/cloudflared.exe --exclude=./scripts/tools/cloudflared --exclude=./docs/speed-reports \
      --exclude=./src/main/resources/static -cf - . | tar -xf - -C "$R"
fi
# fișiere vechi din alte variante (Vue din CDN + vue3-sfc-loader) suprascriu build-ul Vite pe Vercel: le scot din repo
for f in "$R"/index.html "$R"/app.js "$R"/main.js "$R"/frontend/public/index.html; do
  [ -f "$f" ] && grep -q 'vue3-sfc-loader\|vue.global' "$f" && { git -C "$R" rm -q -f --ignore-unmatch "${f#$R/}" 2>/dev/null; rm -f "$f"; echo ">>> Șters fișier vechi: ${f#$R/}"; }
done
# workflow-ul "keepalive" (obsolet, pingua vechiul URL Render și trimitea e-mailuri de eroare) se scoate din repo la fiecare push
if [ -f "$R/.github/workflows/keepalive.yml" ] || git -C "$R" ls-files --error-unmatch .github/workflows/keepalive.yml >/dev/null 2>&1; then
  git -C "$R" rm -q -f --ignore-unmatch .github/workflows/keepalive.yml 2>/dev/null; rm -f "$R/.github/workflows/keepalive.yml"
  echo ">>> Șters din repo: .github/workflows/keepalive.yml (nu mai trimite e-mailuri de eroare)"
fi
# Vercel (plan gratuit) publică doar commit-uri ale contului proprietar: autorul e fixat aici (se poate suprascrie cu GIT_NAME / GIT_EMAIL)
GIT_NAME="${GIT_NAME:-Ed-2-1-9-2}"
GIT_EMAIL="${GIT_EMAIL:-you@example.com}"
git -C "$R" config user.name "$GIT_NAME"
git -C "$R" config user.email "$GIT_EMAIL"
echo ">>> Autor commit: $GIT_NAME <$GIT_EMAIL>"
git -C "$R" add -A
git -C "$R" commit -m "actualizare site" >/dev/null 2>&1 || echo "    (nimic nou de comis)"
# dacă ultimul commit are alt autor (ex. identitate veche/greșită), îl corectez; apoi push forțat doar pe acest caz
FIX=0
if [ "$(git -C "$R" log -1 --format=%ae 2>/dev/null)" != "$GIT_EMAIL" ]; then
  git -C "$R" commit --amend --reset-author --no-edit >/dev/null 2>&1 && FIX=1
fi
# după push: verifică automat pe Vercel că API-ul și login-ul merg (NO_CHECK=1 = sari peste)
after_push() {
  echo ">>> Push făcut în $REPO_URL$1"
  [ "${NO_CHECK:-0}" = 1 ] && exit 0
  CHECK_WAIT="${CHECK_WAIT:-240}" ./scripts/check_vercel.sh && exit 0
  # adresa principală încă arată altceva (alt proiect Vercel / versiune veche): public direct pe ea cu Vercel CLI
  [ "${NO_VERCEL_CLI:-0}" = 1 ] && exit 1
  echo ">>> Public direct pe proiectul care deține adresa, cu Vercel CLI..."
  exec ./scripts/vercel_deploy.sh
}
if [ "$FIX" = 1 ]; then
  git -C "$R" push --force-with-lease || { echo "!!! git push a eșuat (autentificare/remote). Rezolvă și rulează din nou." >&2; exit 1; }
  after_push " (autor corectat)"
fi
git -C "$R" push || { echo "!!! git push a eșuat (autentificare/remote). Rezolvă și rulează din nou." >&2; exit 1; }
after_push ""
