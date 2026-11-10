<script setup>
import { ref, reactive, computed, watch, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../api'
import { t, catName, locale } from '../i18n'
import { catUrl, catKey } from '../urlkeys'
import { pname, pfield } from '../pname'
import Icon from '../components/Icon.vue'
import { setLd, crumbsLd, productLd } from '../jsonld'
import ProductCard from '../components/ProductCard.vue'
import { ron, addToCart, addToCompare, imgSrc, imgFallback, store, isWished, toggleWish, recentIds, pushRecent } from '../store'

const route = useRoute()
const router = useRouter()
const p = ref(null)
const qty = ref(1)
// Țara se afișează în limba activă (Intl); metoda de producție vine din cheia prod_m_* (text liber, dacă nu există cheie)
const originText = computed(() => {
  const x = p.value
  if (!x) return ''
  let c = x.originCountry || ''
  try { if (c) c = new Intl.DisplayNames([locale()], { type: 'region' }).of(c) || c } catch { /* cod necunoscut: rămâne codul */ }
  return [x.originPlace, c].filter(Boolean).join(', ')
})
const methodText = computed(() => {
  const k = p.value && p.value.production
  if (!k) return ''
  const s = t('prod_m_' + k)
  return s === 'prod_m_' + k ? k : s
})
// Recenzii reale (personalizare): doar clienții care au comandat produsul pot scrie una
const rv = ref({ items: [], count: 0, avg: 0, canReview: false, mine: null })
const form = reactive({ stars: 5, text: '' })
const rvErr = ref('')
watch([p, rv], ([v, r]) => {
  if (!v) return
  setLd('product', productLd(v, route.path, r))
  setLd('crumbs', crumbsLd([{ name: t('prod_home'), path: '/' }, { name: t('prod_catalog'), path: '/catalog' }, { name: catName(v.category), path: '/catalog?category=' + encodeURIComponent(v.category) }, { name: v.name, path: route.path }]))
})
onUnmounted(() => { setLd('product', null); setLd('crumbs', null) })
api('/products/' + route.params.id).then(x => (p.value = x)).catch(() => router.replace('/'))

// Set asortat „completează camera”: aceeași colecție + categorii complementare
const match = ref(null)
api('/products/' + route.params.id + '/matching').then(x => (match.value = x)).catch(() => {})
const setItems = computed(() => (match.value ? match.value.items.map(i => i.product).filter(x => x.stock > 0) : []))
const setTotal = computed(() => (p.value && p.value.stock > 0 ? p.value.price : 0) + setItems.value.reduce((s, x) => s + x.price, 0))
async function addSet() {
  if (p.value.stock > 0) await addToCart(p.value.id)
  for (const x of setItems.value) await addToCart(x.id)
  router.push('/cart')
}
async function wish() {
  if (!store.user) return router.push({ path: '/login', query: { next: route.fullPath } })
  await toggleWish(p.value.id)
}

async function add() {
  await addToCart(p.value.id, qty.value)
  router.push('/cart')
}
const rate = r => (r ?? 0).toLocaleString(locale(), { minimumFractionDigits: 1, maximumFractionDigits: 1 })
const stars = n => '★'.repeat(n) + '☆'.repeat(5 - n)
function setReviews(x) {
  rv.value = x
  form.stars = x.mine ? x.mine.stars : 5
  form.text = x.mine ? x.mine.text : ''
}
async function loadReviews() { try { setReviews(await api('/products/' + route.params.id + '/reviews')) } catch { /* fără recenzii */ } }
async function afterReview(x) { setReviews(x); try { p.value = await api('/products/' + route.params.id) } catch { /* ratingul se actualizează la următoarea încărcare */ } }
async function sendReview() {
  rvErr.value = ''
  try { await afterReview(await api('/products/' + route.params.id + '/reviews', { method: 'POST', body: { stars: form.stars, text: form.text } })) } catch (e) { rvErr.value = e.message }
}
async function delReview() {
  rvErr.value = ''
  try { await afterReview(await api('/products/' + route.params.id + '/reviews', { method: 'DELETE' })) } catch (e) { rvErr.value = e.message }
}
loadReviews()

// „Clienții au cumpărat și…” (din comenzi) și „Văzute recent” (localStorage)
const alsoBought = ref([])
const recent = ref([])
{
  const id = Number(route.params.id)
  const seen = recentIds().filter(x => x !== id).slice(0, 6)
  pushRecent(id)
  if (seen.length) api('/products/by-ids?ids=' + seen.join(',')).then(x => (recent.value = x)).catch(() => {})
  api('/products/' + id + '/also-bought').then(x => (alsoBought.value = x)).catch(() => {})
}
</script>

<template>
  <template v-if="p">
    <nav class="crumbs" :aria-label="t('prod_nav')">
      <RouterLink to="/catalog">{{ t('prod_catalog') }}</RouterLink><Icon name="chevron-right" :size="14" />
      <RouterLink :to="{ path: '/catalog', query: { category: catUrl(p.category) } }">{{ catName(p.category) }}</RouterLink><Icon name="chevron-right" :size="14" />
      <span aria-current="page">{{ pname(p.name) }}</span>
    </nav>
    <div class="pdp">
      <div class="pdp-media">
        <div class="plate"><img class="pimg" :src="imgSrc(p)" @error="imgFallback($event, p)" width="400" height="280" fetchpriority="high" :alt="pname(p.name)"><span v-if="p.promo" class="flag">-{{ p.discount }}%</span></div>
      </div>
      <div class="pdp-info">
        <span class="brand">{{ p.brand }}</span>
        <h1>{{ pname(p.name) }}</h1>
        <span class="rate"><Icon name="star" :size="15" />{{ rate(p.rating) }}</span>
        <p class="lead">{{ pfield(p, 'description') }}</p>
        <div class="buybox">
          <span class="buy"><b class="price" :class="{ sale: p.promo }">{{ ron(p.price) }}</b><s v-if="p.promo">{{ ron(p.oldPrice) }}</s></span>
          <span v-if="p.stock > 0" class="stock" :class="p.stock <= 5 ? 'low' : 'ok'"><i class="dot"></i>{{ p.stock <= 5 ? t('prod_last', { n: p.stock }) : t('prod_instock', { n: p.stock }) }}{{ t('prod_deliv', { n: p.deliveryDays }) }}</span>
          <span v-else class="stock bad"><i class="dot"></i>{{ t('prod_oos') }}</span>
          <div class="row">
            <input v-model.number="qty" type="number" min="1" :max="Math.max(p.stock, 1)" :aria-label="t('prod_qty')">
            <button :disabled="p.stock <= 0" @click="add"><Icon name="cart" :size="20" />{{ t('prod_add') }}</button>
          </div>
          <div class="subacts">
            <button class="link" @click="addToCompare(p.id)"><Icon name="scale" :size="18" />{{ t('prod_cmp') }}</button>
            <button class="link" @click="wish"><Icon :name="isWished(p.id) ? 'heart-fill' : 'heart'" :size="18" />{{ isWished(p.id) ? t('prod_wrem') : t('prod_wadd') }}</button>
            <RouterLink :to="{ path: '/planner', query: { add: p.id } }"><Icon name="plan" :size="18" />{{ t('prod_fit') }}</RouterLink>
          </div>
        </div>
        <dl class="specs">
          <dt>{{ t('prod_material') }}</dt><dd>{{ pfield(p, 'material') }}</dd>
          <dt>{{ t('prod_dims') }}</dt><dd>{{ p.dimensions }}</dd>
          <dt>{{ t('prod_color') }}</dt><dd>{{ pfield(p, 'color') }}</dd>
          <template v-if="p.manufacturer"><dt>{{ t('prod_maker') }}</dt><dd>{{ p.manufacturer }}</dd></template>
          <template v-if="originText"><dt>{{ t('prod_origin') }}</dt><dd>{{ originText }}</dd></template>
          <template v-if="methodText"><dt>{{ t('prod_method') }}</dt><dd>{{ methodText }}</dd></template>
          <dt>{{ t('prod_cat') }}</dt><dd>{{ catName(p.category) }}</dd>
        </dl>
        <ul class="assure">
          <li><Icon name="truck" :size="18" />{{ t('prod_ship') }}</li>
          <li><Icon name="undo" :size="18" />{{ t('prod_return') }}</li>
          <li><Icon name="shield" :size="18" />{{ t('prod_warranty') }}</li>
          <li><Icon name="card" :size="18" />{{ t('prod_pay') }}</li>
        </ul>
      </div>
    </div>
    <section v-if="match && match.items.length" class="panel" aria-labelledby="set-h">
      <h2 id="set-h" style="font-size:var(--t-lg)">{{ t('prod_complete') }}<small v-if="match.collection"> {{ t('prod_coll', { name: match.collection }) }}</small></h2>
      <div class="setlist">
        <div v-for="it in match.items" :key="it.product.id" class="setrow">
          <span><RouterLink :to="`/product/${it.product.id}`">{{ pname(it.product.name) }}</RouterLink><br><span class="mut">{{ it.reason }}</span></span>
          <b class="price" style="font-size:1.1rem">{{ ron(it.product.price) }}</b>
          <button class="sec" @click="addToCart(it.product.id)"><Icon name="plus" :size="16" />{{ t('prod_cart') }}</button>
        </div>
      </div>
      <p class="row" style="align-items:center;flex-wrap:wrap;margin-top:16px">{{ t('prod_setnote') }} <b class="price">{{ ron(setTotal) }}</b>
        <button @click="addSet"><Icon name="cart" :size="18" />{{ t('prod_addset') }}</button></p>
    </section>
    <section v-if="alsoBought.length" class="panel" aria-labelledby="ab-h">
      <h2 id="ab-h" style="font-size:var(--t-lg)">{{ t('prod_also') }}</h2>
      <div class="grid"><ProductCard v-for="x in alsoBought" :key="x.id" :p="x" /></div>
    </section>
    <section v-if="recent.length" class="panel" aria-labelledby="rc-h">
      <h2 id="rc-h" style="font-size:var(--t-lg)">{{ t('prod_recent') }}</h2>
      <div class="grid"><ProductCard v-for="x in recent" :key="x.id" :p="x" /></div>
    </section>
    <section class="panel rv" aria-labelledby="rv-h">
      <h2 id="rv-h" style="font-size:var(--t-lg)">{{ t('rv_title') }}</h2>
      <p v-if="rv.count" class="rv-sum"><span class="rv-stars" aria-hidden="true">{{ stars(Math.round(rv.avg)) }}</span> {{ t('rv_summary', { n: rv.count, avg: rate(rv.avg) }) }}</p>
      <p v-else class="mut">{{ t('rv_none') }}</p>
      <ul v-if="rv.items.length" class="rv-list">
        <li v-for="r in rv.items" :key="r.id">
          <span class="rv-stars" role="img" :aria-label="t('rv_star_n', { n: r.stars })">{{ stars(r.stars) }}</span>
          <b>{{ r.author }}</b> <span class="mut">· {{ t('rv_verified') }} · {{ r.date }}</span>
          <p v-if="r.text">{{ r.text }}</p>
        </li>
      </ul>
      <div v-if="rv.canReview" class="rv-form">
        <h3>{{ rv.mine ? t('rv_yours') : t('rv_write') }}</h3>
        <div class="rv-pick" role="group" :aria-label="t('rv_stars')">
          <button v-for="n in 5" :key="n" type="button" class="rv-star" :class="{ on: n <= form.stars }" :aria-pressed="n === form.stars" :aria-label="t('rv_star_n', { n })" @click="form.stars = n">★</button>
        </div>
        <textarea v-model="form.text" maxlength="1000" rows="3" :aria-label="t('rv_text')" :placeholder="t('rv_text')"></textarea>
        <p v-if="rvErr" class="bad">{{ rvErr }}</p>
        <p class="row"><button @click="sendReview">{{ rv.mine ? t('rv_update') : t('rv_send') }}</button>
          <button v-if="rv.mine" class="sec" @click="delReview">{{ t('rv_del') }}</button></p>
      </div>
      <p v-else-if="!store.user" class="mut"><RouterLink :to="{ path: '/login', query: { next: route.fullPath } }">{{ t('rv_login') }}</RouterLink></p>
      <p v-else class="mut">{{ t('rv_buyonly') }}</p>
    </section>
  </template>
</template>
