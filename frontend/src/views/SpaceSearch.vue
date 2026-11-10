<script setup>
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { api } from '../api.js'
import { footprint } from '../footprint.js'
import ProductCard from '../components/ProductCard.vue'

const route = useRoute()
const f = ref({ w: route.query.w || '', d: route.query.d || '', h: route.query.h || '', category: route.query.category || '' })
const all = ref([])
const categories = ref([])
const loaded = ref(false)

// Se încarcă tot catalogul o singură dată (la fel ca în planificator); filtrarea se face apoi instant
async function loadAll() {
  let page = 1, pages = 1, items = []
  do {
    const r = await api('/products?page=' + page)
    items = items.concat(r.items); pages = r.pages; page++
  } while (page <= pages)
  all.value = items.map(p => ({ p, fp: footprint(p.dimensions) })).filter(x => x.fp)
  categories.value = [...new Set(items.map(p => p.category))]
  loaded.value = true
}
loadAll()

const num = v => (v === '' || v === null ? 0 : Number(v))
const hasLimits = computed(() => num(f.value.w) > 0 || num(f.value.d) > 0 || num(f.value.h) > 0)
// Un produs încape dacă lățimea, adâncimea (și înălțimea, când e cunoscută) sunt în spațiul disponibil
const results = computed(() => {
  const W = num(f.value.w), D = num(f.value.d), H = num(f.value.h)
  return all.value
    .filter(x => !f.value.category || x.p.category === f.value.category)
    .map(x => ({ ...x, okW: !W || x.fp.w <= W, okD: !D || x.fp.d <= D, okH: !H || !x.fp.h || x.fp.h <= H }))
    .filter(x => x.okW && x.okD && x.okH)
    .map(x => ({ ...x, spare: (W ? W - x.fp.w : 0) + (D ? D - x.fp.d : 0) }))
    .sort((a, b) => a.spare - b.spare)       // cele mai potrivite (spațiu rămas mic) primele
})
</script>

<template>
  <h1 class="pagetitle">Căutare după spațiu disponibil</h1>
  <p class="intro">Spune-ne cât loc ai (în cm) și îți arătăm doar piesele care încap. Lasă gol ce nu contează. Pentru un plan complet al camerei folosește <RouterLink to="/planner">planificatorul</RouterLink>.</p>
  <form class="panel spaceform" @submit.prevent>
    <label>Lățime disponibilă (cm)<input v-model="f.w" type="number" min="0" inputmode="numeric" placeholder="ex. 250"></label>
    <label>Adâncime disponibilă (cm)<input v-model="f.d" type="number" min="0" inputmode="numeric" placeholder="ex. 100"></label>
    <label>Înălțime disponibilă (cm)<input v-model="f.h" type="number" min="0" inputmode="numeric" placeholder="ex. 240"></label>
    <label>Categorie
      <select v-model="f.category"><option value="">Toate</option><option v-for="c in categories" :key="c" :value="c">{{ c }}</option></select>
    </label>
  </form>
  <p v-if="!loaded" class="mut" aria-busy="true">Se încarcă catalogul...</p>
  <template v-else>
    <p class="mut" role="status">{{ hasLimits ? results.length + ' produse încap în spațiul indicat' : 'Introdu cel puțin o dimensiune ca să filtrăm produsele' }}</p>
    <div v-if="hasLimits && results.length" class="grid">
      <div v-for="x in results" :key="x.p.id" class="spaceitem">
        <ProductCard :p="x.p" :level="2" />
        <p class="fit ok">Dimensiuni: {{ x.p.dimensions }}</p>
      </div>
    </div>
    <p v-else-if="hasLimits" class="panel">Nicio piesă nu încape în acest spațiu. Încearcă dimensiuni mai mari sau o altă categorie.</p>
  </template>
</template>
