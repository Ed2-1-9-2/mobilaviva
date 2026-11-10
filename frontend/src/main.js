import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { store, refresh } from './store'
import './style.css'

// Montare imediată (FCP rapid); sesiunea vine din HTML, iar dacă lipsește se încarcă în fundal.
// După un redeploy, chunk-urile vechi (Register-xxx.js) nu mai există și Vercel întoarce HTML => reîncarc o singură dată
window.addEventListener('vite:preloadError', e => {
  e.preventDefault()
  if (!sessionStorage.getItem('chunk-reload')) { sessionStorage.setItem('chunk-reload', '1'); location.reload() }
})
setTimeout(() => sessionStorage.removeItem('chunk-reload'), 10000)
createApp(App).use(router).mount('#app')
if (!store.loaded) refresh().catch(() => {})

// Banner „serverul pornește”: backend-ul gratuit doarme după inactivitate; prima vizită îl trezește (~1 min), catalogul se vede între timp
const WAKE_MSG = /^ro/i.test(document.documentElement.lang || 'ro') ? 'Serverul pornește după o pauză (~1 minut). Poți răsfoi catalogul între timp.' : 'The server is starting after a pause (~1 minute). You can browse the catalog meanwhile.'
let wakeBar = null
window.addEventListener('server-waking', e => {
  if (e.detail) {
    if (wakeBar) return
    wakeBar = document.createElement('div')
    wakeBar.setAttribute('role', 'status')
    wakeBar.style.cssText = 'position:fixed;top:12px;left:50%;transform:translateX(-50%);z-index:9999;background:#1d5560;color:#fff;padding:10px 16px;border-radius:10px;font:600 14px/1.3 system-ui,sans-serif;box-shadow:0 4px 16px rgba(0,0,0,.25);max-width:90vw;text-align:center'
    wakeBar.textContent = WAKE_MSG
    document.body.appendChild(wakeBar)
  } else if (wakeBar) { wakeBar.remove(); wakeBar = null }
})
window.addEventListener('server-awake', () => refresh().catch(() => {}))
