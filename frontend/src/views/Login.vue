<script setup>
import PasswordInput from '../components/PasswordInput.vue'
import { t } from '../i18n'
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../api'
import { refresh, safeNext } from '../store'

const route = useRoute()
const router = useRouter()
const f = reactive({ email: '', password: '' })
const error = ref('')
const busy = ref(false)

async function submit() {
  if (busy.value) return
  error.value = ''
  busy.value = true
  try {
    await api('/auth/login', { method: 'POST', body: { ...f } })
    await refresh()
    router.push(safeNext(route.query.next))
  } catch (e) {
    error.value = e.message
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <form class="panel form narrow" @submit.prevent="submit">
    <h1>{{ t('login_title') }}</h1>
    <div v-if="error" class="err">{{ error }}</div>
    <input v-model="f.email" type="email" name="email" autocomplete="username" :placeholder="t('login_email')" required :aria-label="t('login_email')">
    <PasswordInput v-model="f.password" :placeholder="t('login_password')" autocomplete="current-password" />
    <button :disabled="busy">{{ busy ? t('login_busy') : t('login_submit') }}</button>
    <span v-if="busy" class="mut" role="status">{{ t('login_wait') }}</span>
    <span class="mut"><RouterLink :to="{ path: '/forgot', query: f.email ? { email: f.email } : {} }">{{ t('fg_link') }}</RouterLink></span>
    <span class="mut">{{ t('login_noacc') }} <RouterLink :to="{ path: '/register', query: route.query }">{{ t('login_create') }}</RouterLink></span>
  </form>
</template>
