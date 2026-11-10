<script setup>
import PasswordInput from '../components/PasswordInput.vue'
import { t } from '../i18n'
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../api'
import { refresh, safeNext } from '../store'

const route = useRoute()
const router = useRouter()
const f = reactive({ name: '', email: '', password: '', acceptTerms: false })
const errors = ref([])

async function submit() {
  errors.value = []
  try {
    await api('/auth/register', { method: 'POST', body: { ...f } })
    await refresh()
    router.push(safeNext(route.query.next))
  } catch (e) {
    errors.value = e.errors?.length ? e.errors : [e.message]
  }
}
</script>

<template>
  <form class="panel form narrow" @submit.prevent="submit">
    <h1>{{ t('register_title') }}</h1>
    <div v-for="e in errors" :key="e" class="err">{{ e }}</div>
    <input v-model="f.name" :placeholder="t('register_fullname')" required :aria-label="t('register_name')">
    <input v-model="f.email" type="email" name="email" autocomplete="username" :placeholder="t('register_email')" required :aria-label="t('register_email')">
    <PasswordInput v-model="f.password" :minlength="8" :placeholder="t('register_password_hint')" autocomplete="new-password" />
    <label class="check">
      <input v-model="f.acceptTerms" type="checkbox" required>
      <span>{{ t('register_agree_pre') }} <RouterLink to="/termeni" target="_blank">{{ t('register_terms') }}</RouterLink> {{ t('register_and') }} <RouterLink to="/confidentialitate" target="_blank">{{ t('register_privacy') }}</RouterLink>{{ t('register_agree_post') }}</span>
    </label>
    <button :disabled="!f.acceptTerms">{{ t('register_submit') }}</button>
    <span class="mut">{{ t('register_have') }} <RouterLink :to="{ path: '/login', query: route.query }">{{ t('register_login') }}</RouterLink></span>
  </form>
</template>
