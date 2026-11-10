// Căutări speciale: bara de căutare înțelege fraze naturale („canapea gri sub 3000 lei, în stoc”)
// și le transformă în filtre existente ale catalogului (preț, stoc, oferte, sortare).
const strip = s => [...s].map(ch => ch.normalize('NFD')[0]).join('').toLowerCase()   // fără diacritice, aceeași lungime
const NUM = '(\\d{1,3}(?:[. ]\\d{3})+|\\d+)'
const toNum = x => Number(String(x).replace(/[. ]/g, ''))

export function parseQuery(raw) {
  let text = String(raw || '')
  let low = strip(text)
  const f = {}
  const cut = (re, fn) => {
    const m = re.exec(low)
    if (!m) return false
    fn(m)
    low = low.slice(0, m.index) + ' ' + low.slice(m.index + m[0].length)
    text = text.slice(0, m.index) + ' ' + text.slice(m.index + m[0].length)
    return true
  }
  cut(new RegExp('\\bintre ' + NUM + ' ?(?:lei|ron)? ?(?:si|-) ?' + NUM + ' ?(?:lei|ron)?\\b'), m => { f.min_price = toNum(m[1]); f.max_price = toNum(m[2]) })
  cut(new RegExp('\\b(?:sub|pana la|maxim|max|cel mult|mai ieftin de) ' + NUM + ' ?(?:lei|ron)?\\b'), m => { f.max_price = toNum(m[1]) })
  cut(new RegExp('\\b(?:peste|minim|min|cel putin|mai scump de) ' + NUM + ' ?(?:lei|ron)?\\b'), m => { f.min_price = toNum(m[1]) })
  if (cut(/\b(?:in stoc|disponibil[ae]?)\b/, () => {})) f.in_stock = 1
  if (cut(/\b(?:la oferta|in oferta|oferte|oferta|la reducere|reducere|reduceri|promotie|promotii)\b/, () => {})) f.promo = 1
  const cheap = cut(/\b(?:cele mai ieftine|cel mai ieftin|ieftine|ieftin)\b/, () => {})
  if (cut(/\b(?:livrare rapida|livrare repede|livrat repede)\b/, () => {})) f.sort = 'delivery'
  else if (cheap) f.sort = 'price_asc'
  const q = text.replace(/[,;]+/g, ' ').replace(/\s+/g, ' ').trim()
  return { q, filters: f }
}

const ron = n => Number(n).toLocaleString('ro-RO') + ' RON'
// Etichete pentru filtrele active (afișate ca „chips” în catalog)
export function chipsFor(query) {
  const out = []
  if (query.min_price) out.push({ key: 'min_price', label: 'Preț minim ' + ron(query.min_price) })
  if (query.max_price) out.push({ key: 'max_price', label: 'Preț maxim ' + ron(query.max_price) })
  if (query.in_stock === '1') out.push({ key: 'in_stock', label: 'În stoc' })
  if (query.promo === '1') out.push({ key: 'promo', label: 'Doar oferte' })
  if (query.sort === 'price_asc') out.push({ key: 'sort', label: 'Cele mai ieftine întâi' })
  if (query.sort === 'delivery') out.push({ key: 'sort', label: 'Livrare rapidă' })
  return out
}
