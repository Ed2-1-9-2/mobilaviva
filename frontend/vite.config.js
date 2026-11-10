import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// Build-ul merge direct în resursele statice ale Spring Boot.
// Fonturile (woff2) păstrează numele fără hash, în assets/fonts/, ca să poată fi preîncărcate din index.html.
export default defineConfig({
  plugins: [vue()],
  build: {
    // Pe Vercel (VERCEL=1 la build) ieșirea e frontend/dist; altfel direct în resursele Spring Boot
    outDir: process.env.VERCEL ? 'dist' : '../src/main/resources/static', emptyOutDir: true,
    target: 'es2020', cssCodeSplit: true, reportCompressedSize: false,
    modulePreload: { polyfill: false },
    rollupOptions: {
      output: {
        assetFileNames: info => {
          const n = info.name || (info.names && info.names[0]) || ''
          return /\.woff2$/.test(n) ? 'assets/fonts/[name]-v1[extname]' : 'assets/[name]-[hash][extname]'
        }
      }
    }
  },
  server: {
    port: 5173,
    proxy: { '/api': 'http://localhost:8000', '/img': 'http://localhost:8000' }
  }
})
