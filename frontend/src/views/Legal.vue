<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

// Date demo (aceleași ca pe factură). Pentru un magazin real, înlocuiește cu datele firmei.
const C = {
  name: 'MobilaViva SRL (date demo)',
  cui: 'RO12345678',
  regcom: 'J40/0000/2026',
  address: 'Str. Exemplu 1, București',
  email: 'contact@mobilaviva.com',
  phone: '0700 000 000'
}
const UPDATED = '1 octombrie 2026'

const seller = `Site-ul MobilaViva este operat de ${C.name}, CUI ${C.cui}, nr. înregistrare Registrul Comerțului ${C.regcom}, sediul social: ${C.address}. Contact: ${C.email}, ${C.phone}.`

const ANPC = { label: 'ANPC – Soluționarea alternativă a litigiilor (SAL)', href: 'https://reclamatiisal.anpc.ro/' }
// Platforma europeană SOL/ODR a fost închisă la 20 iulie 2025; lista organismelor de soluționare a litigiilor se găsește pe portalul Consumer Redress
const SOL = { label: 'Consumer Redress în UE – lista organismelor de soluționare a litigiilor', href: 'https://consumer-redress.ec.europa.eu/list-alternative-dispute-resolution-adr-bodies_ro' }
const DPA = { label: 'ANSPDCP – www.dataprotection.ro', href: 'https://www.dataprotection.ro' }

const PAGES = {
  termeni: {
    title: 'Termeni și condiții',
    sections: [
      { h: '1. Vânzătorul', p: [seller] },
      { h: '2. Acceptarea termenilor', p: [
        'Prin accesarea site-ului, crearea unui cont sau plasarea unei comenzi confirmi că ai citit și accepți acești Termeni și condiții, Politica de confidențialitate și Politica de livrare și retur.',
        'Pentru a plasa comenzi trebuie să ai cel puțin 18 ani și capacitate deplină de exercițiu.'
      ] },
      { h: '3. Contul de utilizator', p: [
        'Ești responsabil pentru corectitudinea datelor introduse și pentru păstrarea confidențialității parolei. Parola este stocată doar sub formă criptată (hash).',
        'Poți solicita oricând ștergerea contului la adresa de email de mai sus, cu respectarea obligațiilor legale de păstrare a documentelor contabile.'
      ] },
      { h: '4. Produse și prețuri', p: [
        'Toate prețurile sunt exprimate în RON și includ TVA (21%). Prețurile și stocurile pot fi modificate fără notificare prealabilă, însă comenzile deja confirmate se onorează la prețul din momentul plasării.',
        'Promoțiile sunt valabile până la epuizarea stocului sau până la data afișată. Imaginile au caracter ilustrativ; culoarea, textura și dimensiunile reale pot diferi ușor, iar caracteristicile tehnice din fișa produsului prevalează.'
      ] },
      { h: '5. Plasarea și confirmarea comenzii', p: [
        'Comanda se consideră plasată după finalizarea plății și afișarea paginii de confirmare. Primești factura în contul tău, la secțiunea Comenzile mele.',
        'Dacă un produs devine indisponibil după plasarea comenzii, te vom contacta și îți vom propune o alternativă sau rambursarea integrală a sumei.'
      ] },
      { h: '6. Plata', p: [
        'Plata se face online, cu cardul bancar. Datele cardului sunt procesate de procesatorul de plăți (Stripe) și nu ajung pe serverele noastre; păstrăm doar ultimele 4 cifre ale cardului pentru identificarea comenzii.',
        'Pe versiunea demonstrativă a site-ului plățile rulează în mod test: nu se debitează sume reale.'
      ] },
      { h: '7. Livrarea', p: [
        'Termenul estimat de livrare este afișat pe fiecare produs și în coș. Livrarea este gratuită pentru comenzile de peste 1500 RON; pentru restul, costul transportului este afișat în coș înainte de plată.',
        'Detalii suplimentare găsești în Politica de livrare și retur.'
      ] },
      { h: '8. Dreptul de retragere (14 zile)', p: [
        'Ca consumator, ai dreptul să te retragi din contractul la distanță în 14 zile calendaristice de la primirea produselor, fără a-ți justifica decizia și fără penalități, conform OUG nr. 34/2014.',
        'Excepție fac produsele confecționate după specificațiile tale sau personalizate în mod clar. Procedura este descrisă în Politica de livrare și retur.'
      ] },
      { h: '9. Garanția legală', p: [
        'Produsele beneficiază de garanția legală de conformitate de 2 ani, conform Legii nr. 449/2003 și Legii nr. 140/2022. În caz de neconformitate ai dreptul la reparare, înlocuire, reducerea prețului sau rezoluțiunea contractului, în condițiile legii.'
      ] },
      { h: '10. Proprietate intelectuală', p: [
        'Conținutul site-ului (texte, imagini, logo, cod) este proprietatea vânzătorului sau a partenerilor săi și nu poate fi copiat sau reutilizat fără acord scris.'
      ] },
      { h: '11. Limitarea răspunderii', p: [
        'Nu răspundem pentru întreruperi ale serviciului cauzate de forță majoră, de furnizori terți (curierat, procesator de plăți, hosting) sau de utilizarea necorespunzătoare a site-ului.'
      ] },
      { h: '12. Soluționarea litigiilor', p: [
        'Orice nemulțumire poți să ne-o adresezi la adresa de email de mai sus; încercăm să o rezolvăm amiabil. Ca consumator poți apela și la:',
        'Legea aplicabilă este legea română.'
      ], links: [ANPC, SOL] },
      { h: '13. Modificarea termenilor', p: [
        'Putem actualiza acești termeni; versiunea în vigoare este cea publicată pe această pagină la data plasării comenzii.'
      ] }
    ]
  },
  confidentialitate: {
    title: 'Politica de confidențialitate',
    sections: [
      { h: '1. Operatorul datelor', p: [seller, 'Prelucrăm datele în conformitate cu Regulamentul (UE) 2016/679 (GDPR).'] },
      { h: '2. Ce date colectăm', p: [
        'Date de cont: nume, adresă de email, parolă (stocată doar criptat), data creării contului și data acceptării termenilor.',
        'Date de comandă: nume, email, adresa de livrare, produsele comandate, valoarea, factura și ultimele 4 cifre ale cardului. Numărul complet al cardului nu este stocat de noi.'
      ] },
      { h: '3. Scopuri și temei legal', p: [
        'Crearea și administrarea contului, precum și livrarea comenzilor: executarea contractului (art. 6 alin. 1 lit. b GDPR).',
        'Emiterea facturilor și păstrarea documentelor contabile: obligație legală (art. 6 alin. 1 lit. c GDPR).',
        'Securitatea site-ului și prevenirea fraudelor: interes legitim (art. 6 alin. 1 lit. f GDPR).',
        'Nu folosim datele pentru marketing și nu trimitem newslettere fără consimțământul tău separat.'
      ] },
      { h: '4. Cui transmitem datele', p: [
        'Doar către furnizorii necesari: firma de curierat (nume, adresă, telefon/email pentru livrare), procesatorul de plăți Stripe și furnizorul de hosting. Nu vindem datele tale.'
      ] },
      { h: '5. Cât păstrăm datele', p: [
        'Datele contului, cât timp ai cont. Datele din comenzi și facturi, pe perioada impusă de legislația contabilă și fiscală. După expirarea termenelor, datele sunt șterse sau anonimizate.'
      ] },
      { h: '6. Drepturile tale', p: [
        'Ai dreptul de acces, rectificare, ștergere, restricționare, portabilitate și opoziție, precum și dreptul de a-ți retrage consimțământul acolo unde prelucrarea se bazează pe el. Scrie-ne la adresa de email de mai sus; răspundem în cel mult 30 de zile.',
        'Ai dreptul să depui o plângere la autoritatea de supraveghere:'
      ], links: [DPA] },
      { h: '7. Cookie-uri', p: [
        'Folosim un singur cookie, strict necesar funcționării (cookie de sesiune pentru autentificare și coș, HttpOnly). Nu folosim cookie-uri de analiză sau de marketing, deci nu este necesar un banner de consimțământ. Dacă vom adăuga astfel de cookie-uri, îți vom cere acordul în prealabil.'
      ] },
      { h: '8. Securitate', p: [
        'Parolele sunt stocate cu hash, sesiunea folosește cookie HttpOnly, iar plata cu cardul este procesată de Stripe. Recomandăm folosirea site-ului numai prin conexiune HTTPS.'
      ] }
    ]
  },
  retur: {
    title: 'Politica de livrare și retur',
    sections: [
      { h: '1. Livrare', p: [
        'Termenul de livrare estimat (în zile lucrătoare) este afișat pe fiecare produs și în coș. Mobilierul voluminos este livrat de firma de curierat sau de echipa proprie, în baza unei programări telefonice.',
        'Livrarea este gratuită pentru comenzile de peste 1500 RON; sub această valoare, costul transportului este afișat în coș înainte de plată.',
        'La primire, verifică ambalajul în prezența curierului. Dacă observi deteriorări, menționează-le pe documentul de livrare și anunță-ne în 48 de ore la adresa de email de mai sus.'
      ] },
      { h: '2. Dreptul de retragere în 14 zile', p: [
        'Ai dreptul să te retragi din comandă fără a-ți justifica decizia, în 14 zile calendaristice de la data primirii produselor (OUG nr. 34/2014).',
        'Pentru a-ți exercita dreptul, trimite-ne o declarație clară (de exemplu prin email) la adresa de contact, cu numărul comenzii și produsele returnate. Poți folosi formularul-tip de retragere, pe care ți-l trimitem la cerere.'
      ] },
      { h: '3. Cum returnezi produsul', p: [
        'Trimite sau predă produsul în cel mult 14 zile de la data la care ne-ai anunțat retragerea. Produsul trebuie să fie în starea în care l-ai primit, în ambalajul original, cu accesoriile și documentele aferente. Răspunzi doar pentru diminuarea valorii produsului rezultată din manipularea altfel decât este necesar pentru a-i stabili natura și funcționarea.',
        'Costul direct al returnării este suportat de client, cu excepția cazului în care produsul este neconform sau deteriorat la livrare.'
      ] },
      { h: '4. Rambursarea', p: [
        'Îți rambursăm contravaloarea produselor și a transportului standard în cel mult 14 zile de la data la care am fost informați despre retragere; putem amâna rambursarea până la primirea produselor sau a dovezii expedierii lor. Rambursarea se face prin aceeași metodă de plată folosită la comandă.'
      ] },
      { h: '5. Excepții', p: [
        'Dreptul de retragere nu se aplică produselor confecționate după specificațiile clientului sau personalizate în mod clar.'
      ] },
      { h: '6. Produse neconforme sau defecte', p: [
        'Dacă produsul este defect sau diferă de descriere, beneficiezi de garanția legală de conformitate de 2 ani. Scrie-ne cu numărul comenzii și fotografii; îți propunem reparare, înlocuire sau rambursare, conform legii.',
        'Pentru litigii nerezolvate amiabil:'
      ], links: [ANPC, SOL] }
    ]
  }
}

const route = useRoute()
const page = computed(() => PAGES[route.meta.page] || PAGES.termeni)
</script>

<template>
  <article class="panel legal">
    <h1>{{ page.title }}</h1>
    <p class="mut">Ultima actualizare: {{ UPDATED }}</p>
    <section v-for="s in page.sections" :key="s.h">
      <h2>{{ s.h }}</h2>
      <p v-for="t in s.p" :key="t">{{ t }}</p>
      <ul v-if="s.links">
        <li v-for="l in s.links" :key="l.href"><a :href="l.href" target="_blank" rel="noopener noreferrer">{{ l.label }}</a></li>
      </ul>
    </section>
    <p class="mut noprint">
      <RouterLink to="/termeni">Termeni și condiții</RouterLink> ·
      <RouterLink to="/confidentialitate">Confidențialitate</RouterLink> ·
      <RouterLink to="/retur">Livrare și retur</RouterLink>
    </p>
  </article>
</template>
