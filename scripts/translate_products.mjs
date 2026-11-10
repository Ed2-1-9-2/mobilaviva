#!/usr/bin/env node
// Traduce produsele (nume, descriere, material, culoare) în EN, DE, FR, IT, PT, ES, NL cu Claude și le salvează în frontend/src/product-i18n/<limba>.json.
//   node scripts/translate_products.mjs          traduce doar ce lipsește
//   node scripts/translate_products.mjs --dry    arată ce ar traduce, fără apeluri API (cost zero)
//   node scripts/translate_products.mjs --force  retraduce tot (după ce modifici texte în română)
// Opțional: TRANSLATE_FROM=https://site.ro include și produsele adăugate din Admin (citite din /api/products).
import fs from 'node:fs'
import path from 'node:path'

const LANGS = ['en', 'de', 'fr', 'it', 'pt', 'es', 'nl']
const LANG_NAME = { en: 'English', de: 'German', fr: 'French', it: 'Italian', pt: 'European Portuguese', es: 'Spanish', nl: 'Dutch' }
const DIR = 'frontend/src/product-i18n'
const SEED = 'src/main/java/com/mobila/shop/SeedData.java'
const BATCH = 8
const args = process.argv.slice(2)
const DRY = args.includes('--dry'), FORCE = args.includes('--force')
const sleep = ms => new Promise(r => setTimeout(r, ms))

function env(k) {
  if (process.env[k]) return process.env[k]
  try {
    const line = fs.readFileSync('.env', 'utf8').split(/\r?\n/).filter(l => l.startsWith(k + '=')).pop()
    return line ? line.slice(k.length + 1).replace(/\s+\/\/.*$/, '').trim().replace(/^["']|["']$/g, '') : ''
  } catch { return '' }
}
const KEY = env('ANTHROPIC_API_KEY')
const MODEL = env('TRANSLATE_MODEL') || env('CHAT_MODEL') || 'claude-haiku-5-5'

const products = new Map()
const src = fs.existsSync(SEED) ? fs.readFileSync(SEED, 'utf8') : ''
const re = /\bp\("([^"]+)",\s*"([^"]*)",\s*"([^"]*)",\s*[\d.]+,\s*[\d.]+,\s*\d+,\s*[\d.]+,\s*\d+,\s*"([^"]*)",\s*"([^"]*)",\s*"([^"]*)",\s*"([^"]*)"\)/g
for (const m of src.matchAll(re)) products.set(m[1], { name: m[1], material: m[4], color: m[6], description: m[7] })
const FROM = (env('TRANSLATE_FROM') || '').replace(/\/$/, '')
if (FROM) {
  try {
    for (let pg = 1, pages = 1; pg <= pages; pg++) {
      const r = await (await fetch(`${FROM}/api/products?page=${pg}`, { signal: AbortSignal.timeout(60000) })).json()
      pages = r.pages || 1
      for (const x of r.items || []) if (x.name && !products.has(x.name)) products.set(x.name, { name: x.name, material: x.material || '', color: x.color || '', description: x.description || '' })
    }
  } catch (e) { console.log('  ! nu am putut citi produsele de la ' + FROM + ': ' + e.message) }
}

fs.mkdirSync(DIR, { recursive: true })
const cur = {}
for (const l of LANGS) { try { cur[l] = JSON.parse(fs.readFileSync(path.join(DIR, l + '.json'), 'utf8')) } catch { cur[l] = {} } }
const save = () => { for (const l of LANGS) fs.writeFileSync(path.join(DIR, l + '.json'), JSON.stringify(cur[l], null, 1) + '\n') }
const done = p => LANGS.every(l => cur[l][p.name] && cur[l][p.name].name)
const todo = [...products.values()].filter(p => FORCE || !done(p))

console.log(`>>> Traduceri produse: ${products.size} produse găsite, ${todo.length} de tradus în ${LANGS.length} limbi (${LANGS.join(', ').toUpperCase()}).`)
if (!todo.length) { console.log('>>> Totul este deja tradus. Nimic de făcut, cost 0.'); process.exit(0) }
const estTok = todo.length * 700
console.log(`>>> Estimare: ~${Math.round(estTok / 1000)}k tokeni cu modelul ${MODEL} (de ordinul a câțiva cenți).`)
if (DRY) { console.log('>>> --dry: nu s-a apelat API-ul. Produse de tradus:\n    ' + todo.map(p => p.name).join('\n    ')); process.exit(0) }
if (!KEY) { console.log('>>> ANTHROPIC_API_KEY lipsește în .env: sar peste. Produsele netraduse rămân în română (nu se strică nimic).'); process.exit(0) }

const str = v => (typeof v === 'string' ? v.trim() : '')
function clean(v) {
  if (!v || typeof v !== 'object' || !str(v.name)) return null
  return { name: str(v.name), description: str(v.description), material: str(v.material), color: str(v.color) }
}

async function translateBatch(batch) {
  const input = batch.map((p, i) => ({ id: i, name: p.name, description: p.description, material: p.material, color: p.color }))
  const system = `You translate product data for a Romanian online furniture store into: ${LANGS.map(l => l + ' (' + LANG_NAME[l] + ')').join(', ')}.
Rules: keep brand and collection proper names unchanged (Milano, Oslo, Luna, Bergen, Nordic, Loft, Chester, Verona, Lyon, Aria, Terra, Scandi, Halo...); keep numbers, sizes and units exactly as they are (160x200, 140-180, cm, RON); use natural, concise, store-quality wording and normal furniture-shop terms for materials and colors; keep "+" separators in materials and colors; if a field is empty return an empty string.
Return ONLY valid JSON, no markdown and no comments: an array with one object per input item, in the same order, shaped like {"id": <id>, "en": {"name":"","description":"","material":"","color":""}, "de": {...}, "fr": {...}, "it": {...}, "pt": {...}, "es": {...}, "nl": {...}}.`
  const res = await fetch('https://api.anthropic.com/v1/messages', {
    method: 'POST',
    headers: { 'content-type': 'application/json', 'x-api-key': KEY, 'anthropic-version': '2023-06-01' },
    body: JSON.stringify({ model: MODEL, max_tokens: 8000, system, messages: [{ role: 'user', content: JSON.stringify(input) }] }),
    signal: AbortSignal.timeout(180000)
  })
  const body = await res.text()
  if (res.status === 401 || res.status === 403) throw Object.assign(new Error('cheie API respinsă (HTTP ' + res.status + ')'), { fatal: true })
  if (!res.ok) throw new Error('HTTP ' + res.status + ' ' + body.slice(0, 160))
  const text = (JSON.parse(body).content || []).filter(b => b.type === 'text').map(b => b.text).join('')
  const a = text.indexOf('['), b = text.lastIndexOf(']')
  if (a < 0 || b < a) throw new Error('răspuns fără JSON')
  return JSON.parse(text.slice(a, b + 1))
}

let ok = 0, failed = 0, fatal = false
for (let i = 0; i < todo.length && !fatal; i += BATCH) {
  const batch = todo.slice(i, i + BATCH)
  let res = null
  for (let att = 1; att <= 3 && !res; att++) {
    try { res = await translateBatch(batch) }
    catch (e) {
      console.log(`  ! lotul ${i / BATCH + 1}, încercarea ${att}: ${e.message}`)
      if (e.fatal) { fatal = true; break }
      await sleep(2000 * att)
    }
  }
  if (!res) { failed += batch.length; continue }
  for (const r of res) {
    const p = batch[Number(r && r.id)]
    if (!p) continue
    let good = 0
    for (const l of LANGS) { const v = clean(r[l]); if (v) { cur[l][p.name] = v; good++ } }
    good === LANGS.length ? ok++ : failed++
  }
  save()
  console.log(`  ✓ ${Math.min(i + BATCH, todo.length)}/${todo.length}`)
}
console.log(`>>> Gata: ${ok} produse traduse complet, ${failed} rămase (se reîncearcă la următoarea rulare). Fișiere: ${DIR}/*.json`)
