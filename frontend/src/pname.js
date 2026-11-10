// Traduce produsele (nume, descriere, material, culoare; stocate în română în baza de date) în limba activă.
// 1) traduceri generate automat (scripts/translate_products.mjs) din src/product-i18n/<limba>.json, încărcate LA CERERE (doar limba aleasă, un chunk mic);
// 2) traduceri scrise de mână mai jos (rezervă); 3) textul original în română.
import { reactive, watch } from 'vue'
import { i18n } from './i18n'
const loaders = import.meta.glob('./product-i18n/*.json')
const GEN = reactive({})
watch(() => i18n.lang, l => {
  if (l === 'ro' || GEN[l] !== undefined) return
  GEN[l] = {}
  const f = loaders['./product-i18n/' + l + '.json']
  if (f) f().then(m => { GEN[l] = m.default || {} }).catch(() => {})
}, { immediate: true })
const N = {
 "Canapea extensibilă 3 locuri Milano": {
  "en": "Milano 3-seater sofa bed",
  "de": "Milano 3-Sitzer Schlafsofa",
  "fr": "Canapé-lit 3 places Milano",
  "it": "Divano letto 3 posti Milano",
  "pt": "Sofá-cama 3 lugares Milano",
  "es": "Sofá cama 3 plazas Milano",
  "nl": "Milano 3-zitsslaapbank"
 },
 "Colțar living Oslo, stânga": {
  "en": "Oslo living room corner sofa, left",
  "de": "Oslo Wohnzimmer-Ecksofa, links",
  "fr": "Canapé d'angle Oslo, gauche",
  "it": "Divano angolare Oslo, sinistra",
  "pt": "Sofá de canto Oslo, esquerdo",
  "es": "Sofá rinconera Oslo, izquierda",
  "nl": "Oslo hoekbank, links"
 },
 "Canapea 2 locuri Verona": {
  "en": "Verona 2-seater sofa",
  "de": "Verona 2-Sitzer-Sofa",
  "fr": "Canapé 2 places Verona",
  "it": "Divano 2 posti Verona",
  "pt": "Sofá 2 lugares Verona",
  "es": "Sofá 2 plazas Verona",
  "nl": "Verona 2-zitsbank"
 },
 "Fotoliu Relax Bergen": {
  "en": "Bergen Relax armchair",
  "de": "Bergen Relax-Sessel",
  "fr": "Fauteuil Relax Bergen",
  "it": "Poltrona Relax Bergen",
  "pt": "Poltrona Relax Bergen",
  "es": "Sillón Relax Bergen",
  "nl": "Bergen Relax fauteuil"
 },
 "Pat dublu tapițat Luna 160x200": {
  "en": "Luna upholstered double bed 160x200",
  "de": "Luna Polsterbett 160x200",
  "fr": "Lit double rembourré Luna 160x200",
  "it": "Letto matrimoniale imbottito Luna 160x200",
  "pt": "Cama de casal estofada Luna 160x200",
  "es": "Cama doble tapizada Luna 160x200",
  "nl": "Luna gestoffeerd tweepersoonsbed 160x200"
 },
 "Pat lemn masiv Rustic 140x200": {
  "en": "Rustic solid wood bed 140x200",
  "de": "Rustic Massivholzbett 140x200",
  "fr": "Lit en bois massif Rustic 140x200",
  "it": "Letto in legno massello Rustic 140x200",
  "pt": "Cama em madeira maciça Rustic 140x200",
  "es": "Cama de madera maciza Rustic 140x200",
  "nl": "Rustic massief houten bed 140x200"
 },
 "Saltea ortopedică Confort 160x200": {
  "en": "Confort orthopedic mattress 160x200",
  "de": "Confort Orthopädische Matratze 160x200",
  "fr": "Matelas orthopédique Confort 160x200",
  "it": "Materasso ortopedico Confort 160x200",
  "pt": "Colchão ortopédico Confort 160x200",
  "es": "Colchón ortopédico Confort 160x200",
  "nl": "Confort orthopedische matras 160x200"
 },
 "Pat metalic Loft 90x200": {
  "en": "Loft metal bed 90x200",
  "de": "Loft Metallbett 90x200",
  "fr": "Lit métallique Loft 90x200",
  "it": "Letto in metallo Loft 90x200",
  "pt": "Cama metálica Loft 90x200",
  "es": "Cama metálica Loft 90x200",
  "nl": "Loft metalen bed 90x200"
 },
 "Dulap 4 uși glisante Modena": {
  "en": "Modena 4-door sliding wardrobe",
  "de": "Modena Schiebetürenschrank 4-türig",
  "fr": "Armoire 4 portes coulissantes Modena",
  "it": "Armadio 4 ante scorrevoli Modena",
  "pt": "Roupeiro 4 portas de correr Modena",
  "es": "Armario 4 puertas correderas Modena",
  "nl": "Modena kledingkast 4 schuifdeuren"
 },
 "Dulap haine 2 uși Nordic": {
  "en": "Nordic 2-door wardrobe",
  "de": "Nordic Kleiderschrank 2-türig",
  "fr": "Armoire 2 portes Nordic",
  "it": "Armadio 2 ante Nordic",
  "pt": "Roupeiro 2 portas Nordic",
  "es": "Armario 2 puertas Nordic",
  "nl": "Nordic kledingkast 2 deuren"
 },
 "Bibliotecă Cube 5 rafturi": {
  "en": "Cube 5-shelf bookcase",
  "de": "Cube Bücherregal 5 Fächer",
  "fr": "Bibliothèque Cube 5 étagères",
  "it": "Libreria Cube 5 ripiani",
  "pt": "Estante Cube 5 prateleiras",
  "es": "Librería Cube 5 estantes",
  "nl": "Cube boekenkast 5 planken"
 },
 "Comodă TV Bergen 180 cm": {
  "en": "Bergen TV unit 180 cm",
  "de": "Bergen TV-Schrank 180 cm",
  "fr": "Meuble TV Bergen 180 cm",
  "it": "Mobile TV Bergen 180 cm",
  "pt": "Móvel de TV Bergen 180 cm",
  "es": "Mueble de TV Bergen 180 cm",
  "nl": "Bergen tv-meubel 180 cm"
 },
 "Comodă 6 sertare Lyon": {
  "en": "Lyon 6-drawer dresser",
  "de": "Lyon Kommode 6 Schubladen",
  "fr": "Commode 6 tiroirs Lyon",
  "it": "Cassettiera 6 cassetti Lyon",
  "pt": "Cómoda 6 gavetas Lyon",
  "es": "Cómoda 6 cajones Lyon",
  "nl": "Lyon commode 6 laden"
 },
 "Noptieră Luna": {
  "en": "Luna bedside table",
  "de": "Luna Nachttisch",
  "fr": "Table de chevet Luna",
  "it": "Comodino Luna",
  "pt": "Mesa de cabeceira Luna",
  "es": "Mesilla de noche Luna",
  "nl": "Luna nachtkastje"
 },
 "Masă extensibilă Oslo 140-180": {
  "en": "Oslo extendable table 140-180",
  "de": "Oslo Ausziehtisch 140-180",
  "fr": "Table extensible Oslo 140-180",
  "it": "Tavolo allungabile Oslo 140-180",
  "pt": "Mesa extensível Oslo 140-180",
  "es": "Mesa extensible Oslo 140-180",
  "nl": "Oslo uitschuifbare tafel 140-180"
 },
 "Măsuță living rotundă Marmo": {
  "en": "Marmo round coffee table",
  "de": "Marmo runder Couchtisch",
  "fr": "Table basse ronde Marmo",
  "it": "Tavolino rotondo Marmo",
  "pt": "Mesa de centro redonda Marmo",
  "es": "Mesa de centro redonda Marmo",
  "nl": "Marmo ronde salontafel"
 },
 "Masă lemn masiv Rustic 200": {
  "en": "Rustic solid wood table 200",
  "de": "Rustic Massivholztisch 200",
  "fr": "Table en bois massif Rustic 200",
  "it": "Tavolo in legno massello Rustic 200",
  "pt": "Mesa em madeira maciça Rustic 200",
  "es": "Mesa de madera maciza Rustic 200",
  "nl": "Rustic massief houten tafel 200"
 },
 "Set 4 scaune tapițate Milano": {
  "en": "Milano set of 4 upholstered chairs",
  "de": "Milano 4er-Set Polsterstühle",
  "fr": "Lot de 4 chaises rembourrées Milano",
  "it": "Set 4 sedie imbottite Milano",
  "pt": "Conjunto 4 cadeiras estofadas Milano",
  "es": "Set 4 sillas tapizadas Milano",
  "nl": "Milano set van 4 gestoffeerde stoelen"
 },
 "Scaune bar Loft (set 2)": {
  "en": "Loft bar stools (set of 2)",
  "de": "Loft Barhocker (2er-Set)",
  "fr": "Tabourets de bar Loft (lot de 2)",
  "it": "Sgabelli bar Loft (set da 2)",
  "pt": "Bancos de bar Loft (conjunto de 2)",
  "es": "Taburetes de bar Loft (set de 2)",
  "nl": "Loft barkrukken (set van 2)"
 },
 "Scaun birou ergonomic Pro": {
  "en": "Pro ergonomic office chair",
  "de": "Pro ergonomischer Bürostuhl",
  "fr": "Chaise de bureau ergonomique Pro",
  "it": "Sedia da ufficio ergonomica Pro",
  "pt": "Cadeira de escritório ergonómica Pro",
  "es": "Silla de oficina ergonómica Pro",
  "nl": "Pro ergonomische bureaustoel"
 },
 "Birou calculator Simple 140": {
  "en": "Simple 140 computer desk",
  "de": "Simple Computertisch 140",
  "fr": "Bureau informatique Simple 140",
  "it": "Scrivania per PC Simple 140",
  "pt": "Secretária de computador Simple 140",
  "es": "Escritorio para ordenador Simple 140",
  "nl": "Simple computerbureau 140"
 },
 "Birou reglabil electric 160": {
  "en": "Electric height-adjustable desk 160",
  "de": "Elektrisch höhenverstellbarer Schreibtisch 160",
  "fr": "Bureau réglable électrique 160",
  "it": "Scrivania elettrica regolabile 160",
  "pt": "Secretária elétrica regulável 160",
  "es": "Escritorio eléctrico regulable 160",
  "nl": "Elektrisch verstelbaar bureau 160"
 },
 "Lustră modernă Halo": {
  "en": "Halo modern chandelier",
  "de": "Halo moderner Kronleuchter",
  "fr": "Lustre moderne Halo",
  "it": "Lampadario moderno Halo",
  "pt": "Lustre moderno Halo",
  "es": "Lámpara de techo moderna Halo",
  "nl": "Halo moderne kroonluchter"
 },
 "Lampadar arcuit Nordic": {
  "en": "Nordic arc floor lamp",
  "de": "Nordic Bogenstehlampe",
  "fr": "Lampadaire arqué Nordic",
  "it": "Lampada da terra ad arco Nordic",
  "pt": "Candeeiro de pé arqueado Nordic",
  "es": "Lámpara de pie arqueada Nordic",
  "nl": "Nordic booglamp"
 },
 "Canapea 3 locuri Chester": {
  "en": "Chester 3-seater sofa",
  "de": "Chester 3-Sitzer-Sofa",
  "fr": "Canapé 3 places Chester",
  "it": "Divano 3 posti Chester",
  "pt": "Sofá 3 lugares Chester",
  "es": "Sofá 3 plazas Chester",
  "nl": "Chester 3-zitsbank"
 },
 "Pat boxspring Royal 160x200": {
  "en": "Royal boxspring bed 160x200",
  "de": "Royal Boxspringbett 160x200",
  "fr": "Lit boxspring Royal 160x200",
  "it": "Letto boxspring Royal 160x200",
  "pt": "Cama boxspring Royal 160x200",
  "es": "Cama boxspring Royal 160x200",
  "nl": "Royal boxspringbed 160x200"
 },
 "Vitrină sufragerie Venezia": {
  "en": "Venezia dining room display cabinet",
  "de": "Venezia Vitrine fürs Esszimmer",
  "fr": "Vitrine salle à manger Venezia",
  "it": "Vetrina soggiorno Venezia",
  "pt": "Vitrine de sala Venezia",
  "es": "Vitrina de salón Venezia",
  "nl": "Venezia vitrinekast"
 },
 "Comodă 3 sertare Aria": {
  "en": "Aria 3-drawer dresser",
  "de": "Aria Kommode 3 Schubladen",
  "fr": "Commode 3 tiroirs Aria",
  "it": "Cassettiera 3 cassetti Aria",
  "pt": "Cómoda 3 gavetas Aria",
  "es": "Cómoda 3 cajones Aria",
  "nl": "Aria commode 3 laden"
 },
 "Masă bucătărie rotundă Terra": {
  "en": "Terra round kitchen table",
  "de": "Terra runder Küchentisch",
  "fr": "Table de cuisine ronde Terra",
  "it": "Tavolo da cucina rotondo Terra",
  "pt": "Mesa de cozinha redonda Terra",
  "es": "Mesa de cocina redonda Terra",
  "nl": "Terra ronde keukentafel"
 },
 "Set 2 scaune dining Scandi": {
  "en": "Scandi set of 2 dining chairs",
  "de": "Scandi 2er-Set Esszimmerstühle",
  "fr": "Lot de 2 chaises de salle à manger Scandi",
  "it": "Set 2 sedie da pranzo Scandi",
  "pt": "Conjunto 2 cadeiras de jantar Scandi",
  "es": "Set 2 sillas de comedor Scandi",
  "nl": "Scandi set van 2 eetkamerstoelen"
 },
 "Fotoliu gaming Racer": {
  "en": "Racer gaming chair",
  "de": "Racer Gaming-Stuhl",
  "fr": "Fauteuil gaming Racer",
  "it": "Poltrona gaming Racer",
  "pt": "Cadeira gaming Racer",
  "es": "Silla gaming Racer",
  "nl": "Racer gamestoel"
 },
 "Birou colț L Studio": {
  "en": "Studio L-shaped corner desk",
  "de": "Studio Eckschreibtisch L-Form",
  "fr": "Bureau d'angle en L Studio",
  "it": "Scrivania angolare a L Studio",
  "pt": "Secretária de canto em L Studio",
  "es": "Escritorio esquinero en L Studio",
  "nl": "Studio hoekbureau L-vorm"
 },
 "Veioză ceramică Luna": {
  "en": "Luna ceramic table lamp",
  "de": "Luna Keramik-Tischlampe",
  "fr": "Lampe de chevet en céramique Luna",
  "it": "Lampada da comodino in ceramica Luna",
  "pt": "Candeeiro de mesa em cerâmica Luna",
  "es": "Lámpara de mesa de cerámica Luna",
  "nl": "Luna keramische tafellamp"
 },
 "Aplică perete Arc": {
  "en": "Arc wall sconce",
  "de": "Arc Wandleuchte",
  "fr": "Applique murale Arc",
  "it": "Applique da parete Arc",
  "pt": "Aplique de parede Arc",
  "es": "Aplique de pared Arc",
  "nl": "Arc wandlamp"
 },
 "Pantofar Slim 3 clapete": {
  "en": "Slim 3-flap shoe cabinet",
  "de": "Slim Schuhschrank 3 Klappen",
  "fr": "Meuble à chaussures Slim 3 abattants",
  "it": "Scarpiera Slim 3 ante ribalta",
  "pt": "Sapateira Slim 3 portas",
  "es": "Zapatero Slim 3 puertas abatibles",
  "nl": "Slim schoenenkast 3 kleppen"
 },
 "Bancă hol cu depozitare Oslo": {
  "en": "Oslo hallway bench with storage",
  "de": "Oslo Flurbank mit Stauraum",
  "fr": "Banc d'entrée avec rangement Oslo",
  "it": "Panca ingresso contenitore Oslo",
  "pt": "Banco de hall com arrumação Oslo",
  "es": "Banco de recibidor con almacenaje Oslo",
  "nl": "Oslo halbank met opberging"
 },
 "Cuier perete Wood": {
  "en": "Wood wall coat rack",
  "de": "Wood Wandgarderobe",
  "fr": "Porte-manteau mural Wood",
  "it": "Appendiabiti da parete Wood",
  "pt": "Cabide de parede Wood",
  "es": "Perchero de pared Wood",
  "nl": "Wood wandkapstok"
 },
 "Oglindă hol Halo": {
  "en": "Halo hallway mirror",
  "de": "Halo Flurspiegel",
  "fr": "Miroir d'entrée Halo",
  "it": "Specchio ingresso Halo",
  "pt": "Espelho de hall Halo",
  "es": "Espejo de recibidor Halo",
  "nl": "Halo halspiegel"
 },
 "Pat copii Casuță 90x200": {
  "en": "Kids' house bed 90x200",
  "de": "Kinder-Hausbett 90x200",
  "fr": "Lit enfant cabane 90x200",
  "it": "Letto bambini a casetta 90x200",
  "pt": "Cama infantil casinha 90x200",
  "es": "Cama infantil casita 90x200",
  "nl": "Kinderhuisbed 90x200"
 },
 "Birou copii Mic Artist": {
  "en": "Mic Artist kids' desk",
  "de": "Mic Artist Kinderschreibtisch",
  "fr": "Bureau enfant Mic Artist",
  "it": "Scrivania bambini Mic Artist",
  "pt": "Secretária infantil Mic Artist",
  "es": "Escritorio infantil Mic Artist",
  "nl": "Mic Artist kinderbureau"
 },
 "Canapea 2 locuri Scandi Lund": {
  "en": "Scandi Lund 2-seater sofa",
  "de": "Scandi Lund 2-Sitzer-Sofa",
  "fr": "Canapé 2 places Scandi Lund",
  "it": "Divano 2 posti Scandi Lund",
  "pt": "Sofá 2 lugares Scandi Lund",
  "es": "Sofá 2 plazas Scandi Lund",
  "nl": "Scandi Lund 2-zitsbank"
 },
 "Canapea 3 locuri Velvet Aurora": {
  "en": "Velvet Aurora 3-seater sofa",
  "de": "Velvet Aurora 3-Sitzer-Sofa",
  "fr": "Canapé 3 places Velvet Aurora",
  "it": "Divano 3 posti Velvet Aurora",
  "pt": "Sofá 3 lugares Velvet Aurora",
  "es": "Sofá 3 plazas Velvet Aurora",
  "nl": "Velvet Aurora 3-zitsbank"
 },
 "Colțar U Panorama": {
  "en": "Panorama U-shaped corner sofa",
  "de": "Panorama U-Ecksofa",
  "fr": "Canapé d'angle en U Panorama",
  "it": "Divano angolare a U Panorama",
  "pt": "Sofá de canto em U Panorama",
  "es": "Sofá rinconera en U Panorama",
  "nl": "Panorama U-hoekbank"
 },
 "Canapea extensibilă click-clack Tokyo": {
  "en": "Tokyo click-clack sofa bed",
  "de": "Tokyo Klappsofa Click-Clack",
  "fr": "Canapé convertible clic-clac Tokyo",
  "it": "Divano letto click-clack Tokyo",
  "pt": "Sofá-cama click-clack Tokyo",
  "es": "Sofá cama click-clack Tokyo",
  "nl": "Tokyo click-clack slaapbank"
 },
 "Canapea 3 locuri Cognac Premium": {
  "en": "Cognac Premium 3-seater sofa",
  "de": "Cognac Premium 3-Sitzer-Sofa",
  "fr": "Canapé 3 places Cognac Premium",
  "it": "Divano 3 posti Cognac Premium",
  "pt": "Sofá 3 lugares Cognac Premium",
  "es": "Sofá 3 plazas Cognac Premium",
  "nl": "Cognac Premium 3-zitsbank"
 },
 "Fotoliu Wing clasic Bergère": {
  "en": "Classic Wing Bergère armchair",
  "de": "Klassischer Bergère-Ohrensessel Wing",
  "fr": "Fauteuil Wing classique Bergère",
  "it": "Poltrona Wing classica Bergère",
  "pt": "Poltrona clássica Wing Bergère",
  "es": "Sillón clásico Wing Bergère",
  "nl": "Klassieke Wing Bergère-fauteuil"
 },
 "Fotoliu balansoar Nordic Rocky": {
  "en": "Nordic Rocky rocking chair",
  "de": "Nordic Rocky Schaukelsessel",
  "fr": "Fauteuil à bascule Nordic Rocky",
  "it": "Poltrona a dondolo Nordic Rocky",
  "pt": "Cadeirão de baloiço Nordic Rocky",
  "es": "Sillón mecedora Nordic Rocky",
  "nl": "Nordic Rocky schommelfauteuil"
 },
 "Pat dublu hotel Grand 180x200": {
  "en": "Grand hotel double bed 180x200",
  "de": "Grand Hotel-Doppelbett 180x200",
  "fr": "Lit double style hôtel Grand 180x200",
  "it": "Letto matrimoniale hotel Grand 180x200",
  "pt": "Cama de casal hotel Grand 180x200",
  "es": "Cama doble hotel Grand 180x200",
  "nl": "Grand hotel-tweepersoonsbed 180x200"
 },
 "Pat lemn Nordic Fjord 160x200": {
  "en": "Nordic Fjord wooden bed 160x200",
  "de": "Nordic Fjord Holzbett 160x200",
  "fr": "Lit en bois Nordic Fjord 160x200",
  "it": "Letto in legno Nordic Fjord 160x200",
  "pt": "Cama de madeira Nordic Fjord 160x200",
  "es": "Cama de madera Nordic Fjord 160x200",
  "nl": "Houten bed Nordic Fjord 160x200"
 },
 "Pat cu ladă Storage 140x200": {
  "en": "Storage bed with chest 140x200",
  "de": "Storage Bett mit Bettkasten 140x200",
  "fr": "Lit coffre Storage 140x200",
  "it": "Letto contenitore Storage 140x200",
  "pt": "Cama com arca Storage 140x200",
  "es": "Cama con arcón Storage 140x200",
  "nl": "Opbergbed Storage 140x200"
 },
 "Pat simplu Studio 90x200": {
  "en": "Studio single bed 90x200",
  "de": "Studio Einzelbett 90x200",
  "fr": "Lit simple Studio 90x200",
  "it": "Letto singolo Studio 90x200",
  "pt": "Cama de solteiro Studio 90x200",
  "es": "Cama individual Studio 90x200",
  "nl": "Studio eenpersoonsbed 90x200"
 },
 "Pat rustic Cabana 180x200": {
  "en": "Cabana rustic bed 180x200",
  "de": "Rustikales Bett Cabana 180x200",
  "fr": "Lit rustique Cabana 180x200",
  "it": "Letto rustico Cabana 180x200",
  "pt": "Cama rústica Cabana 180x200",
  "es": "Cama rústica Cabana 180x200",
  "nl": "Rustiek bed Cabana 180x200"
 },
 "Saltea memory Cloud 140x200": {
  "en": "Cloud memory foam mattress 140x200",
  "de": "Cloud Memory-Schaum-Matratze 140x200",
  "fr": "Matelas à mémoire de forme Cloud 140x200",
  "it": "Materasso memory Cloud 140x200",
  "pt": "Colchão de espuma viscoelástica Cloud 140x200",
  "es": "Colchón viscoelástico Cloud 140x200",
  "nl": "Cloud traagschuimmatras 140x200"
 },
 "Dulap 3 uși Torino": {
  "en": "Torino 3-door wardrobe",
  "de": "Torino Kleiderschrank 3-türig",
  "fr": "Armoire 3 portes Torino",
  "it": "Armadio 3 ante Torino",
  "pt": "Roupeiro 3 portas Torino",
  "es": "Armario 3 puertas Torino",
  "nl": "Torino kast met 3 deuren"
 },
 "Dulap 5 uși glisante Madrid": {
  "en": "Madrid 5-door sliding wardrobe",
  "de": "Madrid Schwebetürenschrank 5-türig",
  "fr": "Armoire 5 portes coulissantes Madrid",
  "it": "Armadio 5 ante scorrevoli Madrid",
  "pt": "Roupeiro 5 portas de correr Madrid",
  "es": "Armario 5 puertas correderas Madrid",
  "nl": "Madrid kast met 5 schuifdeuren"
 },
 "Dulap tineret Studio 2 uși": {
  "en": "Studio 2-door youth wardrobe",
  "de": "Studio Jugendschrank 2-türig",
  "fr": "Armoire ado Studio 2 portes",
  "it": "Armadio ragazzi Studio 2 ante",
  "pt": "Roupeiro juvenil Studio 2 portas",
  "es": "Armario juvenil Studio 2 puertas",
  "nl": "Studio jeugdkast met 2 deuren"
 },
 "Dulap rustic Brașov 2 uși": {
  "en": "Brașov rustic 2-door wardrobe",
  "de": "Rustikaler Schrank Brașov 2-türig",
  "fr": "Armoire rustique Brașov 2 portes",
  "it": "Armadio rustico Brașov 2 ante",
  "pt": "Roupeiro rústico Brașov 2 portas",
  "es": "Armario rústico Brașov 2 puertas",
  "nl": "Rustieke kast Brașov met 2 deuren"
 },
 "Bibliotecă scară Ladder 5 trepte": {
  "en": "Ladder 5-tier shelf bookcase",
  "de": "Ladder Leiterregal 5 Stufen",
  "fr": "Bibliothèque échelle Ladder 5 niveaux",
  "it": "Libreria a scala Ladder 5 ripiani",
  "pt": "Estante escada Ladder 5 degraus",
  "es": "Estantería escalera Ladder 5 peldaños",
  "nl": "Ladder trapboekenkast 5 treden"
 },
 "Vitrină Modern Cristal 2 uși": {
  "en": "Modern Cristal 2-door display cabinet",
  "de": "Vitrine Modern Cristal 2-türig",
  "fr": "Vitrine Modern Cristal 2 portes",
  "it": "Vetrina Modern Cristal 2 ante",
  "pt": "Vitrine Modern Cristal 2 portas",
  "es": "Vitrina Modern Cristal 2 puertas",
  "nl": "Vitrinekast Modern Cristal met 2 deuren"
 },
 "Comodă TV Loft 160": {
  "en": "Loft 160 TV unit",
  "de": "Loft TV-Lowboard 160",
  "fr": "Meuble TV Loft 160",
  "it": "Mobile TV Loft 160",
  "pt": "Móvel de TV Loft 160",
  "es": "Mueble de TV Loft 160",
  "nl": "Loft tv-meubel 160"
 },
 "Comodă TV suspendată Float 200": {
  "en": "Float 200 wall-mounted TV unit",
  "de": "Float TV-Hängeboard 200",
  "fr": "Meuble TV suspendu Float 200",
  "it": "Mobile TV sospeso Float 200",
  "pt": "Móvel de TV suspenso Float 200",
  "es": "Mueble de TV suspendido Float 200",
  "nl": "Float zwevend tv-meubel 200"
 },
 "Comodă TV Rustic 150": {
  "en": "Rustic 150 TV unit",
  "de": "Rustic TV-Lowboard 150",
  "fr": "Meuble TV Rustic 150",
  "it": "Mobile TV Rustic 150",
  "pt": "Móvel de TV Rustic 150",
  "es": "Mueble de TV Rustic 150",
  "nl": "Rustic tv-meubel 150"
 },
 "Noptieră suspendată Float": {
  "en": "Float floating bedside table",
  "de": "Float Nachttisch hängend",
  "fr": "Table de chevet suspendue Float",
  "it": "Comodino sospeso Float",
  "pt": "Mesa de cabeceira suspensa Float",
  "es": "Mesita de noche suspendida Float",
  "nl": "Float zwevend nachtkastje"
 },
 "Noptieră Rustic cu sertar": {
  "en": "Rustic bedside table with drawer",
  "de": "Rustic Nachttisch mit Schublade",
  "fr": "Table de chevet Rustic avec tiroir",
  "it": "Comodino Rustic con cassetto",
  "pt": "Mesa de cabeceira Rustic com gaveta",
  "es": "Mesita de noche Rustic con cajón",
  "nl": "Rustic nachtkastje met lade"
 },
 "Comodă 5 sertare Scandi": {
  "en": "Scandi 5-drawer dresser",
  "de": "Scandi Kommode 5 Schubladen",
  "fr": "Commode 5 tiroirs Scandi",
  "it": "Cassettiera 5 cassetti Scandi",
  "pt": "Cómoda 5 gavetas Scandi",
  "es": "Cómoda 5 cajones Scandi",
  "nl": "Scandi ladekast met 5 lades"
 },
 "Masă extensibilă Berlin 160-200": {
  "en": "Berlin extendable table 160-200",
  "de": "Berlin Ausziehtisch 160-200",
  "fr": "Table extensible Berlin 160-200",
  "it": "Tavolo allungabile Berlin 160-200",
  "pt": "Mesa extensível Berlin 160-200",
  "es": "Mesa extensible Berlin 160-200",
  "nl": "Berlin uitschuifbare tafel 160-200"
 },
 "Masă pătrată Cube 90": {
  "en": "Cube 90 square table",
  "de": "Cube quadratischer Tisch 90",
  "fr": "Table carrée Cube 90",
  "it": "Tavolo quadrato Cube 90",
  "pt": "Mesa quadrada Cube 90",
  "es": "Mesa cuadrada Cube 90",
  "nl": "Cube vierkante tafel 90"
 },
 "Măsuță cafea Pallet": {
  "en": "Pallet coffee table",
  "de": "Pallet Couchtisch",
  "fr": "Table basse Pallet",
  "it": "Tavolino da caffè Pallet",
  "pt": "Mesa de centro Pallet",
  "es": "Mesa de centro Pallet",
  "nl": "Pallet salontafel"
 },
 "Măsuță laterală rotundă Duo": {
  "en": "Duo round side table",
  "de": "Duo runder Beistelltisch",
  "fr": "Table d'appoint ronde Duo",
  "it": "Tavolino rotondo Duo",
  "pt": "Mesa de apoio redonda Duo",
  "es": "Mesa auxiliar redonda Duo",
  "nl": "Duo ronde bijzettafel"
 },
 "Masă bar înaltă Pub 120": {
  "en": "Pub 120 high bar table",
  "de": "Pub hoher Bartisch 120",
  "fr": "Table de bar haute Pub 120",
  "it": "Tavolo bar alto Pub 120",
  "pt": "Mesa de bar alta Pub 120",
  "es": "Mesa alta de bar Pub 120",
  "nl": "Pub hoge bartafel 120"
 },
 "Masă rotundă Marmo 120": {
  "en": "Marmo 120 round table",
  "de": "Marmo runder Tisch 120",
  "fr": "Table ronde Marmo 120",
  "it": "Tavolo rotondo Marmo 120",
  "pt": "Mesa redonda Marmo 120",
  "es": "Mesa redonda Marmo 120",
  "nl": "Marmo ronde tafel 120"
 },
 "Set 4 scaune Velvet Aurora": {
  "en": "Velvet Aurora set of 4 chairs",
  "de": "Velvet Aurora 4er-Stuhlset",
  "fr": "Lot de 4 chaises Velvet Aurora",
  "it": "Set 4 sedie Velvet Aurora",
  "pt": "Conjunto de 4 cadeiras Velvet Aurora",
  "es": "Set de 4 sillas Velvet Aurora",
  "nl": "Velvet Aurora set van 4 stoelen"
 },
 "Set 6 scaune Loft metal": {
  "en": "Loft metal set of 6 chairs",
  "de": "Loft Metall 6er-Stuhlset",
  "fr": "Lot de 6 chaises Loft métal",
  "it": "Set 6 sedie Loft metallo",
  "pt": "Conjunto de 6 cadeiras Loft metal",
  "es": "Set de 6 sillas Loft metal",
  "nl": "Loft metalen set van 6 stoelen"
 },
 "Scaune bar Wood (set 2)": {
  "en": "Wood bar stools (set of 2)",
  "de": "Wood Barhocker (2er-Set)",
  "fr": "Tabourets de bar Wood (lot de 2)",
  "it": "Sgabelli bar Wood (set da 2)",
  "pt": "Bancos de bar Wood (conjunto de 2)",
  "es": "Taburetes de bar Wood (set de 2)",
  "nl": "Wood barkrukken (set van 2)"
 },
 "Scaun birou ergonomic Mesh Basic": {
  "en": "Mesh Basic ergonomic office chair",
  "de": "Mesh Basic ergonomischer Bürostuhl",
  "fr": "Chaise de bureau ergonomique Mesh Basic",
  "it": "Sedia da ufficio ergonomica Mesh Basic",
  "pt": "Cadeira de escritório ergonómica Mesh Basic",
  "es": "Silla de oficina ergonómica Mesh Basic",
  "nl": "Mesh Basic ergonomische bureaustoel"
 },
 "Set 4 scaune Windsor lemn": {
  "en": "Windsor wooden set of 4 chairs",
  "de": "Windsor Holz 4er-Stuhlset",
  "fr": "Lot de 4 chaises Windsor bois",
  "it": "Set 4 sedie Windsor legno",
  "pt": "Conjunto de 4 cadeiras Windsor madeira",
  "es": "Set de 4 sillas Windsor madera",
  "nl": "Windsor houten set van 4 stoelen"
 },
 "Scaun conferință Executive piele": {
  "en": "Executive leather conference chair",
  "de": "Executive Konferenzstuhl Leder",
  "fr": "Chaise de conférence Executive cuir",
  "it": "Sedia conferenza Executive pelle",
  "pt": "Cadeira de conferência Executive pele",
  "es": "Silla de conferencia Executive piel",
  "nl": "Executive vergaderstoel leer"
 },
 "Taburet rotund Cub": {
  "en": "Cub round stool",
  "de": "Cub runder Hocker",
  "fr": "Tabouret rond Cub",
  "it": "Sgabello rotondo Cub",
  "pt": "Banco redondo Cub",
  "es": "Taburete redondo Cub",
  "nl": "Cub ronde kruk"
 },
 "Birou compact Mini 100": {
  "en": "Mini 100 compact desk",
  "de": "Mini kompakter Schreibtisch 100",
  "fr": "Bureau compact Mini 100",
  "it": "Scrivania compatta Mini 100",
  "pt": "Secretária compacta Mini 100",
  "es": "Escritorio compacto Mini 100",
  "nl": "Mini compact bureau 100"
 },
 "Birou rustic lemn masiv Atelier 150": {
  "en": "Atelier 150 rustic solid wood desk",
  "de": "Atelier rustikaler Massivholz-Schreibtisch 150",
  "fr": "Bureau rustique bois massif Atelier 150",
  "it": "Scrivania rustica legno massello Atelier 150",
  "pt": "Secretária rústica madeira maciça Atelier 150",
  "es": "Escritorio rústico madera maciza Atelier 150",
  "nl": "Atelier rustiek massief houten bureau 150"
 },
 "Birou gaming Dragon 140": {
  "en": "Dragon 140 gaming desk",
  "de": "Dragon Gaming-Schreibtisch 140",
  "fr": "Bureau gaming Dragon 140",
  "it": "Scrivania gaming Dragon 140",
  "pt": "Secretária gaming Dragon 140",
  "es": "Escritorio gaming Dragon 140",
  "nl": "Dragon gamingbureau 140"
 },
 "Birou cu rafturi Studio 120": {
  "en": "Studio 120 desk with shelves",
  "de": "Studio Schreibtisch mit Regal 120",
  "fr": "Bureau avec étagères Studio 120",
  "it": "Scrivania con ripiani Studio 120",
  "pt": "Secretária com prateleiras Studio 120",
  "es": "Escritorio con estantes Studio 120",
  "nl": "Studio bureau met planken 120"
 },
 "Birou secretar Wall 80": {
  "en": "Wall 80 secretary desk",
  "de": "Wall Sekretär 80",
  "fr": "Secrétaire mural Wall 80",
  "it": "Scrittoio Wall 80",
  "pt": "Secretária de parede Wall 80",
  "es": "Escritorio secreter Wall 80",
  "nl": "Wall secretaire 80"
 },
 "Lampadar trepied Tripod": {
  "en": "Tripod floor lamp",
  "de": "Tripod Stehlampe mit Dreibein",
  "fr": "Lampadaire trépied Tripod",
  "it": "Piantana treppiede Tripod",
  "pt": "Candeeiro de pé tripé Tripod",
  "es": "Lámpara de pie trípode Tripod",
  "nl": "Tripod driepoot vloerlamp"
 },
 "Lampadar LED Torch Slim": {
  "en": "Torch Slim LED floor lamp",
  "de": "Torch Slim LED-Stehlampe",
  "fr": "Lampadaire LED Torch Slim",
  "it": "Piantana LED Torch Slim",
  "pt": "Candeeiro de pé LED Torch Slim",
  "es": "Lámpara de pie LED Torch Slim",
  "nl": "Torch Slim LED-vloerlamp"
 },
 "Veioză birou LED Flex": {
  "en": "Flex LED desk lamp",
  "de": "Flex LED-Schreibtischlampe",
  "fr": "Lampe de bureau LED Flex",
  "it": "Lampada da scrivania LED Flex",
  "pt": "Candeeiro de secretária LED Flex",
  "es": "Lámpara de escritorio LED Flex",
  "nl": "Flex LED-bureaulamp"
 },
 "Veioză sticlă Opal": {
  "en": "Opal glass table lamp",
  "de": "Opal Glas-Tischlampe",
  "fr": "Lampe de chevet en verre Opal",
  "it": "Lampada da tavolo in vetro Opal",
  "pt": "Candeeiro de mesa em vidro Opal",
  "es": "Lámpara de sobremesa de vidrio Opal",
  "nl": "Opal glazen tafellamp"
 },
 "Aplică dublă Twin": {
  "en": "Twin double wall light",
  "de": "Twin Doppel-Wandleuchte",
  "fr": "Applique double Twin",
  "it": "Applique doppia Twin",
  "pt": "Aplique dupla Twin",
  "es": "Aplique doble Twin",
  "nl": "Twin dubbele wandlamp"
 },
 "Lustră cubică Cube": {
  "en": "Cube cubic chandelier",
  "de": "Cube Kubus-Kronleuchter",
  "fr": "Lustre cubique Cube",
  "it": "Lampadario cubico Cube",
  "pt": "Lustre cúbico Cube",
  "es": "Lámpara de araña cúbica Cube",
  "nl": "Cube kubusvormige kroonluchter"
 },
 "Cuier stativ Tree": {
  "en": "Tree coat stand",
  "de": "Tree Garderobenständer",
  "fr": "Portemanteau sur pied Tree",
  "it": "Appendiabiti a stelo Tree",
  "pt": "Cabide de pé Tree",
  "es": "Perchero de pie Tree",
  "nl": "Tree staande kapstok"
 },
 "Consolă hol Slim 100": {
  "en": "Slim 100 hallway console",
  "de": "Slim Flurkonsole 100",
  "fr": "Console d'entrée Slim 100",
  "it": "Consolle ingresso Slim 100",
  "pt": "Consola de hall Slim 100",
  "es": "Consola de entrada Slim 100",
  "nl": "Slim halconsole 100"
 },
 "Dulap pantofi Step 2 uși": {
  "en": "Step 2-door shoe cabinet",
  "de": "Step Schuhschrank 2-türig",
  "fr": "Meuble à chaussures Step 2 portes",
  "it": "Scarpiera Step 2 ante",
  "pt": "Sapateira Step 2 portas",
  "es": "Zapatero Step 2 puertas",
  "nl": "Step schoenenkast met 2 deuren"
 },
 "Bancă pantofi Nordic": {
  "en": "Nordic shoe bench",
  "de": "Nordic Schuhbank",
  "fr": "Banc à chaussures Nordic",
  "it": "Panca scarpiera Nordic",
  "pt": "Banco sapateira Nordic",
  "es": "Banco zapatero Nordic",
  "nl": "Nordic schoenenbank"
 },
 "Cuier cu oglindă Entry": {
  "en": "Entry coat rack with mirror",
  "de": "Entry Garderobe mit Spiegel",
  "fr": "Portemanteau avec miroir Entry",
  "it": "Appendiabiti con specchio Entry",
  "pt": "Cabide com espelho Entry",
  "es": "Perchero con espejo Entry",
  "nl": "Entry kapstok met spiegel"
 },
 "Pat etajat Bunk 90x200": {
  "en": "Bunk 90x200 bunk bed",
  "de": "Bunk Etagenbett 90x200",
  "fr": "Lit superposé Bunk 90x200",
  "it": "Letto a castello Bunk 90x200",
  "pt": "Beliche Bunk 90x200",
  "es": "Litera Bunk 90x200",
  "nl": "Bunk stapelbed 90x200"
 },
 "Pat copii Montessori 90x190": {
  "en": "Montessori kids' bed 90x190",
  "de": "Montessori Kinderbett 90x190",
  "fr": "Lit enfant Montessori 90x190",
  "it": "Letto bambini Montessori 90x190",
  "pt": "Cama infantil Montessori 90x190",
  "es": "Cama infantil Montessori 90x190",
  "nl": "Montessori kinderbed 90x190"
 },
 "Birou copii Rocket": {
  "en": "Rocket kids' desk",
  "de": "Rocket Kinderschreibtisch",
  "fr": "Bureau enfant Rocket",
  "it": "Scrivania bambini Rocket",
  "pt": "Secretária infantil Rocket",
  "es": "Escritorio infantil Rocket",
  "nl": "Rocket kinderbureau"
 },
 "Dulap copii Zoo": {
  "en": "Zoo kids' wardrobe",
  "de": "Zoo Kinderschrank",
  "fr": "Armoire enfant Zoo",
  "it": "Armadio bambini Zoo",
  "pt": "Roupeiro infantil Zoo",
  "es": "Armario infantil Zoo",
  "nl": "Zoo kinderkast"
 },
 "Comodă copii Colors": {
  "en": "Colors kids' dresser",
  "de": "Colors Kinderkommode",
  "fr": "Commode enfant Colors",
  "it": "Cassettiera bambini Colors",
  "pt": "Cómoda infantil Colors",
  "es": "Cómoda infantil Colors",
  "nl": "Colors kinderladekast"
 },
 "Bibliotecă copii Mini": {
  "en": "Mini kids' bookcase",
  "de": "Mini Kinderbücherregal",
  "fr": "Bibliothèque enfant Mini",
  "it": "Libreria bambini Mini",
  "pt": "Estante infantil Mini",
  "es": "Estantería infantil Mini",
  "nl": "Mini kinderboekenkast"
 },
 "Scaun dining Windsor lemn": {
  "en": "Windsor wooden dining chair",
  "de": "Windsor Holz-Esszimmerstuhl",
  "fr": "Chaise de salle à manger Windsor bois",
  "it": "Sedia da pranzo Windsor legno",
  "pt": "Cadeira de jantar Windsor madeira",
  "es": "Silla de comedor Windsor madera",
  "nl": "Windsor houten eetkamerstoel"
 },
 "Scaun dining Velvet Aurora": {
  "en": "Velvet Aurora dining chair",
  "de": "Velvet Aurora Esszimmerstuhl",
  "fr": "Chaise de salle à manger Velvet Aurora",
  "it": "Sedia da pranzo Velvet Aurora",
  "pt": "Cadeira de jantar Velvet Aurora",
  "es": "Silla de comedor Velvet Aurora",
  "nl": "Velvet Aurora eetkamerstoel"
 },
 "Scaun dining Fjord curbat": {
  "en": "Fjord bentwood dining chair",
  "de": "Fjord Bugholz-Esszimmerstuhl",
  "fr": "Chaise de salle à manger Fjord bois courbé",
  "it": "Sedia da pranzo Fjord in legno curvato",
  "pt": "Cadeira de jantar Fjord madeira curvada",
  "es": "Silla de comedor Fjord madera curvada",
  "nl": "Fjord gebogen houten eetkamerstoel"
 },
 "Scaun dining bouclé Cloud": {
  "en": "Cloud bouclé dining chair",
  "de": "Cloud Bouclé-Esszimmerstuhl",
  "fr": "Chaise de salle à manger bouclé Cloud",
  "it": "Sedia da pranzo bouclé Cloud",
  "pt": "Cadeira de jantar bouclé Cloud",
  "es": "Silla de comedor bouclé Cloud",
  "nl": "Cloud bouclé eetkamerstoel"
 },
 "Scaun dining piele Cognac": {
  "en": "Cognac leather dining chair",
  "de": "Cognac Leder-Esszimmerstuhl",
  "fr": "Chaise de salle à manger cuir Cognac",
  "it": "Sedia da pranzo in pelle Cognac",
  "pt": "Cadeira de jantar em pele Cognac",
  "es": "Silla de comedor de piel Cognac",
  "nl": "Cognac leren eetkamerstoel"
 },
 "Scaun rattan Bali": {
  "en": "Bali rattan chair",
  "de": "Bali Rattanstuhl",
  "fr": "Chaise en rotin Bali",
  "it": "Sedia in rattan Bali",
  "pt": "Cadeira de rattan Bali",
  "es": "Silla de ratán Bali",
  "nl": "Bali rotanstoel"
 },
 "Scaun shell Nordic": {
  "en": "Nordic shell chair",
  "de": "Nordic Schalenstuhl",
  "fr": "Chaise coque Nordic",
  "it": "Sedia a guscio Nordic",
  "pt": "Cadeira concha Nordic",
  "es": "Silla de cáscara Nordic",
  "nl": "Nordic schaalstoel"
 },
 "Scaun metalic Loft Factory": {
  "en": "Loft Factory metal chair",
  "de": "Loft Factory Metallstuhl",
  "fr": "Chaise métallique Loft Factory",
  "it": "Sedia in metallo Loft Factory",
  "pt": "Cadeira metálica Loft Factory",
  "es": "Silla metálica Loft Factory",
  "nl": "Loft Factory metalen stoel"
 },
 "Scaun pliabil Compact": {
  "en": "Compact folding chair",
  "de": "Compact Klappstuhl",
  "fr": "Chaise pliante Compact",
  "it": "Sedia pieghevole Compact",
  "pt": "Cadeira dobrável Compact",
  "es": "Silla plegable Compact",
  "nl": "Compact klapstoel"
 },
 "Scaun bar Wood": {
  "en": "Wood bar stool",
  "de": "Wood Barhocker",
  "fr": "Tabouret de bar Wood",
  "it": "Sgabello bar Wood",
  "pt": "Banco de bar Wood",
  "es": "Taburete de bar Wood",
  "nl": "Wood barkruk"
 },
 "Scaun bar înalt Industrial": {
  "en": "Industrial high bar stool",
  "de": "Industrial hoher Barhocker",
  "fr": "Tabouret de bar haut Industrial",
  "it": "Sgabello bar alto Industrial",
  "pt": "Banco de bar alto Industrial",
  "es": "Taburete alto de bar Industrial",
  "nl": "Industrial hoge barkruk"
 },
 "Scaun gaming Racer Pro": {
  "en": "Racer Pro gaming chair",
  "de": "Racer Pro Gaming-Stuhl",
  "fr": "Chaise gaming Racer Pro",
  "it": "Sedia gaming Racer Pro",
  "pt": "Cadeira gaming Racer Pro",
  "es": "Silla gaming Racer Pro",
  "nl": "Racer Pro gamingstoel"
 },
 "Scaun ergonomic Posture cu tetieră": {
  "en": "Posture ergonomic chair with headrest",
  "de": "Posture ergonomischer Stuhl mit Kopfstütze",
  "fr": "Chaise ergonomique Posture avec appui-tête",
  "it": "Sedia ergonomica Posture con poggiatesta",
  "pt": "Cadeira ergonómica Posture com apoio de cabeça",
  "es": "Silla ergonómica Posture con reposacabezas",
  "nl": "Posture ergonomische stoel met hoofdsteun"
 },
 "Scaun birou Basic Studio": {
  "en": "Basic Studio office chair",
  "de": "Basic Studio Bürostuhl",
  "fr": "Chaise de bureau Basic Studio",
  "it": "Sedia da ufficio Basic Studio",
  "pt": "Cadeira de escritório Basic Studio",
  "es": "Silla de oficina Basic Studio",
  "nl": "Basic Studio bureaustoel"
 }
}
export function pname(name) {
  const g = GEN[i18n.lang] && GEN[i18n.lang][name]
  if (g && g.name) return g.name
  const m = N[name]
  return (m && m[i18n.lang]) || name
}
// câmp tradus al unui produs: description | material | color (cade pe originalul din română)
export function pfield(p, f) {
  const g = GEN[i18n.lang] && GEN[i18n.lang][p.name]
  return (g && g[f]) || p[f]
}
