// Extrage amprenta la sol (lățime x adâncime, cm) din textul de dimensiuni: "220 x 95 x 88 cm", "140-180 x 80 x 75 cm", "Ø 60 cm".
export function footprint(dim) {
  const s = String(dim || '').replace(/,/g, '.')
  const nums = x => (x.match(/\d+(?:\.\d+)?/g) || []).map(Number)
  if (/[Ø⌀]/.test(s)) {
    const d = nums(s)[0]
    return d ? { w: d, d, h: 0, round: true } : null
  }
  const parts = s.replace(/cm/i, '').split(/\s*x\s*/i).map(x => { const n = nums(x); return n.length ? Math.max(...n) : 0 })
  return parts.length >= 2 && parts[0] && parts[1] ? { w: parts[0], d: parts[1], h: parts[2] || 0, round: false } : null
}
