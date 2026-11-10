<script setup>
import { t } from '../i18n'
import { reactive, ref } from 'vue'
import { api } from '../api'
import { store } from '../store'
import PasswordInput from '../components/PasswordInput.vue'

const f = reactive({ current: '', password: '' })
const error = ref('')
const ok = ref(false)
const busy = ref(false)

async function submit() {
  if (busy.value) return
  error.value = ''
  ok.value = false
  busy.value = true
  try {
    await api('/auth/password', { method: 'POST', body: { current: f.current, password: f.password } })
    ok.value = true
    f.current = ''; f.password = ''
  } catch (e) {
    error.value = e.errors?.length ? e.errors.join(' ') : e.message
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <form class="panel form narrow" @submit.prevent="submit">
    <h1>{{ t('ac_title') }}</h1>
    <p class="mut">{{ store.user && store.user.email }}</p>
    <h2>{{ t('ac_pw_h') }}</h2>
    <div v-if="error" class="err" role="alert">{{ error }}</div>
    <div v-if="ok" class="ok" role="status">{{ t('ac_done') }}</div>
    <PasswordInput v-model="f.current" :placeholder="t('ac_cur')" autocomplete="current-password" />
    <PasswordInput v-model="f.password" :minlength="8" :placeholder="t('rs_new')" autocomplete="new-password" />
    <button :disabled="busy">{{ busy ? t('rs_busy') : t('ac_submit') }}</button>
    <span class="mut"><RouterLink to="/forgot">{{ t('ac_forgot') }}</RouterLink></span>
  </form>
</template>
