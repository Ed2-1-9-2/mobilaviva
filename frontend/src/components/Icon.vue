<script setup>
// Iconițe liniare (24x24, trăsătură de 1.75). Înlocuiesc emoji-urile: aspect identic pe orice sistem, culoare din temă, ascunse cititoarelor de ecran.
defineProps({ name: { type: String, required: true }, size: { type: Number, default: 20 } })
const heart = '<path d="M12 20.5s-8-4.9-8-11A4.5 4.5 0 0 1 12 7a4.5 4.5 0 0 1 8 2.5c0 6.1-8 11-8 11z"/>'
const P = {
  search: '<circle cx="11" cy="11" r="7"/><path d="m20 20-3.5-3.5"/>',
  cart: '<circle cx="9" cy="20" r="1.5"/><circle cx="18" cy="20" r="1.5"/><path d="M2.5 3.5h2.7l2.3 11.2a1.5 1.5 0 0 0 1.5 1.2h8.3a1.5 1.5 0 0 0 1.5-1.1L20.5 8H6"/>',
  heart,
  'heart-fill': heart,
  scale: '<path d="M4 8h15m0 0-3.5-3.5M19 8l-3.5 3.5M20 16H5m0 0 3.5-3.5M5 16l3.5 3.5"/>',
  user: '<circle cx="12" cy="8" r="4"/><path d="M4 20c1.2-3.6 4.2-5.5 8-5.5s6.8 1.9 8 5.5"/>',
  plan: '<path d="M3 3h18v18H3z"/><path d="M3 12h8m4 0h6M12 3v6m0 6v6"/>',
  truck: '<path d="M2 6.5h11v10H2zM13 10h4.5l3.5 3.5v3H13z"/><circle cx="7" cy="18" r="1.8"/><circle cx="17.5" cy="18" r="1.8"/>',
  undo: '<path d="M4 9h11a5 5 0 0 1 0 10H8"/><path d="m8 5-4 4 4 4"/>',
  lock: '<rect x="5" y="11" width="14" height="9" rx="2"/><path d="M8 11V8a4 4 0 0 1 8 0v3"/>',
  shield: '<path d="M12 3 4.5 6v5.5c0 4.5 3 8 7.5 9.5 4.5-1.5 7.5-5 7.5-9.5V6z"/><path d="m9 12 2.2 2.2L15.5 10"/>',
  tag: '<path d="M3 12V4h8l10 10-8 8z"/><circle cx="7.5" cy="8.5" r="1.2"/>',
  mail: '<rect x="3" y="5" width="18" height="14" rx="2"/><path d="m3.5 7 8.5 6 8.5-6"/>',
  check: '<path d="m5 12.5 4.5 4.5L19 7.5"/>',
  'check-circle': '<circle cx="12" cy="12" r="9"/><path d="m8 12.5 3 3 5-6"/>',
  x: '<path d="M6 6l12 12M18 6 6 18"/>',
  alert: '<path d="M12 3.5 2.5 20h19z"/><path d="M12 10v4.5M12 17.5v.01"/>',
  rotate: '<path d="M20 12a8 8 0 1 1-2.6-5.9"/><path d="M20 4v5h-5"/>',
  card: '<rect x="2.5" y="5" width="19" height="14" rx="2"/><path d="M2.5 10h19M6 15h4"/>',
  bank: '<path d="M3 9.5 12 4l9 5.5zM5 10v8M9.5 10v8M14.5 10v8M19 10v8M3 20.5h18"/>',
  receipt: '<path d="M6 3h12v18l-3-2-3 2-3-2-3 2z"/><path d="M9 8h6M9 12h6"/>',
  printer: '<path d="M7 9V3h10v6M7 17H4.5v-7h15v7H17"/><rect x="7" y="14" width="10" height="7"/>',
  'chevron-right': '<path d="m9 5 7 7-7 7"/>',
  bell: '<path d="M6 16v-5a6 6 0 0 1 12 0v5l1.5 2h-15z"/><path d="M10 21h4"/>',
  star: '<path d="m12 3 2.7 5.6 6.1.9-4.4 4.3 1 6.1L12 17l-5.4 2.9 1-6.1-4.4-4.3 6.1-.9z"/>',
  plus: '<path d="M12 5v14M5 12h14"/>'
}
</script>

<template>
  <svg class="ic" :class="{ fill: name === 'heart-fill' || name === 'star' }" :width="size" :height="size" viewBox="0 0 24 24" fill="none"
       stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false" v-html="P[name]"></svg>
</template>
