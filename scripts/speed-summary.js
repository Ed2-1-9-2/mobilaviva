// Afișează scorurile și metricile principale dintr-un raport Lighthouse JSON.
const fs = require('fs')
const mode = process.argv[2] || 'mobile'
const r = JSON.parse(fs.readFileSync(`docs/speed-reports/${mode}.report.json`, 'utf8'))
const score = k => Math.round(r.categories[k].score * 100)
const a = r.audits
console.log(`[${mode}] Performance ${score('performance')} | Accessibility ${score('accessibility')} | Best Practices ${score('best-practices')} | SEO ${score('seo')}`)
console.log(`        FCP ${a['first-contentful-paint'].displayValue} | LCP ${a['largest-contentful-paint'].displayValue} | TBT ${a['total-blocking-time'].displayValue} | CLS ${a['cumulative-layout-shift'].displayValue} | SI ${a['speed-index'].displayValue}`)
