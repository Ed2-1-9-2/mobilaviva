// Automatizează print screen-urile cerute (Playwright) și generează PREZENTARE.html cu galeria.
// Folosire (din folderul proiectului, cu aplicația pornită):  ./scripts/screenshots.sh [URL]
// Doar galeria, fără browser:  node screenshots.mjs --gallery
import fs from 'node:fs'
import path from 'node:path'
import { marked } from 'marked'
import { execFileSync, spawn } from 'node:child_process'

const ARGS = process.argv.slice(2)
const GALLERY_ONLY = ARGS.includes('--gallery')
const BASE = (ARGS.find(a => a.startsWith('http')) || 'http://localhost:8000').replace(/\/$/, '')
const ROOT = path.resolve('../..')
const OUT = path.join(ROOT, 'docs', 'screenshots')
fs.mkdirSync(OUT, { recursive: true })
for (const f of fs.readdirSync(OUT)) if (/^EROARE-.*\.png$/.test(f)) fs.rmSync(path.join(OUT, f), { force: true })

const ADMIN = { email: 'admin@mobilaviva.com', password: process.env.ADMIN_PASSWORD || 'Admin123!' }
// Cont de test: implicit se creează unul nou; cu TEST_EMAIL și TEST_PASSWORD (variabile de mediu) se folosește un cont EXISTENT și nu se mai înregistrează altul
const USER = { name: 'Client Test', email: process.env.TEST_EMAIL || `client${Date.now()}@test.ro`, password: process.env.TEST_PASSWORD || 'Parola123!' }
const EXISTING = !!process.env.TEST_EMAIL
console.log('>>> Cont de test: ' + (EXISTING ? 'EXISTENT ' + USER.email : 'NOU ' + USER.email + '  (pentru unul existent pune TEST_EMAIL și TEST_PASSWORD în .env, apoi rulează din nou bash setup_mobilaviva_java_vue.sh)'))
const results = []

async function main() {
  console.log(`\n>>> Capturi pe ${BASE}  (apar mai jos pe măsură ce se fac; PageSpeed vine ultimul și durează câteva minute)`)
  const { chromium } = await import('playwright')
  const browser = await chromium.launch({ args: ['--disable-blink-features=AutomationControlled'] })
  // Format WEB (desktop) 1280x900, fără emulare de telefon
  const ctx = await browser.newContext({ viewport: { width: 1280, height: 900 }, deviceScaleFactor: 1, isMobile: false, hasTouch: false, locale: 'ro-RO' })
  let page = await ctx.newPage()
  // diagnostic autentificare: ce răspunde serverul la /api/auth/* și /api/session și ce cookie-uri trimite (valorile nu se afișează)
  page.on('response', async r => {
    const u = r.url()
    if (!/\/api\/(auth\/|session)/.test(u) || r.request().method() === 'OPTIONS') return
    try {
      const h = await r.allHeaders()
      const sc = (h['set-cookie'] || '').split('\n').map(c => c.replace(/=[^;]*/, '=***')).join(' || ')
      console.log(`  [net] ${r.request().method()} ${u.replace(BASE, '')} -> ${r.status()} set-cookie: ${sc || '(niciunul)'}`)
    } catch {}
  })
  page.setDefaultTimeout(150000)

  const settle = async () => { await page.waitForLoadState('networkidle').catch(() => {}); await page.waitForTimeout(500) }
  const go = async p => { await page.goto(BASE + p, { waitUntil: 'domcontentloaded' }); await settle() }
  const save = async (name, full = false) => page.screenshot({ path: path.join(OUT, name + '.png'), fullPage: full })
  // Rulează un pas; dacă eșuează, salvează o poză de diagnostic și continuă.
  async function step(name, fn, full = false, label = '') {
    try {
      await fn()
      await settle()
      if (name) { await save(name, full); results.push(['OK', name]); console.log(`  ✓ ${name}`) }
    } catch (e) {
      results.push(['EȘUAT', `${label || name || 'pas intermediar'}: ${e.message.split('\n')[0]}`])
      console.log(`  ✗ ${label || name || 'pas intermediar'}: ${e.message.split('\n')[0]}`)
      await page.screenshot({ path: path.join(OUT, `EROARE-${label || name || 'pas'}.png`) }).catch(() => {})
    }
  }
  const login = async u => {
    for (let i = 0; i < 2; i++) {
      await go('/login')
      await page.getByLabel('Email').fill(u.email)
      await page.getByLabel('Parolă').fill(u.password)
      await page.getByRole('button', { name: 'Intră în cont' }).click()
      if (await waitUser(i ? 180000 : 120000).catch(() => false)) { await settle(); return }
    }
    throw new Error('autentificare eșuată: ' + (await page.locator('.err').allInnerTexts().catch(() => [])).join(' | '))
  }
  const tabs = async name => {
    const t = page.locator('nav.tabs')
    await t.waitFor({ timeout: 120000 })
    await t.getByRole('link', { name, exact: true }).click({ timeout: 120000 })
  }
  // Vercel răspunde instant (fișiere statice + snapshot), dar login/coș/comandă cer backend-ul de pe Render+Neon, care poate dormi până la ~4 min:
  // înainte de pașii care îl folosesc aștept ca /api/session să răspundă JSON de la backend.
  const backendReady = async (max = 240000) => {
    const t0 = Date.now()
    while (Date.now() - t0 < max) {
      try { const r = await page.request.get(BASE + '/api/session', { timeout: 35000 }); if (r.ok() && (r.headers()['content-type'] || '').includes('application/json')) return true } catch {}
      await page.waitForTimeout(2000)
    }
    console.log('  ! backend-ul nu a răspuns în 4 min; continui oricum'); return false
  }
  // diagnostic IN BROWSER (cu cookie-urile paginii, ca aplicația): cine e logat, câte linii are coșul, ce cookie-uri există, ce scrie pe pagină
  const inPage = (url, method = 'GET', body) => page.evaluate(async ([u, m, b]) => {
    const r = await fetch(u, { method: m, credentials: 'same-origin', cache: 'no-store', headers: b ? { 'Content-Type': 'application/json' } : {}, body: b ? JSON.stringify(b) : undefined })
    let j = null; try { j = await r.json() } catch {}
    return { status: r.status, json: j }
  }, [url, method, body])
  const cartLines = async l => {
    try {
      const s = await inPage('/api/session'), c = await inPage('/api/cart')
      const n = ((c.json && c.json.lines) || []).length
      if (l) {
        const ck = (await ctx.cookies()).map(x => `${x.name}${x.secure ? '[S]' : ''}${x.httpOnly ? '[H]' : ''}@${x.domain}`).join(', ')
        const txt = (await page.locator('body').innerText().catch(() => '')).replace(/\s+/g, ' ').slice(0, 160)
        console.log(`  [diag ${l}] url=${page.url().replace(BASE, '')} user=${s.json && s.json.user ? s.json.user.email : 'NULL'} linii_coș=${n} cookies=[${ck}]`)
        console.log(`     pagina: ${txt}`)
      }
      return n
    } catch (e) { if (l) console.log(`  [diag ${l}] ${e.message.split('\n')[0]}`); return -1 }
  }
  const waitUser = async (max = 120000) => {   // așteaptă până sesiunea din pagină are utilizator (adresa nu e un semnal sigur: /login -> /autentificare)
    const t0 = Date.now()
    while (Date.now() - t0 < max) {
      const s = await inPage('/api/session').catch(() => null)
      if (s && s.json && s.json.user) return true
      await page.waitForTimeout(1500)
    }
    return false
  }
  const addable = () => page.locator('article.card button:has-text("Adaugă în coș"):not([disabled])')

  // ---- 01: site publicat — pagina reală + bara de adrese cu URL-ul real (un browser headless nu are bară de adrese)
  console.log('>>> Aștept backend-ul (Render/Neon)...'); await backendReady()
  await step('01-publicat-web', async () => {
    await go('/')
    const b64 = (await page.screenshot()).toString('base64')
    const secure = BASE.startsWith('https://')
    await page.setContent(`<body style="margin:0;background:#dfe1e5;font:14px system-ui,sans-serif">
      <div style="display:flex;align-items:center;gap:14px;padding:9px 14px;color:#444;font-size:18px"><span>←</span><span>→</span><span>⟳</span>
        <div style="flex:1;background:#fff;border-radius:18px;padding:6px 16px;font-size:14px;color:#202124">${secure ? '🔒' : '<span style="color:#b06000">Neprotejat</span> ·'} ${BASE}/</div></div>
      <img src="data:image/png;base64,${b64}" style="display:block;width:100%"></body>`)
  }, true)

  // ---- catalog, navigare, categorii, promoții
  await step('05-meniu-navigare', () => go('/'))
  await step('06-catalog', () => go('/catalog'), true)
  await step('07-categorii', () => go('/catalog?category=Canapele'))
  await step('12-promotii', () => go('/catalog?promo=1'))

  // ---- detalii produs
  await step('08-detalii-produs', async () => {
    await go('/catalog')
    await page.locator('article.card h2 a').first().click()
    await page.waitForURL(/\/(product|produs)\//)
  }, true)

  // ---- comparator (personalizarea)
  await step('', async () => {
    await go('/catalog')
    await page.getByRole('button', { name: /Compară/ }).nth(0).click()
    await page.getByRole('button', { name: /Compară/ }).nth(1).click()
    await page.getByRole('button', { name: /Compară/ }).nth(2).click()
  })
  await step('16-personalizare-comparator', () => go('/compare'), true)
  await step('16b-planificator-camera', async () => { await go('/planner?add=1,5,12'); await page.waitForSelector('svg g') }, true)

  // ---- termeni
  await step('17-termeni-si-conditii', () => go('/termeni'), true)
  await step('17b-confidentialitate', () => go('/confidentialitate'), true)

  // ---- asistent AI (personalizare)
  await step('20-chatbot-ai', async () => {
    await go('/')
    await page.getByRole('button', { name: /Asistent/ }).first().click()
    await page.locator('.chat-panel input').fill('Caut o canapea sub 3000 RON pentru living')
    await page.locator('.chat-panel button[type=submit]').click()
    await page.waitForFunction(() => document.querySelectorAll('.chat-panel .msg.bot').length >= 2 && !document.querySelector('.chat-panel .typing'), null, { timeout: 40000 })
  })

  // ---- înregistrare cu acceptarea termenilor
  await step('17c-inregistrare-cu-termeni', async () => {
    await go('/register')
    await page.getByPlaceholder('Nume complet').fill(USER.name)
    await page.getByPlaceholder('Email').fill(USER.email)
    await page.getByPlaceholder(/Parolă/).fill(USER.password)
    await page.getByRole('checkbox').check()
  })
  await step('', async () => {
    if (EXISTING) return   // cont existent: nu mai creez altul (login explicit mai jos)
    await page.getByRole('button', { name: 'Creează cont' }).click()
    if (!(await waitUser(120000))) throw new Error('înregistrare eșuată: ' + (await page.locator('.err').allInnerTexts().catch(() => [])).join(' | '))
  })

  await cartLines('după înregistrare')
  await step('', async () => { await login(USER); await cartLines('după login explicit') })   // nu mă bazez pe autologin-ul de la înregistrare
  // ---- personalizări: set asortat + listă de dorințe (necesită cont: după înregistrare)
  await step('16c-set-asortat', async () => { await go('/product/5'); await page.waitForSelector('text=Completează camera') }, true)
  await step('', async () => {
    await go('/catalog')
    await page.getByRole('button', { name: /dorințe/i }).nth(0).click()
    await page.waitForTimeout(600)
    await page.getByRole('button', { name: /dorințe/i }).nth(1).click()
    await page.waitForTimeout(600)
  })
  await step('16d-lista-dorinte', () => go('/wishlist'), true)

  // ---- coș
  await backendReady()
  await step('', async () => { await login(USER); await cartLines('login înainte de coș') })   // sesiunea poate fi pierdută dacă backend-ul gratuit a repornit
  await step('', async () => {
    await go('/catalog')
    await addable().nth(0).click()
    await page.waitForTimeout(600)
    await addable().nth(1).click()
    await page.waitForTimeout(600)
  })
  await step('', async () => {   // dacă butoanele nu au adăugat nimic, adaug direct prin API (aceeași sesiune)
    if ((await cartLines('după adăugare în coș')) === 0) {
      for (const id of [1, 5]) { const r = await inPage('/api/cart/items', 'POST', { productId: id, qty: 1 }); console.log(`  API coș produs ${id}: HTTP ${r.status}`) }
      await cartLines('după adăugare prin API')
    }
  })
  await step('09-cos', () => go('/cart'), true)
  await step('09b-cos-actualizat', async () => {
    await cartLines('pe pagina coșului')
    await page.locator('input[name=qty]').first().waitFor({ timeout: 120000 })
    await page.locator('input[name=qty]').first().fill('2')
    await page.getByRole('button', { name: 'Actualizează' }).first().click()
    await page.waitForTimeout(800)
  }, true)

  // ---- checkout + plată
  let stripe = false
  await backendReady()
  await step('14b-metode-plata', async () => { await go('/checkout'); await page.getByText('Metodă de plată').first().waitFor() }, true)
  await step('14-plata-facturii', async () => {
    await go('/checkout')
    await page.locator('form.form').getByLabel('Nume', { exact: true }).fill(USER.name)
    await page.locator('form.form').getByLabel('Email', { exact: true }).fill(USER.email)
    await page.locator('form.form').getByLabel('Adresă', { exact: true }).fill('Str. Exemplu 10, București')
    if (await page.locator('form.form').getByLabel('Număr card', { exact: true }).count()) {
      await page.locator('form.form').getByLabel('Număr card', { exact: true }).fill('4242 4242 4242 4242')
      await page.locator('form.form').getByLabel('Expirare', { exact: true }).fill('12/30')
      await page.locator('form.form').getByLabel('CVV', { exact: true }).fill('123')
    } else stripe = true
    await page.locator('form.form').getByRole('checkbox').check()
  }, true)

  if (stripe) {
    await step('', () => page.getByRole('button', { name: /Stripe/ }).click())
    await step('04-plata-stripe-test', async () => {
      await page.waitForURL(/stripe\.com/, { timeout: 30000 })
      await page.waitForTimeout(3000)
    })
    // Completarea formularului Stripe e cea mai fragilă parte (se schimbă des); dacă eșuează, termini manual.
    await step('', async () => {
      // Stripe își schimbă des câmpurile; încearcă mai multe selectoare pentru fiecare
      const fillAny = async (sels, v) => {
        for (const sel of sels) { const l = page.locator(sel).first(); if (await l.count()) { await l.fill(v); return } }
        throw new Error(`câmp Stripe negăsit (${sels[0]}); termină plata manual`)
      }
      await page.waitForSelector('#cardNumber, input[autocomplete="cc-number"], input[name="cardNumber"]', { timeout: 30000 })
      await fillAny(['#cardNumber', 'input[name="cardNumber"]', 'input[autocomplete="cc-number"]', 'input[placeholder*="1234"]'], '4242424242424242')
      await fillAny(['#cardExpiry', 'input[name="cardExpiry"]', 'input[autocomplete="cc-exp"]', 'input[placeholder*="MM"]'], '1230')
      await fillAny(['#cardCvc', 'input[name="cardCvc"]', 'input[autocomplete="cc-csc"]', 'input[placeholder*="CVC" i]'], '123')
      const nm = page.locator('#billingName, input[name="billingName"], input[autocomplete="cc-name"]').first(); if (await nm.count()) await nm.fill(USER.name)
      await page.locator('.SubmitButton, button[type="submit"]').first().click()
      await page.waitForURL(x => x.href.startsWith(BASE), { timeout: 90000 })
    }, false, '04c-stripe-completare')
  } else {
    await step('', async () => {
      await page.getByRole('button', { name: /Plătește/ }).click()
      await page.waitForURL(/\/(order|comanda)\//)
    })
  }
  await step('04b-comanda-confirmata', async () => {
    // Așteaptă fie comanda, fie mesajul de eroare al paginii de confirmare (ex. stoc epuizat → plată rambursată, sesiune pierdută, server adormit)
    const end = Date.now() + 150000
    while (Date.now() < end) {
      if (/\/(order|comanda)\//.test(page.url())) return
      const err = page.locator('.panel.err').first()
      if (await err.count()) throw new Error('confirmarea plății a eșuat: ' + (await err.innerText()).replace(/\s+/g, ' ').slice(0, 200) + ' [URL: ' + page.url() + ']')
      await page.waitForTimeout(1000)
    }
    throw new Error('nu s-a ajuns la /order/ în 150 s; URL curent: ' + page.url())
  }, true)

  // ---- factură, comenzile mele
  await step('13-factura', async () => {
    if (!/\/(order|comanda)\//.test(page.url())) await go('/orders')       // plata automată a eșuat? ia factura din lista de comenzi
    await page.getByRole('link', { name: /Vezi factura|Factură/ }).first().click({ timeout: 30000 })
    await page.waitForURL(/\/(invoice|factura)\//)
  }, true)
  await step('10-comanda-pe-cont', () => go('/orders'), true)

  // ---- administrare (alt context = sesiune separată)
  await ctx.clearCookies()
  await step('', () => login(ADMIN))
  await step('15a-admin-comenzi', async () => { await go('/admin'); await page.locator('nav.tabs').waitFor({ timeout: 120000 }) }, true)
  await step('15b-admin-produse', async () => { await tabs('Produse') }, true)
  await step('11-stocuri', async () => { /* aceeași pagină: coloana de stoc din Admin → Produse */ }, true)
  await step('15c-admin-utilizatori', async () => { await tabs('Utilizatori') }, true)

  // ---- 14c: dovada plății în Stripe (I.10): pagina /dovada-plata citește plățile reușite direct din API-ul Stripe (mod test), doar ADMIN.
  // Bară de adrese cu URL-ul real (un browser headless nu are bară de adrese).
  await step('14c-stripe-plati', async () => {
    await go('/dovada-plata')
    await page.getByText('Mod cont Stripe').first().waitFor({ timeout: 60000 })
    await page.waitForSelector('table tbody tr', { timeout: 30000 })
    const b64 = (await page.screenshot({ fullPage: true })).toString('base64')
    const secure = BASE.startsWith('https://')
    await page.setContent(`<body style="margin:0;background:#dfe1e5;font:14px system-ui,sans-serif">
      <div style="display:flex;align-items:center;gap:14px;padding:9px 14px;color:#444;font-size:18px"><span>←</span><span>→</span><span>⟳</span>
        <div style="flex:1;background:#fff;border-radius:18px;padding:6px 16px;font-size:14px;color:#202124">${secure ? '🔒' : '<span style="color:#b06000">Neprotejat</span> ·'} ${BASE}/dovada-plata</div></div>
      <img src="data:image/png;base64,${b64}" style="display:block;width:100%"></body>`)
  }, true, '14c-stripe-plati')

  // ---- 02: baza de date a site-ului PUBLICAT: Admin → Baza de date (doar ADMIN, doar citire; nicio consolă expusă public).
  // Bară de adrese cu URL-ul real al site-ului (un browser headless nu are bară de adrese).
  await step('02-baza-de-date', async () => {
    await go('/admin')   // pasul 14c a înlocuit pagina cu un HTML static (bara de adrese): revenim în aplicație
    await tabs('Baza de date')
    await page.getByText('Tabele din baza de date').first().waitFor({ timeout: 30000 })
    await page.waitForSelector('text=ultimele', { timeout: 30000 })
    const b64 = (await page.screenshot({ fullPage: true })).toString('base64')
    const secure = BASE.startsWith('https://')
    await page.setContent(`<body style="margin:0;background:#dfe1e5;font:14px system-ui,sans-serif">
      <div style="display:flex;align-items:center;gap:14px;padding:9px 14px;color:#444;font-size:18px"><span>←</span><span>→</span><span>⟳</span>
        <div style="flex:1;background:#fff;border-radius:18px;padding:6px 16px;font-size:14px;color:#202124">${secure ? '🔒' : '<span style="color:#b06000">Neprotejat</span> ·'} ${BASE}/admin</div></div>
      <img src="data:image/png;base64,${b64}" style="display:block;width:100%"></body>`)
  }, true)

  // ---- 03: scripturile de pe server (codul Java), randat ca pagină și fotografiat
  await step('03-scripturi-server', async () => {
    const dir = path.join(ROOT, 'src/main/java/com/mobila/shop')
    const files = fs.readdirSync(dir).filter(f => f.endsWith('.java')).sort()
    const esc = t => t.replace(/&/g, '&amp;').replace(/</g, '&lt;')
    const code = fs.readFileSync(path.join(dir, 'ShopApiController.java'), 'utf8').split('\n').slice(0, 60).join('\n')
    await page.setContent(`<body style="margin:0;font:14px/1.45 Consolas,monospace;background:#1e1e1e;color:#d4d4d4;padding:20px">
      <div style="color:#9cdcfe;font-weight:bold">src/main/java/com/mobila/shop/ — script-uri pe server: Java 17 + Spring Boot 3</div>
      <div style="color:#888;margin:6px 0 14px">${files.map(esc).join(' · ')}</div>
      <div style="color:#ce9178;margin-bottom:6px">// ShopApiController.java</div><pre style="margin:0">${esc(code)}</pre></body>`)
  }, true)

  // ---- 18/19: PageSpeed Insights, doar pe un URL public https (pagespeed.web.dev nu vede localhost)
  // PageSpeed nu termină analiza în Chromium headless, deci rulează în Chrome real, cu fereastră vizibilă (fallback: Chromium vizibil).
  // Ca un om: pagina de start -> lipește URL-ul -> Enter -> schimbă form_factor pe linkul raportului.
  // Așteaptă până când linkul e vizibil din exterior și reîncearcă dacă PageSpeed dă eroare sau nu termină.
  let pb = null
  if (/^https:\/\//.test(BASE) && !/localhost|127\.0\.0\.1/.test(BASE)) {
    const gauge = '.lh-gauge__percentage, .lh-category-header, .lh-report'
    const sleep = ms => new Promise(r => setTimeout(r, ms))
    // browser vizibil pornit chiar înainte de PageSpeed (nu stă deschis degeaba), validat și relansat automat dacă s-a închis
    async function ensurePS() {
      if (pb && pb.isConnected() && page && !page.isClosed()) return
      if (pb) await pb.close().catch(() => {})
      pb = null
      for (const opt of [{ channel: 'chrome' }, { channel: 'msedge' }, {}]) {
        const nm = opt.channel || 'chromium'
        try {
          const b = await chromium.launch({ ...opt, headless: false, args: ['--disable-blink-features=AutomationControlled', '--no-first-run', '--no-default-browser-check'] })
          const c = await b.newContext({ viewport: { width: 1280, height: 900 }, locale: 'en-US' })
          await c.addInitScript(() => { Object.defineProperty(navigator, 'webdriver', { get: () => undefined }) })
          const p = await c.newPage(); p.setDefaultTimeout(15000)
          await p.goto('about:blank'); await p.waitForTimeout(2000)
          if (!b.isConnected() || p.isClosed()) { await b.close().catch(() => {}); console.log(`  … ${nm}: s-a închis imediat, încerc altul`); continue }
          pb = b; page = p
          console.log(`  (PageSpeed rulează în fereastra ${nm}; nu o închide până termină)`)
          return
        } catch (e) { console.log(`  … ${nm}: ${e.message.split('\n')[0]}`) }
      }
      throw new Error('nu pot porni un browser vizibil pentru PageSpeed (instalează Chrome sau Edge)')
    }
    // linkul Cloudflare nou are nevoie de câteva zeci de secunde până e rezolvat și de alții (verificăm prin DNS-ul Cloudflare, ca să nu încurcăm cache-ul Google)
    async function waitPublic() {
      const host = new URL(BASE).hostname
      for (let i = 0; i < 60; i++) {                       // până la 5 minute
        let dns = false, http = false
        try {
          const r = await fetch(`https://cloudflare-dns.com/dns-query?name=${host}&type=A`, { headers: { accept: 'application/dns-json' }, signal: AbortSignal.timeout(8000) })
          const j = await r.json(); dns = !!(j.Answer && j.Answer.length)
        } catch {}
        try { http = (await fetch(BASE + '/', { signal: AbortSignal.timeout(8000) })).ok } catch {}
        if (dns && http) { await sleep(10000); return true }   // încă 10 s de siguranță pentru propagare
        if (i % 3 === 0) console.log(`  … aștept ca linkul public să fie vizibil din exterior (DNS: ${dns ? 'ok' : 'încă nu'}, HTTP: ${http ? 'ok' : 'încă nu'})`)
        await sleep(5000)
      }
      return false
    }
    // 'ok' = scorurile au apărut, 'error' = PageSpeed a afișat o eroare Lighthouse, 'timeout' = nu s-a terminat în timp
    async function psiOutcome(ms) {
      const end = Date.now() + ms, t0 = Date.now()
      let beat = 0
      while (Date.now() < end) {
        if (++beat % 15 === 0) console.log(`  … PageSpeed analizează încă (${Math.round((Date.now() - t0) / 1000)} s)`)
        if (await page.locator(gauge).count()) return 'ok'
        const txt = await page.evaluate(() => document.body.innerText).catch(() => '')
        if (/FAILED_DOCUMENT_REQUEST|ERRORED_DOCUMENT_REQUEST|DNS_FAILURE|NO_FCP|PROTOCOL_TIMEOUT|Lighthouse returned error|Lighthouse a returnat/i.test(txt)) return 'error'
        await page.waitForTimeout(2000)
      }
      return 'timeout'
    }
    // ce vede scriptul pe pagină când nu reușește (text + poză), ca să se poată diagnostica
    async function dump(tag) {
      const t = await page.evaluate(() => document.body.innerText).catch(() => '')
      console.log(`  [diagnostic ${tag}] URL: ${page.url()}\n  [diagnostic ${tag}] text: ${t.replace(/\s+/g, ' ').slice(0, 500)}`)
      await page.screenshot({ path: path.join(OUT, `EROARE-psi-${tag}.png`) }).catch(() => {})
    }
    async function submitUrl(att) {
      if (att === 2) {                                      // varianta 2: link direct de analiză
        await page.goto(`https://pagespeed.web.dev/analysis?url=${encodeURIComponent(BASE + '/')}`, { waitUntil: 'domcontentloaded' })
        return
      }
      await page.goto('https://pagespeed.web.dev/', { waitUntil: 'domcontentloaded' })
      await page.waitForTimeout(2000)
      for (const t of [/got it/i, /accept/i, /agree/i, /am înțeles/i]) {   // banner de cookies, dacă apare
        const bt = page.getByRole('button', { name: t })
        if (await bt.count()) await bt.first().click().catch(() => {})
      }
      const input = page.locator('input[type="url"], input[name="url"], input[placeholder*="URL" i], input[type="text"]').first()
      await input.fill(BASE + '/')
      await input.press('Enter')
      await page.waitForURL(/pagespeed\.web\.dev\/analysis\//, { timeout: 60000 })
    }
    let reportUrl = ''
    console.log('\n>>> PageSpeed (18/19): pornesc')
    await step('', async () => {
      if (!(await waitPublic())) throw new Error('linkul public nu răspunde din exterior după 5 minute')
      try {
        const xr = (await fetch(BASE + '/')).headers.get('x-robots-tag')
        console.log(xr ? `  [SEO] Serverul trimite X-Robots-Tag: ${xr} → scorul SEO din PageSpeed scade; pe un domeniu real dispare` : '  [SEO] fără antet X-Robots-Tag; dacă SEO e mic, vezi în raport „Crawling and Indexing”')
      } catch {}
      let state = 'timeout'
      for (let att = 1; att <= 3 && state !== 'ok'; att++) {
        if (att > 1) { await dump(String(att - 1)); console.log(`  … PageSpeed: ${state}; reîncerc ${att}/3 peste 30 s`); await sleep(30000) }
        try { await ensurePS(); await submitUrl(att); state = await psiOutcome(120000) } catch (e) { console.log('  … ' + e.message.split('\n')[0]); state = 'error' }
      }
      if (state !== 'ok') { await dump('final'); throw new Error(`PageSpeed nu a produs raportul (${state}) după 3 încercări`) }
      reportUrl = page.url()
    }, false, '18-19-pagespeed-start')
    if (reportUrl) {
      fs.rmSync(path.join(OUT, 'PAGESPEED.txt'), { force: true })
      for (const [name, ff] of [['18-pagespeed-mobile', 'mobile'], ['19-pagespeed-desktop', 'desktop']]) {
        await step('', async () => {
          const u = new URL(reportUrl); u.searchParams.set('form_factor', ff)
          let state = 'timeout'
          for (let att = 1; att <= 2 && state !== 'ok'; att++) {
            if (att > 1) await sleep(20000)
            await ensurePS()
            await page.goto(u.toString(), { waitUntil: 'domcontentloaded' })
            state = await psiOutcome(180000)
          }
          if (state !== 'ok') { await dump(ff); throw new Error(`raportul ${ff} nu a apărut (${state})`) }
          await page.waitForTimeout(2500)
          await save(name)                                   // ecranul cu scorurile (18-... / 19-...png)
          const sc = await page.evaluate(() => [...document.querySelectorAll('.lh-gauge__percentage')].slice(0, 4).map(e => e.textContent.trim()))
          if (sc.length) fs.appendFileSync(path.join(OUT, 'PAGESPEED.txt'), `${ff === 'mobile' ? 'Mobile' : 'Desktop'} ${sc.join('/')}
`)
          // deschide toate secțiunile (oportunități, diagnostice, audituri trecute) și fotografiază tot, de sus în jos
          await page.evaluate(() => document.querySelectorAll('details').forEach(d => { d.open = true }))
          await page.waitForTimeout(1000)
          const total = await page.evaluate(() => document.documentElement.scrollHeight)
          let n = 0
          for (let y = 0; y < total && n < 30; y += 850) {
            await page.evaluate(v => window.scrollTo(0, v), y)
            await page.waitForTimeout(400)
            n++
            await page.screenshot({ path: path.join(OUT, `${name}-${String(n).padStart(2, '0')}.png`) })
          }
          results.push(['OK', `${name} (+${n} ecrane cu tot raportul)`])
          console.log(`  ✓ ${name} (+${n} ecrane cu tot raportul)`)
        }, false, name)
      }
    }
    if (pb) await pb.close().catch(() => {})
  }

  if (!(/^https:\/\//.test(BASE) && !/localhost|127\.0\.0\.1/.test(BASE))) results.push(['EȘUAT', '18/19 sărite: URL-ul ' + BASE + ' nu e public https. Pornește ./scripts/public.sh și rulează ./scripts/screenshots.sh fără argumente.'])
  await browser.close()
}

function gallery() {
  const listFile = path.join(OUT, 'LISTA.txt')
  const items = fs.existsSync(listFile)
    ? fs.readFileSync(listFile, 'utf8').split('\n').map(l => l.match(/^(\S+\.png)\s+(.*)$/)).filter(Boolean).map(m => ({ file: m[1], text: m[2] }))
    : []
  const extra = [
    { file: '09b-cos-actualizat.png', text: 'Coș după actualizarea cantității (I.5)' },
    { file: '17b-confidentialitate.png', text: 'Politica de confidențialitate' },
    { file: '17c-inregistrare-cu-termeni.png', text: 'Înregistrare cu bifa obligatorie pentru termeni' }
  ]
  const have = new Set([...items, ...extra].map(i => i.file))
  fs.readdirSync(OUT).filter(f => /^(18|19)-pagespeed-.*-\d\d\.png$/.test(f) && !have.has(f))
    .forEach(f => extra.push({ file: f, text: `PageSpeed Insights, ${f.startsWith('18') ? 'Mobile' : 'Desktop'} — ecran ${f.match(/-(\d\d)\.png/)[1]} din raport` }))
  const all = [...items, ...extra].sort((a, b) => a.file.localeCompare(b.file))
  const figs = all.map(i => {
    const ok = fs.existsSync(path.join(OUT, i.file))
    return ok
      ? `<figure><img src="screenshots/${i.file}" alt="${i.text}" loading="lazy"><figcaption><b>${i.file.replace('.png', '')}</b> — ${i.text}</figcaption></figure>`
      : `<figure class="todo"><div>Lipsește: adaugă manual <code>screenshots/${i.file}</code></div><figcaption><b>${i.file.replace('.png', '')}</b> — ${i.text}</figcaption></figure>`
  }).join('\n')
  const md = fs.readFileSync(path.join(ROOT, 'docs', 'PREZENTARE.md'), 'utf8')
  const html = `<!doctype html><html lang="ro"><head><meta charset="utf-8"><title>Prezentare MobilaViva</title>
<style>body{font:16px/1.5 system-ui,sans-serif;max-width:1000px;margin:24px auto;padding:0 16px;color:#222}
table{border-collapse:collapse;width:100%}td,th{border:1px solid #ccc;padding:6px 8px;text-align:left;vertical-align:top}
figure{margin:24px 0;page-break-inside:avoid}img{max-width:100%;border:1px solid #ccc}figcaption{font-size:14px;color:#555;margin-top:4px}
.todo div{border:2px dashed #c33;padding:30px;color:#c33;text-align:center}</style></head><body>
${marked.parse(md)}
<h2>Print screen-uri</h2>
${figs}
</body></html>`
  fs.writeFileSync(path.join(ROOT, 'docs', 'PREZENTARE.html'), html)
}

if (!GALLERY_ONLY) await main()
gallery()
const bad = results.filter(r => r[0] !== 'OK')
console.log(`\nPoze salvate: ${results.length - bad.length}/${results.length}  (folder: ${OUT})`)
bad.forEach(r => console.log(`  ✗ ${r[1]}`))
console.log('\nTot ce se poate automatiza e făcut. 01 afișează URL-ul real al site-ului într-o fereastră de browser (încadrare generată de script).')
if (!/^https:\/\//.test(BASE) || /localhost|127\.0\.0\.1/.test(BASE)) console.log('18/19 PageSpeed se fac automat doar când rulezi: ./scripts/screenshots.sh https://linkul-tău-public')
if (results.some(r => r[1].startsWith('02-baza-de-date') && r[0] !== 'OK')) console.log('02: pagina Admin → Baza de date nu s-a deschis. Verifică parola de admin (ADMIN_PASSWORD / .runtime/admin-password) și că site-ul publicat are versiunea nouă (rulează ./scripts/push.sh și așteaptă redeploy-ul).')
console.log('Prezentarea cu galerie: docs/PREZENTARE.html')
