<script setup>
import { ref } from 'vue'
import { api } from '../api'
import { t } from '../i18n'
import { pname } from '../pname'
import { ron, refresh, imgSrc, imgFallback, dateText } from '../store'

const cart = ref(null)
api('/cart').then(c => (cart.value = c))

async function setQty(l, q) {
  cart.value = await api('/cart/items/' + l.product.id, { method: 'PUT', body: { qty: Number(q) } })
  await refresh()
}

// Cod promoțional și montaj opțional (calculate pe server; totalul vine din /api/cart)
const code = ref('')
const cerr = ref('')
async function applyCoupon() {
  cerr.value = ''
  try { cart.value = await api('/cart/coupon', { method: 'POST', body: { code: code.value } }); code.value = '' } catch (e) { cerr.value = e.message }
}
async function removeCoupon() { cerr.value = ''; cart.value = await api('/cart/coupon', { method: 'DELETE' }) }
async function setAssembly(on) { cart.value = await api('/cart/assembly', { method: 'PUT', body: { on } }) }
</script>

<template>
  <h1 class="pagetitle">{{ t('cart_title') }}</h1>
  <template v-if="cart">
    <div v-if="!cart.lines.length" class="panel">{{ t('cart_empty') }} <RouterLink to="/catalog">{{ t('cart_view') }}</RouterLink></div>
    <div v-else class="panel">
      <table class="cart-table">
        <thead><tr><th>{{ t('cart_product') }}</th><th>{{ t('cart_price') }}</th><th>{{ t('cart_qty') }}</th><th>{{ t('cart_subtotal') }}</th></tr></thead>
        <tbody>
          <tr v-for="l in cart.lines" :key="l.product.id">
            <td><div class="cartprod"><img class="thumb" :src="imgSrc(l.product)" @error="imgFallback($event, l.product)" width="96" height="67" alt="" loading="lazy"><RouterLink :to="`/product/${l.product.id}`">{{ pname(l.product.name) }}</RouterLink></div></td>
            <td :data-label="t('cart_price')">{{ ron(l.product.price) }}</td>
            <td>
              <form @submit.prevent="setQty(l, $event.target.qty.value)">
                <input name="qty" type="number" min="0" :max="l.product.stock" :value="l.qty" :aria-label="t('cart_qty')">
                <button class="sec">{{ t('cart_update') }}</button>
                <button type="button" class="sec rm" :aria-label="t('cart_remove') + ': ' + pname(l.product.name)" @click="setQty(l, 0)">{{ t('cart_remove') }}</button>
              </form>
            </td>
            <td :data-label="t('cart_subtotal')">{{ ron(l.sub) }}</td>
          </tr>
        </tbody>
      </table>
      <div style="max-width:340px;margin-left:auto;margin-top:14px">
        <form v-if="!cart.couponCode" class="row" @submit.prevent="applyCoupon">
          <input v-model="code" :placeholder="t('cp_ph')" :aria-label="t('cp_label')" maxlength="30" autocomplete="off" style="text-transform:uppercase">
          <button class="sec" :disabled="!code.trim()">{{ t('cp_apply') }}</button>
        </form>
        <p v-else class="ok"><b>{{ cart.couponCode }}</b> <button type="button" class="link" @click="removeCoupon">{{ t('cp_remove') }}</button></p>
        <p v-if="cerr" class="bad">{{ cerr }}</p>
        <label v-if="cart.assemblyOffer > 0" class="check"><input type="checkbox" :checked="cart.assemblyOn" @change="setAssembly($event.target.checked)"><span>{{ t('asm_opt', { price: ron(cart.assemblyOffer) }) }}</span></label>
        <div class="tot"><span>{{ t('cart_subtotal') }}</span><span>{{ ron(cart.subtotal) }}</span></div>
        <div v-if="cart.discount > 0" class="tot"><span>{{ t('cp_discount', { code: cart.couponCode }) }}</span><span>-{{ ron(cart.discount) }}</span></div>
        <div v-if="cart.assembly > 0" class="tot"><span>{{ t('asm_line') }}</span><span>{{ ron(cart.assembly) }}</span></div>
        <div class="tot"><span>{{ t('cart_shipping') }}</span><span>{{ cart.shipping === 0 ? t('cart_free') : ron(cart.shipping) }}</span></div>
        <div class="tot"><b>{{ t('cart_total') }}</b><b>{{ ron(cart.total) }}</b></div>
        <p class="mut">{{ t('cart_eta', { days: cart.days, from: cart.freeFrom }) }}</p>
        <p class="mut">{{ t('cart_eta_date', { date: dateText(cart.eta) }) }}</p>
        <RouterLink class="btn" to="/checkout">{{ t('cart_checkout') }}</RouterLink>
      </div>
    </div>
  </template>
</template>
