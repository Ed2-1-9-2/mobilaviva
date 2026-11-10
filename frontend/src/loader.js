// Încărcător de componente .vue în browser (Vue 3 + vue3-sfc-loader), fără Vite/Webpack și fără pas de build.
// Modulele .js (store, api, router...) se încarcă NATIV (import), deci există o singură instanță a lor în toată aplicația.
import * as Vue from 'vue'
import * as VueRouter from 'vue-router'
import { loadModule } from 'vue3-sfc-loader'

const options = {
  moduleCache: { vue: Vue, 'vue-router': VueRouter },
  async getFile(url) {
    const res = await fetch(url)
    if (!res.ok) throw Object.assign(new Error(url + ' ' + res.status), { res })
    return await res.text()
  },
  addStyle(css) {
    const el = document.createElement('style')
    el.textContent = css
    document.head.appendChild(el)
  },
  async loadModule(path) {
    if (/\.js$/.test(path)) return await import(path)
  }
}

export const loadVue = path => loadModule(path, options)
