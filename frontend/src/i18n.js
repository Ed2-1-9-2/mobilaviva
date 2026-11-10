// Straturi de traducere minimale: română (implicit) + EN, DE, FR, IT, PT, ES, NL. Textele lipsă cad înapoi pe română.
import { reactive } from 'vue'
import EXTRA from './i18n-extra'

export const LANGS = [
  { code: 'ro', name: 'Română', locale: 'ro-RO' }, { code: 'en', name: 'English', locale: 'en-GB' },
  { code: 'de', name: 'Deutsch', locale: 'de-DE' }, { code: 'fr', name: 'Français', locale: 'fr-FR' },
  { code: 'it', name: 'Italiano', locale: 'it-IT' }, { code: 'pt', name: 'Português', locale: 'pt-PT' },
  { code: 'es', name: 'Español', locale: 'es-ES' }, { code: 'nl', name: 'Nederlands', locale: 'nl-NL' }
]

const D = {
  ro: {
    skip: 'Sari la conținut', ship: 'Livrare gratuită peste 1500 RON', ret: 'Retur în 14 zile', pay: 'Plată securizată cu cardul (mod test)',
    ph: 'Caută canapea, pat, masă, stejar...', sbtn: 'Caută', saria: 'Caută', login: 'Autentificare', reg: 'Cont nou', out: 'Ieșire',
    cmp: 'Comparator', cmpa: 'Comparator de produse', wsh: 'Dorințe', wsha: 'Lista de dorințe', crt: 'Coș', crta: 'Coș de cumpărături',
    all: 'Toate', off: 'Oferte', pln: 'Planificator cameră', home: 'MobilaViva, pagina de start',
    fdesc: 'Magazin online de mobilier: canapele, paturi, dulapuri, mese, scaune, birouri și iluminat.', shop: 'Magazin', info: 'Informații',
    terms: 'Termeni și condiții', priv: 'Politica de confidențialitate', rets: 'Livrare și retur', disp: 'Litigii',
    legal: 'MobilaViva SRL (date demo). Prețurile includ TVA. Plata cu cardul rulează în mod test: nu se debitează sume reale.',
    chat: 'Asistent MobilaViva: deschide conversația', tcart: 'Adăugat în coș', twon: 'Salvat în lista de dorințe', twoff: 'Eliminat din lista de dorințe', tcmp: 'Adăugat la comparator',
    instock: 'În stoc', last: 'Ultimele {n} bucăți', deliv: 'livrare ~{d} zile', oos: 'Stoc epuizat', wadd: 'Adaugă la lista de dorințe', wrem: 'Elimină din lista de dorințe',
    wdrop: 'Au scăzut prețurile la {n} produse salvate', lang: 'Limba',
    c_Canapele: 'Canapele', c_Paturi: 'Paturi', c_Dulapuri: 'Dulapuri', c_Comode: 'Comode', c_Mese: 'Mese', c_Scaune: 'Scaune', c_Birouri: 'Birouri', c_Iluminat: 'Iluminat', c_Hol: 'Hol', c_Copii: 'Copii'
  },
  en: {
    skip: 'Skip to content', ship: 'Free delivery over 1500 RON', ret: '14-day returns', pay: 'Secure card payment (test mode)',
    ph: 'Search sofa, bed, table, oak...', sbtn: 'Search', saria: 'Search', login: 'Sign in', reg: 'New account', out: 'Sign out',
    cmp: 'Compare', cmpa: 'Product comparison', wsh: 'Wishlist', wsha: 'Wishlist', crt: 'Cart', crta: 'Shopping cart',
    all: 'All', off: 'Deals', pln: 'Room planner', home: 'MobilaViva, home page',
    fdesc: 'Online furniture store: sofas, beds, wardrobes, tables, chairs, desks and lighting.', shop: 'Shop', info: 'Information',
    terms: 'Terms and conditions', priv: 'Privacy policy', rets: 'Delivery and returns', disp: 'Disputes',
    legal: 'MobilaViva SRL (demo data). Prices include VAT. Card payments run in test mode: no real money is charged.',
    chat: 'MobilaViva assistant: open chat', tcart: 'Added to cart', twon: 'Saved to wishlist', twoff: 'Removed from wishlist', tcmp: 'Added to comparison',
    instock: 'In stock', last: 'Last {n} items', deliv: 'delivery ~{d} days', oos: 'Out of stock', wadd: 'Add to wishlist', wrem: 'Remove from wishlist',
    wdrop: 'Prices dropped on {n} saved products', lang: 'Language',
    c_Canapele: 'Sofas', c_Paturi: 'Beds', c_Dulapuri: 'Wardrobes', c_Comode: 'Dressers', c_Mese: 'Tables', c_Scaune: 'Chairs', c_Birouri: 'Desks', c_Iluminat: 'Lighting', c_Hol: 'Hallway', c_Copii: 'Kids'
  },
  de: {
    skip: 'Zum Inhalt springen', ship: 'Kostenlose Lieferung ab 1500 RON', ret: '14 Tage Rückgabe', pay: 'Sichere Kartenzahlung (Testmodus)',
    ph: 'Sofa, Bett, Tisch, Eiche suchen...', sbtn: 'Suchen', saria: 'Suchen', login: 'Anmelden', reg: 'Neues Konto', out: 'Abmelden',
    cmp: 'Vergleich', cmpa: 'Produktvergleich', wsh: 'Wunschliste', wsha: 'Wunschliste', crt: 'Warenkorb', crta: 'Warenkorb',
    all: 'Alle', off: 'Angebote', pln: 'Raumplaner', home: 'MobilaViva, Startseite',
    fdesc: 'Online-Möbelgeschäft: Sofas, Betten, Schränke, Tische, Stühle, Schreibtische und Beleuchtung.', shop: 'Shop', info: 'Informationen',
    terms: 'Allgemeine Geschäftsbedingungen', priv: 'Datenschutzerklärung', rets: 'Lieferung und Rückgabe', disp: 'Streitfälle',
    legal: 'MobilaViva SRL (Demodaten). Preise inkl. MwSt. Kartenzahlungen laufen im Testmodus: Es wird kein echtes Geld abgebucht.',
    chat: 'MobilaViva-Assistent: Chat öffnen', tcart: 'In den Warenkorb gelegt', twon: 'In der Wunschliste gespeichert', twoff: 'Von der Wunschliste entfernt', tcmp: 'Zum Vergleich hinzugefügt',
    instock: 'Auf Lager', last: 'Nur noch {n} Stück', deliv: 'Lieferung in ~{d} Tagen', oos: 'Ausverkauft', wadd: 'Zur Wunschliste hinzufügen', wrem: 'Von der Wunschliste entfernen',
    wdrop: 'Preissenkung bei {n} gespeicherten Produkten', lang: 'Sprache',
    c_Canapele: 'Sofas', c_Paturi: 'Betten', c_Dulapuri: 'Schränke', c_Comode: 'Kommoden', c_Mese: 'Tische', c_Scaune: 'Stühle', c_Birouri: 'Schreibtische', c_Iluminat: 'Beleuchtung', c_Hol: 'Flur', c_Copii: 'Kinder'
  },
  fr: {
    skip: 'Aller au contenu', ship: 'Livraison gratuite dès 1500 RON', ret: 'Retour sous 14 jours', pay: 'Paiement sécurisé par carte (mode test)',
    ph: 'Rechercher canapé, lit, table, chêne...', sbtn: 'Rechercher', saria: 'Rechercher', login: 'Connexion', reg: 'Nouveau compte', out: 'Déconnexion',
    cmp: 'Comparateur', cmpa: 'Comparateur de produits', wsh: 'Favoris', wsha: 'Liste de souhaits', crt: 'Panier', crta: 'Panier',
    all: 'Tout', off: 'Offres', pln: 'Planificateur de pièce', home: 'MobilaViva, page d’accueil',
    fdesc: 'Boutique de meubles en ligne : canapés, lits, armoires, tables, chaises, bureaux et luminaires.', shop: 'Boutique', info: 'Informations',
    terms: 'Conditions générales', priv: 'Politique de confidentialité', rets: 'Livraison et retours', disp: 'Litiges',
    legal: 'MobilaViva SRL (données de démonstration). Prix TTC. Le paiement par carte fonctionne en mode test : aucun montant réel n’est débité.',
    chat: 'Assistant MobilaViva : ouvrir la conversation', tcart: 'Ajouté au panier', twon: 'Enregistré dans la liste de souhaits', twoff: 'Retiré de la liste de souhaits', tcmp: 'Ajouté au comparateur',
    instock: 'En stock', last: 'Plus que {n} pièces', deliv: 'livraison ~{d} jours', oos: 'Rupture de stock', wadd: 'Ajouter à la liste de souhaits', wrem: 'Retirer de la liste de souhaits',
    wdrop: 'Baisse de prix sur {n} produits enregistrés', lang: 'Langue',
    c_Canapele: 'Canapés', c_Paturi: 'Lits', c_Dulapuri: 'Armoires', c_Comode: 'Commodes', c_Mese: 'Tables', c_Scaune: 'Chaises', c_Birouri: 'Bureaux', c_Iluminat: 'Luminaires', c_Hol: 'Entrée', c_Copii: 'Enfants'
  },
  it: {
    skip: 'Vai al contenuto', ship: 'Consegna gratuita oltre 1500 RON', ret: 'Reso entro 14 giorni', pay: 'Pagamento sicuro con carta (modalità test)',
    ph: 'Cerca divano, letto, tavolo, rovere...', sbtn: 'Cerca', saria: 'Cerca', login: 'Accedi', reg: 'Nuovo account', out: 'Esci',
    cmp: 'Confronto', cmpa: 'Confronto prodotti', wsh: 'Desideri', wsha: 'Lista dei desideri', crt: 'Carrello', crta: 'Carrello',
    all: 'Tutti', off: 'Offerte', pln: 'Pianificatore stanza', home: 'MobilaViva, pagina iniziale',
    fdesc: 'Negozio online di mobili: divani, letti, armadi, tavoli, sedie, scrivanie e illuminazione.', shop: 'Negozio', info: 'Informazioni',
    terms: 'Termini e condizioni', priv: 'Informativa sulla privacy', rets: 'Consegna e resi', disp: 'Controversie',
    legal: 'MobilaViva SRL (dati demo). Prezzi IVA inclusa. I pagamenti con carta funzionano in modalità test: nessun importo reale viene addebitato.',
    chat: 'Assistente MobilaViva: apri la chat', tcart: 'Aggiunto al carrello', twon: 'Salvato nella lista dei desideri', twoff: 'Rimosso dalla lista dei desideri', tcmp: 'Aggiunto al confronto',
    instock: 'Disponibile', last: 'Ultimi {n} pezzi', deliv: 'consegna ~{d} giorni', oos: 'Esaurito', wadd: 'Aggiungi alla lista dei desideri', wrem: 'Rimuovi dalla lista dei desideri',
    wdrop: 'Prezzi in calo su {n} prodotti salvati', lang: 'Lingua',
    c_Canapele: 'Divani', c_Paturi: 'Letti', c_Dulapuri: 'Armadi', c_Comode: 'Cassettiere', c_Mese: 'Tavoli', c_Scaune: 'Sedie', c_Birouri: 'Scrivanie', c_Iluminat: 'Illuminazione', c_Hol: 'Ingresso', c_Copii: 'Bambini'
  },
  pt: {
    skip: 'Saltar para o conteúdo', ship: 'Entrega grátis acima de 1500 RON', ret: 'Devolução em 14 dias', pay: 'Pagamento seguro com cartão (modo de teste)',
    ph: 'Pesquisar sofá, cama, mesa, carvalho...', sbtn: 'Pesquisar', saria: 'Pesquisar', login: 'Entrar', reg: 'Nova conta', out: 'Sair',
    cmp: 'Comparador', cmpa: 'Comparador de produtos', wsh: 'Desejos', wsha: 'Lista de desejos', crt: 'Carrinho', crta: 'Carrinho de compras',
    all: 'Todos', off: 'Ofertas', pln: 'Planeador de divisão', home: 'MobilaViva, página inicial',
    fdesc: 'Loja online de mobiliário: sofás, camas, roupeiros, mesas, cadeiras, secretárias e iluminação.', shop: 'Loja', info: 'Informações',
    terms: 'Termos e condições', priv: 'Política de privacidade', rets: 'Entrega e devoluções', disp: 'Litígios',
    legal: 'MobilaViva SRL (dados de demonstração). Preços com IVA incluído. O pagamento com cartão funciona em modo de teste: não é cobrado nenhum valor real.',
    chat: 'Assistente MobilaViva: abrir conversa', tcart: 'Adicionado ao carrinho', twon: 'Guardado na lista de desejos', twoff: 'Removido da lista de desejos', tcmp: 'Adicionado ao comparador',
    instock: 'Em stock', last: 'Últimas {n} unidades', deliv: 'entrega ~{d} dias', oos: 'Esgotado', wadd: 'Adicionar à lista de desejos', wrem: 'Remover da lista de desejos',
    wdrop: 'Preços reduzidos em {n} produtos guardados', lang: 'Idioma',
    c_Canapele: 'Sofás', c_Paturi: 'Camas', c_Dulapuri: 'Roupeiros', c_Comode: 'Cómodas', c_Mese: 'Mesas', c_Scaune: 'Cadeiras', c_Birouri: 'Secretárias', c_Iluminat: 'Iluminação', c_Hol: 'Hall', c_Copii: 'Crianças'
  },
  es: {
    skip: 'Saltar al contenido', ship: 'Envío gratis a partir de 1500 RON', ret: 'Devolución en 14 días', pay: 'Pago seguro con tarjeta (modo de prueba)',
    ph: 'Buscar sofá, cama, mesa, roble...', sbtn: 'Buscar', saria: 'Buscar', login: 'Iniciar sesión', reg: 'Nueva cuenta', out: 'Salir',
    cmp: 'Comparador', cmpa: 'Comparador de productos', wsh: 'Deseos', wsha: 'Lista de deseos', crt: 'Cesta', crta: 'Cesta de la compra',
    all: 'Todos', off: 'Ofertas', pln: 'Planificador de habitación', home: 'MobilaViva, página de inicio',
    fdesc: 'Tienda de muebles online: sofás, camas, armarios, mesas, sillas, escritorios e iluminación.', shop: 'Tienda', info: 'Información',
    terms: 'Términos y condiciones', priv: 'Política de privacidad', rets: 'Envío y devoluciones', disp: 'Litigios',
    legal: 'MobilaViva SRL (datos de demostración). Precios con IVA incluido. El pago con tarjeta funciona en modo de prueba: no se cobra ningún importe real.',
    chat: 'Asistente MobilaViva: abrir conversación', tcart: 'Añadido a la cesta', twon: 'Guardado en la lista de deseos', twoff: 'Eliminado de la lista de deseos', tcmp: 'Añadido al comparador',
    instock: 'En stock', last: 'Últimas {n} unidades', deliv: 'entrega ~{d} días', oos: 'Agotado', wadd: 'Añadir a la lista de deseos', wrem: 'Quitar de la lista de deseos',
    wdrop: 'Han bajado los precios de {n} productos guardados', lang: 'Idioma',
    c_Canapele: 'Sofás', c_Paturi: 'Camas', c_Dulapuri: 'Armarios', c_Comode: 'Cómodas', c_Mese: 'Mesas', c_Scaune: 'Sillas', c_Birouri: 'Escritorios', c_Iluminat: 'Iluminación', c_Hol: 'Recibidor', c_Copii: 'Infantil'
  },
  nl: {
    skip: 'Naar de inhoud', ship: 'Gratis levering vanaf 1500 RON', ret: 'Retour binnen 14 dagen', pay: 'Veilig betalen met kaart (testmodus)',
    ph: 'Zoek bank, bed, tafel, eiken...', sbtn: 'Zoeken', saria: 'Zoeken', login: 'Inloggen', reg: 'Nieuw account', out: 'Uitloggen',
    cmp: 'Vergelijken', cmpa: 'Productvergelijking', wsh: 'Verlanglijst', wsha: 'Verlanglijst', crt: 'Winkelmand', crta: 'Winkelmand',
    all: 'Alles', off: 'Aanbiedingen', pln: 'Kamerplanner', home: 'MobilaViva, startpagina',
    fdesc: 'Online meubelwinkel: banken, bedden, kasten, tafels, stoelen, bureaus en verlichting.', shop: 'Winkel', info: 'Informatie',
    terms: 'Algemene voorwaarden', priv: 'Privacybeleid', rets: 'Levering en retour', disp: 'Geschillen',
    legal: 'MobilaViva SRL (demogegevens). Prijzen incl. btw. Kaartbetalingen draaien in testmodus: er wordt geen echt geld afgeschreven.',
    chat: 'MobilaViva-assistent: chat openen', tcart: 'Toegevoegd aan winkelmand', twon: 'Opgeslagen in verlanglijst', twoff: 'Verwijderd uit verlanglijst', tcmp: 'Toegevoegd aan vergelijking',
    instock: 'Op voorraad', last: 'Nog {n} stuks', deliv: 'levering ~{d} dagen', oos: 'Uitverkocht', wadd: 'Toevoegen aan verlanglijst', wrem: 'Verwijderen uit verlanglijst',
    wdrop: 'Prijsdaling bij {n} opgeslagen producten', lang: 'Taal',
    c_Canapele: 'Banken', c_Paturi: 'Bedden', c_Dulapuri: 'Kasten', c_Comode: 'Ladekasten', c_Mese: 'Tafels', c_Scaune: 'Stoelen', c_Birouri: 'Bureaus', c_Iluminat: 'Verlichting', c_Hol: 'Hal', c_Copii: 'Kinderen'
  }
}

for (const l in EXTRA) Object.assign(D[l] || (D[l] = {}), EXTRA[l])

function initial() {
  try { const l = localStorage.getItem('lang'); if (l && D[l]) return l } catch { /* storage blocat */ }
  return 'ro'   // implicit română (nu se ghicește din browser)
}
export const i18n = reactive({ lang: initial() })
if (typeof document !== 'undefined') document.documentElement.lang = i18n.lang

export function setLang(l) {
  if (!D[l]) return
  i18n.lang = l
  try { localStorage.setItem('lang', l) } catch { /* ignorat */ }
  document.documentElement.lang = l
}
export const locale = () => (LANGS.find(x => x.code === i18n.lang) || LANGS[0]).locale
export function t(k, v) {
  let s = (D[i18n.lang] && D[i18n.lang][k]) ?? D.ro[k] ?? k
  if (v) for (const x in v) s = s.replace('{' + x + '}', v[x])
  return s
}
export const catName = c => t('c_' + c) === 'c_' + c ? c : t('c_' + c)
