<script setup>
import { ref, computed } from 'vue'
import { api } from '../api'
import { imgSrc, imgFallback } from '../store'
import { t, catName } from '../i18n'
import { catUrl, catKey } from '../urlkeys'
import ProductCard from '../components/ProductCard.vue'
import Icon from '../components/Icon.vue'

const facets = ref({ categories: [] })
const pool = ref([])
const fP = api('/facets').then(x => (facets.value = { categories: [], ...(x || {}) })).catch(() => {})
// Primele două pagini din catalog: din ele alegem imaginea fiecărei categorii și ofertele
const pP = (async () => {
  try {
    const r = await api('/products?')
    let items = (r && r.items) || []
    if (r && r.pages > 1) items = items.concat(((await api('/products?page=2')) || {}).items || [])
    pool.value = items
  } catch { /* pagina rămâne utilă și fără ele */ }
})()
const extra = ref([])   // categoriile care nu apar în primele pagini (ex. Hol, Iluminat) își iau imaginea din propria listă
Promise.all([fP, pP]).then(async () => {
  const miss = ((facets.value && facets.value.categories) || []).map(c => c.name).filter(n => !pool.value.some(p => p.category === n))
  const got = await Promise.all(miss.map(n => api('/products?category=' + encodeURIComponent(n)).then(r => ((r && r.items) || []).slice(0, 1)).catch(() => [])))
  extra.value = got.flat()
})
const cats = computed(() => ((facets.value && facets.value.categories) || []).map(c => ({ ...c, p: pool.value.find(p => p.category === c.name) || extra.value.find(p => p.category === c.name) })))
const offers = computed(() => pool.value.filter(p => p.promo && p.stock > 0).slice(0, 4))
</script>

<template>
  <section class="hero" aria-labelledby="hero-h">
    <div>
      <h1 id="hero-h">{{ t('home_title') }}</h1>
      <p class="lead">{{ t('home_lead') }}</p>
      <div class="cta">
        <RouterLink class="btn" to="/planner"><Icon name="plan" :size="20" />{{ t('home_planner') }}</RouterLink>
        <RouterLink class="textlink" to="/catalog">{{ t('home_see_all') }}</RouterLink>
      </div>
    </div>
    <figure class="hero-plan">
      <svg viewBox="0 0 520 396" width="520" height="396" role="img" :aria-label="t('home_plan_aria')">
        <defs><pattern id="hg" width="50" height="50" patternUnits="userSpaceOnUse" patternTransform="translate(40 44)"><path class="rm-grid" d="M50 0H0V50" /></pattern></defs>
        <path class="rm-dim" d="M40 26H480M40 19v14M480 19v14M26 44V364M19 44h14M19 364h14" />
        <text class="rm-txt" x="260" y="17" text-anchor="middle">440 cm</text>
        <text class="rm-txt" transform="translate(14 204) rotate(-90)" text-anchor="middle">320 cm</text>
        <rect class="rm-wall" x="40" y="44" width="440" height="320" />
        <rect x="40" y="44" width="440" height="320" fill="url(#hg)" />
        <path class="rm-gap" d="M400 364H460" />
        <path class="rm-door" d="M460 364V304M400 364A60 60 0 0 1 460 304" />
        <g class="pc" style="--d:.1s"><rect class="rm-piece" x="170" y="44" width="180" height="40" rx="2" /><text class="rm-lbl" x="260" y="68" text-anchor="middle"><tspan>{{ t('home_tv') }}</tspan><tspan dx="6">180 × 40</tspan></text></g>
        <g class="pc" style="--d:.22s"><rect class="rm-piece oak" x="130" y="170" width="100" height="55" rx="2" /><text class="rm-lbl" x="180" y="194" text-anchor="middle"><tspan x="180">{{ t('home_table') }}</tspan><tspan x="180" dy="16">100 × 55</tspan></text></g>
        <g class="pc" style="--d:.34s"><rect class="rm-piece" x="70" y="269" width="220" height="95" rx="2" /><text class="rm-lbl" x="180" y="312" text-anchor="middle"><tspan x="180">{{ t('home_sofa') }}</tspan><tspan x="180" dy="16">220 × 95</tspan></text></g>
        <g class="pc" style="--d:.46s"><rect class="rm-piece" x="385" y="160" width="78" height="82" rx="2" /><text class="rm-lbl" x="424" y="197" text-anchor="middle"><tspan x="424">{{ t('home_armchair') }}</tspan><tspan x="424" dy="16">78 × 82</tspan></text></g>
      </svg>
      <figcaption class="verdict ok"><Icon name="check-circle" :size="20" />{{ t('home_verdict') }}</figcaption>
    </figure>
  </section>

  <section class="home-sec" aria-labelledby="cat-h">
    <div class="sec-head"><h2 id="cat-h">{{ t('home_by_cat') }}</h2><RouterLink to="/catalog">{{ t('home_all_catalog') }}</RouterLink></div>
    <div class="cats">
      <RouterLink v-for="c in cats" :key="c.name" class="cat" :to="{ path: '/catalog', query: { category: catUrl(c.name) } }">
        <div class="plate"><img v-if="c.p" class="pimg" :src="imgSrc(c.p)" @error="imgFallback($event, c.p)" width="400" height="280" loading="lazy" decoding="async" alt=""></div>
        <b>{{ catName(c.name) }}</b><span class="mut">{{ t('home_models', { n: c.count }) }}</span>
      </RouterLink>
    </div>
  </section>

  <section v-if="offers.length" class="home-sec" aria-labelledby="off-h">
    <div class="sec-head"><h2 id="off-h">{{ t('home_offers') }}</h2><RouterLink :to="{ path: '/catalog', query: { promo: 1 } }">{{ t('home_all_offers') }}</RouterLink></div>
    <div class="grid"><ProductCard v-for="p in offers" :key="p.id" :p="p" :level="3" /></div>
  </section>
</template>
