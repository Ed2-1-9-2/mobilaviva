// Aranjare automată a mobilei, pe reguli de amenajare (nu AI): piese mari la perete cu spațiu liber în față,
// masa în centru cu scaunele în jur, scaun de birou la birou, noptiere lângă pat, TV vizavi de canapea etc.
import { rectInPoly, rectInPolyLite, ovl, bboxOf, polyCentroid } from './geo'

const fold = s => String(s || '').normalize('NFD').replace(/\p{M}/gu, '').toLowerCase()

export function classify(name, cat) {
  const n = fold(name)
  if (cat === 'Paturi') return /saltea/.test(n) ? 'mattress' : 'bed'
  if (cat === 'Copii') return /\bpat\b/.test(n) ? 'bed' : /birou/.test(n) ? 'desk' : 'other'
  if (cat === 'Canapele') return /fotoliu/.test(n) ? 'armchair' : 'sofa'
  if (cat === 'Dulapuri') return /bibliotec|vitrin/.test(n) ? 'shelf' : 'wardrobe'
  if (cat === 'Comode') return /noptier/.test(n) ? 'nightstand' : /\btv\b/.test(n) ? 'tv' : 'dresser'
  if (cat === 'Mese') return /masut|living/.test(n) ? 'coffee' : 'table'
  if (cat === 'Scaune') return /birou|gaming|ergonomic/.test(n) ? 'deskchair' : /\bbar\b/.test(n) ? 'barstool' : 'chair'
  if (cat === 'Birouri') return 'desk'
  if (cat === 'Iluminat') return /veioz/.test(n) ? 'tablelamp' : /aplic/.test(n) ? 'sconce' : 'floorlamp'
  if (cat === 'Hol') return 'hall'
  return 'other'
}

// wall: must = obligatoriu lipită de perete; prefer = preferabil. front = spațiu liber (cm) în fața piesei; ring = în jurul piesei.
const P = {
  bed: { wall: 'must', front: 60, side: 40, centered: true, order: 0 },
  mattress: { order: 1 },
  sofa: { wall: 'must', front: 70, centered: true, order: 2 },
  wardrobe: { wall: 'must', front: 80, order: 3 },
  shelf: { wall: 'must', front: 50, order: 7 },
  tv: { wall: 'must', front: 30, order: 5 },
  dresser: { wall: 'must', front: 60, order: 6 },
  desk: { wall: 'must', front: 70, corner: true, order: 4 },
  table: { ring: 60, order: 9 },
  nightstand: { wall: 'prefer', order: 1.5 },
  coffee: { order: 8 },
  armchair: { wall: 'prefer', front: 50, order: 11 },
  deskchair: { order: 13 },
  chair: { order: 12 },
  barstool: { wall: 'prefer', order: 14 },
  floorlamp: { wall: 'prefer', corner: true, order: 15 },
  tablelamp: { order: 1.6 },
  sconce: { wall: 'prefer', order: 1.7 },
  hall: { wall: 'must', front: 50, order: 18 },
  other: { wall: 'prefer', front: 40, order: 19 }
}
// ce roluri au voie să intre în zona liberă a altora (masă + scaune, canapea + măsuță etc.)
const REL = {
  sofa: ['coffee', 'armchair', 'floorlamp'], coffee: ['sofa', 'armchair'], armchair: ['sofa', 'coffee', 'floorlamp'],
  table: ['chair', 'barstool'], desk: ['deskchair', 'chair'], bed: ['nightstand', 'floorlamp', 'tablelamp'], nightstand: ['bed']
}
const related = (a, b) => (REL[a] || []).includes(b) || (REL[b] || []).includes(a)

function tokens(name) {
  const w = String(name || '').split(/[^\p{L}\p{N}]+/u).slice(1)
  return w.filter(t => t.length >= 3 && /^\p{Lu}/u.test(t) && /^\p{L}+$/u.test(t))
}
const shareCollection = (a, b) => { const tb = tokens(b.name); return tokens(a.name).some(t => tb.includes(t)) }

const STYLES = [['classic', /chester|royal|venezia|capitonat|boxspring|clasic/], ['rustic', /rustic|lemn masiv|stejar|\bpin\b|fag/],
  ['loft', /loft|metalic|industrial/], ['modern', /nordic|scandi|modern|halo|studio|minimal|mdf|melamin|\balb\b|\bgri\b/]]
export function styleOf(it) {
  const t = fold([it.name, it.material, it.color, it.brand].join(' '))
  for (const [k, re] of STYLES) if (re.test(t)) return k
  return 'modern'
}
export function styleReport(items) {
  const cnt = {}
  const big = items.filter(i => !/lamp|veioz|aplic|cuier|oglind/.test(fold(i.name)))
  for (const it of big) { const s = styleOf(it); cnt[s] = (cnt[s] || 0) + 1 }
  const e = Object.entries(cnt).sort((a, b) => b[1] - a[1])
  if (!e.length) return null
  const tot = big.length
  if (e.length > 1 && e[1][1] / tot >= 0.3) return { dominant: e[0][0], second: e[1][0] }
  return { dominant: e[0][0], second: null }
}

export function arrange({ items, poly, obstacles }) {
  const B = bboxOf(poly), cen = polyCentroid(poly)
  const RW = B.x1 - B.x0, RH = B.y1 - B.y0
  const obsRaw = obstacles.map(o => ({ x: o.x, y: o.y, w: o.w, d: o.d, kind: o.kind }))
  const isDoor = o => o.kind === 'door' || o.kind === 'balcony'
  const wallObs = obsRaw.filter(o => o.kind === 'wall')   // doar pereții/ieșiturile contează ca „perete”; stâlpii și caloriferele nu
  const doors = obsRaw.filter(isDoor)
  const list = items.map(it => ({ ...it, role: classify(it.name, it.cat) }))
  // scaunele de birou în plus (sau fără birou) stau lângă masă ca scaune obișnuite, nu rătăcite în colț
  { let nDesk = list.filter(i => i.role === 'desk').length
    for (const i of list) if (i.role === 'deskchair') { if (nDesk > 0) nDesk--; else i.role = 'chair' } }
  list.sort((a, b) => P[a.role].order - P[b.role].order || b.w * b.d - a.w * a.d)
  const placed = []
  const result = [], failed = []
  const dist = (a, b) => Math.hypot(a[0] - b[0], a[1] - b[1])
  const ctr = r => [r.x + r.w / 2, r.y + r.d / 2]
  const rectDist = (a, b) => Math.hypot(Math.max(b.x - (a.x + a.w), a.x - (b.x + b.w), 0), Math.max(b.y - (a.y + a.d), a.y - (b.y + b.d), 0))

  const obsFor = f => obsRaw.map(o => isDoor(o) ? { x: o.x - 40 * f, y: o.y - 40 * f, w: o.w + 80 * f, d: o.d + 80 * f } : o)

  function contacts(x, y, w, d) {
    const out = []
    const blocked = (rx, ry, rw, rd) => !rectInPolyLite(rx, ry, rw, rd, poly) || wallObs.some(o => ovl(rx, ry, rw, rd, o.x, o.y, o.w, o.d))
    if (blocked(x, y - 3, w, 3)) out.push({ dx: 0, dy: 1, len: w })      // perete sus  -> fața spre jos
    if (blocked(x, y + d, w, 3)) out.push({ dx: 0, dy: -1, len: w })
    if (blocked(x - 3, y, 3, d)) out.push({ dx: 1, dy: 0, len: d })
    if (blocked(x + w, y, 3, d)) out.push({ dx: -1, dy: 0, len: d })
    return out
  }
  function frontOf(cs, prof, x, y, w, d) {
    if (cs.length) {
      const pick = prof === P.bed ? Math.min : Math.max
      const len = pick(...cs.map(c => c.len))
      return cs.find(c => c.len === len)
    }
    const c = [x + w / 2, y + d / 2], vx = cen[0] - c[0], vy = cen[1] - c[1]
    return Math.abs(vx) > Math.abs(vy) ? { dx: Math.sign(vx) || 1, dy: 0 } : { dx: 0, dy: Math.sign(vy) || 1 }
  }
  function clearsOf(prof, x, y, w, d, s, f) {
    const out = []
    if (prof.ring) { const r = prof.ring * f; if (r > 0) out.push({ x, y: y - r, w, d: r }, { x, y: y + d, w, d: r }, { x: x - r, y, w: r, d }, { x: x + w, y, w: r, d }); return out }
    const F = (prof.front || 0) * f
    if (F > 0) {
      if (s.dy === 1) out.push({ x, y: y + d, w, d: F }); else if (s.dy === -1) out.push({ x, y: y - F, w, d: F })
      else if (s.dx === 1) out.push({ x: x + w, y, w: F, d }); else out.push({ x: x - F, y, w: F, d })
    }
    const S = (prof.side || 0) * f
    if (S > 0) { if (s.dy) out.push({ x: x - S, y, w: S, d }, { x: x + w, y, w: S, d }); else out.push({ x, y: y - S, w, d: S }, { x, y: y + d, w, d: S }) }
    return out
  }
  function freeDepth(x, y, w, d, s, obs) {
    let n = 0
    for (let k = 0; k < 25; k++) {
      const r = s.dy === 1 ? { x, y: y + d + 20 * k, w, d: 20 } : s.dy === -1 ? { x, y: y - 20 * (k + 1), w, d: 20 }
        : s.dx === 1 ? { x: x + w + 20 * k, y, w: 20, d } : { x: x - 20 * (k + 1), y, w: 20, d }
      if (!rectInPolyLite(r.x, r.y, r.w, r.d, poly) || obs.some(o => ovl(r.x, r.y, r.w, r.d, o.x, o.y, o.w, o.d))) break
      n += 20
    }
    return n
  }

  function evalCand(it, role, x, y, w, d, f, obs) {
    const prof = P[role]
    for (const p of placed) if (ovl(x, y, w, d, p.x, p.y, p.w, p.d)) return null
    for (const o of obs) if (ovl(x, y, w, d, o.x, o.y, o.w, o.d)) return null
    if (!rectInPoly(x, y, w, d, poly)) return null
    const cs = contacts(x, y, w, d)
    let back = cs
    if (prof.wall === 'must' && f > 0) {
      // spatele piesei = latura lungă (la pat: latura scurtă, tăblia) lipită de perete
      const want = prof === P.bed ? Math.min(w, d) : Math.max(w, d)
      back = cs.filter(c => c.len === want)
      if (!back.length) return null
    }
    const s = frontOf(back, prof, x, y, w, d)
    const clears = clearsOf(prof, x, y, w, d, s, f)
    for (const c of clears) {
      if (!rectInPolyLite(c.x, c.y, c.w, c.d, poly)) return null
      if (obsRaw.some(o => ovl(c.x, c.y, c.w, c.d, o.x, o.y, o.w, o.d))) return null
      for (const p of placed) if (!related(role, p.role) && ovl(c.x, c.y, c.w, c.d, p.x, p.y, p.w, p.d)) return null
    }
    for (const p of placed) if (!related(role, p.role)) for (const c of p.clears) if (ovl(x, y, w, d, c.x, c.y, c.w, c.d)) return null
    return { x, y, w, d, cs, side: s, clears }
  }

  const firstOf = (role, pred = () => true) => placed.find(p => p.role === role && pred(p))

  function score(it, role, c, obs) {
    const prof = P[role]
    let s = 0
    const k = c.cs.length
    const C = ctr(c)
    if (prof.wall === 'must') s += k ? 40 : -50
    if (prof.wall === 'prefer') s += k ? 20 : 0
    if (prof.corner && k >= 2) s += 25
    if (k && prof.front) s += Math.min(freeDepth(c.x, c.y, c.w, c.d, c.side, obs), 400) * 0.06
    if (prof.centered && k) s -= 0.06 * (c.side.dy ? Math.abs(C[0] - cen[0]) : Math.abs(C[1] - cen[1]))
    if (role === 'bed' && k) s += (c.side.dy ? c.w : c.d) === Math.min(c.w, c.d) ? 40 : -60
    for (const p of placed) if (shareCollection(it, p.it)) s -= Math.min(0.03 * dist(C, ctr(p)), 15)
    const sofa = firstOf('sofa'), bed = firstOf('bed')
    if (role !== 'hall') for (const o of doors) if (rectDist(c, o) < 60) s -= 15   // nu bloca ușile
    switch (role) {
      case 'tv': if (sofa) {
        const S = ctr(sofa), v = [S[0] - C[0], S[1] - C[1]], L = Math.hypot(v[0], v[1]) || 1
        s += 40 * ((c.side.dx * v[0] + c.side.dy * v[1]) / L)
        if (L >= 200 && L <= 450) s += 10
        s -= 0.1 * (sofa.side.dy ? Math.abs(C[0] - S[0]) : Math.abs(C[1] - S[1]))
        if ((c.side.dx * v[0] + c.side.dy * v[1]) / L < 0.3) return -Infinity   // TV-ul trebuie să privească spre canapea
        if ((sofa.side.dx * -v[0] + sofa.side.dy * -v[1]) / L < 0.9) return -Infinity   // și canapeaua spre TV (față în față)
      } break
      case 'nightstand': if (bed) {
        const g = rectDist(c, bed)
        if (g > 30) return -Infinity
        s += g <= 6 ? 60 : -0.3 * g
        if (bed.side.dx === c.side.dx && bed.side.dy === c.side.dy) s += 40
        for (const p of placed) if (p.role === 'nightstand' && dist(C, ctr(p)) < 100) s -= 50
      } break
      case 'coffee': {
        const t = sofa ? (() => { const S = ctr(sofa); return [S[0] + sofa.side.dx * (sofa.w / 2 * Math.abs(sofa.side.dx) + sofa.d / 2 * Math.abs(sofa.side.dy) + 40 + (sofa.side.dx ? c.w : c.d) / 2),
          S[1] + sofa.side.dy * (sofa.d / 2 * Math.abs(sofa.side.dy) + sofa.w / 2 * Math.abs(sofa.side.dx) + 40 + (sofa.side.dy ? c.d : c.w) / 2)] })() : cen
        if (dist(C, t) > 110) return -Infinity   // măsuța stă lângă canapea, nu oriunde în cameră
        s -= 0.5 * dist(C, t)
      } break
      case 'armchair': if (sofa) {
        const S = ctr(sofa), L = dist(C, S)
        if (L >= 120 && L <= 300) s += 30
        const v = [S[0] - C[0], S[1] - C[1]], n = Math.hypot(v[0], v[1]) || 1
        s += 20 * ((c.side.dx * v[0] + c.side.dy * v[1]) / n)
      } break
      case 'table':
        if ((c.w >= c.d) === (RW >= RH)) s += 15
        s -= 0.25 * dist(C, cen)
        break
      case 'chair': {
        const t = firstOf('table')
        if (!t) { s += k ? 15 : 0; s -= 0.03 * dist(C, cen); break }
        const g = rectDist(c, t)
        if (g > 30) return -Infinity
        s += g <= 8 ? 50 : -g
        const T = ctr(t), rx = (C[0] - T[0]) / (t.w / 2), ry = (C[1] - T[1]) / (t.d / 2)
        const side = Math.abs(rx) > Math.abs(ry) ? (rx > 0 ? 'E' : 'W') : (ry > 0 ? 'S' : 'N')
        let same = 0
        for (const p of placed) if (p.role === 'chair') {
          if (dist(C, ctr(p)) < 50) s -= 100
          const P2 = ctr(p), qx = (P2[0] - T[0]) / (t.w / 2), qy = (P2[1] - T[1]) / (t.d / 2)
          if ((Math.abs(qx) > Math.abs(qy) ? (qx > 0 ? 'E' : 'W') : (qy > 0 ? 'S' : 'N')) === side) same++
        }
        s -= 12 * same
        s -= 0.05 * (side === 'N' || side === 'S' ? Math.abs(C[0] - T[0]) : Math.abs(C[1] - T[1]))
      } break
      case 'deskchair': {
        const dk = firstOf('desk')
        if (!dk) { s += k ? 10 : 0; break }
        const D = ctr(dk)
        const t = [D[0] + dk.side.dx * (dk.side.dx ? dk.w / 2 + c.w / 2 : 0), D[1] + dk.side.dy * (dk.side.dy ? dk.d / 2 + c.d / 2 : 0)]
        if (dist(C, t) > 70) return -Infinity
        s -= 0.5 * dist(C, t)
      } break
      case 'floorlamp': {
        const tg = firstOf('sofa') || firstOf('armchair') || bed
        if (tg && rectDist(c, tg) <= 25) s += 30
      } break
      case 'hall': if (doors.length) s -= 0.05 * Math.min(...doors.map(o => dist(C, [o.x + o.w / 2, o.y + o.d / 2]))); break
      default:
    }
    return s
  }

  function stackOn(it, role, target) {
    const w = target.rot ? it.d : it.w, d = target.rot ? it.w : it.d
    const x = target.x + (target.w - w) / 2, y = target.y + (target.d - d) / 2
    result.push({ uid: it.uid, x: Math.round(x), y: Math.round(y), rot: role === 'mattress' ? target.rot : false, on: target.uid })
  }
  const used = new Set()   // piese pe care s-a așezat deja ceva
  const free = (role, uidSet) => placed.find(p => uidSet.includes(p.role) && !used.has(p.uid))

  for (const it of list) {
    const role = it.role
    // piese care se așază PE altă piesă (nu ocupă podea): saltea pe pat, veioză pe noptieră/comodă/birou, aplică deasupra patului
    const host = role === 'mattress' ? free(role, ['bed'])
      : role === 'tablelamp' ? (free(role, ['nightstand']) || free(role, ['dresser', 'tv', 'desk']))
        : role === 'sconce' ? free(role, ['bed']) : null
    if (host) { used.add(host.uid); stackOn(it, role, host); continue }

    let best = null
    for (const f of [1, 0.6, 0.3, 0]) {
      const obs = obsFor(f)
      let bs = -Infinity
      for (const rot of it.round ? [false] : [false, true]) {
        const w = rot ? it.d : it.w, d = rot ? it.w : it.d
        const xs = new Set(), ys = new Set()
        const aX = v => { v = Math.round(v); if (v >= B.x0 && v + w <= B.x1) xs.add(v) }
        const aY = v => { v = Math.round(v); if (v >= B.y0 && v + d <= B.y1) ys.add(v) }
        const STEP = items.length > 40 ? 20 : 10
        for (let x = B.x0; x + w <= B.x1; x += STEP) aX(x)
        for (let y = B.y0; y + d <= B.y1; y += STEP) aY(y)
        poly.forEach(p => { aX(p[0]); aX(p[0] - w); aY(p[1]); aY(p[1] - d) })
        obsRaw.forEach(o => { aX(o.x + o.w); aX(o.x - w); aY(o.y + o.d); aY(o.y - d) })
        placed.forEach(p => { aX(p.x + p.w); aX(p.x - w); aY(p.y + p.d); aY(p.y - d); aX(p.x + (p.w - w) / 2); aY(p.y + (p.d - d) / 2) })
        const tg = role === 'coffee' ? firstOf('sofa') : role === 'chair' ? firstOf('table') : role === 'deskchair' ? firstOf('desk') : role === 'tv' ? firstOf('sofa') : null
        const tg2 = tg || (role === 'nightstand' ? firstOf('bed') : null)
        if (tg2) { const T = ctr(tg2); for (let o = -300; o <= 300; o += 10) { aX(T[0] - w / 2 + o); aY(T[1] - d / 2 + o) } }
        const nearOnly = role === 'coffee' || role === 'chair' || role === 'deskchair' || role === 'nightstand'   // doar piesele care stau lângă „gazdă”
        const near = (v, lo, hi) => !tg2 || !nearOnly || (v >= lo - 280 && v <= hi + 280)
        for (const y of ys) for (const x of xs) {
          if (tg2 && !(near(x, tg2.x, tg2.x + tg2.w) && near(y, tg2.y, tg2.y + tg2.d))) continue
          const c = evalCand(it, role, x, y, w, d, f, obs)
          if (!c) continue
          const sc = score(it, role, c, obs)
          if (sc === -Infinity) continue
          if (sc > bs) { bs = sc; best = { ...c, rot, role, uid: it.uid } }
        }
      }
      if (best) break
    }
    if (!best) { failed.push({ uid: it.uid, name: it.name }); continue }
    placed.push({ uid: it.uid, role, it, x: best.x, y: best.y, w: best.w, d: best.d, rot: best.rot, side: best.side, clears: best.clears })
    result.push({ uid: it.uid, x: best.x, y: best.y, rot: best.rot, on: null, side: best.side ? { dx: best.side.dx || 0, dy: best.side.dy || 0 } : null })
  }
  return { placed: result, failed, style: styleReport(items) }
}
