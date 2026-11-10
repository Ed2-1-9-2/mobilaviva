// „Văzute recent”: ultimele produse deschise, păstrate local în browser (fără cont, fără cookie-uri noi pe server)
const KEY = 'mv_recent'
export function recentIds() {
  try {
    const a = JSON.parse(localStorage.getItem(KEY) || '[]')
    return Array.isArray(a) ? a.filter(Number.isInteger).slice(0, 8) : []
  } catch { return [] }
}
export function remember(id) {
  try { localStorage.setItem(KEY, JSON.stringify([id, ...recentIds().filter(x => x !== id)].slice(0, 8))) } catch { /* stocare indisponibilă */ }
}
