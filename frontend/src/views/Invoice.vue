<script setup>
import Icon from '../components/Icon.vue'
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import { api } from '../api'
import { ron } from '../store'

const TVA = 1.21
const route = useRoute()
const o = ref(null)
const error = ref('')
api('/orders/' + route.params.id).then(x => (o.value = x)).catch(e => (error.value = e.message))
const net = v => v / TVA
const vat = v => v - v / TVA
const print = () => window.print()
</script>

<template>
  <div v-if="error" class="panel err">{{ error }}</div>
  <div v-else-if="o" class="panel">
    <div class="row" style="justify-content:space-between;flex-wrap:wrap;gap:20px">
      <div>
        <h1>FACTURĂ</h1>
        <b>{{ o.invoiceNumber }}</b><br>
        <span class="mut">Data: {{ o.date }}</span><br>
        <span class="mut">Comanda #{{ o.id }} · {{ o.paymentMethod === 'ramburs' ? 'Plată ramburs la livrare' : o.paymentMethod === 'transfer' ? 'Plată prin transfer bancar' : 'Plătită cu cardul **** ' + o.cardLast4 }}<template v-if="o.stripeSessionId"> · Confirmare Stripe (mod test): <b>{{ o.stripeSessionId.slice(0, 14) }}…{{ o.stripeSessionId.slice(-6) }}</b></template></span>
      </div>
      <div><b>Furnizor</b><br>MobilaViva SRL (date demo)<br>CUI RO12345678 · J40/0000/2026<br>Str. Exemplu 1, București<br>contact@mobilaviva.com</div>
      <div><b>Client</b><br>{{ o.name }}<br>{{ o.address }}<br>{{ o.email }}</div>
    </div>
    <table style="margin-top:20px">
      <thead>
        <tr><th>Nr.</th><th>Produs</th><th>Cant.</th><th>Preț unitar fără TVA</th><th>Valoare fără TVA</th><th>TVA 21%</th><th>Valoare cu TVA</th></tr>
      </thead>
      <tbody>
        <tr v-for="(i, k) in o.items" :key="i.id">
          <td>{{ k + 1 }}</td><td>{{ i.name }}</td><td>{{ i.qty }}</td>
          <td>{{ ron(net(i.price)) }}</td><td>{{ ron(net(i.price * i.qty)) }}</td>
          <td>{{ ron(vat(i.price * i.qty)) }}</td><td>{{ ron(i.price * i.qty) }}</td>
        </tr>
        <tr v-if="o.shipping > 0">
          <td>-</td><td>Transport</td><td>1</td>
          <td>{{ ron(net(o.shipping)) }}</td><td>{{ ron(net(o.shipping)) }}</td>
          <td>{{ ron(vat(o.shipping)) }}</td><td>{{ ron(o.shipping) }}</td>
        </tr>
        <tr v-if="o.assembly > 0">
          <td>-</td><td>Montaj</td><td>1</td>
          <td>{{ ron(net(o.assembly)) }}</td><td>{{ ron(net(o.assembly)) }}</td>
          <td>{{ ron(vat(o.assembly)) }}</td><td>{{ ron(o.assembly) }}</td>
        </tr>
        <tr v-if="o.discount > 0">
          <td>-</td><td>Reducere (cod {{ o.couponCode }})</td><td>1</td>
          <td>-{{ ron(net(o.discount)) }}</td><td>-{{ ron(net(o.discount)) }}</td>
          <td>-{{ ron(vat(o.discount)) }}</td><td>-{{ ron(o.discount) }}</td>
        </tr>
      </tbody>
    </table>
    <div style="max-width:340px;margin-left:auto;margin-top:14px">
      <div class="tot"><span>Total fără TVA</span><span>{{ ron(net(o.total)) }}</span></div>
      <div class="tot"><span>TVA 21%</span><span>{{ ron(vat(o.total)) }}</span></div>
      <div class="tot"><b>Total de plată</b><b>{{ ron(o.total) }}</b></div>
    </div>
    <p class="noprint"><button @click="print"><Icon name="printer" :size="18" />Tipărește</button></p>
  </div>
</template>
