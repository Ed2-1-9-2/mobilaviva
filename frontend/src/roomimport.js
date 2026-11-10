// Import automat de cameră din fișiere de plan / cadastru: DXF, SVG, GeoJSON. Rezultat în cm.
import { polyArea, bboxOf, inPoly, simplifyRing } from './geo'

const MIN_ROOM = 20000   // 2 m² – sub asta nu e cameră
const MAX_OBS = 22500    // 2,25 m² – inelele mici din interior devin obstacole (stâlpi, coloane)

function cleanRing(r, eps = 1e-9) {
  const out = []
  for (const p of r) {
    if (!isFinite(p[0]) || !isFinite(p[1])) continue
    const q = out[out.length - 1]
    if (!q || Math.hypot(p[0] - q[0], p[1] - q[1]) > eps) out.push([p[0], p[1]])
  }
  if (out.length > 1 && Math.hypot(out[0][0] - out[out.length - 1][0], out[0][1] - out[out.length - 1][1]) <= eps) out.pop()
  return out
}

// unește segmente separate (LINE din DXF/SVG) în inele închise
function chain(segs, tol) {
  const rings = []
  const left = segs.slice()
  const eq = (a, b) => Math.hypot(a[0] - b[0], a[1] - b[1]) <= tol
  while (left.length) {
    const s = left.pop()
    const pts = [s[0], s[1]]
    let grew = true
    while (grew) {
      grew = false
      if (eq(pts[0], pts[pts.length - 1]) && pts.length > 3) break
      for (let i = 0; i < left.length; i++) {
        const [a, b] = left[i], end = pts[pts.length - 1], start = pts[0]
        if (eq(a, end)) pts.push(b)
        else if (eq(b, end)) pts.push(a)
        else if (eq(b, start)) pts.unshift(a)
        else if (eq(a, start)) pts.unshift(b)
        else continue
        left.splice(i, 1); grew = true; break
      }
    }
    if (pts.length >= 4 && eq(pts[0], pts[pts.length - 1])) rings.push(pts)
  }
  return rings
}

// ---------- DXF
function parseDxf(text) {
  const L = text.split(/\r?\n/)
  const pairs = []
  for (let i = 0; i + 1 < L.length; i += 2) pairs.push([parseInt(L[i].trim(), 10), L[i + 1].trim()])
  let insunits = 0
  const rings = [], segs = []
  let i = 0
  const num = v => parseFloat(v)
  while (i < pairs.length) {
    const [c, v] = pairs[i]
    if (c === 9 && v === '$INSUNITS' && pairs[i + 1]) { insunits = parseInt(pairs[i + 1][1], 10) || 0; i++; continue }
    if (c === 0 && v === 'LWPOLYLINE') {
      i++
      const pts = []; let closed = false, x = null
      while (i < pairs.length && pairs[i][0] !== 0) {
        const [cc, vv] = pairs[i]
        if (cc === 70) closed = (parseInt(vv, 10) & 1) === 1
        if (cc === 10) x = num(vv)
        if (cc === 20 && x !== null) { pts.push([x, num(vv)]); x = null }
        i++
      }
      if (pts.length >= 3 && (closed || Math.hypot(pts[0][0] - pts[pts.length - 1][0], pts[0][1] - pts[pts.length - 1][1]) < 1e-6 * 1 + 0.5)) rings.push(pts)
      else for (let k = 0; k + 1 < pts.length; k++) segs.push([pts[k], pts[k + 1]])
      continue
    }
    if (c === 0 && v === 'POLYLINE') {
      i++
      let closed = false
      while (i < pairs.length && !(pairs[i][0] === 0)) { if (pairs[i][0] === 70) closed = (parseInt(pairs[i][1], 10) & 1) === 1; i++ }
      const pts = []
      while (i < pairs.length && !(pairs[i][0] === 0 && pairs[i][1] === 'SEQEND')) {
        if (pairs[i][0] === 0 && pairs[i][1] === 'VERTEX') {
          i++; let x = null, y = null
          while (i < pairs.length && pairs[i][0] !== 0) { if (pairs[i][0] === 10) x = num(pairs[i][1]); if (pairs[i][0] === 20) y = num(pairs[i][1]); i++ }
          if (x !== null && y !== null) pts.push([x, y])
        } else i++
      }
      if (pts.length >= 3 && closed) rings.push(pts)
      else for (let k = 0; k + 1 < pts.length; k++) segs.push([pts[k], pts[k + 1]])
      continue
    }
    if (c === 0 && v === 'LINE') {
      i++
      const q = {}
      while (i < pairs.length && pairs[i][0] !== 0) { q[pairs[i][0]] = num(pairs[i][1]); i++ }
      if ([10, 20, 11, 21].every(k => isFinite(q[k]))) segs.push([[q[10], q[20]], [q[11], q[21]]])
      continue
    }
    i++
  }
  const scale = { 1: 2.54, 2: 30.48, 4: 0.1, 5: 1, 6: 100, 3: 160934.4, 7: 100000, 8: 2.54e-6 * 0 + 1 }[insunits]
  return { rings, segs, scale: insunits && scale ? scale : null, flip: true }
}

// ---------- SVG
function parsePathD(d) {
  const toks = d.match(/[a-zA-Z]|-?\d*\.?\d+(?:e[-+]?\d+)?/g) || []
  const rings = [], open = []
  let cur = [], x = 0, y = 0, sx = 0, sy = 0, cmd = '', i = 0
  const n = () => parseFloat(toks[i++])
  const flush = closed => { if (cur.length > 1) (closed ? rings : open).push(cur); cur = [] }
  while (i < toks.length) {
    if (/[a-zA-Z]/.test(toks[i])) cmd = toks[i++]
    const rel = cmd === cmd.toLowerCase()
    const C = cmd.toUpperCase()
    if (C === 'Z') { flush(true); x = sx; y = sy; if (i < toks.length && !/[a-zA-Z]/.test(toks[i])) cmd = 'M'; continue }
    if (C === 'M') { flush(false); const a = n(), b = n(); x = rel ? x + a : a; y = rel ? y + b : b; sx = x; sy = y; cur = [[x, y]]; cmd = rel ? 'l' : 'L'; continue }
    if (C === 'L') { const a = n(), b = n(); x = rel ? x + a : a; y = rel ? y + b : b }
    else if (C === 'H') { const a = n(); x = rel ? x + a : a }
    else if (C === 'V') { const b = n(); y = rel ? y + b : b }
    else if (C === 'C') { n(); n(); n(); n(); const a = n(), b = n(); x = rel ? x + a : a; y = rel ? y + b : b }
    else if (C === 'S' || C === 'Q') { n(); n(); const a = n(), b = n(); x = rel ? x + a : a; y = rel ? y + b : b }
    else if (C === 'T') { const a = n(), b = n(); x = rel ? x + a : a; y = rel ? y + b : b }
    else if (C === 'A') { n(); n(); n(); n(); n(); const a = n(), b = n(); x = rel ? x + a : a; y = rel ? y + b : b }
    else { i++; continue }
    cur.push([x, y])
  }
  flush(false)
  return { rings, open }
}
function parseSvg(text) {
  const rings = [], segs = []
  const attr = (tag, k) => { const m = tag.match(new RegExp('\\b' + k + '\\s*=\\s*["\']([^"\']*)["\']', 'i')); return m ? m[1] : null }
  const tags = text.match(/<(path|polygon|polyline|rect|line)\b[^>]*>/gi) || []
  for (const t of tags) {
    const name = t.match(/^<(\w+)/)[1].toLowerCase()
    if (name === 'path') {
      const r = parsePathD(attr(t, 'd') || '')
      r.rings.forEach(p => rings.push(p))
      r.open.forEach(p => { for (let k = 0; k + 1 < p.length; k++) segs.push([p[k], p[k + 1]]) })
    } else if (name === 'polygon' || name === 'polyline') {
      const nums = (attr(t, 'points') || '').trim().split(/[\s,]+/).map(parseFloat).filter(isFinite)
      const pts = []; for (let k = 0; k + 1 < nums.length; k += 2) pts.push([nums[k], nums[k + 1]])
      if (name === 'polygon') rings.push(pts); else for (let k = 0; k + 1 < pts.length; k++) segs.push([pts[k], pts[k + 1]])
    } else if (name === 'rect') {
      const x = parseFloat(attr(t, 'x') || 0), y = parseFloat(attr(t, 'y') || 0), w = parseFloat(attr(t, 'width')), h = parseFloat(attr(t, 'height'))
      if (w > 0 && h > 0) rings.push([[x, y], [x + w, y], [x + w, y + h], [x, y + h]])
    } else {
      const q = ['x1', 'y1', 'x2', 'y2'].map(k => parseFloat(attr(t, k)))
      if (q.every(isFinite)) segs.push([[q[0], q[1]], [q[2], q[3]]])
    }
  }
  return { rings, segs, scale: null, flip: false }
}

// ---------- GeoJSON
function parseGeoJson(text) {
  let g
  try { g = JSON.parse(text) } catch (e) { throw new Error('fmt') }
  const rings = []
  const walk = o => {
    if (!o) return
    if (o.type === 'FeatureCollection') (o.features || []).forEach(walk)
    else if (o.type === 'Feature') walk(o.geometry)
    else if (o.type === 'GeometryCollection') (o.geometries || []).forEach(walk)
    else if (o.type === 'Polygon') (o.coordinates || []).forEach(r => rings.push(r.map(p => [p[0], p[1]])))
    else if (o.type === 'MultiPolygon') (o.coordinates || []).forEach(pg => pg.forEach(r => rings.push(r.map(p => [p[0], p[1]]))))
    else if (o.type === 'LineString') { if (o.coordinates && o.coordinates.length > 3) rings.push(o.coordinates.map(p => [p[0], p[1]])) }
    else if (o.type === 'MultiLineString') (o.coordinates || []).forEach(r => { if (r.length > 3) rings.push(r.map(p => [p[0], p[1]])) })
  }
  walk(g)
  const all = rings.flat()
  const xs = all.map(p => p[0]), ys = all.map(p => p[1])
  const geographic = all.length && all.every(p => Math.abs(p[0]) <= 180 && Math.abs(p[1]) <= 90) &&
    Math.max(...xs) - Math.min(...xs) < 0.05 && Math.max(...ys) - Math.min(...ys) < 0.05 && Math.min(...xs.map(Math.abs)) >= 1 && Math.min(...ys.map(Math.abs)) >= 1
  if (geographic) {   // grade (WGS84) -> cm
    const lat0 = (Math.min(...ys) + Math.max(...ys)) / 2
    const ky = 111320 * 100, kx = ky * Math.cos(lat0 * Math.PI / 180)
    return { rings: rings.map(r => r.map(p => [p[0] * kx, p[1] * ky])), segs: [], scale: 1, flip: true }
  }
  return { rings, segs: [], scale: null, flip: true, meters: true }
}

export function parseRoomFile(text, ext, unit = 'auto') {
  ext = String(ext || '').toLowerCase().replace('.', '')
  const head = String(text || '').slice(0, 400).trim()
  let r
  if (ext === 'dxf' || /^0\s*\r?\n\s*SECTION/.test(head)) r = parseDxf(text)
  else if (ext === 'svg' || /<svg\b/i.test(String(text).slice(0, 2000))) r = parseSvg(text)
  else if (ext === 'json' || ext === 'geojson' || head[0] === '{') r = parseGeoJson(text)
  else throw new Error('fmt')

  let rings = r.rings.map(p => cleanRing(p)).filter(p => p.length >= 3)
  if (r.segs.length) {
    const sp = r.segs.flat(); const b = bboxOf(sp)
    const tol = Math.max(b.x1 - b.x0, b.y1 - b.y0) * 1e-4 + 1e-9
    rings = rings.concat(chain(r.segs, tol).map(p => cleanRing(p)).filter(p => p.length >= 3))
  }
  if (!rings.length) throw new Error('none')

  // unitate -> cm
  let k
  if (unit === 'm') k = 100; else if (unit === 'cm') k = 1; else if (unit === 'mm') k = 0.1
  else if (r.scale) k = r.scale
  else if (r.meters) k = 100
  else {
    const b = bboxOf(rings.flat()), span = Math.max(b.x1 - b.x0, b.y1 - b.y0)
    k = span < 100 ? 100 : span <= 2500 ? 1 : 0.1   // <100 → metri, ≤2500 → cm, altfel mm
  }
  rings = rings.map(p => p.map(q => [q[0] * k, (r.flip ? -q[1] : q[1]) * k]))
  rings = rings.map(p => simplifyRing(cleanRing(p, 0.5), 5)).filter(p => p.length >= 3 && polyArea(p) > 100)
  if (!rings.length) throw new Error('none')

  // camere = inele mari care nu sunt în alt inel mare
  const big = rings.map((p, i) => i).filter(i => polyArea(rings[i]) >= MIN_ROOM)
  const inside = (a, b) => rings[a].every(p => inPoly(p, rings[b]))
  let rooms = big.filter(i => !big.some(j => j !== i && polyArea(rings[j]) > polyArea(rings[i]) && inside(i, j)))
  if (!rooms.length) rooms = [rings.map((p, i) => i).sort((a, b) => polyArea(rings[b]) - polyArea(rings[a]))[0]]
  rooms.sort((a, b) => polyArea(rings[b]) - polyArea(rings[a]))
  return { rings, rooms }
}

export function pickRoom(rings, idx) {
  const ring = rings[idx]
  if (!ring) throw new Error('none')
  const b = bboxOf(ring)
  const sh = p => [Math.round(p[0] - b.x0), Math.round(p[1] - b.y0)]
  let poly = ring.map(sh)
  // orientare constantă (ca în editor)
  const obstacles = []
  rings.forEach((p, i) => {
    if (i === idx || polyArea(p) > MAX_OBS) return
    if (!p.every(q => inPoly(q, ring))) return
    const bb = bboxOf(p)
    const w = Math.max(10, Math.round(bb.x1 - bb.x0)), d = Math.max(10, Math.round(bb.y1 - bb.y0))
    obstacles.push({ kind: 'support', x: Math.round(bb.x0 - b.x0), y: Math.round(bb.y0 - b.y0), w, d })
  })
  return { poly, obstacles, area: polyArea(poly) }
}
