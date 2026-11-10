<script setup>
import { ref, reactive, watch, computed, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../api'
import ProductCard from '../components/ProductCard.vue'
import Icon from '../components/Icon.vue'
import { store } from '../store'
import { setLd, crumbsLd } from '../jsonld'
import { t, catName } from '../i18n'
import { catUrl, sortUrl, catKey, canonicalQuery, internalPath, localizePath } from '../urlkeys'

const route = useRoute()
const router = useRouter()
const data = ref({ items: [], total: 0, pages: 1, page: 1, relaxed: false })
const facets = ref({ categories: [], brands: [] })
const loading = ref(true)
const f = reactive({ category: '', brand: '', min_price: '', max_price: '', in_stock: false, promo: false, sort: 'relevance' })
// Filtrele sunt deschise pe desktop și pliate pe telefon (mai mult loc pentru produse)
const mq = typeof matchMedia === 'function' ? matchMedia('(min-width:801px)') : null
const open = ref(mq ? mq.matches : true)
if (mq) mq.addEventListener('change', e => (open.value = e.matches))

let reqId = 0   // doar ultimul răspuns se afișează (navigare rapidă între filtre)
async function load() {
  if (internalPath(route.path) !== '/catalog') return   // load pornește și când se pleacă din pagină
  const q = canonicalQuery(route.query)    // spre API merg mereu valorile românești (adresa o rescrie routerul în limba activă)
  f.category = q.category || ''; f.brand = q.brand || ''
  f.min_price = q.min_price || ''; f.max_price = q.max_price || ''
  f.in_stock = q.in_stock === '1'; f.promo = q.promo === '1'; f.sort = q.sort || 'relevance'
  loading.value = true
  const mine = ++reqId
  try {
    const r = await api('/products?' + new URLSearchParams(q))
    if (mine === reqId) data.value = r
  } finally { if (mine === reqId) loading.value = false }
}
watch(() => route.query, load, { immediate: true })
api('/facets').then(x => (facets.value = { categories: [], brands: [], ...(x || {}) })).catch(() => {})

function apply() {
  const q = {}
  if (route.query.q) q.q = route.query.q
  if (f.category) q.category = catUrl(f.category)
  if (f.brand) q.brand = f.brand
  if (f.min_price) q.min_price = f.min_price
  if (f.max_price) q.max_price = f.max_price
  if (f.in_stock) q.in_stock = '1'
  if (f.promo) q.promo = '1'
  if (f.sort !== 'relevance') q.sort = sortUrl(f.sort)
  router.push({ path: localizePath('/catalog'), query: q })
}

const INTRO = computed(() => ({
  "Canapele": t('cat_intro_canapele'),
  "Paturi": t('cat_intro_paturi'),
  "Dulapuri": t('cat_intro_dulapuri'),
  "Comode": t('cat_intro_comode'),
  "Mese": t('cat_intro_mese'),
  "Scaune": t('cat_intro_scaune'),
  "Birouri": t('cat_intro_birouri'),
  "Iluminat": t('cat_intro_iluminat'),
  "Hol": t('cat_intro_hol'),
  "Copii": t('cat_intro_copii')
}))
// Text introductiv unic pe categorie (conținut util și cuvinte-cheie naturale, conform ghidului SEO)
const cat = computed(() => catKey(route.query.category || ''))
const intro = computed(() => (cat.value ? INTRO.value[cat.value] || '' : ''))
watch(cat, c => setLd('crumbs', crumbsLd([{ name: 'Acasă', path: '/' }, { name: 'Catalog', path: '/catalog' }, ...(c ? [{ name: c, path: '/catalog?category=' + encodeURIComponent(c) }] : [])])), { immediate: true })
onUnmounted(() => setLd('crumbs', null))

const title = computed(() =>
  route.query.q ? t('cat_results_for', { q: route.query.q }) : (cat.value ? catName(cat.value) : (route.query.promo ? t('cat_offers') : t('cat_all_products'))))
</script>

<template>
  <nav class="crumbs" :aria-label="t('cat_nav_aria')"><RouterLink to="/">{{ t('cat_home') }}</RouterLink><Icon name="chevron-right" :size="14" />
    <template v-if="cat"><RouterLink to="/catalog">{{ t('cat_catalog') }}</RouterLink><Icon name="chevron-right" :size="14" /><span aria-current="page">{{ catName(cat) }}</span></template>
    <span v-else aria-current="page">{{ t('cat_catalog') }}</span></nav>
  <div class="layout">
    <aside>
      <details class="filters" :open="open" @toggle="open = $event.target.open">
        <summary>{{ t('cat_filters') }}</summary>
        <form @submit.prevent="apply">
          <label>{{ t('cat_category') }}
            <select v-model="f.category"><option value="">{{ t('cat_all') }}</option>
              <option v-for="c in facets.categories" :key="c.name" :value="c.name">{{ catName(c.name) }} ({{ c.count }})</option>
            </select></label>
          <label>{{ t('cat_brand') }}
            <select v-model="f.brand"><option value="">{{ t('cat_all') }}</option>
              <option v-for="b in facets.brands" :key="b.name" :value="b.name">{{ b.name }} ({{ b.count }})</option>
            </select></label>
          <label>{{ t('cat_price_ron') }}
            <span class="row">
              <input v-model="f.min_price" type="number" min="0" :placeholder="t('cat_min_ph')" :aria-label="t('cat_price_min_aria')">
              <input v-model="f.max_price" type="number" min="0" :placeholder="t('cat_max_ph')" :aria-label="t('cat_price_max_aria')">
            </span></label>
          <label class="chk"><input v-model="f.in_stock" type="checkbox"> {{ t('cat_in_stock') }}</label>
          <label class="chk"><input v-model="f.promo" type="checkbox"> {{ t('cat_promo_only') }}</label>
          <button>{{ t('cat_apply') }}</button>
          <RouterLink :to="{ path: '/catalog', query: route.query.q ? { q: route.query.q } : {} }">{{ t('cat_reset') }}</RouterLink>
        </form>
      </details>
    </aside>
    <section :aria-label="t('cat_list_aria')">
      <div class="top">
        <h1 class="ctitle" style="font-size:var(--t-xl)">{{ title }} <small>({{ data.total }})</small></h1>
        <select v-model="f.sort" :aria-label="t('cat_sort_aria')" @change="apply">
          <option value="relevance">{{ t('cat_sort_relevance') }}</option>
          <option value="price_asc">{{ t('cat_sort_price_asc') }}</option>
          <option value="price_desc">{{ t('cat_sort_price_desc') }}</option>
          <option value="delivery">{{ t('cat_sort_delivery') }}</option>
        </select>
      </div>
      <p v-if="intro" class="intro">{{ intro }}</p>
      <p v-if="data.relaxed" class="note">{{ t('cat_relaxed') }}</p>
      <div v-if="!loading && !data.items.length" class="panel">
        {{ t('cat_none') }}
      </div>
      <div v-if="loading && !data.items.length" class="grid" aria-busy="true" :aria-label="t('cat_loading')">
        <div v-for="n in 9" :key="n" class="card skel" aria-hidden="true"></div>
      </div>
      <div v-else class="grid"><ProductCard v-for="(p, i) in data.items" :key="p.id" :p="p" :eager="i < 6" :high="i < 3" /></div>
      <nav v-if="data.pages > 1" class="pager" :aria-label="t('cat_pages_aria')">
        <template v-for="i in data.pages" :key="i">
          <span v-if="i === data.page" aria-current="page">{{ i }}</span>
          <RouterLink v-else :to="{ path: '/catalog', query: { ...route.query, page: i } }">{{ i }}</RouterLink>
        </template>
      </nav>
      <nav v-if="cat" class="related" :aria-label="t('cat_other_aria')">
        <h2>{{ t('cat_other_h') }}</h2>
        <ul><li v-for="c in store.categories.filter(x => x !== cat)" :key="c"><RouterLink :to="{ path: '/catalog', query: { category: catUrl(c) } }">{{ catName(c) }}</RouterLink></li></ul>
      </nav>
    </section>
  </div>
</template>
