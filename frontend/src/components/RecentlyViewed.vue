<script setup>
import { ref } from 'vue'
import { api } from '../api.js'
import { recentIds } from '../recent.js'
import ProductCard from './ProductCard.vue'

const props = defineProps({ exclude: { type: Number, default: 0 } })
const items = ref([])
// Se cer produsele din nou de la server: prețul și stocul afișate sunt mereu cele curente
const ids = recentIds().filter(i => i !== props.exclude).slice(0, 4)
Promise.all(ids.map(i => api('/products/' + i).catch(() => null))).then(r => (items.value = r.filter(Boolean)))
</script>

<template>
  <section v-if="items.length" class="home-sec" aria-labelledby="recent-h">
    <div class="sec-head"><h2 id="recent-h">Văzute recent</h2></div>
    <div class="grid"><ProductCard v-for="p in items" :key="p.id" :p="p" :level="3" /></div>
  </section>
</template>
