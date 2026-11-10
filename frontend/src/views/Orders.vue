<script setup>
import { ref } from 'vue'
import { api } from '../api'
import { t, locale } from '../i18n'
import { ron } from '../store'

const orders = ref(null)
api('/orders').then(x => (orders.value = x))

// raw server status -> translation key suffix; unknown statuses are shown as-is
const STATUS = { 'Așteaptă plata': 'awaiting_payment', 'Plată la livrare': 'cod', 'Plătită': 'paid', 'Expediată': 'shipped', 'Livrată': 'delivered', 'Anulată': 'cancelled' }
const statusLabel = s => (STATUS[s] ? t('ords_status_' + STATUS[s]) : s)
</script>

<template>
  <h1 class="pagetitle">{{ t('ords_title') }}</h1>
  <template v-if="orders">
    <div v-if="!orders.length" class="panel">{{ t('ords_empty') }} <RouterLink to="/catalog">{{ t('ords_see_catalog') }}</RouterLink></div>
    <div v-else class="panel">
      <table>
        <thead><tr><th>#</th><th>{{ t('ords_date') }}</th><th>{{ t('ords_status') }}</th><th>{{ t('ords_total') }}</th><th></th></tr></thead>
        <tbody>
          <tr v-for="o in orders" :key="o.id">
            <td>{{ o.id }}</td><td>{{ o.date }}</td><td>{{ statusLabel(o.status) }}</td><td>{{ ron(o.total) }}</td>
            <td><RouterLink :to="`/order/${o.id}`">{{ t('ords_details') }}</RouterLink> · <RouterLink :to="`/invoice/${o.id}`">{{ t('ords_invoice') }}</RouterLink></td>
          </tr>
        </tbody>
      </table>
    </div>
  </template>
</template>
