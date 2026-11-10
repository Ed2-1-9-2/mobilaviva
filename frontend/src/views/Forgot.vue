<script setup>
import { t } from '../i18n'
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import { api } from '../api'

const qe = useRoute().query.email
const email = ref(typeof qe === 'string' ? qe.slice(0, 160) : '')
const error = ref('')
const busy = ref(false)
const done = ref(false)

async function submit() {
  if (busy.value) return
  error.value = ''
  busy.value = true
  try {
    await api('/auth/forgot', { method: 'POST', body: { email: email.value, origin: window.location.origin } })
    done.value = true
  } catch (e) {
    error.value = e.message
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <div v-if="done" class="panel form narrow">
    <h1>{{ t('fg_title') }}</h1>
    <p role="status">{{ t('fg_done') }}</p>
    <p><strong>{{ email }}</strong></p>
    <RouterLink to="/login">{{ t('fg_back') }}</RouterLink>
  </div>
  <form v-else class="panel form narrow" @submit.prevent="submit">
    <h1>{{ t('fg_title') }}</h1>
    <p class="mut">{{ t('fg_intro') }}</p>
    <div v-if="error" class="err" role="alert">{{ error }}</div>
    <input v-model="email" type="email" :placeholder="t('login_email')" required autocomplete="email" :aria-label="t('login_email')">
    <button :disabled="busy">{{ busy ? t('fg_busy') : t('fg_send') }}</button>
    <RouterLink to="/login">{{ t('fg_back') }}</RouterLink>
  </form>
</template>
