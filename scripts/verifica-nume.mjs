// Verifică acoperirea traducerilor listei de mobilier: numele produselor (pname.js), categoriile și cheile planificatorului.
// Rulare: node scripts/verifica-nume.mjs   (cod de ieșire 1 dacă lipsește ceva)
import fs from 'fs'
import path from 'path'
const rd = f => fs.readFileSync(f, 'utf8')
const SKIP = new Set(['node_modules', 'target', '.git', 'dist'])
const walk = d => fs.readdirSync(d, { withFileTypes: true }).flatMap(e => e.isDirectory() ? (SKIP.has(e.name) ? [] : walk(path.join(d, e.name))) : [path.join(d, e.name)])
const LANGS = ['en', 'de', 'fr', 'it', 'pt', 'es', 'nl']
const errs = []
const DESCR = new Set(['Casuță'])   // cuvinte descriptive scrise cu majusculă, nu nume de model

const pn = rd('frontend/src/pname.js')
const N = new Function('return ' + pn.slice(pn.indexOf('const N = ') + 10, pn.indexOf('\nexport function pname')))()
const names = new Map()
for (const f of walk('.').filter(f => f.endsWith('.java')))
  for (const m of rd(f).matchAll(/\bp\("([^"]+)",\s*"[^"]*",\s*"([^"]+)"/g)) names.set(m[1], m[2])
if (names.size < 50) errs.push('catalog negăsit în sursele Java (' + names.size + ' produse)')

for (const [n] of names) {
  const e = N[n]
  if (!e) { errs.push('produs fără traduceri: ' + n); continue }
  const model = n.split(/\s+/).slice(1).filter(w => /^[A-ZȘȚĂÎÂ]\p{L}+$/u.test(w) && !/^[A-Z]+$/.test(w) && !DESCR.has(w))
  const nums = n.match(/\d[\d.,x-]*/g) || []
  for (const L of LANGS) {
    const v = e[L]
    if (!v || !v.trim()) { errs.push(L + ': lipsește „' + n + '"'); continue }
    for (const w of [...model, ...nums]) if (!v.includes(w)) errs.push(L + ': „' + v + '" pierde „' + w + '" din „' + n + '"')
  }
}

const ex = rd('frontend/src/i18n-extra.js')
for (const k of ['plan_color', 'plan_preset', 'plan_pre_living', 'plan_pre_dorm', 'plan_pre_birou', 'plan_pre_sufr', 'plan_pre_hol', 'plan_pre_copil'])
  { const c = ex.split('"' + k + '":').length - 1; if (c !== 8) errs.push('cheia ' + k + ' apare în ' + c + ' limbi din 8') }
for (const k of ['prod_maker', 'prod_origin', 'prod_method', ...['lighting', 'mattress', 'upholstered', 'rattan', 'mesh', 'solid_wood', 'panel', 'metal', 'glass', 'assembled'].map(x => 'prod_m_' + x)])
  { const c = ex.split('"' + k + '":').length - 1; if (c !== 8) errs.push('cheia ' + k + ' apare în ' + c + ' limbi din 8') }
const i18 = rd('frontend/src/i18n.js')
for (const c of new Set(names.values()))
  { const k = ex.split('c_' + c + ':').length - 1 + i18.split('c_' + c + ':').length - 1; if (k < 8) errs.push('categoria ' + c + ' tradusă în ' + k + ' limbi din 8') }
if (!rd('frontend/src/views/Planner.vue').includes('pname(c.p.name)')) errs.push('Planner.vue: opțiunile listei nu folosesc pname()')

if (errs.length) { console.error(errs.join('\n')); process.exit(1) }
console.log('liste mobilier traduse: ' + names.size + ' produse x ' + LANGS.length + ' limbi, categorii și chei planificator complete')
