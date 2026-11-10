<script setup>
// Câmp de parolă cu buton Arată/Ascunde: înlocuiește câmpul „Repetă parola” (greșelile se văd direct)
import { ref } from 'vue'
import { t } from '../i18n'
defineProps({ modelValue: { type: String, default: '' }, placeholder: { type: String, default: '' }, autocomplete: { type: String, default: 'current-password' }, minlength: { type: Number, default: undefined } })
defineEmits(['update:modelValue'])
const show = ref(false)
</script>

<template>
  <div class="pw">
    <input :value="modelValue" @input="$emit('update:modelValue', $event.target.value)" :type="show ? 'text' : 'password'" :placeholder="placeholder" :aria-label="placeholder" :autocomplete="autocomplete" :minlength="minlength" maxlength="128" required>
    <button type="button" class="pwt" :aria-pressed="show" @click="show = !show">{{ show ? t('pw_hide') : t('pw_show') }}</button>
  </div>
</template>
