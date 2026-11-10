#!/usr/bin/env bash
# Salvează catalogul într-un fișier static (frontend/public/snapshot.json), servit de Vercel.
# Cât timp backend-ul (Render + Neon) doarme după 8-24 h fără trafic, site-ul arată catalogul din acest fișier și îl trezește în fundal.
#   ./scripts/snapshot.sh [https://backend.onrender.com]     (implicit: adresa din vercel.json)
# Rulează automat din push.sh și vercel_deploy.sh. Trezește singur backend-ul (așteaptă până la 5 min).
cd "$(dirname "$0")/.." || exit 1
command -v node >/dev/null 2>&1 || { echo "!!! snapshot: lipsește node." >&2; exit 1; }
BACKEND="${1:-${BACKEND_URL:-$(sed -n 's#.*"destination": *"\(https://[^"/]*\)/api/.*#\1#p' vercel.json 2>/dev/null | head -1)}}"
BACKEND="${BACKEND:-https://mobilaviva.onrender.com}"
mkdir -p frontend/public
echo ">>> snapshot catalog din $BACKEND"
BASE="$BACKEND" node --input-type=module - <<'JS' || exit 1
import fs from 'fs'
const base = process.env.BASE.replace(/\/$/, '')
const sleep = ms => new Promise(r => setTimeout(r, ms))
async function get(p) {
  const t0 = Date.now()
  for (;;) {
    try {
      const r = await fetch(base + p, { signal: AbortSignal.timeout(40000) })
      if (r.ok && (r.headers.get('content-type') || '').includes('json')) return await r.json()
    } catch { /* doarme */ }
    if (Date.now() - t0 > 300000) throw new Error('Backend-ul nu răspunde: ' + base + p)
    console.log('  aștept backend-ul (se trezește)...')
    await sleep(5000)
  }
}
const sess = await get('/api/session')
const facets = await get('/api/facets')
let items = [], page = 1, pages = 1
do { const r = await get('/api/products?page=' + page); items = items.concat(r.items || []); pages = r.pages || 1; page++ } while (page <= pages)
if (!items.length) throw new Error('catalogul e gol')
fs.writeFileSync('frontend/public/snapshot.json', JSON.stringify({ at: new Date().toISOString(), session: { ...sess, user: null, cartCount: 0, compareCount: 0 }, facets, items }))
console.log('>>> frontend/public/snapshot.json: ' + items.length + ' produse')
JS
