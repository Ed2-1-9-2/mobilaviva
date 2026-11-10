#!/usr/bin/env bash
# Generează documentarea: docs/documentare/documentatie.docx + prezentare.pptx (din docs/screenshots/, fără date personale).
#   ./scripts/docs.sh
# Textele se editează în docs/continut-documentare.json (creat la prima rulare; completează zonele [COMPLETEAZĂ ...]).
# Link public final: SITE_LINK=https://... ./scripts/docs.sh   sau scrie-l în docs/link-public.txt (fără el: se atașează sursa în zip).
# PageSpeed: capturile 18/19 se fac cu ./scripts/public.sh screenshots (sau le adaugi manual în docs/screenshots/).
# Limita de pagini a documentației: implicit 21; schimb-o cu  DOC_PAGINI=20 ./scripts/docs.sh   (0 = fără limită).
cd "$(dirname "$0")/.." || exit 1
PY="$(command -v python3 || command -v python)" || { echo "Lipsește Python 3: documentarea .docx/.pptx nu se generează." >&2; exit 1; }
VENV=.runtime/venv-docs
[ -x "$VENV/bin/python" ] || [ -x "$VENV/Scripts/python.exe" ] || "$PY" -m venv "$VENV" || exit 1
VPY="$VENV/bin/python"; [ -x "$VPY" ] || VPY="$VENV/Scripts/python.exe"
"$VPY" -c "import docx, pptx, PIL" 2>/dev/null || "$VPY" -m pip install -q python-docx python-pptx pillow || exit 1
LINK="${SITE_LINK:-}"
[ -z "$LINK" ] && [ -f docs/link-public.txt ] && LINK="$(tr -d '\r\n' < docs/link-public.txt)"
"$VPY" scripts/tools/genereaza_documentare.py --shots docs/screenshots --out docs/documentare --continut docs/continut-documentare.json --pagini "${DOC_PAGINI:-21}" ${LINK:+--link "$LINK"}
