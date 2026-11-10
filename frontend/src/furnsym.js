// Simboluri de mobilier văzut de sus pentru planificator. Se desenează în caseta W x D (cm), cu „fața” piesei în jos (+y);
// rotirea (4 direcții) o face planificatorul prin transform.
const fold = s => String(s || '').normalize('NFD').replace(/\p{M}/gu, '').toLowerCase()
const MAP = [['antracit', '#3a3d42'], ['galben', '#e0b43a'], ['albastru petrol', '#2f6f7e'], ['albastru', '#3b6ea5'], ['gri', '#8b9099'], ['maro', '#7a4b2a'],
  ['crem', '#eadfc8'], ['bej', '#cfbb9a'], ['stejar', '#b88a55'], ['negru', '#2b2b2e'], ['alb', '#f1f1ee'], ['nuc', '#6b4a33'], ['auriu', '#c9a24a'],
  ['verde', '#4f7a4a'], ['rosu', '#a8392f']]
export function colorOf(name) {
  const n = fold(name).split('+')[0]
  for (const [k, v] of MAP) if (n.includes(k)) return v
  return '#9e9e9e'
}
export function shade(hex, f) {
  const v = parseInt(String(hex).slice(1), 16)
  const c = [(v >> 16) & 255, (v >> 8) & 255, v & 255].map(x => Math.round(f < 0 ? x * (1 + f) : x + (255 - x) * f))
  return '#' + c.map(x => x.toString(16).padStart(2, '0')).join('')
}
const n1 = x => Math.round(x * 10) / 10
const R = (x, y, w, h, rx, fill, ex = '') => `<rect x="${n1(x)}" y="${n1(y)}" width="${n1(Math.max(0, w))}" height="${n1(Math.max(0, h))}" rx="${n1(rx)}" fill="${fill}" ${ex}/>`
const E = (cx, cy, rx, ry, fill, ex = '') => `<ellipse cx="${n1(cx)}" cy="${n1(cy)}" rx="${n1(Math.max(0, rx))}" ry="${n1(Math.max(0, ry))}" fill="${fill}" ${ex}/>`
const L = (x1, y1, x2, y2, col, w = 1, op = 1) => `<line x1="${n1(x1)}" y1="${n1(y1)}" x2="${n1(x2)}" y2="${n1(y2)}" stroke="${col}" stroke-width="${w}" stroke-opacity="${op}" stroke-linecap="round"/>`
const OUT = 'stroke="#000" stroke-opacity=".5" stroke-width="1.5"'
const HI = '#fff'

export function symbolSvg(role, W, D, round, col, name) {
  const d = shade(col, -0.3), dd = shade(col, -0.5), l = shade(col, 0.25), s = Math.min(W, D)
  const nm = fold(name)
  let o = ''
  if (round && !['floorlamp', 'tablelamp', 'barstool', 'deskchair'].includes(role)) {
    const cx = W / 2, cy = D / 2
    if (role === 'coffee') return E(cx, cy, W / 2, D / 2, shade(col, 0.35), `fill-opacity=".8" ${OUT}`) + E(cx, cy, W / 2 - 4, D / 2 - 4, 'none', `stroke="${HI}" stroke-opacity=".6" stroke-width="1.5"`) + E(cx - W * .15, cy - D * .15, W * .12, D * .06, HI, 'fill-opacity=".45"')
    if (role === 'chair' || role === 'other') {
      o = E(cx, cy, W / 2, D / 2, col, OUT) + E(cx, cy, W * .36, D * .36, l, 'fill-opacity=".8"')
      for (let k = 0; k < 14; k++) o += E(cx + Math.cos(k * 2.4) * W * .22 * ((k % 3) / 2 + .3), cy + Math.sin(k * 2.4) * D * .22 * ((k % 3) / 2 + .3), 1.6, 1.6, HI, 'fill-opacity=".3"')
      return o
    }
    o = E(cx, cy, W / 2, D / 2, col, OUT) + E(cx, cy, W * .44, D * .44, 'none', `stroke="${dd}" stroke-opacity=".5"`)
    for (let k = -2; k <= 2; k++) o += L(cx - W * .4, cy + k * D * .12, cx + W * .4, cy + k * D * .12, dd, 1, .25)
    return o + E(cx - W * .15, cy - D * .18, W * .16, D * .07, HI, 'fill-opacity=".22"')
  }
  switch (role) {
    case 'sofa': case 'armchair': {
      const arm = Math.max(9, W * (role === 'sofa' ? 0.085 : 0.15)), back = Math.max(12, D * 0.28)
      o += R(0, 0, W, D, 7, d, OUT) + R(2, 2, W - 4, back, 6, shade(col, -0.12)) + R(0, 0, arm, D, 6, d, OUT) + R(W - arm, 0, arm, D, 6, d, OUT)
      const n = role === 'armchair' ? 1 : W > 230 ? 3 : W > 140 ? 2 : 1, cw = (W - 2 * arm - 2) / n
      for (let i = 0; i < n; i++) o += R(arm + 1 + i * cw + 1, back + 1, cw - 2, D - back - 4, 5, l, `stroke="#000" stroke-opacity=".25"`) + R(arm + 5 + i * cw, back + 4, cw - 10, 3, 2, HI, 'fill-opacity=".3"')
      if (role === 'sofa' && W > 130) o += R(arm + 4, back + 2, Math.min(26, cw * .3), Math.min(26, D * .28), 4, shade(col, .45), 'fill-opacity=".9" transform="rotate(-8 ' + n1(arm + 14) + ' ' + n1(back + 14) + ')"')
      return o
    }
    case 'bed': case 'mattress': {
      const hb = role === 'bed' ? Math.max(5, D * 0.045) : 0
      if (role === 'bed') o += R(0, 0, W, D, 4, dd, OUT)
      o += R(role === 'bed' ? 3 : 0, hb, role === 'bed' ? W - 6 : W, D - hb - (role === 'bed' ? 3 : 0), 3, '#f3efe6', role === 'bed' ? '' : OUT)
      const two = W > 120, pw = two ? (W - 18) / 2 : W - 16, ph = Math.max(14, D * 0.13)
      for (let i = 0; i < (two ? 2 : 1); i++) o += R(7 + i * (pw + 4), hb + 6, pw, ph, 6, '#fff', 'stroke="#000" stroke-opacity=".25"')
      if (role === 'bed') o += R(3, hb + ph + 14, W - 6, D - hb - ph - 17, 3, shade(col, 0.15), 'stroke="#000" stroke-opacity=".3"') + R(3, hb + ph + 14, W - 6, Math.max(6, D * .05), 2, shade(col, 0.4), 'fill-opacity=".9"')
      else for (let k = 1; k < 5; k++) o += L(0, D * k / 5, W, D * k / 5, '#000', 1, .12)
      return o
    }
    case 'nightstand': return R(0, 0, W, D, 3, col, OUT) + R(3, 3, W - 6, D - 6, 2, 'none', 'stroke="#000" stroke-opacity=".3"') + R(W * .35, D - 6, W * .3, 3, 1.5, '#e8e8e8')
    case 'wardrobe': {
      o = R(0, 0, W, D, 2, col, OUT) + R(2, 2, W - 4, D - 4, 1, 'none', 'stroke="#000" stroke-opacity=".25"')
      const n = Math.max(2, Math.round(W / 55))
      for (let i = 1; i < n; i++) o += L(W * i / n, D * .55, W * i / n, D, '#000', 1.2, .45)
      for (let i = 0; i < n; i++) { const x = W * (i + .5) / n; o += R(x - 1, D - 9, 2, 6, 1, '#e8e8e8') }
      return o + L(4, D * .3, W - 4, D * .3, HI, 1, .12)
    }
    case 'shelf': {
      o = R(0, 0, W, D, 2, col, OUT) + R(2, 2, W - 4, D - 4, 1, shade(col, -0.25))
      const n = Math.max(3, Math.round(W / 14)), cols = ['#b5493a', '#2f6b8a', '#d6a23f', '#56803c', '#e6dfd0']
      for (let i = 0; i < n; i++) o += R(3 + i * (W - 6) / n, 3, (W - 6) / n - 1, D - 8, 0.8, cols[i % 5], 'fill-opacity=".85"')
      return o
    }
    case 'tv': {
      o = R(0, 0, W, D, 2, col, OUT) + R(3, 3, W - 6, D - 6, 1, 'none', 'stroke="#000" stroke-opacity=".25"')
      o += R(W * .2, D * .22, W * .6, Math.max(3, D * .09), 1.5, '#0e1116') + R(W * .44, D * .3, W * .12, D * .12, 1, '#222') + R(W * .22, D * .24, W * .56, 1.4, 1, '#5b7fb0', 'fill-opacity=".7"')
      for (let i = 0; i < 3; i++) o += R(W * (.1 + i * .3), D - 6, W * .2, 2.5, 1, '#e8e8e8', 'fill-opacity=".8"')
      return o
    }
    case 'dresser': {
      o = R(0, 0, W, D, 3, col, OUT) + R(3, 3, W - 6, D - 6, 2, 'none', 'stroke="#000" stroke-opacity=".28"')
      const n = W > 100 ? 3 : 2
      for (let i = 0; i < n; i++) o += E(W * (i + .5) / n, D - 6, 2.2, 2.2, '#e8e8e8')
      return o + L(5, D * .35, W - 5, D * .35, HI, 1, .14)
    }
    case 'desk': {
      o = R(0, 0, W, D, 3, col, OUT) + R(3, 3, W - 6, D - 6, 2, 'none', 'stroke="#000" stroke-opacity=".2"')
      o += R(W * .5 - W * .13, D * .1, W * .26, Math.max(3, D * .06), 1.5, '#0e1116') + R(W * .5 - 3, D * .16, 6, D * .08, 1, '#333')
      o += R(W * .5 - W * .1, D * .42, W * .2, D * .13, 2, '#26272b') + E(W * .5 + W * .17, D * .5, 3.5, 5, '#26272b')
      return o + R(W * .06, D * .1, 8, 8, 4, '#e8e2d4', 'fill-opacity=".9"')
    }
    case 'table': {
      o = R(0, 0, W, D, 6, col, OUT)
      const n = Math.max(3, Math.round(Math.min(W, D) / 14))
      if (W >= D) for (let i = 1; i < n; i++) o += L(4, D * i / n, W - 4, D * i / n, '#000', 1, .16)
      else for (let i = 1; i < n; i++) o += L(W * i / n, 4, W * i / n, D - 4, '#000', 1, .16)
      return o + R(3, 3, W - 6, D - 6, 4, 'none', `stroke="${HI}" stroke-opacity=".3"`)
    }
    case 'coffee':
      return R(0, 0, W, D, 5, shade(col, 0.35), `fill-opacity=".85" ${OUT}`) + R(3, 3, W - 6, D - 6, 3, 'none', `stroke="${HI}" stroke-opacity=".6" stroke-width="1.5"`) + L(W * .15, D * .25, W * .45, D * .1, HI, 2, .5)
    case 'chair':
      return R(W * .06, D * .2, W * .88, D * .76, 6, l, OUT) + R(W * .03, D * .02, W * .94, D * .2, 5, d, OUT) + R(W * .16, D * .3, W * .68, D * .5, 4, col, 'fill-opacity=".55"')
    case 'deskchair': {
      const cx = W / 2, cy = D * .56, r = s * .36
      for (let k = 0; k < 5; k++) o += L(cx, cy, cx + Math.cos(k * 1.2566 + .6) * s * .48, cy + Math.sin(k * 1.2566 + .6) * s * .48, '#222', 2.2, .7)
      o += R(W * .12, D * .04, W * .76, D * .2, 7, dd, OUT) + E(cx, cy, r, r, l, OUT) + E(cx - r * .25, cy - r * .3, r * .5, r * .3, HI, 'fill-opacity=".25"')
      return o + R(W * .02, D * .3, 5, D * .26, 2, '#1a1a1d') + R(W - W * .02 - 5, D * .3, 5, D * .26, 2, '#1a1a1d')
    }
    case 'barstool': return E(W / 2, D / 2, W / 2, D / 2, '#2a2a2e', OUT) + E(W / 2, D / 2, W * .4, D * .4, col) + E(W / 2 - W * .1, D / 2 - D * .12, W * .16, D * .1, HI, 'fill-opacity=".25"')
    case 'floorlamp': case 'tablelamp': {
      const cx = W / 2, cy = D / 2, r = s / 2
      return E(cx, cy, r, r, '#ffe9a8', 'fill-opacity=".35"') + E(cx, cy, r * .7, r * .7, '#fff3c4', `fill-opacity=".8" stroke="#000" stroke-opacity=".4" stroke-width="1.2"`) + E(cx, cy, r * .22, r * .22, col)
    }
    case 'sconce': return R(0, 0, W, D, 3, col, OUT) + E(W / 2, D * .6, Math.min(W, D) * .25, Math.min(W, D) * .25, '#ffe9a8')
    case 'hall': {
      if (/oglind/.test(nm)) return R(0, 0, W, D, 2, '#2b2b2e', OUT) + R(2, 2, W - 4, D - 4, 1, '#bcd3dc') + L(W * .2, D * .8, W * .5, D * .2, HI, 2, .6)
      o = R(0, 0, W, D, 3, col, OUT)
      if (/cuier/.test(nm)) for (let i = 0; i < 5; i++) o += E(W * (i + .5) / 5, D / 2, Math.min(3, D / 3), Math.min(3, D / 3), '#e8e8e8')
      else for (let i = 1; i < 4; i++) o += L(W * i / 4, 3, W * i / 4, D - 3, '#000', 1, .2)
      return o + R(3, 3, W - 6, D - 6, 2, 'none', `stroke="${HI}" stroke-opacity=".25"`)
    }
    default: return R(0, 0, W, D, 4, col, OUT) + R(3, 3, W - 6, D - 6, 3, 'none', `stroke="${HI}" stroke-opacity=".3"`)
  }
}
