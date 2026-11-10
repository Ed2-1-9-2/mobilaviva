#!/usr/bin/env bash
# Captură automată a paginii Stripe -> Payments (sandbox): docs/screenshots/14d-stripe-dashboard.png (cerința I.10)
# Adresele de email din coloana Customer se estompează (blur) înainte de captură, ca să nu apară datele altor persoane.
#   ./scripts/stripe_dashboard.sh            prima dată te loghezi tu în fereastra deschisă (profil salvat în .runtime/stripe-profile)
#   BLUR=0 ./scripts/stripe_dashboard.sh     fără estompare
cd "$(dirname "$0")/.."
mkdir -p .runtime docs/screenshots
KEY="${STRIPE_SECRET_KEY:-}"
[ -z "$KEY" ] && [ -f .env ] && KEY="$(grep -m1 '^STRIPE_SECRET_KEY=' .env | cut -d= -f2- | tr -d '\r"')"
case "$KEY" in sk_test_*) ;; "") echo "Nu găsesc STRIPE_SECRET_KEY (în .env sau exportată)." >&2; exit 1 ;; *) echo "Cheia nu este de test (sk_test_...). Nu rulez pe un cont live." >&2; exit 1 ;; esac
# id-ul contului (acct_...) se citește de la Stripe din cheie, ca linkul să fie mereu cel corect
ACCT="$(curl -s https://api.stripe.com/v1/account -u "$KEY:" | grep -m1 '"id": "acct_' | sed -E 's/.*"(acct_[^"]+)".*/\1/')"
[ -n "$ACCT" ] || { echo "Stripe nu a recunoscut cheia (verifică STRIPE_SECRET_KEY)." >&2; exit 1; }
echo ">>> Cont Stripe: $ACCT"
if ! node -e "require('playwright')" >/dev/null 2>&1; then
  echo ">>> Instalez playwright (o singură dată)..."
  npm i --no-audit --no-fund playwright >/dev/null 2>&1 || { echo "!!! npm i playwright a eșuat" >&2; exit 1; }
fi
cat > .runtime/stripe-shot.js <<'EOF'
const { chromium } = require('playwright')
const URL = `https://dashboard.stripe.com/${process.env.ACCT}/test/payments`
const BLUR = process.env.BLUR !== '0'
// estompează orice adresă de email din pagină (text împărțit în fragmente; doar emailul primește blur)
const blurEmails = () => {
  const re = /[^\s@]+@[^\s@]+\.[^\s@]+/g
  const w = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT)
  const nodes = []
  for (let n = w.nextNode(); n; n = w.nextNode()) {
    const p = n.parentElement
    if (p && !/^(SCRIPT|STYLE|NOSCRIPT)$/.test(p.tagName) && !p.dataset.blurred && n.nodeValue.includes('@')) nodes.push(n)
  }
  for (const n of nodes) {
    const t = n.nodeValue, frag = document.createDocumentFragment()
    let last = 0, m, any = false
    re.lastIndex = 0
    while ((m = re.exec(t))) {
      any = true
      if (m.index > last) frag.append(t.slice(last, m.index))
      const s = document.createElement('span')
      s.textContent = m[0]; s.dataset.blurred = '1'
      s.style.cssText = 'filter:blur(6px);user-select:none'
      frag.append(s)
      last = m.index + m[0].length
    }
    if (any) { if (last < t.length) frag.append(t.slice(last)); n.replaceWith(frag) }
  }
}
;(async () => {
  let ctx
  for (const o of [{ channel: 'chrome' }, { channel: 'msedge' }, {}]) {
    try {
      ctx = await chromium.launchPersistentContext('.runtime/stripe-profile', { headless: false, viewport: { width: 1600, height: 900 }, ...o })
      break
    } catch {}
  }
  if (!ctx) { console.error('!!! Nu pot porni Chrome/Edge'); process.exit(1) }
  const page = ctx.pages()[0] || await ctx.newPage()
  await page.goto(URL)
  console.log('>>> Dacă apare login Stripe, loghează-te cu contul sandbox-ului (aștept max 10 min)...')
  await page.getByText(/Succeeded|Reușit/i).first().waitFor({ timeout: 600000 })
  await page.waitForTimeout(3000)
  if (BLUR) {
    await page.evaluate(blurEmails)
    await page.waitForTimeout(1500)
    await page.evaluate(blurEmails)          // a doua trecere: prinde rândurile redesenate de Stripe
  }
  await page.screenshot({ path: 'docs/screenshots/14d-stripe-dashboard.png' })
  console.log('✓ docs/screenshots/14d-stripe-dashboard.png' + (BLUR ? '  (emailurile estompate)' : ''))
  await ctx.close()
})()
EOF
ACCT="$ACCT" BLUR="${BLUR:-1}" node .runtime/stripe-shot.js
