// Cheia categoriei în URL: neutră și aceeași în toate limbile (/catalog?category=kids). În aplicație și în baza de date rămâne numele românesc (Copii).
const URL = { Canapele: 'sofas', Paturi: 'beds', Dulapuri: 'wardrobes', Comode: 'dressers', Mese: 'tables', Scaune: 'chairs', Birouri: 'desks', Iluminat: 'lighting', Hol: 'hallway', Copii: 'kids' }
const RO = {}
for (const [ro, k] of Object.entries(URL)) { RO[k] = ro; RO[ro.toLowerCase()] = ro }
export const catUrl = ro => URL[ro] || ro
// acceptă cheia neutră și numele vechi românesc (linkuri vechi: ?category=Copii)
export const catKey = v => { const x = String(v || ''); return RO[x.trim().toLowerCase()] || x }
