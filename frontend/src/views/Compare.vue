<script setup>
import { ref, computed } from 'vue'
import { api } from '../api'
import { t, catName } from '../i18n'
import { pname, pfield } from '../pname'
import { ron, refresh, addToCart, imgSrc, imgFallback } from '../store'

const items = ref([])
api('/compare').then(x => (items.value = x))

async function remove(id) {
  items.value = await api('/compare/' + id, { method: 'DELETE' })
  await refresh()
}
async function clear() {
  items.value = await api('/compare', { method: 'DELETE' }) || []
  await refresh()
}
const rows = computed(() => [
  [t('cmpv_brand'), p => p.brand], [t('cmpv_category'), p => catName(p.category)], [t('cmpv_material'), p => pfield(p, 'material')],
  [t('cmpv_dimensions'), p => p.dimensions], [t('cmpv_color'), p => pfield(p, 'color')],
  [t('cmpv_rating'), p => `${p.stars} ${p.rating}`],
  [t('cmpv_availability'), p => (p.stock > 0 ? t('cmpv_in_stock') : t('cmpv_out_of_stock'))],
  [t('cmpv_delivery'), p => t('cmpv_days', { n: p.deliveryDays })]
])
</script>

<template>
  <h1 class="pagetitle">{{ t('cmpv_title') }}</h1>
  <div v-if="!items.length" class="panel">
    {{ t('cmpv_empty') }} <RouterLink to="/catalog">{{ t('cmpv_see_catalog') }}</RouterLink>
  </div>
  <div v-else class="panel">
    <table class="cmp-table">
      <tr>
        <th></th>
        <td v-for="p in items" :key="p.id">
          <img class="pimg" style="height:auto;aspect-ratio:10/7;border-radius:4px" :src="imgSrc(p)" @error="imgFallback($event, p)" width="400" height="280" loading="lazy" :alt="p.name">
          <RouterLink :to="`/product/${p.id}`">{{ pname(p.name) }}</RouterLink><br>
          <button class="link" @click="remove(p.id)">{{ t('cmpv_remove') }}</button>
        </td>
      </tr>
      <tr>
        <th>{{ t('cmpv_price') }}</th>
        <td v-for="p in items" :key="p.id"><b>{{ ron(p.price) }}</b> <s v-if="p.promo" class="mut">{{ ron(p.oldPrice) }}</s></td>
      </tr>
      <tr v-for="[label, get] in rows" :key="label">
        <th>{{ label }}</th>
        <td v-for="p in items" :key="p.id">{{ get(p) }}</td>
      </tr>
      <tr>
        <th></th>
        <td v-for="p in items" :key="p.id"><button :disabled="p.stock <= 0" @click="addToCart(p.id)">{{ t('cmpv_add_to_cart') }}</button></td>
      </tr>
    </table>
    <button class="sec" style="margin-top:12px" @click="clear">{{ t('cmpv_clear') }}</button>
  </div>
</template>
