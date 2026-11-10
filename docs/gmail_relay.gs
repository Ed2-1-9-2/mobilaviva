// Releu email prin contul Gmail (HTTPS, fără SMTP). Emailul pleacă de pe mobilaviva.shop@gmail.com și ajunge în Inbox.
// 1) Intră pe script.google.com cu mobilaviva.shop@gmail.com -> Proiect nou -> lipește tot codul de aici -> Salvează.
// 2) Deploy -> Deployment nou -> tip „Aplicație web” -> Execută ca: Eu -> Acces: Oricine -> Deploy -> Autorizează (Avansat -> Continuă).
// 3) Copiază „URL aplicație web” (https://script.google.com/macros/s/.../exec) în setup_mobila_java_vue.sh: GMAIL_RELAY_URL_DEFAULT="..."
var TOKEN = '895c1393ab4975fb5dcca12c2cf714d0';
function doPost(e) {
  try {
    var d = JSON.parse(e.postData.contents);
    if (d.token !== TOKEN) return out({ ok: false, error: 'token' });
    if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(d.to || '')) return out({ ok: false, error: 'destinatar' });
    MailApp.sendEmail({ to: d.to, subject: String(d.subject || ''), body: String(d.body || ''), name: 'MobilaViva' });
    return out({ ok: true });
  } catch (err) {
    return out({ ok: false, error: String(err) });
  }
}
function doGet() { return out({ ok: true, info: 'releu activ' }); }
function out(o) { return ContentService.createTextOutput(JSON.stringify(o)).setMimeType(ContentService.MimeType.JSON); }
