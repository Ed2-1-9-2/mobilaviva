import { reactive } from 'vue'
import { api, bootTake } from './api'
import { t, locale } from './i18n'

export const store = reactive({ user: null, cartCount: 0, compareCount: 0, categories: [], payment: 'test', iban: '', loaded: false, wishIds: [], wishDrops: 0, toast: '' })
let toastTimer
// Confirmare scurtă după acțiunile utilizatorului (adaugă în coș, dorințe, comparator); anunțată și cititoarelor de ecran
export function notify(msg) { store.toast = msg; clearTimeout(toastTimer); toastTimer = setTimeout(() => (store.toast = ''), 2600) }
let wishFor = null   // utilizatorul pentru care e încărcată lista de dorințe

function apply(s) {
  if (!s || typeof s !== 'object') return
  store.user = s.user
  store.cartCount = s.cartCount
  store.compareCount = s.compareCount
  // ordinea categoriilor ca într-un magazin de mobilă (după importanță, nu alfabetic)
  const ORDER = ['Canapele', 'Paturi', 'Dulapuri', 'Comode', 'Mese', 'Scaune', 'Birouri', 'Iluminat', 'Hol', 'Copii']
  const rank = c => (ORDER.indexOf(c) === -1 ? 99 : ORDER.indexOf(c))
  store.categories = [...(s.categories || [])].sort((a, b) => rank(a) - rank(b))
  store.payment = s.payment
  store.iban = s.iban
  store.loaded = true
  const uid = s.user ? s.user.id : null
  if (uid !== wishFor) { wishFor = uid; loadWish() }
}

// Lista de dorințe (personalizare): id-urile produselor salvate + câte au scăzut în preț de la salvare
export async function loadWish() {
  if (!store.user) { store.wishIds = []; store.wishDrops = 0; return }
  try {
    const r = await api('/wishlist')
    store.wishIds = r.ids
    store.wishDrops = r.items.filter(i => i.dropped).length
  } catch { /* ignorat */ }
}
export const isWished = id => store.wishIds.includes(id)
export async function toggleWish(id) {
  const was = isWished(id)
  await api('/wishlist/' + id, { method: was ? 'DELETE' : 'PUT' })
  await loadWish()
  notify(t(was ? 'twoff' : 'twon'))
}

export async function refresh() { apply(await api('/session')) }

// Prima randare folosește sesiunea din HTML (meniul de categorii apare imediat, fără salt de layout).
const s0 = bootTake('/session')
if (s0) apply(s0)

// ?v=2 invalidează imaginile vechi (emoji) rămase în cache-ul browserului
const art = p => `/img/${p.id}.svg?v=4`
export const imgSrc = p => p.imageUrl || art(p)
export function imgFallback(e, p) {
  if (!e.target.src.includes('/img/')) e.target.src = art(p)
}

export const ron = v =>
  (v ?? 0).toLocaleString(locale(), { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + ' RON'

export async function addToCart(id, qty = 1) {
  await api('/cart/items', { method: 'POST', body: { productId: id, qty } })
  await refresh()
  notify(t('tcart'))
}

export async function addToCompare(id) {
  try { await api('/compare/' + id, { method: 'POST' }); notify(t('tcmp')) } catch (e) { alert(e.message) }
  await refresh()
}

export const safeNext = n => (typeof n === 'string' && n.startsWith('/') && !n.startsWith('//') ? n : '/')

// Data estimată (ISO yyyy-MM-dd) în limba activă
export const dateText = iso => (iso ? new Date(iso + 'T12:00:00').toLocaleDateString(locale(), { day: 'numeric', month: 'long', year: 'numeric' }) : '')

// „Văzute recent”: id-urile ultimelor produse deschise, doar în browser (localStorage; fără date pe server)
const RECENT_KEY = 'mv_recent'
export function recentIds() {
  try { return JSON.parse(localStorage.getItem(RECENT_KEY) || '[]').filter(Number.isInteger).slice(0, 12) } catch { return [] }
}
export function pushRecent(id) {
  try { localStorage.setItem(RECENT_KEY, JSON.stringify([id, ...recentIds().filter(x => x !== id)].slice(0, 12))) } catch { /* mod privat: fără istoric */ }
}
