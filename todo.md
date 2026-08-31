1) vystavit a poslat faktury vsem prihlasenym
2) [HOTOVO ✅ nasazeno] pridat export a filtr faktur tabulku - cislo fa, odberatel, castka, obdobi (datumy), rozdelemy - krouzek + dres
   - filtr: obdobi od-do, stav, typ (krouzek/skola/kemp); export XLSX+CSV; sloupce Krouzek/Dres (rozdeleni castky z polozek faktury)
3) [HOTOVO ✅ nasazeno] vykazy prace pro brigadniky - staci hodiny
   - /admin/vykazy: brigadnik zapisuje/upravuje/maze vlastni hodiny (pending), admin schvaluje/zamita
   - souhrn per osoba (schvaleno/ceka) + export XLSX/CSV pro vyplaty; "Nacist ze smen" predvyplni ze schvalenych smen
4) [HOTOVO ✅ nasazeno] spojit registracni email s fakturou do jednoho
   - AUTO-FAKTURA: kdyz rodic zaregistruje dite na PLACENY krouzek/kemp (club/camp, cena>0, ma email),
     system automaticky vytvori fakturu + posle dekovny email s prilozenou fakturou (PDF) + nastavi 'faktura odeslana'
   - Skolky (school) se fakturuji rucne adminem (tlacitko Vystavit fakturu zustava)
   - Nove stavy platby: nezaplaceno / faktura odeslana / po splatnosti (odvozene z dueDate) / zaplaceno
5) [HOTOVO ✅ nasazeno] pridat nahrani vypisu z uctu a parovani faktur
   - /admin/parovani: nahraj Raiffeisen CSV -> parser (BOM strip, ; delimiter, dd.MM.yyyy)
   - parovani dle VS + castka: EXACT (predvybrano) / PARTIAL (castka nesedi) / NONE / OUTGOING / ALREADY
   - preview (bez zapisu) -> admin vybere -> confirm oznaci faktury zaplacene
   - IDEMPOTENCE pres bank tx id (V17 bank_transaction, unique tx_id) - re-upload nikdy nezaplati dvakrat
6) gdpr na webu
7) notifikace
8) [HOTOVO ✅ nasazeno] nahravani uctenek, DPP, smlouvy
   - /admin/doklady: novy modul "Doklady", upload PDF/obrazku + metadata (typ uctenka/DPP/smlouva/ostatni, osoba, datum, castka, poznamka)
   - SOUKROME uloziste (mimo public /media), download jen pres auth endpoint; RBAC owner/admin/accountant
   - V16 record_document; novy docker volume hopik_cms_records (/data/records)
9) pridat vek deti
10) barevne rozlisit, ze je krouzek bud pro verejnost (vsechny deti), nebo jen pro deti z dane skolky
11) do listy pridat GDPR
    - v zalozce Registrace (seznam vsech deti) u kazdeho ditete na prvni pohled videt GDPR souhlasy
      (souhlas s porizovanim fotografii/media + souhlas s osobnimi udaji) - ikonka/badge ve sloupci

---
POZNAMKY / TECH DLUH:
- DEPLOY: GitHub fetch na VPS selhava (chybi credential/token, repo private). Docasne reseno git bundle (scp souboru). NUTNO SPRAVIT: deploy token na VPS nebo prepnout remote na SSH.
- Migrace na prod: aplikovano do V17 (bank_transaction). Dalsi bude V18.
- Dark mode: barvy sjednocene pres CSS promenne (success/warning/danger/info). Pokud neco svetli v tmavem rezimu, poslat screenshot.
