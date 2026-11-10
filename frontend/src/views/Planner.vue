<script setup>
import Icon from '../components/Icon.vue'
import { ref, reactive, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../api'
import { addToCart, ron } from '../store'
import { footprint } from '../footprint'
import { cross, rectInPoly, ovl, polyArea } from '../geo'
import { arrange, classify } from '../arrange'
import { symbolSvg, colorOf } from '../furnsym'
import { parseRoomFile, pickRoom } from '../roomimport'
import { pname } from '../pname'
import { t, catName } from '../i18n'

const route = useRoute()
const router = useRouter()
// Forma camerei: dreptunghi, L, U, cu balcon sau contur liber (colțuri trase cu mouse-ul)
const SHAPES = ['rect', 'L', 'U', 'balcon', 'custom']
const OBS = { support: [40, 40], wall: [100, 20], radiator: [100, 15], door: [90, 90], balcony: [150, 30], other: [60, 60] }
const ORDER = ['Canapele', 'Paturi', 'Dulapuri', 'Comode', 'Mese', 'Scaune', 'Birouri', 'Iluminat', 'Hol', 'Copii']
const room = reactive({ shape: 'rect', w: 400, l: 500, cw: 150, cl: 150, bw: 200, bd: 120, bx: 100 })
const custom = ref([])
const obstacles = ref([])
const obsKind = ref('support')
const bgUrl = ref('')
const bg = reactive({ op: 0.5, scale: 100 })
const imp = reactive({ text: '', ext: '', unit: 'auto', rings: [], rooms: [], pick: 0, msg: '', err: '' })
const arr = reactive({ msg: '', warn: '', style: '' })
const catalog = ref([])
const pick = ref('')
const items = ref([])
const svg = ref(null)
let uid = 1
let drag = null

const num = v => Math.max(0, Number(v) || 0)
function genPoly(shape) {
  const w = num(room.w), l = num(room.l)
  const cw = Math.min(num(room.cw), Math.max(0, w - 20)), cl = Math.min(num(room.cl), Math.max(0, l - 20))
  if (shape === 'L') return [[0, 0], [w - cw, 0], [w - cw, cl], [w, cl], [w, l], [0, l]]
  if (shape === 'U') { const a = (w - cw) / 2, b = a + cw; return [[0, 0], [a, 0], [a, cl], [b, cl], [b, 0], [w, 0], [w, l], [0, l]] }
  if (shape === 'balcon') {
    const bw = Math.min(num(room.bw), w), bx = Math.min(num(room.bx), w - bw), bd = num(room.bd)
    return [[0, 0], [w, 0], [w, l], [bx + bw, l], [bx + bw, l + bd], [bx, l + bd], [bx, l], [0, l]]
  }
  return [[0, 0], [w, 0], [w, l], [0, l]]
}
const poly = computed(() => (room.shape === 'custom' ? custom.value : genPoly(room.shape)))
watch(() => room.shape, (s, old) => {
  if (s === 'custom' && custom.value.length < 3) custom.value = genPoly(old).map(p => [p[0], p[1]])
})
const bbox = computed(() => {
  const xs = poly.value.map(p => p[0]), ys = poly.value.map(p => p[1])
  return { x0: Math.min(...xs), y0: Math.min(...ys), x1: Math.max(...xs), y1: Math.max(...ys) }
})
const bw_ = computed(() => bbox.value.x1 - bbox.value.x0)
const bh_ = computed(() => bbox.value.y1 - bbox.value.y0)
const fs = computed(() => Math.max(10, Math.round(Math.max(bw_.value, bh_.value) / 32)))
const hr = computed(() => Math.max(6, Math.round(Math.max(bw_.value, bh_.value) / 55)))
const viewBox = computed(() => { const m = 45; return `${bbox.value.x0 - m} ${bbox.value.y0 - m} ${bw_.value + 2 * m} ${bh_.value + 2 * m}` })
const pts = computed(() => poly.value.map(p => p.join(',')).join(' '))
const area = computed(() => polyArea(poly.value))
const edges = computed(() => poly.value.map((a, i) => {
  const b = poly.value[(i + 1) % poly.value.length]
  return { x: (a[0] + b[0]) / 2, y: (a[1] + b[1]) / 2, len: Math.round(Math.hypot(b[0] - a[0], b[1] - a[1])) }
}).filter(e => e.len >= 30))

const size = it => (it.rot ? { w: it.fp.d, d: it.fp.w } : { w: it.fp.w, d: it.fp.d })
// '' = încape; altfel motivul: out (în afara camerei) / obs (obstacol) / item (altă piesă)
function why(it) {
  const s = size(it)
  if (!rectInPoly(it.x, it.y, s.w, s.d, poly.value)) return 'out'
  if (obstacles.value.some(o => ovl(it.x, it.y, s.w, s.d, o.x, o.y, o.w, o.d))) return 'obs'
  if (items.value.some(o => o !== it && o.uid !== it.on && o.on !== it.uid && ovl(it.x, it.y, s.w, s.d, o.x, o.y, size(o).w, size(o).d))) return 'item'
  return ''
}
const status = computed(() => new Map(items.value.map(it => [it.uid, why(it)])))
const ok = it => !status.value.get(it.uid)
const short = n => pname(n).split(' ').slice(0, 2).join(' ')

function add(id) {
  const c = catalog.value.find(x => x.p.id === Number(id))
  if (!c) return
  const it = { uid: uid++, p: c.p, fp: c.fp, rot: false, x: bbox.value.x0, y: bbox.value.y0 }
  const b = bbox.value
  search: for (const rot of [false, true]) {
    it.rot = rot
    const s = size(it)
    for (let y = b.y0; y + s.d <= b.y1; y += 10)
      for (let x = b.x0; x + s.w <= b.x1; x += 10) {
        it.x = x; it.y = y
        if (!why(it)) break search
      }
    if (rot) { it.rot = false; it.x = b.x0; it.y = b.y0 }
  }
  it.face = it.rot ? 1 : 0
  items.value.push(it)
}
function remove(it) { items.value = items.value.filter(x => x !== it) }
function addObstacle() {
  const [w, d] = OBS[obsKind.value]
  const b = bbox.value
  let x = b.x0, y = b.y0
  const o = { uid: uid++, kind: obsKind.value, x, y, w, d }
  search: for (y = b.y0; y + d <= b.y1; y += 10)
    for (x = b.x0; x + w <= b.x1; x += 10) {
      o.x = x; o.y = y
      if (rectInPoly(x, y, w, d, poly.value) && !obstacles.value.some(q => ovl(x, y, w, d, q.x, q.y, q.w, q.d))) break search
    }
  obstacles.value.push(o)
}
function removeObstacle(o) { obstacles.value = obstacles.value.filter(x => x !== o) }

function addCorner() {
  const P = custom.value
  let best = 0, bl = -1
  P.forEach((a, i) => { const b = P[(i + 1) % P.length], l = Math.hypot(b[0] - a[0], b[1] - a[1]); if (l > bl) { bl = l; best = i } })
  const a = P[best], b = P[(best + 1) % P.length]
  P.splice(best + 1, 0, [Math.round((a[0] + b[0]) / 10) * 5, Math.round((a[1] + b[1]) / 10) * 5])
}
function removeCorner(i) { if (custom.value.length > 3) custom.value.splice(i, 1) }
function loadBg(e) {
  const f = e.target.files && e.target.files[0]
  if (!f) return
  if (bgUrl.value) URL.revokeObjectURL(bgUrl.value)
  bgUrl.value = URL.createObjectURL(f)
}
function clearBg() { if (bgUrl.value) URL.revokeObjectURL(bgUrl.value); bgUrl.value = '' }

const setCount = name => { const m = String(name).match(/set\s*(\d)/i); const n = Number(m && m[1]); return n > 1 ? n : 1 }

// ---- import automat din plan / cadastru (DXF, SVG, GeoJSON); imaginile devin fundal de urmărit
async function loadPlan(e) {
  const f = e.target.files && e.target.files[0]
  e.target.value = ''
  if (!f) return
  imp.err = ''; imp.msg = ''
  const ext = (f.name.split('.').pop() || '').toLowerCase()
  if (/^(png|jpe?g|webp|gif|bmp)$/.test(ext)) {
    if (bgUrl.value) URL.revokeObjectURL(bgUrl.value)
    bgUrl.value = URL.createObjectURL(f)
    if (room.shape !== 'custom') room.shape = 'custom'
    imp.msg = t('plan_imp_img')
    return
  }
  imp.text = await f.text(); imp.ext = ext
  runImport()
}
function runImport() {
  imp.err = ''; imp.msg = ''
  try {
    const r = parseRoomFile(imp.text, imp.ext, imp.unit)
    imp.rings = r.rings; imp.rooms = r.rooms; imp.pick = r.rooms[0]
    applyPick()
  } catch (e) {
    imp.err = t(e.message === 'fmt' ? 'plan_imp_fmt' : 'plan_imp_err')
  }
}
function applyPick() {
  const r = pickRoom(imp.rings, Number(imp.pick))
  custom.value = r.poly
  room.shape = 'custom'
  obstacles.value = r.obstacles.map(o => ({ uid: uid++, ...o }))
  imp.msg = t('plan_imp_ok', { a: (r.area / 10000).toFixed(1), c: r.poly.length, o: r.obstacles.length })
}

// ---- aranjare automată (reguli de amenajare, vezi arrange.js)
function autoArrange() {
  arr.msg = ''; arr.warn = ''; arr.style = ''
  if (!items.value.some(i => !i.copy)) { arr.msg = t('plan_arr_none'); return }
  items.value = items.value.filter(i => !i.copy)
  const base = [...items.value]
  for (const it of base) for (let k = 1; k < setCount(it.p.name); k++) items.value.push({ uid: uid++, p: it.p, fp: it.fp, rot: false, x: 0, y: 0, copy: true, on: null })
  items.value.forEach(i => { i.on = null })
  const res = arrange({
    items: items.value.map(i => ({ uid: i.uid, name: i.p.name, cat: i.p.category, w: i.fp.w, d: i.fp.d, round: !!i.fp.round, material: i.p.material, color: i.p.color, brand: i.p.brand })),
    poly: poly.value.map(p => [p[0], p[1]]),
    obstacles: obstacles.value.map(o => ({ kind: o.kind, x: o.x, y: o.y, w: o.w, d: o.d }))
  })
  for (const r of res.placed) {
    const it = items.value.find(x => x.uid === r.uid)
    if (it) { it.x = r.x; it.y = r.y; it.rot = r.rot; it.on = r.on; it.face = faceFrom(r.side, r.rot, it) }
  }
  for (const it of items.value) if (it.on) { const h = items.value.find(x => x.uid === it.on); if (h && it.rot === h.rot) it.face = h.face }
  const b = bbox.value
  for (const f of res.failed) { const it = items.value.find(x => x.uid === f.uid); if (it) { it.x = b.x0; it.y = b.y0; it.rot = false; it.face = 0 } }
  arr.msg = t('plan_arr_done', { n: res.placed.length })
  if (res.failed.length) arr.warn = t('plan_arr_fail', { n: res.failed.length, list: [...new Set(res.failed.map(f => pname(f.name)))].join(', ') })
  if (res.style) arr.style = res.style.second ? t('plan_arr_style_mix', { a: t('plan_style_' + res.style.dominant), b: t('plan_style_' + res.style.second) }) : t('plan_arr_style_ok', { s: t('plan_style_' + res.style.dominant) })
}

// ---- orientare: face 0=fața în jos, 1=stânga, 2=sus, 3=dreapta (paritatea trebuie să coincidă cu rot)
function faceFrom(side, rot, it) {
  const cx = it.x + size(it).w / 2, cy = it.y + size(it).d / 2, c = bbox.value
  const mx = (c.x0 + c.x1) / 2, my = (c.y0 + c.y1) / 2
  if (!rot) return side && side.dy ? (side.dy < 0 ? 2 : 0) : (my < cy ? 2 : 0)
  return side && side.dx ? (side.dx > 0 ? 3 : 1) : (mx < cx ? 1 : 3)
}
function turn(it) { it.face = ((it.face || 0) + 1) % 4; it.rot = it.face % 2 === 1 }
const roleOf = it => classify(it.p.name, it.p.category)
const sym = it => symbolSvg(roleOf(it), it.fp.w, it.fp.d, !!it.fp.round, it.col || colorOf(it.p.color), it.p.name)
const tf = it => {
  const s = size(it)
  return `translate(${it.x + s.w / 2} ${it.y + s.d / 2}) rotate(${[0, 90, 180, 270][it.face || 0]}) translate(${-it.fp.w / 2} ${-it.fp.d / 2})`
}
const drawn = computed(() => [...items.value].sort((a, b) => (a.on ? 1 : 0) - (b.on ? 1 : 0)))

// ---- camere rapide: alege automat piese variate (aleator, potrivite ca dimensiune) și le aranjează
const PRESETS = {
  living: ['sofa', 'tv', 'coffee', 'armchair', 'shelf', 'floorlamp', 'dresser'],
  dorm: ['bed', 'nightstand', 'nightstand', 'wardrobe', 'dresser', 'tablelamp', 'tablelamp', 'armchair'],
  birou: ['desk', 'deskchair', 'shelf', 'floorlamp', 'tablelamp', 'armchair'],
  sufr: ['table', 'chair', 'shelf', 'dresser', 'floorlamp', 'hall'],
  hol: ['hall', 'hall', 'hall', 'hall'],
  copil: ['bed', 'desk', 'other', 'other', 'other', 'floorlamp']
}
function quickRoom(key) {
  if (!roomOk.value) return
  const kids = key === 'copil'
  const room_ = usable.value
  const taken = new Set()
  const out = []
  for (const role of PRESETS[key]) {
    const same = out.find(o => o.role === role && role === 'nightstand')
    if (same) { out.push({ role, c: same.c }); continue }
    let cand = catalog.value.filter(c => !taken.has(c.p.id) && c.p.stock > 0 && (kids ? c.p.category === 'Copii' || ['floorlamp'].includes(classify(c.p.name, c.p.category)) : c.p.category !== 'Copii')
      && classify(c.p.name, c.p.category) === role && c.fp.w * c.fp.d <= room_ * (role === 'bed' || role === 'sofa' ? 0.3 : 0.15) && Math.max(c.fp.w, c.fp.d) <= Math.max(bw_.value, bh_.value) - 20)
    if (!cand.length) continue
    const c = cand[Math.floor(Math.random() * cand.length)]
    taken.add(c.p.id)
    out.push({ role, c })
  }
  items.value = out.map(o => ({ uid: uid++, p: o.c.p, fp: o.c.fp, rot: false, face: 0, x: bbox.value.x0, y: bbox.value.y0, on: null }))
  autoArrange()
}

function toRoom(e) {
  const pt = svg.value.createSVGPoint()
  pt.x = e.clientX; pt.y = e.clientY
  return pt.matrixTransform(svg.value.getScreenCTM().inverse())
}
function down(e, obj, vertex) {
  const p = toRoom(e)
  drag = vertex === undefined ? { obj, dx: p.x - obj.x, dy: p.y - obj.y } : { v: vertex }
  svg.value.setPointerCapture(e.pointerId)
}
function move(e) {
  if (!drag) return
  const p = toRoom(e)
  if (drag.v !== undefined) {
    custom.value[drag.v] = [Math.round(p.x / 5) * 5, Math.round(p.y / 5) * 5]
    return
  }
  drag.obj.x = Math.round((p.x - drag.dx) / 5) * 5
  drag.obj.y = Math.round((p.y - drag.dy) / 5) * 5
}
function up() { drag = null }

const roomOk = computed(() => poly.value.length >= 3 && bw_.value >= 100 && bh_.value >= 100 && area.value >= 10000)
const selfCross = computed(() => {
  const P = poly.value, n = P.length
  for (let i = 0; i < n; i++) for (let j = i + 2; j < n; j++) {
    if (i === 0 && j === n - 1) continue
    if (cross(P[i], P[(i + 1) % n], P[j], P[(j + 1) % n])) return true
  }
  return false
})
const usable = computed(() => Math.max(1, area.value - obstacles.value.reduce((s, o) => s + o.w * o.d, 0)))
const itemArea = it => (it.on ? 0 : it.fp.round ? Math.PI * it.fp.w * it.fp.d / 4 : it.fp.w * it.fp.d)
const pct = computed(() => (roomOk.value ? Math.round(items.value.reduce((s, it) => s + itemArea(it), 0) / usable.value * 100) : 0))
const bad = computed(() => items.value.filter(it => !ok(it)))
// „Adaugă toate piesele în coș”: piesele din plan (inclusiv copiile) grupate pe produs; cantitatea e limitată la stoc, produsele fără stoc se omit
const toBuy = computed(() => {
  const m = new Map()
  for (const it of items.value) {
    if (it.p.stock <= 0) continue
    const e = m.get(it.p.id) || { p: it.p, qty: 0 }
    e.qty = Math.min(e.qty + 1, it.p.stock)
    m.set(it.p.id, e)
  }
  return [...m.values()]
})
const toBuyQty = computed(() => toBuy.value.reduce((s, e) => s + e.qty, 0))
const toBuyTotal = computed(() => toBuy.value.reduce((s, e) => s + e.p.price * e.qty, 0))
const outOfStock = computed(() => new Set(items.value.filter(it => it.p.stock <= 0).map(it => it.p.id)).size)
async function addAll() {
  for (const e of toBuy.value) await addToCart(e.p.id, e.qty)
  router.push('/cart')
}
const verdict = computed(() => {
  if (!items.value.length) return ''
  if (bad.value.length) return t('plan_verdict_bad', { n: bad.value.length })
  if (pct.value > 50) return t('plan_verdict_crowded', { pct: pct.value })
  return t('plan_verdict_ok', { pct: pct.value, rest: 100 - pct.value })
})
const reason = it => { const r = status.value.get(it.uid); return r ? t(r === 'out' ? 'plan_out' : r === 'obs' ? 'plan_hit_obs' : 'plan_hit_item') : t('plan_fits') }
const groups = computed(() => {
  const m = new Map()
  for (const c of catalog.value) { if (!m.has(c.p.category)) m.set(c.p.category, []); m.get(c.p.category).push(c) }
  const rank = c => (ORDER.indexOf(c) === -1 ? 99 : ORDER.indexOf(c))
  return [...m].sort((a, b) => rank(a[0]) - rank(b[0]))
})

async function loadAll() {
  let page = 1, pages = 1, all = []
  do {
    const r = await api('/products?page=' + page)
    all = all.concat(r.items); pages = r.pages; page++
  } while (page <= pages)
  catalog.value = all.map(p => ({ p, fp: footprint(p.dimensions) })).filter(x => x.fp && !/lustr/i.test(x.p.name))
  pick.value = catalog.value[0]?.p.id ?? ''
  String(route.query.add || '').split(',').map(Number).filter(Boolean).forEach(add)
}
loadAll()
</script>

<template>
  <h1 class="pagetitle">{{ t('plan_title') }}</h1>
  <div class="panel plan">
    <p class="mut">{{ t('plan_intro') }}</p>
    <div class="row" style="flex-wrap:wrap;align-items:end">
      <label class="mut">{{ t('plan_shape') }}
        <select v-model="room.shape"><option v-for="s in SHAPES" :key="s" :value="s">{{ t('plan_shape_' + s) }}</option></select>
      </label>
      <label v-if="room.shape !== 'custom'" class="mut">{{ t('plan_width_label') }}<input v-model.number="room.w" type="number" min="100" max="2000" step="10"></label>
      <label v-if="room.shape !== 'custom'" class="mut">{{ t('plan_length_label') }}<input v-model.number="room.l" type="number" min="100" max="2000" step="10"></label>
      <template v-if="room.shape === 'L' || room.shape === 'U'">
        <label class="mut">{{ t('plan_notch_w') }}<input v-model.number="room.cw" type="number" min="0" max="1900" step="10"></label>
        <label class="mut">{{ t('plan_notch_d') }}<input v-model.number="room.cl" type="number" min="0" max="1900" step="10"></label>
      </template>
      <template v-if="room.shape === 'balcon'">
        <label class="mut">{{ t('plan_balc_w') }}<input v-model.number="room.bw" type="number" min="50" max="2000" step="10"></label>
        <label class="mut">{{ t('plan_balc_d') }}<input v-model.number="room.bd" type="number" min="50" max="500" step="10"></label>
        <label class="mut">{{ t('plan_balc_x') }}<input v-model.number="room.bx" type="number" min="0" max="2000" step="10"></label>
      </template>
      <button v-if="room.shape === 'custom'" class="sec" @click="addCorner">{{ t('plan_corner_add') }}</button>
    </div>
    <p v-if="room.shape === 'custom'" class="mut">{{ t('plan_corner_hint') }}</p>
    <div class="row" style="flex-wrap:wrap;align-items:end">
      <label class="mut" style="flex:2">{{ t('plan_piece') }}
        <select v-model="pick">
          <optgroup v-for="g in groups" :key="g[0]" :label="catName(g[0])">
            <option v-for="c in g[1]" :key="c.p.id" :value="c.p.id">{{ pname(c.p.name) }} ({{ t('plan_dims_short', { w: c.fp.w, d: c.fp.d }) }})</option>
          </optgroup>
        </select>
      </label>
      <button @click="add(pick)">{{ t('plan_add') }}</button>
    </div>
    <div class="row" style="flex-wrap:wrap;align-items:end">
      <label class="mut">{{ t('plan_obs_kind') }}
        <select v-model="obsKind"><option v-for="(v, k) in OBS" :key="k" :value="k">{{ t('plan_obs_' + k) }} ({{ t('plan_dims_short', { w: v[0], d: v[1] }) }})</option></select>
      </label>
      <button class="sec" @click="addObstacle">{{ t('plan_obs_add') }}</button>
      <label class="mut">{{ t('plan_bg_load') }}<input type="file" accept="image/*" @change="loadBg"></label>
      <template v-if="bgUrl">
        <label class="mut">{{ t('plan_bg_opacity') }}<input v-model.number="bg.op" type="range" min="0.1" max="1" step="0.05"></label>
        <label class="mut">{{ t('plan_bg_scale') }}<input v-model.number="bg.scale" type="range" min="10" max="300" step="5"></label>
        <button class="link" @click="clearBg">{{ t('plan_bg_clear') }}</button>
      </template>
    </div>
    <div class="row" style="flex-wrap:wrap;align-items:end">
      <label class="mut" style="flex:2">{{ t('plan_imp_load') }}<input type="file" accept=".dxf,.svg,.json,.geojson,image/*" @change="loadPlan"></label>
      <label class="mut">{{ t('plan_imp_unit') }}
        <select v-model="imp.unit" @change="imp.text && runImport()"><option value="auto">{{ t('plan_imp_unit_auto') }}</option><option value="m">m</option><option value="cm">cm</option><option value="mm">mm</option></select>
      </label>
      <label v-if="imp.rooms.length > 1" class="mut">{{ t('plan_imp_room') }}
        <select v-model="imp.pick" @change="applyPick"><option v-for="(r, i) in imp.rooms" :key="r" :value="r">{{ t('plan_imp_room_opt', { n: i + 1, a: (imp.rings[r].length ? Math.round(polyArea(imp.rings[r]) / 100) / 100 : 0) }) }}</option></select>
      </label>
    </div>
    <p class="mut" style="font-size:.85rem">{{ t('plan_imp_hint') }}</p>
    <p v-if="imp.msg" class="ok">{{ imp.msg }}</p>
    <p v-if="imp.err" class="err">{{ imp.err }}</p>
    <div class="row" style="flex-wrap:wrap;align-items:center">
      <span class="mut">{{ t('plan_preset') }}</span>
      <button v-for="k in ['living', 'dorm', 'birou', 'sufr', 'hol', 'copil']" :key="k" class="sec" :disabled="!roomOk || !catalog.length" @click="quickRoom(k)">{{ t('plan_pre_' + k) }}</button>
    </div>
    <div class="row" style="flex-wrap:wrap;align-items:center">
      <button :disabled="!items.length" @click="autoArrange"><Icon name="plan" :size="18" />{{ t('plan_arr') }}</button>
      <span class="mut" style="flex:2;font-size:.85rem">{{ t('plan_arr_hint') }}</span>
    </div>
    <p v-if="arr.msg" class="ok">{{ arr.msg }}</p>
    <p v-if="arr.warn" class="bad">{{ arr.warn }}</p>
    <p v-if="arr.style" class="mut">{{ arr.style }}</p>
    <p v-if="!roomOk" class="err">{{ t('plan_room_min') }}</p>
    <p v-else-if="selfCross" class="err">{{ t('plan_self_cross') }}</p>
    <svg v-if="roomOk" ref="svg" :viewBox="viewBox" @pointermove="move" @pointerup="up" @pointercancel="up">
      <defs>
        <pattern id="g" width="50" height="50" patternUnits="userSpaceOnUse"><path d="M50 0H0V50" fill="none" stroke="currentColor" stroke-opacity=".12" /></pattern>
        <pattern id="h" width="10" height="10" patternUnits="userSpaceOnUse" patternTransform="rotate(45)"><rect width="10" height="10" fill="currentColor" fill-opacity=".18" /><path d="M0 0V10" stroke="currentColor" stroke-width="3" stroke-opacity=".55" /></pattern>
      </defs>
      <image v-if="bgUrl" :href="bgUrl" :x="bbox.x0" :y="bbox.y0" :width="bw_ * bg.scale / 100" preserveAspectRatio="xMinYMin meet" :opacity="bg.op" style="pointer-events:none" />
      <polygon :points="pts" fill="url(#g)" stroke="currentColor" stroke-width="3" stroke-linejoin="round" />
      <text v-for="(e, i) in edges" :key="'e' + i" :x="e.x" :y="e.y" text-anchor="middle" dominant-baseline="middle" :font-size="fs * 0.85" fill="currentColor" fill-opacity=".7"
            style="pointer-events:none;paint-order:stroke;stroke:var(--surface);stroke-width:4px">{{ t('plan_cm', { n: e.len }) }}</text>
      <g v-for="o in obstacles" :key="o.uid" style="cursor:grab" @pointerdown.prevent="down($event, o)">
        <title>{{ t('plan_obs_' + o.kind) }}</title>
        <rect :x="o.x" :y="o.y" :width="o.w" :height="o.d" fill="url(#h)" stroke="currentColor" stroke-width="2" />
        <text :x="o.x + o.w / 2" :y="o.y + o.d / 2" text-anchor="middle" dominant-baseline="middle" :font-size="fs * 0.8" fill="currentColor"
              style="pointer-events:none;paint-order:stroke;stroke:var(--surface);stroke-width:4px">{{ t('plan_obs_' + o.kind) }}</text>
      </g>
      <g v-for="it in drawn" :key="it.uid" style="cursor:grab" @pointerdown.prevent="down($event, it)">
        <title>{{ pname(it.p.name) }}</title>
        <g :transform="tf(it)" v-html="sym(it)" />
        <rect :x="it.x" :y="it.y" :width="size(it).w" :height="size(it).d" rx="4" fill="transparent" />
        <rect v-if="!ok(it)" :x="it.x" :y="it.y" :width="size(it).w" :height="size(it).d" rx="4" style="fill:var(--bad)" fill-opacity=".45" stroke="var(--bad)" stroke-width="3" />
        <text :x="it.x + size(it).w / 2" :y="it.y + size(it).d / 2" text-anchor="middle" dominant-baseline="middle" :font-size="fs * 0.8"
              style="pointer-events:none;fill:#fff;stroke:#000;stroke-opacity:.6;stroke-width:3px;paint-order:stroke">{{ short(it.p.name) }}</text>
      </g>
      <template v-if="room.shape === 'custom'">
        <circle v-for="(p, i) in custom" :key="'v' + i" :cx="p[0]" :cy="p[1]" :r="hr" fill="var(--acc)" stroke="#fff" stroke-width="2" style="cursor:move"
                @pointerdown.prevent.stop="down($event, null, i)" @dblclick.prevent="removeCorner(i)" />
      </template>
    </svg>
    <p v-if="roomOk" class="mut" style="margin-top:8px">{{ t('plan_area', { a: (area / 10000).toFixed(1) }) }}</p>
    <p v-if="verdict" :class="bad.length ? 'bad' : 'ok'" style="margin-top:12px;display:flex;gap:10px;align-items:center"><Icon :name="bad.length ? 'x' : pct > 50 ? 'alert' : 'check-circle'" :size="20" /><b>{{ verdict }}</b></p>
  </div>
  <div v-if="obstacles.length" class="panel">
    <h2 style="font-size:1.05rem;margin:0 0 8px">{{ t('plan_obs_title') }}</h2>
    <table>
      <tbody>
        <tr v-for="o in obstacles" :key="o.uid">
          <td>{{ t('plan_obs_' + o.kind) }}</td>
          <td><label class="mut">{{ t('plan_obs_w') }}<input v-model.number="o.w" type="number" min="5" max="2000" step="5"></label></td>
          <td><label class="mut">{{ t('plan_obs_d') }}<input v-model.number="o.d" type="number" min="5" max="2000" step="5"></label></td>
          <td><button class="link" @click="removeObstacle(o)">{{ t('plan_delete') }}</button></td>
        </tr>
      </tbody>
    </table>
  </div>
  <div v-if="items.length" class="panel">
    <table>
      <thead><tr><th>{{ t('plan_th_piece') }}</th><th>{{ t('plan_th_verdict') }}</th><th></th></tr></thead>
      <tbody>
        <tr v-for="it in items" :key="it.uid">
          <td>{{ pname(it.p.name) }}<span v-if="it.copy" class="mut"> ({{ t('plan_copy') }})</span><span v-if="it.on" class="mut"> ({{ t('plan_stacked') }})</span><br><span class="mut">{{ t('plan_dims', { w: it.fp.w, d: it.fp.d }) }}{{ it.fp.h ? t('plan_height', { h: it.fp.h }) : '' }}</span></td>
          <td :class="ok(it) ? 'ok' : 'bad'"><Icon :name="ok(it) ? 'check' : 'x'" :size="16" /> {{ reason(it) }}</td>
          <td>
            <button class="sec" @click="turn(it)"><Icon name="rotate" :size="16" />{{ t('plan_rotate') }}</button>
            <input type="color" :value="it.col || colorOf(it.p.color)" :title="t('plan_color')" :aria-label="t('plan_color')" style="width:36px;height:32px;padding:0;border:0;background:none;vertical-align:middle;cursor:pointer" @input="it.col = $event.target.value">
            <button class="link" @click="remove(it)">{{ t('plan_delete') }}</button>
            <button v-if="!it.copy" :disabled="it.p.stock <= 0" @click="addToCart(it.p.id)"><Icon name="cart" :size="16" />{{ t('plan_to_cart') }}</button>
          </td>
        </tr>
      </tbody>
    </table>
    <p class="row" style="align-items:center;flex-wrap:wrap;margin-top:16px">
      <span>{{ t('plan_all_note', { n: toBuyQty, total: ron(toBuyTotal) }) }}</span>
      <button :disabled="!toBuy.length" @click="addAll"><Icon name="cart" :size="18" />{{ t('plan_all') }}</button>
    </p>
    <p v-if="outOfStock" class="mut">{{ t('plan_all_oos', { n: outOfStock }) }}</p>
    <p v-if="bad.length" class="mut">{{ t('plan_all_misfit', { n: bad.length }) }}</p>
  </div>
</template>
