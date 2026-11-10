#!/usr/bin/env bash
# Publică frontend-ul DIRECT pe https://mobilaviva.vercel.app cu Vercel CLI, indiferent în ce cont/echipă e proiectul care deține adresa.
#   ./scripts/vercel_deploy.sh [https://adresa.vercel.app]     (implicit LIVE_URL sau https://mobilaviva.vercel.app)
#   VERCEL_SCOPE=echipa  = forțează contul/echipa;  VERCEL_TOKEN=...  = fără login în browser
# Pași: login -> găsește contul/proiectul care are adresa -> deploy de producție (folosește vercel.json) -> alias pe adresă
#       -> leagă proiectul la repo-ul GitHub (push-urile viitoare publică singure) -> ./scripts/check_vercel.sh
cd "$(dirname "$0")/.." || exit 1
[ -f scripts/snapshot.sh ] && { ./scripts/snapshot.sh || echo "!!! snapshot catalog nereușit (continuu fără el)." >&2; }
URL="${1:-${LIVE_URL:-https://mobilaviva.vercel.app}}"; URL="${URL%/}"; HOST="${URL#https://}"
REPO_URL="${REPO_URL:-https://github.com/Ed-2-1-9-2/mobilaviva.git}"
command -v node >/dev/null 2>&1 || { echo "!!! Lipsește Node 18+ (winget install OpenJS.NodeJS.LTS)" >&2; exit 1; }
VC=(npx --yes vercel@latest)
[ -n "${VERCEL_TOKEN:-}" ] && VC+=(--token "$VERCEL_TOKEN")
export VERCEL_TELEMETRY_DISABLED=1
mkdir -p .runtime
./scripts/translate.sh

"${VC[@]}" whoami >/dev/null 2>&1 || { echo ">>> Login Vercel (se deschide browserul)..."; "${VC[@]}" login || exit 1; }
echo ">>> Cont Vercel: $("${VC[@]}" whoami 2>/dev/null | tail -1)"

# conturile în care caut: VERCEL_SCOPE sau contul personal + toate echipele
SCOPES=()
if [ -n "${VERCEL_SCOPE:-}" ]; then SCOPES=("$VERCEL_SCOPE")
else
  SCOPES=("")
  while IFS= read -r t; do [ -n "$t" ] && SCOPES+=("$t"); done < <("${VC[@]}" teams ls --json 2>/dev/null | node -e '
    let d="";process.stdin.on("data",c=>d+=c).on("end",()=>{try{const j=JSON.parse(d.slice(d.indexOf("{")));(j.teams||[]).forEach(t=>console.log(t.slug||t.id))}catch{}})')
fi

# caut proiectul care deține adresa (după alias); dacă nu-l găsesc, proiectul cu numele adresei
FOUND_SCOPE=""; FOUND_PROJECT=""
for S in "${SCOPES[@]}"; do
  SO=(); [ -n "$S" ] && SO=(--scope "$S")
  PID="$("${VC[@]}" alias ls --json --limit 100 "${SO[@]}" 2>/dev/null | node -e '
    const h=process.argv[1];let d="";process.stdin.on("data",c=>d+=c).on("end",()=>{try{const j=JSON.parse(d.slice(d.indexOf("{")));
    const a=(j.aliases||[]).find(x=>x.alias===h);if(a)console.log(a.projectId||(a.project&&a.project.id)||"?")}catch{}})' "$HOST")"
  if [ -n "$PID" ]; then
    NAME="$("${VC[@]}" project ls --json "${SO[@]}" 2>/dev/null | node -e '
      const id=process.argv[1];let d="";process.stdin.on("data",c=>d+=c).on("end",()=>{try{const j=JSON.parse(d.slice(d.indexOf("{")));
      const p=(j.projects||[]).find(x=>x.id===id);if(p)console.log(p.name)}catch{}})' "$PID")"
    FOUND_SCOPE="$S"; FOUND_PROJECT="${NAME:-${HOST%.vercel.app}}"; break
  fi
done
if [ -z "$FOUND_PROJECT" ]; then
  FOUND_PROJECT="${VERCEL_PROJECT:-${HOST%.vercel.app}}"
  echo ">>> Nu am găsit aliasul $HOST în conturile tale; folosesc proiectul „$FOUND_PROJECT” din contul implicit."
fi
SO=(); [ -n "$FOUND_SCOPE" ] && SO=(--scope "$FOUND_SCOPE")
echo ">>> Proiect: $FOUND_PROJECT   cont: ${FOUND_SCOPE:-personal}"

# fișierele trimise la Vercel: doar ce trebuie pentru build-ul frontend
cat > .vercelignore <<'VIGN'
target
data
.runtime
.env
.env.*
frontend/node_modules
frontend/dist
scripts/tools/node_modules
src/main/resources/static
docs/speed-reports
VIGN

rm -rf .vercel
"${VC[@]}" link --yes --project "$FOUND_PROJECT" "${SO[@]}" || { echo "!!! Nu pot lega folderul la proiectul $FOUND_PROJECT." >&2; exit 1; }
echo ">>> Deploy de producție (build pe Vercel: 1-3 minute)..."
"${VC[@]}" deploy --prod --yes "${SO[@]}" | tee .runtime/vercel-deploy.log
DEP="$(grep -o 'https://[a-zA-Z0-9.-]*\.vercel\.app' .runtime/vercel-deploy.log | grep -vx "https://$HOST" | head -1)"
[ -n "$DEP" ] || { echo "!!! Deploy-ul nu a întors un URL (vezi mai sus)." >&2; exit 1; }
# promote = repune adresa pe deploy-ul nou ȘI anulează un „Instant Rollback” vechi (care oprește actualizarea automată a adresei)
"${VC[@]}" promote "$DEP" --yes "${SO[@]}" >/dev/null 2>&1 || true
"${VC[@]}" alias set "$DEP" "$HOST" "${SO[@]}" || echo "!!! Nu am putut pune aliasul $HOST pe $DEP (vezi mesajul de mai sus)." >&2
# push-urile viitoare pe GitHub publică automat pe acest proiect
"${VC[@]}" git connect "$REPO_URL" --yes "${SO[@]}" >/dev/null 2>&1 && echo ">>> Proiectul $FOUND_PROJECT e legat la $REPO_URL" || true
rm -rf .vercel
CHECK_WAIT="${CHECK_WAIT:-120}" exec ./scripts/check_vercel.sh "$URL"
