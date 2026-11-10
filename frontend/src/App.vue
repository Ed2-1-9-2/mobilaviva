<script setup>
import { ref, watch, computed, defineAsyncComponent } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { store, refresh } from './store'
import { api } from './api'
import { t, catName, LANGS, i18n, setLang } from './i18n'
import { catUrl, catKey, internalPath } from './urlkeys'
import Icon from './components/Icon.vue'
import { setLd, orgLd, siteLd } from './jsonld'
// Asistent AI: panoul se încarcă (chunk separat) abia la primul click, ca să nu încarce pagina la PageSpeed
const ChatWidget = defineAsyncComponent(() => import('./components/ChatWidget.vue'))
const chatLoaded = ref(false)
const chatOpen = ref(false)
function openChat() { chatLoaded.value = true; chatOpen.value = true }
let warmed = false
function warm() { if (!warmed) { warmed = true; import('./components/ChatWidget.vue') } }   // preîncarcă chunk-ul doar la hover/focus/touch pe buton

setLd('org', orgLd()); setLd('site', siteLd())   // date structurate pe toate paginile
const router = useRouter()
const route = useRoute()
const q = ref('')
const sugg = ref([])
let timer
watch(q, v => {
  clearTimeout(timer)
  if (v.trim().length < 2) return
  timer = setTimeout(async () => {
    try { sugg.value = await api('/suggest?q=' + encodeURIComponent(v)) } catch { /* ignorat */ }
  }, 180)
})

function search() {
  const t = q.value.trim()
  router.push({ path: '/catalog', query: t ? { q: t } : {} })
}

async function logout() {
  await api('/auth/logout', { method: 'POST' })
  await refresh()
  router.push('/')
}

// Starea „pagina curentă” din meniu (RouterLink ignoră query-ul, deci o calculăm aici)
const inCatalog = computed(() => internalPath(route.path) === '/catalog')
const onAll = computed(() => inCatalog.value && !route.query.category && !route.query.promo)
const onPromo = computed(() => inCatalog.value && !!route.query.promo)
const onCat = c => inCatalog.value && catKey(route.query.category) === c
const cur = on => (on ? 'page' : undefined)
</script>

<template>
  <a class="skip" href="#main">{{ t('skip') }}</a>
  <div class="strip">
    <div class="wrap">
      <span><Icon name="truck" :size="16" />{{ t('ship') }}</span>
      <span class="hide-s"><Icon name="undo" :size="16" />{{ t('ret') }}</span>
      <span class="hide-s"><Icon name="lock" :size="16" />{{ t('pay') }}</span>
    </div>
  </div>
  <header>
    <div class="wrap bar">
      <RouterLink class="logo" to="/" :aria-label="t('home')">
        <svg class="mark" viewBox="0 0 32 32" aria-hidden="true"><rect width="32" height="32" rx="7" /><path d="M8 19v-5a3 3 0 0 1 3-3h10a3 3 0 0 1 3 3v5" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" /><path d="M6 19h20v4H6z" /></svg>
        <span>mobila<b>viva</b></span>
      </RouterLink>
      <form class="search" role="search" @submit.prevent="search">
        <Icon name="search" :size="18" />
        <input v-model="q" list="sugg" :placeholder="t('ph')" autocomplete="off" :aria-label="t('saria')">
        <datalist id="sugg"><option v-for="s in sugg" :key="s.id" :value="s.name" /></datalist>
        <button>{{ t('sbtn') }}</button>
      </form>
      <div class="acct">
        <template v-if="!store.user">
          <RouterLink class="act" to="/login"><Icon name="user" /><span class="lbl">{{ t('login') }}</span></RouterLink>
          <RouterLink class="act txt" to="/register">{{ t('reg') }}</RouterLink>
        </template>
        <template v-else>
          <RouterLink class="act" to="/orders"><Icon name="user" /><span class="lbl">{{ store.user.name }}</span></RouterLink>
          <RouterLink class="act txt" to="/account">{{ t('ac_link') }}</RouterLink>
          <RouterLink v-if="store.user.admin" class="act txt" to="/admin">Admin</RouterLink>
          <RouterLink v-if="store.user.admin" class="act txt" to="/dovada-plata">Plăți Stripe</RouterLink>
          <button class="act txt" @click="logout">{{ t('out') }}</button>
        </template>
        <label class="lang"><span class="sr">{{ t('lang') }}</span><select :value="i18n.lang" :aria-label="t('lang')" @change="setLang($event.target.value)"><option v-for="l in LANGS" :key="l.code" :value="l.code">{{ l.code.toUpperCase() }} · {{ l.name }}</option></select></label>
        <RouterLink class="act" to="/compare" :aria-label="t('cmpa')">
          <span class="ico"><Icon name="scale" /><span v-if="store.compareCount" class="cnt">{{ store.compareCount }}</span></span><span class="lbl">{{ t('cmp') }}</span>
        </RouterLink>
        <RouterLink class="act" to="/wishlist" :title="store.wishDrops ? t('wdrop', { n: store.wishDrops }) : t('wsha')" :aria-label="t('wsha')">
          <span class="ico"><Icon :name="store.wishDrops ? 'bell' : 'heart'" /><span v-if="store.wishIds.length" class="cnt">{{ store.wishIds.length }}</span></span><span class="lbl">{{ t('wsh') }}</span>
        </RouterLink>
        <RouterLink class="act" to="/cart" :aria-label="t('crta')">
          <span class="ico"><Icon name="cart" /><span v-if="store.cartCount" class="cnt">{{ store.cartCount }}</span></span><span class="lbl">{{ t('crt') }}</span>
        </RouterLink>
      </div>
    </div>
    <div class="menubar">
      <nav class="wrap menu" aria-label="Categorii">
        <RouterLink to="/catalog" :aria-current="cur(onAll)">{{ t('all') }}</RouterLink>
        <RouterLink v-for="c in store.categories" :key="c" :to="{ path: '/catalog', query: { category: catUrl(c) } }" :aria-current="cur(onCat(c))">{{ catName(c) }}</RouterLink>
        <RouterLink class="promo" :to="{ path: '/catalog', query: { promo: 1 } }" :aria-current="cur(onPromo)">{{ t('off') }}</RouterLink>
        <RouterLink class="tool" to="/planner" :aria-current="cur(internalPath(route.path) === '/planner')"><Icon name="plan" :size="18" />{{ t('pln') }}</RouterLink>
      </nav>
    </div>
  </header>
  <main id="main" class="wrap" tabindex="-1"><RouterView :key="internalPath($route.path)" /></main>
  <footer>
    <div class="wrap foot">
      <div>
        <RouterLink class="logo" to="/" :aria-label="t('home')"><span>mobila<b>viva</b></span></RouterLink>
        <p>{{ t('fdesc') }}</p>
        <p><a href="mailto:contact@mobilaviva.com">contact@mobilaviva.com</a></p>
      </div>
      <div>
        <h2>{{ t('shop') }}</h2>
        <ul>
          <li v-for="c in store.categories" :key="c"><RouterLink :to="{ path: '/catalog', query: { category: catUrl(c) } }">{{ catName(c) }}</RouterLink></li>
          <li><RouterLink :to="{ path: '/catalog', query: { promo: 1 } }">{{ t('off') }}</RouterLink></li>
          <li><RouterLink to="/planner">{{ t('pln') }}</RouterLink></li>
        </ul>
      </div>
      <div>
        <h2>{{ t('info') }}</h2>
        <ul>
          <li><RouterLink to="/termeni">{{ t('terms') }}</RouterLink></li>
          <li><RouterLink to="/confidentialitate">{{ t('priv') }}</RouterLink></li>
          <li><RouterLink to="/retur">{{ t('rets') }}</RouterLink></li>
        </ul>
      </div>
      <div>
        <h2>{{ t('disp') }}</h2>
        <ul>
          <li><a href="https://reclamatiisal.anpc.ro/" target="_blank" rel="noopener noreferrer">ANPC – SAL</a></li>
          <li><a href="https://consumer-redress.ec.europa.eu/list-alternative-dispute-resolution-adr-bodies_ro" target="_blank" rel="noopener noreferrer">Consumer Redress (UE)</a></li>
        </ul>
      </div>
    </div>
    <div class="wrap legalbar">{{ t('legal') }}</div>
  </footer>
  <div class="toast-live" role="status" aria-live="polite">
    <Transition name="toast">
      <div v-if="store.toast" class="toast"><Icon name="check" :size="18" />{{ store.toast }}</div>
    </Transition>
  </div>
  <button v-show="!chatOpen" type="button" class="chat-fab" :aria-label="t('chat')" aria-controls="chat-panel" :aria-expanded="chatOpen" @click="openChat" @pointerenter="warm" @focus="warm" @touchstart.passive="warm">
    <svg width="30" height="30" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M5 3.5h14a3 3 0 0 1 3 3v8a3 3 0 0 1-3 3h-7l-5 4v-4H5a3 3 0 0 1-3-3v-8a3 3 0 0 1 3-3z" /><path fill="currentColor" stroke="none" d="M12 6.8l1.4 2.9 2.9 1.4-2.9 1.4L12 15.4l-1.4-2.9-2.9-1.4 2.9-1.4z" /></svg>
  </button>
  <ChatWidget v-if="chatLoaded" v-show="chatOpen" @close="chatOpen = false" />
</template>
