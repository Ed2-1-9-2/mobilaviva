<script setup>
import { t } from '../i18n'
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../api'
import { refresh } from '../store'

const route = useRoute()
const router = useRouter()
const error = ref('')

// Stripe ne întoarce aici după plată; serverul verifică sesiunea direct la Stripe înainte să creeze comanda.
api('/checkout/stripe/confirm', { method: 'POST', body: { sessionId: route.query.session_id } })
  .then(async o => { await refresh(); router.replace('/order/' + o.id) })
  .catch(e => (error.value = e.message))
</script>

<template>
  <div v-if="error" class="panel err">{{ error }} <RouterLink to="/cart">{{ t('cos_back') }}</RouterLink></div>
  <div v-else class="panel">{{ t('cos_confirming') }}</div>
</template>
