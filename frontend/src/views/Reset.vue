<script setup>
import { t } from '../i18n'
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../api'
import { refresh } from '../store'
import PasswordInput from '../components/PasswordInput.vue'

const route = useRoute()
const router = useRouter()
const token = typeof route.query.token === 'string' ? route.query.token : ''
const password = ref('')
const error = ref(token ? '' : t('rs_notoken'))
const busy = ref(false)

async function submit() {
  if (busy.value || !token) return
  error.value = ''
  busy.value = true
  try {
    await api('/auth/reset', { method: 'POST', body: { token, password: password.value } })
    await refresh()          // serverul te-a autentificat deja cu parola nouă
    router.replace('/')      // token-ul dispare din istoric
  } catch (e) {
    error.value = e.errors?.length ? e.errors.join(' ') : e.message
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <form class="panel form narrow" @submit.prevent="submit">
    <h1>{{ t('rs_title') }}</h1>
    <div v-if="error" class="err" role="alert">{{ error }}</div>
    <template v-if="token">
      <PasswordInput v-model="password" :minlength="8" :placeholder="t('rs_new')" autocomplete="new-password" />
      <button :disabled="busy">{{ busy ? t('rs_busy') : t('rs_submit') }}</button>
    </template>
    <RouterLink v-else to="/forgot">{{ t('fg_title') }}</RouterLink>
  </form>
</template>
