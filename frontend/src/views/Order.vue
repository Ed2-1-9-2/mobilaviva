<script setup>
import Icon from '../components/Icon.vue'
import { ref } from 'vue'
import { t } from '../i18n'
import { pname } from '../pname'
import { useRoute } from 'vue-router'
import { api } from '../api'
import { ron, store } from '../store'

const route = useRoute()
const o = ref(null)
const error = ref('')
const STATUS = { 'Așteaptă plata': 'awaiting_payment', 'Plată la livrare': 'cod', 'Plătită': 'paid', 'Expediată': 'shipped', 'Livrată': 'delivered', 'Anulată': 'cancelled' }
const statusLabel = s => (STATUS[s] ? t('ord_status_' + STATUS[s]) : s)
const payLabel = o => (o.paymentMethod === 'ramburs' ? t('ord_pay_cod') : o.paymentMethod === 'transfer' ? t('ord_pay_transfer') : t('ord_pay_card', { last4: o.cardLast4 }))
api('/orders/' + route.params.id).then(x => (o.value = x)).catch(e => (error.value = e.message))
</script>

<template>
  <div v-if="error" class="panel err">{{ error }}</div>
  <div v-else-if="o" class="panel">
    <div class="big ok"><Icon name="check-circle" :size="56" /></div>
    <h1 style="text-align:center">{{ t('ord_thanks', { name: o.name, id: o.id }) }}</h1>
    <p style="text-align:center" class="mut">
      {{ t('ord_status') }}: {{ statusLabel(o.status) }} · {{ t('ord_confirmation_sent', { email: o.email }) }} · {{ payLabel(o) }}{{ o.stripeSessionId ? ' (Stripe test ' + o.stripeSessionId.slice(0, 14) + '…)' : '' }} · {{ t('ord_eta') }}: {{ o.eta }}
    </p>
    <p v-if="o.paymentMethod === 'transfer'" class="note">{{ t('ord_transfer_pre') }} <b>{{ ron(o.total) }}</b> {{ t('ord_transfer_mid') }} <b>{{ store.iban }}</b> {{ t('ord_transfer_post', { id: o.id }) }}</p>
    <p v-else-if="o.paymentMethod === 'ramburs'" class="note">{{ t('ord_cod_pre') }} <b>{{ ron(o.total) }}</b> {{ t('ord_cod_post') }}</p>
    <table>
      <tr v-for="i in o.items" :key="i.id"><td>{{ i.qty }} × {{ pname(i.name) }}</td><td style="text-align:right">{{ ron(i.price * i.qty) }}</td></tr>
      <tr><td>{{ t('ord_shipping') }}</td><td style="text-align:right">{{ o.shipping === 0 ? t('ord_free') : ron(o.shipping) }}</td></tr>
      <tr v-if="o.assembly > 0"><td>{{ t('asm_line') }}</td><td style="text-align:right">{{ ron(o.assembly) }}</td></tr>
      <tr v-if="o.discount > 0"><td>{{ t('cp_discount', { code: o.couponCode }) }}</td><td style="text-align:right">-{{ ron(o.discount) }}</td></tr>
      <tr><td><b>{{ t('ord_total') }}</b></td><td style="text-align:right"><b>{{ ron(o.total) }}</b></td></tr>
    </table>
    <p style="text-align:center">
      <RouterLink class="btn" :to="`/invoice/${o.id}`"><Icon name="receipt" :size="18" />{{ t('ord_view_invoice') }}</RouterLink>
      <RouterLink class="btn" to="/catalog">{{ t('ord_continue') }}</RouterLink>
    </p>
  </div>
</template>
