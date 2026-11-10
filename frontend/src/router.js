import { createRouter, createWebHistory, START_LOCATION } from 'vue-router'
import { watch } from 'vue'
import { i18n, setLang } from './i18n'
import { localizePath, localizeLocation, pathVariants, detectLang } from './urlkeys'
import { store, refresh } from './store'
import Home from './views/Home.vue'         // start și catalog în pachetul inițial (fără cerere JS suplimentară)
import Catalog from './views/Catalog.vue'

const RAW = [
  { path: '/', component: Home },
  { path: '/catalog', component: Catalog },
  { path: '/product/:id', component: () => import('./views/Product.vue') },
  { path: '/cart', component: () => import('./views/Cart.vue') },
  { path: '/checkout', component: () => import('./views/Checkout.vue'), meta: { auth: true } },
  { path: '/checkout/success', component: () => import('./views/CheckoutSuccess.vue'), meta: { auth: true } },
  { path: '/checkout/cancel', redirect: '/checkout?cancelled=1' },
  { path: '/login', component: () => import('./views/Login.vue') },
  { path: '/register', component: () => import('./views/Register.vue') },
  { path: '/forgot', component: () => import('./views/Forgot.vue') },
  { path: '/reset', component: () => import('./views/Reset.vue') },
  { path: '/account', component: () => import('./views/Account.vue'), meta: { auth: true } },
  { path: '/compare', component: () => import('./views/Compare.vue') },
  { path: '/planner', component: () => import('./views/Planner.vue') },
  { path: '/wishlist', component: () => import('./views/Wishlist.vue'), meta: { auth: true } },
  { path: '/termeni', component: () => import('./views/Legal.vue'), meta: { page: 'termeni' } },
  { path: '/confidentialitate', component: () => import('./views/Legal.vue'), meta: { page: 'confidentialitate' } },
  { path: '/retur', component: () => import('./views/Legal.vue'), meta: { page: 'retur' } },
  { path: '/orders', component: () => import('./views/Orders.vue'), meta: { auth: true } },
  { path: '/order/:id', component: () => import('./views/Order.vue'), meta: { auth: true } },
  { path: '/invoice/:id', component: () => import('./views/Invoice.vue'), meta: { auth: true } },
  { path: '/admin', component: () => import('./views/Admin.vue'), meta: { auth: true, admin: true } },
  { path: '/dovada-plata', component: () => import('./views/DovadaPlata.vue'), meta: { auth: true, admin: true } },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]
// fiecare rută primește ca alias căile traduse (/catalogo, /warenkorb, /produs/:id ...)
const ROUTES = RAW.map(r => { const a = pathVariants(r.path); return a.length ? { ...r, alias: a } : r })

const router = createRouter({ history: createWebHistory(), routes: ROUTES, scrollBehavior: () => ({ top: 0 }) })

router.beforeEach(async (to, from) => {
  if (to.path === '/' && Object.keys(to.query).length) return { path: '/catalog', query: to.query, replace: true }   // adrese vechi: /?category=...
  if (from === START_LOCATION) { const l = detectLang(to.path); if (l) setLang(l) }   // link primit în altă limbă: se trece pe acea limbă
  const loc = localizeLocation(to.path, to.query)                                      // adresa mereu în limba activă (cale + categorie/sortare)
  if (loc.path !== to.path || JSON.stringify(loc.query) !== JSON.stringify(to.query)) return { ...loc, hash: to.hash, replace: true }
  if ((to.meta.auth || to.meta.admin) && !store.loaded) await refresh().catch(() => {})
  if (to.meta.auth && !store.user) return { path: '/login', query: { next: to.fullPath } }
  if (to.meta.admin && !store.user?.admin) return '/'
})

// linkurile din interfață (RouterLink) au href-ul direct în limba activă, fără să fie rescrise fiecare
const baseResolve = router.resolve.bind(router)
router.resolve = (to, cur) => {
  if (typeof to === 'string' && to[0] === '/') { const m = to.match(/^[^?#]*/)[0]; to = localizePath(m) + to.slice(m.length) }
  else if (to && typeof to === 'object' && typeof to.path === 'string' && to.path[0] === '/') to = { ...to, ...localizeLocation(to.path, to.query || {}) }
  return baseResolve(to, cur)
}
// la schimbarea limbii se rescrie adresa curentă
watch(() => i18n.lang, () => { const r = router.currentRoute.value; router.replace({ ...localizeLocation(r.path, r.query), hash: r.hash }) })

export default router
