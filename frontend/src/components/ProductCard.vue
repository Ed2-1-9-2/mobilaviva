<script setup>
import { useRouter } from 'vue-router'
import Icon from './Icon.vue'
import { t } from '../i18n'
import { pname, pfield } from '../pname'
import { ron, addToCart, addToCompare, imgSrc, imgFallback, store, isWished, toggleWish } from '../store'
defineProps({ level: { type: Number, default: 2 }, p: { type: Object, required: true }, eager: { type: Boolean, default: false }, high: { type: Boolean, default: false } })
const router = useRouter()
async function wish(id) {
  if (!store.user) return router.push({ path: '/login', query: { next: location.pathname + location.search } })
  await toggleWish(id)
}
const rate = r => (r ?? 0).toLocaleString('ro-RO', { minimumFractionDigits: 1, maximumFractionDigits: 1 })
</script>

<template>
  <article class="card">
    <div class="plate">
      <img class="pimg" :src="imgSrc(p)" @error="imgFallback($event, p)" width="400" height="280" :loading="eager ? 'eager' : 'lazy'" :fetchpriority="high ? 'high' : 'auto'" decoding="async" alt="">
      <span v-if="p.promo" class="flag">-{{ p.discount }}%</span>
      <button class="wish" :class="{ on: isWished(p.id) }" :aria-label="t(isWished(p.id) ? 'wrem' : 'wadd')" @click="wish(p.id)">
        <Icon :name="isWished(p.id) ? 'heart-fill' : 'heart'" :size="20" />
      </button>
    </div>
    <span class="brand">{{ p.brand }}</span>
    <component :is="'h' + level"><RouterLink :to="`/product/${p.id}`">{{ pname(p.name) }}</RouterLink></component>
    <span class="mat">{{ pfield(p, 'material') }}</span>
    <span class="rate"><Icon name="star" :size="14" />{{ rate(p.rating) }}</span>
    <span class="buy"><b class="price" :class="{ sale: p.promo }">{{ ron(p.price) }}</b><s v-if="p.promo">{{ ron(p.oldPrice) }}</s></span>
    <span v-if="p.stock > 0" class="stock" :class="p.stock <= 5 ? 'low' : 'ok'"><i class="dot"></i>{{ p.stock <= 5 ? t('last', { n: p.stock }) : t('instock') }}, {{ t('deliv', { d: p.deliveryDays }) }}</span>
    <span v-else class="stock bad"><i class="dot"></i>{{ t('oos') }}</span>
    <div class="acts">
      <button :disabled="p.stock <= 0" @click="addToCart(p.id)">{{ t('prod_add') }}</button>
      <button class="sec icon" :aria-label="`Compară ${p.name}`" title="Compară" @click="addToCompare(p.id)"><Icon name="scale" :size="20" /></button>
    </div>
  </article>
</template>
