#!/usr/bin/env bash
# Măsoară viteza site-ului (mobile + desktop) cu Lighthouse și salvează rapoartele în docs/speed-reports/.
# Folosire:  ./scripts/measure_speed.sh [URL]        (implicit http://localhost:8000/)
# Necesită: Node 18+ și Chrome/Chromium instalat (sau CHROME_PATH setat).
# Pentru print screen-ul cerut în proiect, rulează și https://pagespeed.web.dev pe URL-ul public al site-ului.
set -e
cd "$(dirname "$0")/.."
URL="${1:-$(cat .runtime/url 2>/dev/null || echo http://localhost:8000)}"
if [ -z "$CHROME_PATH" ] && ! command -v google-chrome >/dev/null && ! command -v chromium >/dev/null && ! command -v chromium-browser >/dev/null; then
  echo "Nu am găsit Chrome/Chromium. Instalează-l sau setează CHROME_PATH." >&2; exit 1
fi
mkdir -p docs/speed-reports
for MODE in mobile desktop; do
  EXTRA=""; [ "$MODE" = "desktop" ] && EXTRA="--preset=desktop"
  echo ">>> Lighthouse ($MODE) pe $URL"
  npx --yes lighthouse "$URL" $EXTRA --quiet --output=html --output=json \
    --output-path="docs/speed-reports/$MODE" --chrome-flags="--headless=new --no-sandbox" \
    --only-categories=performance,accessibility,best-practices,seo
  node scripts/speed-summary.js "$MODE"
done
echo ">>> Rapoarte HTML: docs/speed-reports/mobile.report.html și docs/speed-reports/desktop.report.html (le poți folosi la print screen)"
