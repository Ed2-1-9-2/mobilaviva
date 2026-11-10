#!/usr/bin/env node
// Descarca automat poze realiste pentru produse (licente libere, uz comercial).
// Folosire:  PEXELS_API_KEY=xxxx node scripts/auto_photos.mjs     (recomandat, cheie gratuita: pexels.com/api)
//            node scripts/auto_photos.mjs                          (fara cheie: foloseste Openverse)
// Rezultat:  frontend/public/photos/<slug>.jpg  +  docs/photos-credits.txt
import fs from 'node:fs'
import path from 'node:path'

const OUT = 'frontend/public/photos'
const CREDITS = 'docs/photos-credits.txt'
const KEY = process.env.PEXELS_API_KEY || ''
fs.mkdirSync(OUT, { recursive: true })
fs.mkdirSync(path.dirname(CREDITS), { recursive: true })

const Q = {
  'canapea-extensibila-3-locuri-milano': 'gray fabric three seater sofa wooden legs',
  'coltar-living-oslo-stanga': 'teal blue velvet corner sectional sofa',
  'canapea-2-locuri-verona': 'brown leather two seater sofa',
  'fotoliu-relax-bergen': 'cream boucle armchair',
  'pat-dublu-tapitat-luna-160x200': 'beige upholstered double bed tall headboard',
  'pat-lemn-masiv-rustic-140x200': 'solid oak wooden bed frame rustic bedroom',
  'saltea-ortopedica-confort-160x200': 'white mattress on bed frame',
  'pat-metalic-loft-90x200': 'black metal single bed frame',
  'dulap-4-usi-glisante-modena': 'white oak sliding door wardrobe',
  'dulap-haine-2-usi-nordic': 'white two door wardrobe',
  'biblioteca-cube-5-rafturi': 'walnut cube bookcase shelving unit',
  'comoda-tv-bergen-180-cm': 'walnut tv stand media console',
  'comoda-6-sertare-lyon': 'white pine chest of drawers six drawers',
  'noptiera-luna': 'beige upholstered bedside table',
  'masa-extensibila-oslo-140-180': 'white extendable dining table black metal legs',
  'masuta-living-rotunda-marmo': 'round glass coffee table gold metal frame',
  'masa-lemn-masiv-rustic-200': 'rustic solid oak dining table large',
  'set-4-scaune-tapitate-milano': 'gray upholstered dining chairs set',
  'scaune-bar-loft-set-2': 'black faux leather bar stools metal',
  'scaun-birou-ergonomic-pro': 'black mesh ergonomic office chair',
  'birou-calculator-simple-140': 'white simple computer desk',
  'birou-reglabil-electric-160': 'walnut standing desk black frame',
  'lustra-moderna-halo': 'gold ring halo chandelier modern',
  'lampadar-arcuit-nordic': 'black arc floor lamp fabric shade',
  'canapea-3-locuri-chester': 'brown chesterfield tufted sofa',
  'pat-boxspring-royal-160x200': 'gray boxspring bed upholstered headboard',
  'vitrina-sufragerie-venezia': 'walnut glass display cabinet living room',
  'comoda-3-sertare-aria': 'white chest of drawers three drawers',
  'masa-bucatarie-rotunda-terra': 'round wooden kitchen dining table',
  'set-2-scaune-dining-scandi': 'beige scandinavian dining chair wooden legs',
  'fotoliu-gaming-racer': 'black gaming chair',
  'birou-colt-l-studio': 'oak l shaped corner desk home office',
  'veioza-ceramica-luna': 'cream ceramic table lamp fabric shade',
  'aplica-perete-arc': 'black wall sconce arm lamp',
  'pantofar-slim-3-clapete': 'white slim shoe cabinet hallway',
  'banca-hol-cu-depozitare-oslo': 'gray upholstered storage bench entryway',
  'cuier-perete-wood': 'wooden wall mounted coat rack',
  'oglinda-hol-halo': 'black frame round mirror hallway',
  'pat-copii-casuta-90x200': 'white house shaped kids bed',
  'birou-copii-mic-artist': 'white kids desk children room',
  'canapea-2-locuri-scandi-lund': 'gray woven fabric two seater sofa scandinavian',
  'canapea-3-locuri-velvet-aurora': 'green velvet three seater sofa',
  'coltar-u-panorama': 'beige u shaped sectional sofa',
  'canapea-extensibila-click-clack-tokyo': 'dark gray sofa bed click clack',
  'canapea-3-locuri-cognac-premium': 'cognac brown leather three seater sofa',
  'fotoliu-wing-clasic-bergere': 'blue wingback armchair',
  'fotoliu-balansoar-nordic-rocky': 'wooden rocking chair cream cushion',
  'pat-dublu-hotel-grand-180x200': 'gray upholstered king bed hotel style',
  'pat-lemn-nordic-fjord-160x200': 'light wood scandinavian bed frame',
  'pat-cu-lada-storage-140x200': 'anthracite bed with storage drawers',
  'pat-simplu-studio-90x200': 'white single bed frame',
  'pat-rustic-cabana-180x200': 'dark walnut rustic pine bed frame',
  'saltea-memory-cloud-140x200': 'memory foam mattress white',
  'dulap-3-usi-torino': 'white and oak three door wardrobe',
  'dulap-5-usi-glisante-madrid': 'gray mirrored sliding wardrobe',
  'dulap-tineret-studio-2-usi': 'white teen bedroom wardrobe',
  'dulap-rustic-brasov-2-usi': 'rustic pine wardrobe walnut',
  'biblioteca-scara-ladder-5-trepte': 'black metal ladder bookshelf',
  'vitrina-modern-cristal-2-usi': 'white display cabinet glass doors',
  'comoda-tv-loft-160': 'black metal industrial tv stand',
  'comoda-tv-suspendata-float-200': 'white floating wall mounted tv unit',
  'comoda-tv-rustic-150': 'oak rustic tv stand',
  'noptiera-suspendata-float': 'white floating bedside table',
  'noptiera-rustic-cu-sertar': 'rustic wooden nightstand drawer',
  'comoda-5-sertare-scandi': 'scandinavian oak chest of drawers',
  'masa-extensibila-berlin-160-200': 'dark gray extendable dining table',
  'masa-patrata-cube-90': 'white square dining table',
  'masuta-cafea-pallet': 'pallet wood coffee table',
  'masuta-laterala-rotunda-duo': 'gold round glass side table',
  'masa-bar-inalta-pub-120': 'high bar table black metal wood',
  'masa-rotunda-marmo-120': 'round white marble dining table',
  'set-4-scaune-velvet-aurora': 'green velvet dining chairs gold legs',
  'set-6-scaune-loft-metal': 'black metal industrial dining chairs',
  'scaune-bar-wood-set-2': 'walnut wooden bar stools leather seat',
  'scaun-birou-ergonomic-mesh-basic': 'gray mesh office chair',
  'set-4-scaune-windsor-lemn': 'oak windsor dining chairs',
  'scaun-conferinta-executive-piele': 'brown leather executive office chair',
  'taburet-rotund-cub': 'cream boucle round pouf stool',
  'birou-compact-mini-100': 'small white compact desk',
  'birou-rustic-lemn-masiv-atelier-150': 'solid oak rustic writing desk',
  'birou-gaming-dragon-140': 'black gaming desk',
  'birou-cu-rafturi-studio-120': 'walnut desk with shelves',
  'birou-secretar-wall-80': 'white wall mounted secretary desk',
  'lampadar-trepied-tripod': 'wooden tripod floor lamp',
  'lampadar-led-torch-slim': 'slim black led floor lamp',
  'veioza-birou-led-flex': 'white led desk lamp flexible arm',
  'veioza-sticla-opal': 'opal glass table lamp',
  'aplica-dubla-twin': 'gold double wall sconce',
  'lustra-cubica-cube': 'black cube glass ceiling light',
  'cuier-stativ-tree': 'wooden coat tree stand',
  'consola-hol-slim-100': 'slim oak console table hallway',
  'dulap-pantofi-step-2-usi': 'white shoe storage cabinet two doors',
  'banca-pantofi-nordic': 'oak shoe bench metal legs',
  'cuier-cu-oglinda-entry': 'white coat rack with mirror entryway',
  'pat-etajat-bunk-90x200': 'white wooden bunk bed',
  'pat-copii-montessori-90x190': 'montessori floor bed wood',
  'birou-copii-rocket': 'blue kids desk',
  'dulap-copii-zoo': 'green kids wardrobe',
  'comoda-copii-colors': 'yellow kids chest of drawers',
  'biblioteca-copii-mini': 'white kids bookshelf low',
  'scaun-dining-windsor-lemn': 'oak windsor dining chair',
  'scaun-dining-velvet-aurora': 'green velvet dining chair gold legs',
  'scaun-dining-fjord-curbat': 'bentwood beech dining chair',
  'scaun-dining-boucle-cloud': 'cream boucle dining chair',
  'scaun-dining-piele-cognac': 'brown leather dining chair metal legs',
  'scaun-rattan-bali': 'rattan dining chair',
  'scaun-shell-nordic': 'white shell chair wooden legs',
  'scaun-metalic-loft-factory': 'black metal industrial stackable chair',
  'scaun-pliabil-compact': 'gray folding chair',
  'scaun-bar-wood': 'walnut bar stool backrest',
  'scaun-bar-inalt-industrial': 'black metal high bar stool',
  'scaun-gaming-racer-pro': 'red black racing gaming chair',
  'scaun-ergonomic-posture-cu-tetiera': 'ergonomic mesh chair with headrest',
  'scaun-birou-basic-studio': 'gray mesh task chair',
}

const only = process.argv.slice(2)
const credits = []
const sleep = ms => new Promise(r => setTimeout(r, ms))

async function pexels(q) {
  const r = await fetch(`https://api.pexels.com/v1/search?query=${encodeURIComponent(q)}&per_page=8&orientation=landscape`, { headers: { Authorization: KEY } })
  if (!r.ok) throw new Error('pexels ' + r.status)
  const j = await r.json()
  return (j.photos || []).map(p => ({ url: p.src.large, credit: `${p.photographer} / Pexels ${p.url}` }))
}
async function openverse(q) {
  const u = `https://api.openverse.org/v1/images/?q=${encodeURIComponent(q)}&license_type=commercial&category=photograph&extension=jpg&page_size=8`
  const r = await fetch(u, { headers: { 'User-Agent': 'mobilaviva-school-project' } })
  if (!r.ok) throw new Error('openverse ' + r.status)
  const j = await r.json()
  return (j.results || []).filter(x => (x.width || 0) >= 800).map(x => ({ url: x.url, credit: `${x.creator || '?'} / ${x.license} ${x.foreign_landing_url}` }))
}

let ok = 0, fail = 0
for (const [slug, q] of Object.entries(Q)) {
  if (only.length && !only.includes(slug)) continue
  const dest = path.join(OUT, slug + '.jpg')
  if (fs.existsSync(dest) && fs.statSync(dest).size > 5000) { ok++; continue }
  let done = false
  try {
    const cands = KEY ? await pexels(q) : await openverse(q)
    for (const c of cands) {
      try {
        const r = await fetch(c.url, { redirect: 'follow' })
        const ct = r.headers.get('content-type') || ''
        if (!r.ok || !ct.startsWith('image/')) continue
        const buf = Buffer.from(await r.arrayBuffer())
        if (buf.length < 20000) continue
        fs.writeFileSync(dest, buf)
        credits.push(`${slug}.jpg  <-  ${c.credit}`)
        done = true
        break
      } catch {}
    }
  } catch (e) { console.log('! API', slug, e.message) }
  console.log(done ? 'OK  ' : 'FAIL', slug)
  done ? ok++ : fail++
  await sleep(KEY ? 250 : 1200)
}
if (credits.length) fs.appendFileSync(CREDITS, credits.join('\n') + '\n')
console.log(`\nGata: ${ok} ok, ${fail} esuate. Credite: ${CREDITS}`)
console.log('Urmeaza: ./scripts/push.sh  (poze verificate vizual inainte, daca vrei)')
