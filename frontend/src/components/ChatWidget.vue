<script setup>
import { ref, computed, nextTick, onMounted, onBeforeUnmount, watch } from 'vue'
import { api } from '../api'
import { ron, addToCart, imgSrc, imgFallback } from '../store'
import { t, i18n } from '../i18n'
const emit = defineEmits(['close'])
// Închidere cu animație (fără ea dacă utilizatorul a cerut „reduce motion”); închiderea reală se emite după animationend
const closing = ref(false)
function close() {
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return emit('close')
  closing.value = true
}
function onEnd(e) { if (closing.value && e.target === e.currentTarget) { closing.value = false; emit('close') } }

// Salutul se traduce la afișare (welcome: true), ca să urmeze limba aleasă
const msgs = ref([{ role: 'assistant', content: '', welcome: true, products: [] }])
const text = m => (m.welcome ? t('chat_welcome') : m.content)
const input = ref('')
const busy = ref(false)
const err = ref('')
const mode = ref('')
const box = ref(null)
const field = ref(null)
const chips = computed(() => [t('chat_chip1'), t('chat_chip2'), t('chat_chip3'), t('chat_chip4')])

function parts(text) {
  const out = []
  const re = /(?:mobilaviva\.com)?\/(termeni|confidentialitate|retur)\b/g
  let last = 0, m
  while ((m = re.exec(text))) {
    if (m.index > last) out.push({ t: text.slice(last, m.index) })
    out.push({ to: '/' + m[1], t: t('chat_link_' + m[1]) })
    last = m.index + m[0].length
  }
  if (last < text.length) out.push({ t: text.slice(last) })
  return out
}
// Răspuns live, în timp ce tastezi (logică locală, fără AI): produse potrivite și linkuri către politici
const live = ref({ reply: '', products: [] })
let liveTimer = null, liveSeq = 0
function clearLive() { clearTimeout(liveTimer); liveSeq++; live.value = { reply: '', products: [] } }
watch(input, v => {
  clearTimeout(liveTimer)
  const q = v.trim()
  if (q.length < 3 || busy.value) return clearLive()
  const seq = ++liveSeq
  liveTimer = setTimeout(async () => {
    try {
      const r = await api('/chat/live?q=' + encodeURIComponent(q))
      if (seq === liveSeq) live.value = { reply: (r.reply || ''), products: r.products || [] }
    } catch { if (seq === liveSeq) live.value = { reply: '', products: [] } }
  }, 250)
})
onBeforeUnmount(() => clearTimeout(liveTimer))
function scroll() { nextTick(() => { if (box.value) box.value.scrollTop = box.value.scrollHeight }) }
onMounted(() => { field.value?.focus({ preventScroll: true }); api('/chat/info').then(r => (mode.value = r.mode)).catch(() => {}) })

async function send(msg) {
  const q = (msg ?? input.value).trim()
  if (!q || busy.value) return
  err.value = ''
  input.value = ''
  clearLive()
  msgs.value.push({ role: 'user', content: q, products: [] })
  busy.value = true
  scroll()
  try {
    const history = msgs.value.map(m => ({ role: m.role, content: text(m) })).slice(-10)
    const r = await api('/chat', { method: 'POST', body: { messages: history, lang: i18n.lang } })
    mode.value = r.mode
    msgs.value.push({ role: 'assistant', content: r.reply, products: r.products || [] })
  } catch (e) {
    err.value = e.message || t('chat_err_default')
  } finally {
    busy.value = false
    scroll()
    nextTick(() => field.value?.focus())
  }
}
</script>

<template>
  <section id="chat-panel" class="chat-panel" :class="{ closing }" role="dialog" :aria-label="t('chat_title')" @keydown.esc="close" @animationend="onEnd">
    <header class="chat-head">
      <div>
        <strong>{{ t('chat_title') }}</strong>
        <small>{{ mode === 'ai' ? t('chat_mode_ai') : t('chat_mode_shop') }}</small>
      </div>
      <button type="button" class="chat-x" :aria-label="t('chat_close')" @click="close()">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true"><path d="M6 6l12 12M18 6 6 18" /></svg>
      </button>
    </header>
    <div ref="box" class="chat-log" role="log" aria-live="polite">
      <div v-for="(m, i) in msgs" :key="i" class="msg" :class="m.role === 'user' ? 'me' : 'bot'">
        <p v-if="m.role === 'user'">{{ text(m) }}</p>
        <p v-else><template v-for="(s, k) in parts(text(m))" :key="k"><RouterLink v-if="s.to" :to="s.to">{{ s.t }}</RouterLink><template v-else>{{ s.t }}</template></template></p>
        <ul v-if="m.products && m.products.length" class="chat-prods">
          <li v-for="p in m.products" :key="p.id">
            <img :src="imgSrc(p)" width="64" height="48" alt="" loading="lazy" @error="imgFallback($event, p)">
            <div>
              <RouterLink :to="`/product/${p.id}`">{{ p.name }}</RouterLink>
              <span><b>{{ ron(p.price) }}</b><s v-if="p.promo"> {{ ron(p.oldPrice) }}</s></span>
              <button type="button" :disabled="p.stock <= 0" @click="addToCart(p.id)">{{ p.stock > 0 ? t('chat_add') : t('chat_oos') }}</button>
            </div>
          </li>
        </ul>
      </div>
      <div v-if="busy" class="msg bot typing"><p><span class="dots" aria-hidden="true"><i></i><i></i><i></i></span><span class="sr">{{ t('chat_typing') }}</span></p></div>
      <p v-if="err" class="chat-err" role="alert">{{ err }}</p>
      <div v-if="msgs.length === 1" class="chat-chips">
        <button v-for="c in chips" :key="c" type="button" @click="send(c)">{{ c }}</button>
      </div>
    </div>
    <div v-if="live.reply || live.products.length" class="chat-live" aria-live="polite">
      <p v-if="live.reply" class="live-txt"><template v-for="(s, k) in parts(live.reply)" :key="k"><RouterLink v-if="s.to" :to="s.to">{{ s.t }}</RouterLink><template v-else>{{ s.t }}</template></template></p>
      <ul v-if="live.products.length" class="chat-prods">
        <li v-for="p in live.products" :key="p.id">
          <img :src="imgSrc(p)" width="64" height="48" alt="" loading="lazy" @error="imgFallback($event, p)">
          <div>
            <RouterLink :to="`/product/${p.id}`">{{ p.name }}</RouterLink>
            <span><b>{{ ron(p.price) }}</b><s v-if="p.promo"> {{ ron(p.oldPrice) }}</s></span>
            <button type="button" :disabled="p.stock <= 0" @click="addToCart(p.id)">{{ p.stock > 0 ? t('chat_add') : t('chat_oos') }}</button>
          </div>
        </li>
      </ul>
    </div>
    <form class="chat-form" @submit.prevent="send()">
      <input ref="field" v-model="input" maxlength="500" :placeholder="t('chat_placeholder')" :aria-label="t('chat_input')" autocomplete="off">
      <button type="submit" :disabled="busy || !input.trim()">{{ t('chat_send') }}</button>
    </form>
    <small class="chat-note">{{ t('chat_note') }}</small>
  </section>
</template>
