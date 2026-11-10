#!/usr/bin/env bash
# Listează produsele și numele de fișier al pozei fiecăruia (✔ = poza există în frontend/public/photos/).
# Creează/actualizează docs/photos-urls.txt: pui URL-ul pozei lângă fiecare nume, apoi rulezi ./scripts/fetch_photos.sh
cd "$(dirname "$0")/.." || exit 1
command -v node >/dev/null 2>&1 || { echo "Lipsește Node 18+" >&2; exit 1; }
node -e '
const fs = require("fs");
const src = fs.readFileSync("src/main/java/com/mobila/shop/SeedData.java", "utf8");
const slug = n => n.normalize("NFD").replace(/\p{M}/gu, "").toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "");
const names = [...src.matchAll(/\bp\("([^"]+)"/g)].map(m => m[1]);
const f = "docs/photos-urls.txt";
const old = fs.existsSync(f) ? Object.fromEntries(fs.readFileSync(f, "utf8").split("\n").map(l => l.trim().split(/\s+/)).filter(a => a[0] && a[0][0] !== "#" && a[1]).map(a => [a[0], a[1]])) : {};
const out = ["# nume-fisier<spațiu>URL-ul pozei (fără URL = fără poză). Apoi: ./scripts/fetch_photos.sh",
  "# Unsplash: deschizi poza, copiezi ID-ul de la finalul adresei și folosești https://unsplash.com/photos/ID/download?force=true&w=900",
  ...names.map(n => slug(n) + " " + (old[slug(n)] || ""))];
fs.mkdirSync("docs", { recursive: true });
fs.writeFileSync(f, out.join("\n") + "\n");
let have = 0;
for (const n of names) {
  const s = slug(n), ok = ["webp", "jpg", "jpeg", "png"].some(e => fs.existsSync("frontend/public/photos/" + s + "." + e));
  if (ok) have++;
  console.log((ok ? "✔ " : "  ") + s + "   (" + n + ")");
}
console.log("\n" + have + "/" + names.length + " produse au poză. URL-uri de completat în docs/photos-urls.txt");
'
