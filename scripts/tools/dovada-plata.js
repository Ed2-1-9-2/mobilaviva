// Dovadă I.10: afișează plățile reușite din Stripe (mod test), fără a expune cheia secretă.
let d = "";
process.stdin.on("data", c => (d += c)).on("end", () => {
  let j;
  try { j = JSON.parse(d); } catch (e) { console.error("Răspuns invalid de la Stripe."); process.exit(1); }
  if (j.error) { console.error("Stripe: " + j.error.message); process.exit(1); }
  const rows = (j.data || []).filter(s => s.payment_status === "paid");
  const pad = (v, n) => String(v).padEnd(n);
  const L = [];
  L.push("DOVADĂ I.10 — Plata facturii: plată electronică TESTATĂ");
  L.push("Procesator: Stripe Checkout (cont de test / sandbox), card de test 4242 4242 4242 4242");
  L.push("Generat la: " + new Date().toLocaleString("ro-RO"));
  L.push("");
  L.push(pad("Data", 18) + pad("Sesiune Stripe", 22) + pad("Sumă", 16) + pad("Stare", 8) + pad("Card", 18) + "Mod");
  L.push("-".repeat(90));
  let live = false;
  for (const s of rows) {
    const pm = s.payment_intent && s.payment_intent.payment_method && s.payment_intent.payment_method.card;
    const card = pm ? pm.brand.toUpperCase() + " **** " + pm.last4 : "card";
    live = live || s.livemode;
    const data = new Date(s.created * 1000).toLocaleString("ro-RO", { dateStyle: "short", timeStyle: "short" });
    const suma = (s.amount_total / 100).toLocaleString("ro-RO", { minimumFractionDigits: 2 }) + " " + s.currency.toUpperCase();
    L.push(pad(data, 18) + pad(s.id.slice(0, 13) + "…" + s.id.slice(-6), 22) + pad(suma, 16) + pad("plătit", 8) + pad(card, 18) + (s.livemode ? "LIVE" : "TEST"));
  }
  L.push("");
  L.push(rows.length
    ? `Concluzie: ${rows.length === 1 ? "1 plată reușită" : rows.length + " plăți reușite"}${live ? "" : ", în mod TEST (livemode=false)"} — cerința I.10 îndeplinită.`
    : "Nicio plată reușită. Fă o comandă cu cardul de test și rulează din nou.");
  if (live) L.push("ATENȚIE: există plăți LIVE; folosește doar chei sk_test_.");
  console.log(L.join("\n"));
});
