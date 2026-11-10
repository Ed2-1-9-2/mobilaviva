<script setup>
import { ref } from 'vue'
import { api } from '../api'
import { store, ron } from '../store'

const STATUSES = ['Așteaptă plata', 'Plată la livrare', 'Plătită', 'Expediată', 'Livrată', 'Anulată']
const tab = ref('orders')
const orders = ref([])
const products = ref([])
const users = ref([])
const db = ref(null)
const email = ref(null)
const emailTo = ref('')
const emailBusy = ref(false)
const emailResult = ref(null)
const edit = ref(null)
const err = ref('')

async function load() {
  if (tab.value === 'orders') orders.value = await api('/admin/orders')
  else if (tab.value === 'products') products.value = await api('/admin/products')
  else if (tab.value === 'db') db.value = await api('/admin/db')
  else if (tab.value === 'email') { email.value = await api('/admin/email/status'); emailResult.value = null }
  else users.value = await api('/admin/users')
}
function open(t) { tab.value = t; edit.value = null; err.value = ''; load() }
async function setStatus(o) { await api(`/admin/orders/${o.id}/status`, { method: 'PUT', body: { status: o.status } }) }
async function sendTestEmail() {
  if (emailBusy.value || !emailTo.value) return
  emailBusy.value = true
  emailResult.value = null
  try { emailResult.value = await api('/admin/email/test', { method: 'POST', body: { email: emailTo.value } }) }
  catch (e) { emailResult.value = { ok: false, error: e.message, methods: [] } }
  finally { emailBusy.value = false; email.value = await api('/admin/email/status') }
}

const blank = () => ({ id: null, name: '', brand: '', category: '', price: 0, oldPrice: 0, stock: 0, deliveryDays: 5, rating: 4, material: '', dimensions: '', color: '', manufacturer: '', originPlace: '', originCountry: '', production: '', imageUrl: '', description: '' })
async function save() {
  err.value = ''
  try {
    const p = edit.value
    await api(p.id ? `/admin/products/${p.id}` : '/admin/products', { method: p.id ? 'PUT' : 'POST', body: p })
    edit.value = null
    load()
  } catch (e) {
    err.value = (e.errors || []).join(' ') || e.message
  }
}
async function del(p) {
  if (confirm('Ștergi produsul?')) { await api('/admin/products/' + p.id, { method: 'DELETE' }); load() }
}
async function setRole(u) {
  try { await api(`/admin/users/${u.id}/role`, { method: 'PUT', body: { role: u.role } }) } catch (e) { alert(e.message); load() }
}
async function delUser(u) {
  if (!confirm('Ștergi utilizatorul?')) return
  try { await api('/admin/users/' + u.id, { method: 'DELETE' }) } catch (e) { alert(e.message) }
  load()
}
load()
</script>

<template>
  <h1 class="pagetitle">Administrare</h1>
  <nav class="tabs">
    <a href="#" @click.prevent="open('orders')">Comenzi</a>
    <a href="#" @click.prevent="open('products')">Produse</a>
    <a href="#" @click.prevent="open('users')">Utilizatori</a>
    <a href="#" @click.prevent="open('email')">Email</a>
    <a href="#" @click.prevent="open('db')">Baza de date</a>
  </nav>

  <div v-if="tab === 'orders'" class="panel">
    <table>
      <thead><tr><th>#</th><th>Data</th><th>Client</th><th>Total</th><th>Status</th><th></th></tr></thead>
      <tbody>
        <tr v-for="o in orders" :key="o.id">
          <td>{{ o.id }}</td><td>{{ o.date }}</td>
          <td>{{ o.name }}<br><span class="mut">{{ o.email }}</span></td>
          <td>{{ ron(o.total) }}</td>
          <td><select v-model="o.status" aria-label="Status" @change="setStatus(o)"><option v-for="s in STATUSES" :key="s">{{ s }}</option></select></td>
          <td><RouterLink :to="`/invoice/${o.id}`">Factură</RouterLink></td>
        </tr>
      </tbody>
    </table>
  </div>

  <div v-if="tab === 'products'">
    <p><button @click="edit = blank()">+ Produs nou</button></p>
    <form v-if="edit" class="panel form" style="max-width:640px" @submit.prevent="save">
      <h2>{{ edit.id ? 'Editare produs #' + edit.id : 'Produs nou' }}</h2>
      <div v-if="err" class="err">{{ err }}</div>
      <input v-model="edit.name" placeholder="Nume" required aria-label="Nume">
      <div class="row">
        <input v-model="edit.brand" placeholder="Brand" required aria-label="Brand">
        <input v-model="edit.category" placeholder="Categorie" required aria-label="Categorie">
      </div>
      <div class="row">
        <input v-model.number="edit.price" type="number" step="0.01" min="0" placeholder="Preț" required aria-label="Preț">
        <input v-model.number="edit.oldPrice" type="number" step="0.01" min="0" placeholder="Preț vechi (promoție, 0 = fără)" aria-label="Preț vechi">
      </div>
      <div class="row">
        <input v-model.number="edit.stock" type="number" min="0" placeholder="Stoc" required aria-label="Stoc">
        <input v-model.number="edit.deliveryDays" type="number" min="0" placeholder="Zile livrare" aria-label="Zile livrare">
        <input v-model.number="edit.rating" type="number" step="0.1" min="0" max="5" placeholder="Rating" aria-label="Rating">
      </div>
      <div class="row">
        <input v-model="edit.material" placeholder="Material" aria-label="Material">
        <input v-model="edit.dimensions" placeholder="Dimensiuni" aria-label="Dimensiuni">
        <input v-model="edit.color" placeholder="Culoare" aria-label="Culoare">
      </div>
      <div class="row">
        <input v-model="edit.manufacturer" placeholder="Producător (gol = după brand)" aria-label="Producător">
        <input v-model="edit.originPlace" placeholder="Localitatea fabricii" aria-label="Localitatea fabricii">
        <input v-model="edit.originCountry" placeholder="Țara (cod, ex. RO)" maxlength="2" aria-label="Țara fabricației">
        <input v-model="edit.production" placeholder="Mod de producție (gol = automat)" aria-label="Mod de producție">
      </div>
      <div class="row">
        <input v-model="edit.imageUrl" placeholder="URL imagine (opțional, https://...) — gol = ilustrație automată" aria-label="URL imagine">
      </div>
      <textarea v-model="edit.description" rows="3" placeholder="Descriere" aria-label="Descriere" />
      <div class="row"><button>Salvează</button><button type="button" class="sec" @click="edit = null">Renunță</button></div>
    </form>
    <div class="panel">
      <table>
        <thead><tr><th>#</th><th>Produs</th><th>Categorie</th><th>Preț</th><th>Preț vechi</th><th>Stoc</th><th></th></tr></thead>
        <tbody>
          <tr v-for="p in products" :key="p.id">
            <td>{{ p.id }}</td><td>{{ p.name }}</td><td>{{ p.category }}</td><td>{{ ron(p.price) }}</td>
            <td>{{ p.oldPrice > 0 ? ron(p.oldPrice) : '-' }}</td>
            <td :class="{ bad: p.stock <= 0 }">{{ p.stock }}</td>
            <td>
              <a href="#" @click.prevent="edit = { ...p }">Editează</a> ·
              <button class="link" @click="del(p)">Șterge</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>

  <div v-if="tab === 'email'" class="panel">
    <h2>Trimitere emailuri</h2>
    <p class="mut">Pentru fiecare cerere („Am uitat parola”, confirmări), serverul încearcă metodele de mai jos în ordine. Prima care reușește trimite emailul. Clienții văd mereu un mesaj neutru, indiferent dacă trimiterea a reușit sau nu.</p>
    <template v-if="email">
      <table>
        <thead><tr><th>Metodă</th><th>Expeditor</th><th>Stare</th><th>Ultima reușită</th><th>Ultima eroare</th></tr></thead>
        <tbody>
          <tr v-for="m in email.methods" :key="m.id">
            <td><b>{{ m.label }}</b><br><span class="mut">{{ m.hint }}</span></td>
            <td>{{ m.from || '—' }}</td>
            <td>
              <span v-if="m.configured" style="color:var(--ok)">activă</span>
              <span v-else class="mut">neconfigurată</span>
            </td>
            <td>{{ m.lastSuccessAt ? m.lastSuccessAt.replace('T',' ').slice(0,19) : '—' }}</td>
            <td :class="{ bad: m.lastError }">{{ m.lastError || '—' }}</td>
          </tr>
        </tbody>
      </table>
      <form class="form" style="margin-top:18px;max-width:520px" @submit.prevent="sendTestEmail">
        <h3 style="font-size:var(--t-base)">Trimite un email de test</h3>
        <input v-model="emailTo" type="email" placeholder="adresa@exemplu.com" required aria-label="Adresa de test">
        <button :disabled="emailBusy">{{ emailBusy ? 'Se trimite…' : 'Trimite email de test' }}</button>
        <div v-if="emailResult" :class="emailResult.ok ? 'ok' : 'err'" role="status" style="margin-top:8px">
          <span v-if="emailResult.ok">Email de test trimis către {{ emailResult.to }}. Verifică Inbox / Spam.</span>
          <span v-else>Trimitere eșuată: {{ emailResult.error || 'motiv necunoscut' }}</span>
        </div>
      </form>
    </template>
  </div>

  <div v-if="tab === 'db'" class="panel">
    <h2>Tabele din baza de date <small v-if="db">({{ db.engine }})</small></h2>
    <p class="mut">Vizualizare doar pentru citire, vizibilă numai administratorului. Valorile secrete (hash-urile parolelor) sunt ascunse.</p>
    <template v-if="db">
      <table>
        <thead><tr><th>Tabel</th><th>Rânduri</th><th>Coloane</th></tr></thead>
        <tbody><tr v-for="t in db.tables" :key="t.name"><td><b>{{ t.name }}</b></td><td>{{ t.rows }}</td><td class="mut">{{ t.columns.join(', ') }}</td></tr></tbody>
      </table>
      <div v-for="t in db.tables" :key="'s' + t.name" style="margin-top:18px">
        <h3 style="font-size:var(--t-base)">{{ t.name }} <small>(ultimele {{ t.sample.length }} rânduri)</small></h3>
        <table>
          <thead><tr><th v-for="c in t.columns" :key="c">{{ c }}</th></tr></thead>
          <tbody><tr v-for="(r, i) in t.sample" :key="i"><td v-for="(v, j) in r" :key="j">{{ v }}</td></tr></tbody>
        </table>
      </div>
    </template>
  </div>

  <div v-if="tab === 'users'" class="panel">
    <table>
      <thead><tr><th>#</th><th>Nume</th><th>Email</th><th>Rol</th><th>Creat</th><th></th></tr></thead>
      <tbody>
        <tr v-for="u in users" :key="u.id">
          <td>{{ u.id }}</td><td>{{ u.name }}</td><td>{{ u.email }}</td>
          <td><select v-model="u.role" :disabled="u.id === store.user.id" aria-label="Rol" @change="setRole(u)"><option>USER</option><option>ADMIN</option></select></td>
          <td>{{ (u.createdAt || '').slice(0, 10) }}</td>
          <td><button class="link" :disabled="u.id === store.user.id" @click="delUser(u)">Șterge</button></td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
