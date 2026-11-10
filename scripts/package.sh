#!/usr/bin/env bash
# Creează arhiva pentru upload:  ./scripts/package.sh nume_prenume   ->  ../site_nume_prenume.zip
# Conține codul, build-ul frontend, docs/PREZENTARE.md, docs/documentare/ (.docx + .pptx) și docs/screenshots/. Exclude target, data, node_modules, .git.
cd "$(dirname "$0")/.." || exit 1
./scripts/docs.sh || echo "(documentarea .docx/.pptx nu s-a generat; continui cu arhivarea)" >&2
./scripts/verifica.sh offline 2>/dev/null | grep -E '✘|Rezultat' >&2 || true   # rezumat: ce cerințe lipsesc din arhivă
NAME="site_${1:-nume_prenume}.zip"
OUT="$(cd .. && pwd)/$NAME"
STAGE="$(mktemp -d)/site"; mkdir -p "$STAGE"
tar --exclude=./target --exclude=./data --exclude=./frontend/node_modules --exclude=./scripts/tools/node_modules --exclude=./.git --exclude=./.env --exclude=./.runtime --exclude=./scripts/tools/cloudflared.exe --exclude=./scripts/tools/cloudflared --exclude=./docs/speed-reports -cf - . | tar -xf - -C "$STAGE"
if [ "${INCLUDE_STRIPE_TEST:-0}" = "1" ] && grep -q '^STRIPE_SECRET_KEY=sk_test_' .env 2>/dev/null; then
  grep -E '^(STRIPE_SECRET_KEY|STRIPE_PUBLISHABLE_KEY)=' .env > "$STAGE/.env"     # doar cheile de TEST (sk_test_), fără parola de admin
  echo ">>> Atenție: arhiva conține cheile Stripe de TEST, ca plata să meargă și din zip. Nu o urca pe GitHub." >&2
fi
rm -f "$OUT"
OUTW="$OUT"; STAGEW="$STAGE"
if command -v cygpath >/dev/null 2>&1; then OUTW="$(cygpath -m "$OUT")"; STAGEW="$(cygpath -m "$STAGE")"; fi
DONE=0
if command -v zip >/dev/null 2>&1 && (cd "$STAGE" && zip -qr "$OUT" .); then DONE=1; fi
if [ "$DONE" = 0 ]; then
  for PY in python3 python py; do   # pe Windows „python3” poate fi doar scurtătura Microsoft Store (nu e Python real): o sar
    if command -v "$PY" >/dev/null 2>&1 && "$PY" -c 'import zipfile' >/dev/null 2>&1; then
      "$PY" - "$STAGEW" "$OUTW" <<'PYX' && DONE=1
import os, sys, zipfile
src, out = sys.argv[1], sys.argv[2]
with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as z:
    for root, _, files in os.walk(src):
        for f in files:
            full = os.path.join(root, f)
            z.write(full, os.path.relpath(full, src).replace(os.sep, "/"))
PYX
      [ "$DONE" = 1 ] && break
    fi
  done
fi
if [ "$DONE" = 0 ] && command -v powershell.exe >/dev/null 2>&1; then
  powershell.exe -NoProfile -Command "Compress-Archive -Path '$STAGEW/*' -DestinationPath '$OUTW' -Force" >/dev/null 2>&1 && DONE=1
fi
if [ "$DONE" = 0 ] && command -v jar >/dev/null 2>&1; then
  jar --create --file "$OUTW" --no-manifest -C "$STAGEW" . && DONE=1
fi
rm -rf "$(dirname "$STAGE")"
if [ "$DONE" != 1 ] || [ ! -s "$OUT" ]; then
  echo "!!! Arhiva NU s-a creat (nu am găsit zip, Python real, PowerShell sau jar). Arhivează manual folderul $(pwd)." >&2; exit 1
fi
echo ">>> Arhivă creată: $OUT"
echo ">>> Verifică înainte de upload: docs/PREZENTARE.md completat și print screen-urile din docs/screenshots/."
