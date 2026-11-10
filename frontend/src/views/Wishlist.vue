<script setup>
import Icon from '../components/Icon.vue'
import { ref } from 'vue'
import { api } from '../api'
import { t } from '../i18n'
import { pname, pfield } from '../pname'
import { ron, addToCart, imgSrc, imgFallback, toggleWish, loadWish } from '../store'

const items = ref([])
const loaded = ref(false)

async function load() {
  try { items.value = (await api('/wishlist')).items } finally { loaded.value = true }
}
load()

async function seen(id) {
  await api('/wishlist/' + id + '/ack', { method: 'POST' })
  await load()
  await loadWish()
}

async function remove(id) {
  await toggleWish(id)
  items.value = items.value.filter(x => x.product.id !== id)
}
</script>

<template>
  <h1 class="pagetitle">{{ t('wl_title') }}</h1>
  <p v-if="loaded && !items.length" class="panel mut">{{ t('wl_empty') }}</p>
  <div v-for="it in items" :key="it.product.id" class="panel row" style="align-items:center;flex-wrap:wrap">
    <RouterLink :to="`/product/${it.product.id}`"><img class="thumb" style="width:160px" :src="imgSrc(it.product)" @error="imgFallback($event, it.product)" width="160" height="112" loading="lazy" :alt="it.product.name"></RouterLink>
    <div style="flex:2;min-width:220px">
      <RouterLink :to="`/product/${it.product.id}`"><b>{{ pname(it.product.name) }}</b></RouterLink><br>
      <span class="mut">{{ it.product.brand }} · {{ pfield(it.product, 'material') }}</span><br>
      <b class="price">{{ ron(it.product.price) }}</b>
      <b v-if="it.dropped" class="ok"><Icon name="bell" :size="16" /> {{ t('wl_dropped', { pct: it.dropPct, price: ron(it.priceAtAdd) }) }}</b>
      <span v-else class="mut"> · {{ t('wl_saved', { price: ron(it.priceAtAdd) }) }}</span><br>
      <span v-if="it.product.stock > 0" class="ok">{{ t('wl_instock', { n: it.product.deliveryDays }) }}</span>
      <span v-else class="bad">{{ t('wl_oos') }}</span>
    </div>
    <div class="acts">
      <button :disabled="it.product.stock <= 0" @click="addToCart(it.product.id)"><Icon name="cart" :size="16" />{{ t('wl_add') }}</button>
      <button v-if="it.dropped" class="sec" @click="seen(it.product.id)">{{ t('wl_seen') }}</button>
      <button class="sec" @click="remove(it.product.id)">{{ t('wl_remove') }}</button>
    </div>
  </div>
</template>
