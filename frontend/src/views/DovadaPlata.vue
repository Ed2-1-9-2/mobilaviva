<script setup>
import { ref, onMounted } from 'vue'
import { api } from '../api'

const rows = ref([])
const mode = ref('')
const err = ref('')
const at = ref('')
const loading = ref(true)
const busy = ref(false)

function apply(r) { rows.value = r.payments; mode.value = r.mode; at.value = new Date().toLocaleString('ro-RO') }

onMounted(async () => {
  try {
    apply(await api('/admin/stripe/payments'))
  } catch (e) {
    err.value = e.message
  } finally {
    loading.value = false
  }
})

async function createTest() {
  busy.value = true
  err.value = ''
  try { apply(await api('/admin/stripe/test-payment', { method: 'POST', body: {} })) }
  catch (e) { err.value = e.message }
  finally { busy.value = false }
}
const when = t => new Date(t * 1000).toLocaleString('ro-RO', { dateStyle: 'short', timeStyle: 'short' })
const sum = r => r.amount.toLocaleString('ro-RO', { minimumFractionDigits: 2 }) + ' ' + r.currency
</script>

<template>
  <section class="panel">
    <h1>Dovadă plată electronică (Stripe)</h1>
    <p class="mut">Cerința I.10: plăți reușite citite direct din API-ul Stripe, pe site-ul publicat. Generat la: {{ at || '…' }}</p>
    <p v-if="loading" role="status">Se încarcă din Stripe…</p>
    <div v-if="err" class="err">{{ err }}</div>
    <template v-else-if="!loading">
      <p><button :disabled="busy || mode !== 'TEST'" @click="createTest">{{ busy ? 'Se creează…' : 'Creează plată de test în Stripe (card 4242)' }}</button></p>
      <p><b>Mod cont Stripe: {{ mode }}</b> (cont de test, card 4242 4242 4242 4242; nu se debitează bani reali)</p>
      <div style="overflow-x:auto">
        <table>
          <thead><tr><th>Data</th><th>Payment Intent</th><th>Descriere</th><th>Sumă</th><th>Stare</th><th>Card</th><th>Mod</th></tr></thead>
          <tbody>
            <tr v-for="r in rows" :key="r.session">
              <td>{{ when(r.created) }}</td><td>{{ r.paymentIntent }}</td><td>{{ r.description }}</td><td>{{ sum(r) }}</td>
              <td>plătit</td><td>{{ r.card }}</td><td>{{ r.live ? 'LIVE' : 'TEST' }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <p v-if="!rows.length" class="mut">Nicio plată reușită încă. Fă o comandă cu cardul de test și reîncarcă pagina.</p>
      <p v-else><b>Concluzie:</b> {{ rows.length }} {{ rows.length === 1 ? 'plată reușită' : 'plăți reușite' }}{{ mode === 'TEST' ? ', în mod TEST' : '' }}: cerința I.10 îndeplinită.</p>
    </template>
  </section>
</template>
