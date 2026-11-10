<script setup>
import { t, i18n } from '../i18n'
import Icon from '../components/Icon.vue'
import { ref, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../api'
import { store, ron, refresh, dateText } from '../store'
import { pname } from '../pname'

const route = useRoute()
const router = useRouter()
const cart = ref(null)
const errors = ref([])
const busy = ref(false)
const accept = ref(false)
const method = ref('card')
const f = reactive({ name: store.user?.name || '', email: store.user?.email || '', address: '', card: '', exp: '', cvv: '' })

api('/cart').then(c => {
  if (!c.lines.length) router.replace('/cart')
  cart.value = c
})

async function pay() {
  errors.value = []
  if (!accept.value) { errors.value = [t('co_err_terms')]; return }
  busy.value = true
  try {
    if (method.value !== 'card') {
      const o = await api('/checkout/offline', { method: 'POST', body: { name: f.name, email: f.email, address: f.address, method: method.value } })
      await refresh()
      router.push('/order/' + o.id)
      return
    }
    if (store.payment === 'stripe') {
      const r = await api('/checkout/stripe', { method: 'POST', body: { name: f.name, email: f.email, address: f.address, origin: window.location.origin, lang: i18n.lang } })
      window.location.href = r.url
      return
    }
    const o = await api('/checkout', { method: 'POST', body: { ...f } })
    await refresh()
    router.push('/order/' + o.id)
  } catch (e) {
    errors.value = e.errors?.length ? e.errors : [e.message]
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <ol class="steps" :aria-label="t('co_steps')"><li>{{ t('co_step1') }}</li><li class="on" aria-current="step">{{ t('co_step2') }}</li><li>{{ t('co_step3') }}</li></ol>
  <h1 class="pagetitle">{{ t('co_title') }}</h1>
  <div v-if="cart" class="two">
    <form class="panel form" @submit.prevent="pay">
      <div v-if="route.query.cancelled" class="note">{{ t('co_cancelled') }}</div>
      <div v-for="e in errors" :key="e" class="err">{{ e }}</div>
      <h2>{{ t('co_h_delivery') }}</h2>
      <input v-model="f.name" :placeholder="t('co_fullname')" autocomplete="name" required :aria-label="t('co_name')">
      <input v-model="f.email" type="email" :placeholder="t('co_email')" autocomplete="email" required :aria-label="t('co_email')">
      <textarea v-model="f.address" rows="2" :placeholder="t('co_address_ph')" autocomplete="street-address" required :aria-label="t('co_address')" />
      <h2>{{ t('co_h_method') }}</h2>
      <label class="pm"><input v-model="method" type="radio" value="card"><span><b><Icon name="card" :size="18" /> {{ t('co_pm_card') }}</b><small>{{ t('co_pm_card_sub') }}</small></span></label>
      <label class="pm"><input v-model="method" type="radio" value="ramburs"><span><b><Icon name="truck" :size="18" /> {{ t('co_pm_cod') }}</b><small>{{ t('co_pm_cod_sub') }}</small></span></label>
      <label class="pm"><input v-model="method" type="radio" value="transfer"><span><b><Icon name="bank" :size="18" /> {{ t('co_pm_bank') }}</b><small>{{ t('co_pm_bank_sub') }}</small></span></label>
      <template v-if="method === 'card' && store.payment !== 'stripe'">
        <h2>{{ t('co_h_card') }} <small>{{ t('co_h_card_test') }}</small></h2>
        <p class="note">{{ t('co_testcard') }} <b>4242 4242 4242 4242</b> · {{ t('co_expiry_word') }} <b>12/30</b> · CVV <b>123</b></p>
        <input v-model="f.card" :placeholder="t('co_card_number_ph')" inputmode="numeric" autocomplete="off" required :aria-label="t('co_card_number')">
        <div class="row">
          <input v-model="f.exp" :placeholder="t('co_exp_ph')" required :aria-label="t('co_exp')">
          <input v-model="f.cvv" placeholder="CVV" inputmode="numeric" required aria-label="CVV">
        </div>
      </template>
      <p v-else-if="method === 'card'" class="note">{{ t('co_stripe_pre') }} <b>4242 4242 4242 4242</b>{{ t('co_stripe_post') }}</p>
      <p v-else-if="method === 'ramburs'" class="note">{{ t('co_cod_pre') }} <b>{{ ron(cart.total) }}</b> {{ t('co_cod_post') }}</p>
      <p v-else class="note">{{ t('co_bank_pre') }} <b>{{ ron(cart.total) }}</b> {{ t('co_bank_mid') }} <b>{{ store.iban }}</b> {{ t('co_bank_post') }}</p>
      <label class="check">
        <input v-model="accept" type="checkbox" required>
        <span>{{ t('co_agree_pre') }} <RouterLink to="/termeni" target="_blank">{{ t('co_terms') }}</RouterLink> {{ t('co_agree_mid') }} <RouterLink to="/retur" target="_blank">{{ t('co_returns') }}</RouterLink>{{ t('co_agree_post') }}</span>
      </label>
      <button :disabled="busy || !accept">{{ method !== 'card' ? t('co_place') + ' ' + ron(cart.total) : store.payment === 'stripe' ? t('co_continue_stripe') : t('co_pay') + ' ' + ron(cart.total) }}</button>
      <div class="trust"><span><Icon name="lock" :size="16" />{{ t('co_trust_secure') }}</span><span><Icon name="undo" :size="16" />{{ t('co_trust_return') }}</span><span><Icon name="receipt" :size="16" />{{ t('co_trust_vat') }}</span></div>
      <span v-if="method === 'card' && store.payment !== 'stripe'" class="mut">{{ t('co_last4') }}</span>
    </form>
    <div class="panel sum">
      <h2>{{ t('co_h_proforma') }}</h2>
      <p class="mut">{{ t('co_proforma_note') }}</p>
      <table class="pro-table">
        <thead><tr><th>{{ t('co_th_product') }}</th><th>{{ t('co_th_qty') }}</th><th>{{ t('co_th_exvat') }}</th><th>{{ t('co_th_vat') }}</th><th>{{ t('co_th_total') }}</th></tr></thead>
        <tbody>
          <tr v-for="l in cart.lines" :key="l.product.id">
            <td>{{ pname(l.product.name) }}</td><td :data-l="t('co_th_qty')">{{ l.qty }}</td><td :data-l="t('co_th_exvat')">{{ ron(l.sub / 1.21) }}</td><td :data-l="t('co_th_vat')">{{ ron(l.sub - l.sub / 1.21) }}</td><td :data-l="t('co_th_total')">{{ ron(l.sub) }}</td>
          </tr>
          <tr>
            <td>{{ t('co_shipping') }}</td><td :data-l="t('co_th_qty')">1</td><td :data-l="t('co_th_exvat')">{{ ron(cart.shipping / 1.21) }}</td><td :data-l="t('co_th_vat')">{{ ron(cart.shipping - cart.shipping / 1.21) }}</td><td :data-l="t('co_th_total')">{{ cart.shipping === 0 ? t('co_free') : ron(cart.shipping) }}</td>
          </tr>
          <tr v-if="cart.assembly > 0">
            <td>{{ t('asm_line') }}</td><td :data-l="t('co_th_qty')">1</td><td :data-l="t('co_th_exvat')">{{ ron(cart.assembly / 1.21) }}</td><td :data-l="t('co_th_vat')">{{ ron(cart.assembly - cart.assembly / 1.21) }}</td><td :data-l="t('co_th_total')">{{ ron(cart.assembly) }}</td>
          </tr>
          <tr v-if="cart.discount > 0">
            <td>{{ t('cp_discount', { code: cart.couponCode }) }}</td><td :data-l="t('co_th_qty')">1</td><td :data-l="t('co_th_exvat')">-{{ ron(cart.discount / 1.21) }}</td><td :data-l="t('co_th_vat')">-{{ ron(cart.discount - cart.discount / 1.21) }}</td><td :data-l="t('co_th_total')">-{{ ron(cart.discount) }}</td>
          </tr>
        </tbody>
      </table>
      <div class="tot"><span>{{ t('co_total_exvat') }}</span><span>{{ ron(cart.total / 1.21) }}</span></div>
      <div class="tot"><span>{{ t('co_th_vat') }}</span><span>{{ ron(cart.total - cart.total / 1.21) }}</span></div>
      <div class="tot"><b>{{ t('co_total_due') }}</b><b>{{ ron(cart.total) }}</b></div>
      <p class="mut">{{ t('co_delivery_est', { n: cart.days }) }}</p>
      <p class="mut">{{ t('cart_eta_date', { date: dateText(cart.eta) }) }}</p>
    </div>
  </div>
</template>
