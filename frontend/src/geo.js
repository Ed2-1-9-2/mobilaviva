// Geometrie 2D pentru planificator (cm). Poligoane = liste de [x, y].
export const ori = (a, b, c) => Math.sign((b[0] - a[0]) * (c[1] - a[1]) - (b[1] - a[1]) * (c[0] - a[0]))
export const cross = (a, b, c, d) => ori(a, b, c) * ori(a, b, d) < 0 && ori(c, d, a) * ori(c, d, b) < 0
export function onSeg(p, a, b) {
  if (Math.abs((b[0] - a[0]) * (p[1] - a[1]) - (b[1] - a[1]) * (p[0] - a[0])) > 1e-6) return false
  return Math.min(a[0], b[0]) <= p[0] && p[0] <= Math.max(a[0], b[0]) && Math.min(a[1], b[1]) <= p[1] && p[1] <= Math.max(a[1], b[1])
}
export function inPoly(p, P) {
  let c = false
  for (let i = 0, j = P.length - 1; i < P.length; j = i++) {
    const a = P[i], b = P[j]
    if (onSeg(p, a, b)) return true
    if ((a[1] > p[1]) !== (b[1] > p[1]) && p[0] < (b[0] - a[0]) * (p[1] - a[1]) / (b[1] - a[1]) + a[0]) c = !c
  }
  return c
}
export function rectInPoly(x, y, w, d, P) {
  if (P.length < 3) return false
  const cs = [[x, y], [x + w, y], [x + w, y + d], [x, y + d]]
  if (!cs.every(c => inPoly(c, P))) return false
  if (P.some(v => v[0] > x && v[0] < x + w && v[1] > y && v[1] < y + d)) return false
  for (let i = 0; i < P.length; i++) {
    const a = P[i], b = P[(i + 1) % P.length]
    for (let k = 0; k < 4; k++) if (cross(a, b, cs[k], cs[(k + 1) % 4])) return false
  }
  for (let i = 0; i <= 6; i++) for (let j = 0; j <= 6; j++) if (!inPoly([x + w * i / 6, y + d * j / 6], P)) return false
  return true
}
// variantă rapidă (colțuri, mijloace de laturi, centru) pentru zonele libere din fața pieselor
export function rectInPolyLite(x, y, w, d, P) {
  const xs = [x, x + w / 2, x + w], ys = [y, y + d / 2, y + d]
  for (const a of xs) for (const b of ys) if (!inPoly([a, b], P)) return false
  return !P.some(v => v[0] > x && v[0] < x + w && v[1] > y && v[1] < y + d)
}
export const ovl = (ax, ay, aw, ad, bx, by, bw, bd) => ax < bx + bw && bx < ax + aw && ay < by + bd && by < ay + ad
export function polyArea(P) {
  let s = 0
  for (let i = 0; i < P.length; i++) { const a = P[i], b = P[(i + 1) % P.length]; s += a[0] * b[1] - b[0] * a[1] }
  return Math.abs(s) / 2
}
export function bboxOf(P) {
  const xs = P.map(p => p[0]), ys = P.map(p => p[1])
  return { x0: Math.min(...xs), y0: Math.min(...ys), x1: Math.max(...xs), y1: Math.max(...ys) }
}
export function polyCentroid(P) {
  let a = 0, cx = 0, cy = 0
  for (let i = 0; i < P.length; i++) {
    const p = P[i], q = P[(i + 1) % P.length], f = p[0] * q[1] - q[0] * p[1]
    a += f; cx += (p[0] + q[0]) * f; cy += (p[1] + q[1]) * f
  }
  if (Math.abs(a) < 1e-6) { const b = bboxOf(P); return [(b.x0 + b.x1) / 2, (b.y0 + b.y1) / 2] }
  return [cx / (3 * a), cy / (3 * a)]
}
const distSeg = (p, a, b) => {
  const dx = b[0] - a[0], dy = b[1] - a[1], l2 = dx * dx + dy * dy
  const t = l2 ? Math.max(0, Math.min(1, ((p[0] - a[0]) * dx + (p[1] - a[1]) * dy) / l2)) : 0
  return Math.hypot(p[0] - a[0] - t * dx, p[1] - a[1] - t * dy)
}
// elimină colțurile aproape coliniare / mai mici decât toleranța (cm)
export function simplifyRing(P, tol) {
  let R = P.map(p => [p[0], p[1]])
  if (R.length > 1500) { const k = Math.ceil(R.length / 1500); R = R.filter((_, i) => i % k === 0) }
  let changed = true
  while (changed && R.length > 3) {
    changed = false
    let best = -1, bd = tol
    for (let i = 0; i < R.length; i++) {
      const d = distSeg(R[i], R[(i + R.length - 1) % R.length], R[(i + 1) % R.length])
      if (d < bd) { bd = d; best = i }
    }
    if (best >= 0) { R.splice(best, 1); changed = true }
  }
  return R
}
