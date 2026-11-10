// Client HTTP minimal pentru API-ul REST (cookie de sesiune, același origin).
// Funcționează și după 8-24 h fără trafic: backend-ul gratuit (Render + Neon) doarme, iar prima vizită îl trezește singură, fără keep-alive.
//  - paginile publice (catalog, categorii, produs, căutare) se afișează INSTANT din /snapshot.json (fișier static de pe Vercel) cât timp backend-ul pornește;
//  - acțiunile personale (login, coș, comandă) așteaptă trezirea (până la 4 min), cu mesaj vizibil, apoi se execută normal.
const boot = window.__BOOT__ || {}
export function bootTake(path) {
  const d = boot[path]
  if (d !== undefined) delete boot[path]
  return d
}

const RETRY = [502, 503, 504]
const WAKE_MAX = 240000   // cât aștept trezirea backend-ului
const FAST_MS = 3000      // peste atât, o cerere publică trece pe snapshot
const emit = (n, d) => window.dispatchEvent(new CustomEvent(n, { detail: d }))
const isJson = r => (r.headers.get('content-type') || '').includes('application/json')
const usable = r => !!r && !RETRY.includes(r.status) && !(r.headers.get('content-type') || '').includes('text/html')
const err = (m, status = 0) => Object.assign(new Error(m), { status, errors: [] })

// ---- trezirea backend-ului (o singură buclă, partajată de toate cererile)
let waking = null
export function wake() {
  if (waking) return waking
  emit('server-waking', true)
  waking = (async () => {
    const t0 = Date.now()
    while (Date.now() - t0 < WAKE_MAX) {
      try {
        const r = await fetch('/api/session', { credentials: 'same-origin', cache: 'no-store', signal: AbortSignal.timeout(35000) })
        if (r.ok && isJson(r)) return true
      } catch { /* încă doarme */ }
      await new Promise(r => setTimeout(r, 2000))
    }
    return false
  })().finally(() => { waking = null; emit('server-waking', false) })
  waking.then(ok => { if (ok) emit('server-awake') })
  return waking
}

// ---- snapshot static (generat de scripts/snapshot.sh)
let snap, snapP
function snapshot() {
  if (!snapP) snapP = fetch('/snapshot.json').then(r => (r.ok && (r.headers.get('content-type') || '').includes('json') ? r.json() : null)).catch(() => null)
  return snapP.then(s => (snap = s))
}
function listFrom(items, q) {
  const num = k => { const v = parseFloat(q.get(k)); return isNaN(v) ? null : v }
  const cat = q.get('category'), br = q.get('brand'), lo = num('min_price'), hi = num('max_price'), s = (q.get('q') || '').toLowerCase().trim()
  let l = items.filter(p => (!cat || p.category === cat) && (!br || p.brand === br) && (lo == null || p.price >= lo) && (hi == null || p.price <= hi)
    && (q.get('in_stock') !== '1' || p.stock > 0) && (q.get('promo') !== '1' || p.oldPrice > p.price)
    && (!s || s.split(/\s+/).every(t => `${p.name} ${p.brand} ${p.category}`.toLowerCase().includes(t))))
  const so = q.get('sort')
  const by = so === 'price_asc' ? (a, b) => a.price - b.price : so === 'price_desc' ? (a, b) => b.price - a.price
    : so === 'delivery' ? (a, b) => a.deliveryDays - b.deliveryDays : (a, b) => b.rating - a.rating
  l = [...l].sort((a, b) => by(a, b) || a.id - b.id)
  const page = Math.max(1, parseInt(q.get('page')) || 1)
  return { items: l.slice((page - 1) * 12, page * 12), total: l.length, pages: Math.max(1, Math.ceil(l.length / 12)), page, relaxed: false }
}
async function fromSnapshot(path) {
  const s = await snapshot()
  if (!s) return undefined
  if (path === '/session') return s.session
  if (path === '/facets') return s.facets
  const m = path.match(/^\/products\/(\d+)$/)
  if (m) return s.items.find(p => String(p.id) === m[1])
  if (path === '/products' || path.startsWith('/products?')) return listFrom(s.items, new URLSearchParams(path.split('?')[1] || ''))
  if (path.startsWith('/suggest')) {
    const q = (new URLSearchParams(path.split('?')[1] || '').get('q') || '').toLowerCase().trim()
    return q ? s.items.filter(p => q.split(/\s+/).every(t => p.name.toLowerCase().includes(t))).slice(0, 6).map(p => ({ id: p.id, name: p.name })) : []
  }
  return undefined
}

async function attempt(path, opts, fast) {
  try {
    const p = fetch('/api' + path, opts)
    p.catch(() => {})
    const r = fast ? await Promise.race([p, new Promise(res => setTimeout(() => res(undefined), FAST_MS))]) : await p
    return r === undefined ? null : r
  } catch { return null }
}

export async function api(path, { method = 'GET', body } = {}) {
  const isGet = method === 'GET' && body === undefined
  if (isGet) {
    const d = bootTake(path)
    if (d !== undefined) return d
  }
  const opts = {
    method,
    credentials: 'same-origin',
    headers: body !== undefined ? { 'Content-Type': 'application/json' } : {},
    body: body !== undefined ? JSON.stringify(body) : undefined
  }
  // backend-ul se trezește deja: paginile publice vin din snapshot, restul așteaptă
  if (waking) {
    if (isGet) { const a = await fromSnapshot(path); if (a !== undefined) return a }
    await waking
  }
  const safe = isGet || path === '/auth/login'   // idempotente: sigur de reîncercat; POST doar la 502/503 (n-a ajuns la server), ca să nu dublez comenzi
  let res = await attempt(path, opts, isGet)
  if (!usable(res)) {
    if (isGet) { const a = await fromSnapshot(path); if (a !== undefined) { wake(); return a } }
    if (safe || (res && [502, 503].includes(res.status))) {
      for (let i = 0; i < 2 && !usable(res); i++) { await wake(); res = await attempt(path, opts, false) }
    } else wake()
    if (!usable(res)) throw err('Serverul pornește încă. Reîncearcă în câteva secunde.', res ? res.status : 0)
  }
  let data = null
  try { data = await res.json() } catch { /* răspuns fără corp */ }
  if (res.status === 405) throw err('Serverul nu răspunde momentan. Reîncearcă în câteva secunde.', 405)
  if (!res.ok) {
    const errors = (data && data.errors) || []
    throw Object.assign(new Error(errors[0] || 'A apărut o eroare.'), { status: res.status, errors })
  }
  return data
}
