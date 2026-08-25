# PRD — Hopík4Kids CMS & provozní platforma (náhrada za Strapi)

> **Účel dokumentu:** Kompletní zadání pro postavení nové webové aplikace, která nahradí
> současné Strapi CMS a zároveň se stane **rozšiřitelnou provozní platformou** Hopíka.
> Aplikace: (1) vystaví **vlastní čisté REST API** pro veřejný web (+ dodá PR, který web na
> nové API přepojí — viz sekce 5), (2) nabídne **admin UI s RBAC** pro tým, který se bude
> rozrůstat, a (3) je navržena **modulárně** pro postupné rozšiřování (rozvrh, fakturace,
> dokumenty…).
>
> Tento dokument je **master prompt / PRD** — lze ho předat AI agentovi nebo vývojáři jako
> úplné zadání. Obsahuje přesný datový model odvozený z reálného provozu, business pravidla,
> API kontrakt, RBAC a UX požadavky.
>
> **Orientace v dokumentu:**
> - Sekce **3** = současný Strapi model (as-is reference pro migraci).
> - Sekce **3B** = ⭐ **cílový datový model** (podle něj se staví).
> - Sekce **5** = API kontrakt (varianta B — vlastní čisté API).
> - Sekce **7** = auth + RBAC + správa uživatelů.
> - Sekce **12A** = modulární architektura.
> - Sekce **15** = fázování; **14** = rozhodnuté otázky.

---

## 1. Kontext a motivace

### 1.1 Současný stav
- Web **hopik4kids.cz** = Next.js 16 (App Router), React 19, TypeScript, Tailwind 4.
- Data (kurzy, školky, kempy, registrace, aktuality) žijí ve **Strapi v5**.
- Frontend čte přes REST (`/api/kindergartens`, `/api/courses`, `/api/camps`, `/api/registrations`,
  `/api/camp-registrations`, `/api/articles`) s ISR revalidací 60 s.
- Registrace se zapisují **přímo z prohlížeče** klienta POSTem do Strapi (token v
  `NEXT_PUBLIC_STRAPI_API_TOKEN` — **exposován v browseru**, viz bezpečnostní dluh, sekce 9).
- Potvrzovací e-maily řeší Next.js API routes přes Nodemailer (WEDOS SMTP).

### 1.2 Proč měnit
- Strapi "začíná být krátký" — složitá správa, těžkopádné pro netechnické správce, náročný
  provoz/upgrady, špatně se rozšiřuje o vlastní logiku (RBAC, rozvrh, fakturace).
- Cíl: **vlastní rozšiřitelná platforma** s čistým API + **admin UI s rolemi** šité na míru
  workflow Hopíka (programy, registrace, aktuality, tým), připravená růst.

### 1.3 Cíloví uživatelé
- **Founders** (Matěj, Petr) — plná správa vč. přidávání dalších členů týmu.
- **Trenéři** — rozrůstající se tým; svůj rozvrh, hlášení na hodiny, interní dokumenty.
- Všichni netechnici → priorita: přehlednost, minimum klikání, žádný žargon.
- Hlavní úkony: číst/exportovat registrace, spravovat programy (kurzy/kempy), psát aktuality,
  hlídat naplněnost, spravovat tým a role.

---

## 2. Rozsah (Scope)

### 2.1 In-scope (Fáze 0 — základ)
1. **Modulární datový backend** (DB) — entity dle sekce **3B** (Program, Location, Registration,
   Child/Parent, Article, Media, User, Invitation, AuditLog).
2. **Veřejné REST API** (vlastní čisté, sekce 5) pro čtení webem + zápis registrací.
3. **PR do webového repa** — přepojení webu na nové API (sekce 5.7).
4. **Admin UI s RBAC** — role owner/admin/trainer, správa uživatelů, pozvánky (sekce 7).
5. **Autentizace** (login, pozvánky e-mailem, reset hesla).
6. **Media upload** (cover aktualit).
7. **CSV/Excel export** registrací.
8. **Migrace** dat ze Strapi (skript, sekce 3C).
9. **Oprava bezpečnostního dluhu** (token v browseru, sekce 9).

Pozdější fáze (1–3): dashboard, docházkové listy, stav plateb, fakturace, rozvrh, shift-signup,
dokumenty — viz roadmapa (sekce 15).

### 2.2 Out-of-scope
- Platební brána (fakturace = generování dokladů s QR, ne online platba; fáze 2).
- Vícejazyčnost (web je pouze `cs`).
- Poptávky škol jako evidované leady (rozhodnuto 14.3 — řeší trenéři domluvou).
- Ověření dětí proti seznamu školky (`roster`, 3B.10) — jen jako budoucí možnost.
- Odesílání **potvrzovacích** e-mailů rodičům zůstává na webu (14.4). CMS posílá jen
  **systémové** e-maily (pozvánky, reset hesla, notifikace) — sekce 8.

---

## 3. Datový model — SOUČASNÝ STAV (Strapi, "as-is" reference)

> ⚠ Tato sekce popisuje **stávající Strapi model se všemi jeho problémy** — slouží jako
> reference pro migraci a pochopení, co dnes web čeká. **Cílový (nový) model je v sekci 3B.**
> Nestavěj podle sekce 3, stavěj podle **3B**.

> Strapi v5 vrací **ploché** objekty (žádné vnořené `attributes`). Každá entita má
> `id` (number), `documentId` (string, veřejný identifikátor používaný v URL a relacích),
> `createdAt`, `updatedAt`, `publishedAt`.

### 3.1 Kindergarten (školka) — `kindergartens`
Jedna školka má více kurzů (1:N na Course).

| Pole | Typ | Povinné | Pozn. |
|------|-----|---------|-------|
| `id` | number (auto) | ✔ | interní |
| `documentId` | string (uuid-like) | ✔ | veřejný ID |
| `name` | string | ✔ | název školky |
| `address` | string | ✘ | adresa |
| `courses` | relation 1:N → Course | ✘ | populate přes `?populate=courses` |
| `createdAt` / `updatedAt` / `publishedAt` | datetime | ✔ | audit |

### 3.2 Course (kurz / pravidelný kroužek) — `courses`
Patří jedné školce (N:1 → Kindergarten). Má registrace (1:N → Registration).

| Pole | Typ | Povinné | Pozn. |
|------|-----|---------|-------|
| `id` | number | ✔ | |
| `documentId` | string | ✔ | používá se v URL `/prihlaseni/[courseId]` a ve filtrech |
| `courseName` | string | ✔ | název kurzu |
| `dayAndTime` | **datetime (ISO string)** | ✔ | ⚠ ukládá se jako datetime; frontend z něj bere **den v týdnu + čas** (`formatCourseDateTime`). Datum samotné je irelevantní, důležitý je weekday + HH:MM. |
| `price` | number (integer, Kč) | ✔ | cena kurzu |
| `capacity` | number (integer) | ✔ | max. počet dětí |
| `kindergarten` | relation N:1 → Kindergarten | ✔ | vlastník |
| `currentRegistrations` | **derived, ne v DB** | — | dopočítává frontend z počtu registrací |

### 3.3 Camp (příměstský tábor / turnus) — `camps`
Nezávislá entita (bez vazby na školku). Má registrace (1:N → CampRegistration).

| Pole | Typ | Povinné | Pozn. |
|------|-----|---------|-------|
| `id` | number | ✔ | |
| `documentId` | string | ✔ | URL `/prihlaseni/kemp/[campId]` + filtry |
| `name` | string | ✔ | název turnusu (např. "HOPÍKEMP — TK Škoda Plzeň") |
| `address` | string | ✘ | místo konání |
| `startDate` | date (ISO) | ✔ | začátek turnusu |
| `endDate` | date (ISO) | ✔ | konec turnusu |
| `price` | number (Kč) | ✔ | cena |
| `capacity` | number \| null | ✘ | max. dětí; **null = neomezeno / neuvedeno** |
| `currentRegistrations` | derived | — | dopočítává frontend |

### 3.4 Registration (registrace do kurzu) — `registrations`
Vytváří se z veřejného webu (POST). Patří jednomu kurzu (N:1 → Course přes `documentId`).

> ⚠ Frontend (dnes) počítá naplněnost tak, že načte **všechny** registrace a seskupí podle
> `registration.course` (= `documentId` kurzu) — pomalé a vystavuje osobní data. **Nový model
> to řeší přes `Program.spotsTaken`** (sekce 3B.9, 5.5).

| Pole | Typ | Povinné | Pozn. |
|------|-----|---------|-------|
| `id` / `documentId` | | ✔ | |
| `course` | relation N:1 → Course | ✔ | posílá se `documentId` kurzu |
| `childName` | string | ✔ | jméno a příjmení dítěte |
| `birthDate` | date | ✔ | datum narození (ne v budoucnosti) |
| `personalId` | string | ✔ | rodné číslo, formát `XXXXXX/XXXX` |
| `className` | string | ✔ | třída (např. "Motýlci") |
| `address` | string | ✔ | adresa dítěte |
| `healthInsuranceText` | string | ✔ | zdravotní pojišťovna (volný text) |
| `parentName` | string | ✔ | jméno rodiče |
| `parentPhone` | string | ✔ | telefon `+420 XXX XXX XXX` |
| `parentEmail` | email | ✔ | e-mail rodiče |
| `secondParentName` | string | ✘ | druhý rodič |
| `secondParentPhone` | string | ✘ | |
| `note` | text | ✘ | poznámka |
| `wantsShirt` | boolean | ✔ | objednávka dresu (+500 Kč) |
| `shirtSize` | string | ✘ | povinné pokud `wantsShirt` (hodnoty: `118-127`,`128-134`,`140-146`,`152-158`,`160-164`,`S`,`M`) |
| `nickName` | string | ✘ | jméno/přezdívka na dres |
| `consentPersonalData` | boolean | ✔ | GDPR souhlas (musí být true) |
| `consentMedia` | boolean | ✔ | souhlas s foto/video |

### 3.5 CampRegistration (registrace na kemp) — `camp-registrations`
Analogické, patří jednomu kempu (N:1 → Camp přes `documentId`).

| Pole | Typ | Povinné | Pozn. |
|------|-----|---------|-------|
| `id` / `documentId` | | ✔ | |
| `camp` | relation N:1 → Camp | ✔ | posílá se `documentId` kempu |
| `childName` | string | ✔ | |
| `birthDate` | date | ✔ | |
| `personalId` | string | ✔ | `XXXXXX/XXXX` |
| `address` | string | ✔ | |
| `healthInsuranceText` | string | ✔ | |
| `shirtSize` | string | ✔ | **povinné** (u kempu je dres vždy) |
| `nickName` | string | ✘ | |
| `allergies` | text | ✘ | alergie / zdravotní omezení |
| `parentName` | string | ✔ | |
| `parentPhone` | string | ✔ | |
| `parentEmail` | email | ✔ | |
| `secondParentName` | string | ✘ | |
| `secondParentPhone` | string | ✘ | |
| `note` | text | ✘ | |
| `consentPersonalData` | boolean | ✔ | |
| `consentMedia` | boolean | ✔ | |

### 3.6 Article (aktualita / blog) — `articles`
Čte se na `/aktuality` a `/aktuality/[slug]`. Každá aktualita musí být v sitemap.

| Pole | Typ | Povinné | Pozn. |
|------|-----|---------|-------|
| `id` / `documentId` | | ✔ | |
| `title` | string | ✔ | |
| `slug` | string (unique) | ✔ | URL identifikátor, filtr `?filters[slug][$eq]=` |
| `excerpt` | text | ✘ | perex |
| `content` | rich text / HTML | ✘ | tělo článku (renderuje se přes `dangerouslySetInnerHTML`) |
| `cover` | media (image) | ✘ | úvodní obrázek; frontend čte `cover.url` + `cover.alternativeText` |
| `publishedAt` | datetime | ✔ | řazení `?sort=publishedAt:desc`; draft = `null` |

### 3.7 Media (upload) — `media`
Ploché (Strapi v5): `id`, `url`, `alternativeText?`, `width?`, `height?`,
`formats?` (varianty thumbnail/small/medium/large). Relativní URL se na webu prefixuje
host adresou Strapi (`resolveMediaUrl`).

### 3.8 Diagram vztahů (současný, Strapi)
```
Kindergarten 1 ───< N Course 1 ───< N Registration
Camp 1 ───< N CampRegistration
Article ───(cover) Media
```

### 3.9 Problémy současného modelu (co redesign řeší)
1. **Duplicita registrací:** `Registration` a `CampRegistration` jsou z 90 % identické
   (dítě, rodič, souhlasy, dres) → 2× stejná pole, 2× validace, 2× admin UI, 2× export.
2. **Nekonzistentní vazba na místo:** `Course` MUSÍ mít `Kindergarten`, ale pohybové kroužky
   (B2C) žádnou školku nemají. Model to neumí čistě vyjádřit → hack (fake školka / prázdná adresa).
3. **`Course` = jen kroužek:** ale Hopík reálně nabízí i **cvičení ve školách** (dopoledne/
   odpoledne) a **sportovní akce** — model to nezachycuje, jsou "nalepené" jako kurzy.
4. **`dayAndTime` jako datetime:** sémanticky špatně (potřeba jen weekday+čas).
5. **Kapacita/naplněnost:** počítá se stažením všech registrací klientem (osobní data + pomalé).
6. **Osobní údaje dítěte se opakují** v každé registraci — když je dítě na kroužku i kempu,
   RČ/adresa jsou 2×, bez vazby (nejde dělat retenci/cross-sell čistě).

---

## 3B. Datový model — CÍLOVÝ (nový, efektivní) ⭐

> **Toto je závazný model pro implementaci.** Principy návrhu:
> - **Jedna entita registrace** pro kroužky i kempy (polymorfní přes `programId`), ne duplicity.
> - **Jedna entita "program"** (`Program`) jako nadtyp pro vše, do čeho se lze přihlásit,
>   odlišené `type`. Volitelné pole podle typu.
> - **Denormalizovaný `spotsTaken`** čítač na programu (rychlá naplněnost bez skenu registrací).
> - **`Location`** jako samostatná entita (školka/sportoviště) — sdílená, nepovinná.
> - **`id`** = veřejný identifikátor (string, zachovává hodnotu dřívějšího `documentId`).
> - Vše má `createdAt`, `updatedAt`.

### 3B.1 `Program` — vše, do čeho se lze přihlásit
Nadtyp pro **pohybový kroužek**, **cvičení ve škole** (dopo/odpo) i **kemp/turnus**.
Sjednocuje dnešní `Course` + `Camp`.

| Pole | Typ | Povinné | Pozn. |
|------|-----|---------|-------|
| `id` | string (cuid/uuid) | ✔ | veřejný ID (URL, relace) |
| `type` | enum `club` \| `school` \| `camp` | ✔ | kroužek / cvičení ve škole / kemp |
| `name` | string | ✔ | název (např. "Pondělní kroužek Slovany", "HOPÍKEMP TK Škoda") |
| `slug` | string (unique) | ✘ | volitelně pro hezčí URL |
| `locationId` | FK → Location \| null | ✘ | kde se koná (nepovinné pro akce na míru) |
| `price` | int (Kč) | ✔ | cena |
| `capacity` | int \| null | ✘ | `null` = neomezeno |
| `spotsTaken` | int (default 0) | ✔ | **denormalizovaný čítač** aktivních registrací |
| `accessMode` | enum `public` \| `noticeOnly` \| `code` \| `unlisted` | ✔ | omezení přístupu — viz 3B.10 |
| `restrictionNote` | text \| null | ✘ | text pro rodiče (např. "Pouze pro děti z MŠ Chudenice") |
| `accessCode` | string \| null (**hash**) | ✘ | jen pro `accessMode=code` |
| `shirtPolicy` | enum `none` \| `optional` \| `required` | ✔ | kroužek=`optional`, kemp=`required` |
| `status` | enum `active` \| `hidden` \| `archived` | ✔ | řízení viditelnosti (bez publish workflow) |
| **Kroužek/škola (type=club\|school):** | | | |
| `weekday` | int 1–7 \| null | podm. | den v týdnu |
| `time` | string `"HH:MM"` \| null | podm. | čas lekce |
| `schoolPart` | enum `morning` \| `afternoon` \| null | ✘ | jen `type=school` (dopo/odpo) |
| **Kemp (type=camp):** | | | |
| `startDate` | date \| null | podm. | začátek turnusu |
| `endDate` | date \| null | podm. | konec turnusu |

> Pozn.: `type=event` (sportovní akce na míru) se **needituje jako program** — akce jsou
> poptávkové, ne self-service registrace. Do modelu je nedáváme (jsou to jen marketingové
> stránky). Pokud by v budoucnu měly mít registrace, přidá se `type=event`.

### 3B.2 `Location` — místo konání
Sdílená entita (školka, tělocvična, sportoviště). Nahrazuje `Kindergarten`, ale je obecnější.

| Pole | Typ | Povinné | Pozn. |
|------|-----|---------|-------|
| `id` | string | ✔ | |
| `name` | string | ✔ | "MŠ Chudenice", "TK Škoda Plzeň" |
| `kind` | enum `kindergarten` \| `school` \| `venue` | ✔ | typ místa |
| `address` | string | ✘ | |
| `note` | text | ✘ | interní poznámka |

### 3B.3 `Child` — dítě (deduplikace osobních údajů) *(volitelné, doporučené)*
Umožňuje retenci a cross-sell (dítě na kroužku i kempu = 1 záznam). Pokud se zdá overkill
pro MVP, lze vynechat a osobní data držet přímo na registraci (jako dnes) — ale **doporučeno**.

| Pole | Typ | Povinné | Pozn. |
|------|-----|---------|-------|
| `id` | string | ✔ | |
| `fullName` | string | ✔ | |
| `birthDate` | date | ✔ | |
| `personalId` | string (**šifrováno**) | ✔ | RČ, at-rest encryption |
| `address` | string | ✔ | |
| `healthInsurance` | string | ✔ | |
| `parentId` | FK → Parent | ✔ | |

### 3B.4 `Parent` — rodič/zákonný zástupce *(volitelné, s Child)*
| Pole | Typ | Povinné | Pozn. |
|------|-----|---------|-------|
| `id` | string | ✔ | |
| `name` | string | ✔ | |
| `phone` | string | ✔ | `+420…` |
| `email` | email | ✔ | |
| `secondName` | string | ✘ | druhý rodič |
| `secondPhone` | string | ✘ | |

### 3B.5 `Registration` — JEDNA entita pro vše
Sjednocuje `Registration` + `CampRegistration`. Odkazuje na `Program` (jakéhokoli typu).

| Pole | Typ | Povinné | Pozn. |
|------|-----|---------|-------|
| `id` | string | ✔ | |
| `programId` | FK → Program | ✔ | co si přihlašuje |
| `childId` | FK → Child | ✔ (varianta A) | nebo inline pole (varianta B) |
| **Inline varianta (bez Child entity):** | | | *(pokud 3B.3 vynecháš)* |
| `childName`,`birthDate`,`personalId`,`address`,`healthInsuranceText` | | podm. | osobní údaje dítěte |
| `className` | string | ✘ | jen kroužky (třída ve školce) |
| `parentName`,`parentPhone`,`parentEmail`,`secondParentName`,`secondParentPhone` | | podm. | rodič |
| **Společné:** | | | |
| `wantsShirt` | bool | ✔ | dle `program.shirtPolicy` |
| `shirtSize` | string \| null | podm. | povinné pokud shirt |
| `nickName` | string \| null | ✘ | jméno na dres |
| `allergies` | text \| null | ✘ | relevantní hlavně u kempu |
| `note` | text \| null | ✘ | |
| `consentPersonalData` | bool | ✔ | musí být true |
| `consentMedia` | bool | ✔ | |
| `paymentStatus` | enum `unpaid` \| `paid` \| `cancelled` | ✔ | default `unpaid` (viz 6A.3) |
| `priceSnapshot` | int (Kč) | ✔ | cena v okamžiku registrace (program + dres) — nemění se zpětně |
| `status` | enum `active` \| `cancelled` | ✔ | cancelled nesnižuje historii, ale `spotsTaken` |
| `source` | string \| null | ✘ | UTM/kanál (6A.2), volitelné |
| `createdAt` | datetime | ✔ | datum přihlášení |

> **Doporučení:** jít variantou A (`Child`+`Parent`) — čistší, umožní byznys featury (retence,
> cross-sell, GDPR retenci na jednom místě). Varianta B (inline) je rychlejší na MVP, ale
> zadělává na stejný problém jako dnes.

### 3B.6 `Article` — aktualita (beze změny logiky)
| Pole | Typ | Povinné | Pozn. |
|------|-----|---------|-------|
| `id` | string | ✔ | |
| `title` | string | ✔ | |
| `slug` | string unique | ✔ | |
| `excerpt` | text \| null | ✘ | |
| `content` | rich text/HTML | ✘ | |
| `coverId` | FK → Media \| null | ✘ | |
| `publishedAt` | datetime \| null | ✘ | `null` = koncept (jediná entita s draft/publish) |

### 3B.7 `Media` — upload
`id`, `url`, `alt?`, `width?`, `height?`, `variants?` (thumbnail/small/medium/large).

### 3B.7b `User` — člen týmu (auth + RBAC)
| Pole | Typ | Povinné | Pozn. |
|------|-----|---------|-------|
| `id` | string | ✔ | |
| `name` | string | ✔ | |
| `email` | email unique | ✔ | login |
| `passwordHash` | string \| null | ✘ | null dokud nepřijme pozvánku |
| `role` | enum `owner`\|`admin`\|`trainer`\|`accountant`\|`viewer` | ✔ | pevné role (7.2) |
| `status` | enum `invited`\|`active`\|`disabled` | ✔ | |
| `phone` | string \| null | ✘ | |
| `color` | string \| null | ✘ | odlišení v rozvrhu |
| `lastLoginAt` | datetime \| null | ✘ | |

> `Trainer` z 6A.8 = `User` s `role=trainer` (žádná zvláštní entita — sjednoceno).

### 3B.7c `Invitation` — pozvánka do týmu
| Pole | Typ | Pozn. |
|------|-----|-------|
| `id` | string | |
| `email` | email | koho zveme |
| `role` | enum (viz User) | přiřazená role |
| `token` | string (hash) | jednorázový odkaz |
| `invitedBy` | FK → User | kdo pozval |
| `expiresAt` | datetime | platnost |
| `acceptedAt` | datetime \| null | |

### 3B.7d `LessonInstance` — konkrétní výskyt lekce *(fáze 3, shift-signup + rozvrh)*
`Program` popisuje **opakující se** lekci (weekday+time+období). Pro rozvrh a hlášení
trenérů na konkrétní datum je potřeba instance:

| Pole | Typ | Pozn. |
|------|-----|-------|
| `id` | string | |
| `programId` | FK → Program | ze kterého programu |
| `date` | date | konkrétní den |
| `startTime` / `endTime` | string `"HH:MM"` | okno (z `time` + `durationMin`) |
| `trainersNeeded` | int | kolik trenérů slot potřebuje |
| `note` | text \| null | úkoly/pomůcky k lekci |

> Generují se z `Program` (weekday/time/validFrom/validTo) — buď dopředu, nebo lazy.
> MVP rozvrhu může běžet i jen nad `Program` bez instancí; instance jsou nutné až pro
> shift-signup na konkrétní datum a odchylky (změna času, zrušená lekce).

### 3B.7e `ShiftSignup` — přihlášení trenéra na lekci *(fáze 3)*
| Pole | Typ | Pozn. |
|------|-----|-------|
| `id` | string | |
| `lessonInstanceId` | FK → LessonInstance | na kterou lekci |
| `trainerId` | FK → User (role=trainer) | kdo se hlásí |
| `status` | enum `pending`\|`approved`\|`rejected`\|`cancelled` | dle režimu potvrzování (7.4) |
| `createdAt` | datetime | |
| unique | `(lessonInstanceId, trainerId)` | nelze se přihlásit 2× |

### 3B.7f `Document` — interní dokumenty *(fáze 3, 6A.8 B)*
`id`, `title`, `category` (enum), `fileId` (FK→Media) \| `content` (rich text),
`visibility` (`trainers`\|`admin`), `order`, `updatedAt`.

### 3B.7g `AuditLog` — kdo co udělal
`id`, `userId` (FK→User), `action`, `entity`, `entityId`, `at`, `meta` (JSON).
Zapisuje se u zápisových operací (create/update/delete) — dohledatelnost.

### 3B.8 Cílový diagram
```
Location 1 ──< N Program 1 ──< N Registration >── N:1 Child N:1 ── Parent
                    │                                  (varianta A)
                    └─ type: club | school | camp
                    └─ spotsTaken (čítač)
Article ──(cover) Media
```

### 3B.9 Klíčová pravidla integrity
- **`spotsTaken`** se inkrementuje/dekrementuje **transakčně** při vzniku/zrušení aktivní
  registrace. Naplněnost = `spotsTaken` (žádný sken registrací). Kontrola kapacity při zápisu.
- **`priceSnapshot`** zmrazí cenu — změna ceny programu neovlivní staré registrace (účetní správnost).
- **Měkké mazání** registrace (`status=cancelled`) — historie + audit zůstává, ale uvolní místo.
- **Šifrování `personalId`** at-rest (RČ je citlivý údaj).

### 3B.10 Omezení přístupu ke kurzu (eligibility) ⭐
Reálná potřeba: kurz v konkrétní školce má být určen **jen dětem z té školky** (ne veřejnosti).
Řešeno škálovatelně přes `Program.accessMode` — od měkkého po tvrdé:

| `accessMode` | Chování | Kontrola | Kdy použít |
|--------------|---------|----------|------------|
| `public` | běžný veřejný kurz | žádná | pohybové kroužky pro veřejnost |
| `noticeOnly` | ve výpisu + `restrictionNote` ("Pouze pro děti z MŠ X") | **žádná tvrdá** — jen informuje, kluci řeší ručně | **výchozí pro školkové kurzy (MVP)** |
| `code` | přihláška vyžaduje **kód/heslo** (školka rozdá rodičům) | server ověří `accessCode` (hash) při POST | když chtějí reálnou bariéru bez účtů |
| `unlisted` | **není** ve veřejném výpisu; dostupný jen přes přímý odkaz | skrytí z `/api/programs` (jen přímý `/:id`) | školka rozešle unikátní link rodičům |

**Pravidla:**
- Web ve výpisu (`GET /api/programs`) **nevrací** programy s `accessMode=unlisted`.
- U `noticeOnly` se `restrictionNote` zobrazí na kartě kurzu i na detailu/registraci (info).
- U `code`: POST registrace bez správného kódu → `403 { error: { code: "INVALID_ACCESS_CODE" } }`.
  Kód se ukládá **hashovaný**, porovnává se server-side. Formulář má pole "Přístupový kód".
- **Neřeší identitu dítěte** — ani `code`/`unlisted` neověřují, že dítě fakticky chodí do školky
  (to by vyžadovalo seznam dětí — viz níže, mimo scope).
- 🔴 **Budoucí tvrdá varianta (`roster`):** ověření proti nahranému seznamu dětí školky
  (jméno/RČ). Silné, ale klade nároky na správce (nahrát a udržovat seznamy) + GDPR (RČ školky
  u nás). **Zatím nedoporučeno**, ponecháno jako fáze 3 pokud vznikne reálná potřeba.

> **MVP doporučení:** implementovat `public` + `noticeOnly` + `unlisted` (levné, pokrývají
> 90 % potřeb). `code` přidat, pokud kluci chtějí bariéru. `roster` až dle poptávky.

---

## 3C. Migrace ze starého modelu na nový
| Staré (Strapi) | Nové (3B) |
|----------------|-----------|
| `Kindergarten` | `Location` (`kind=kindergarten`) |
| `Course` | `Program` (`type=club` nebo `school`), `weekday`/`time` z `dayAndTime` |
| `Camp` | `Program` (`type=camp`), `startDate`/`endDate` |
| `Registration` | `Registration` (`programId` z `course`), `Child`+`Parent` extrahovat |
| `CampRegistration` | `Registration` (`programId` z `camp`) |
| `Article` | `Article` (beze změny) |
- **Zachovat staré `documentId` → nové `Program.id`/`Location.id`** (kvůli existujícím URL
  `/prihlaseni/[courseId]`, `/prihlaseni/kemp/[campId]` a redirectům).
- `spotsTaken` dopočítat jednorázově z počtu aktivních registrací.
- `paymentStatus` starých registrací = `unpaid` (nebo `paid`, dle domluvy s kluky).

---

## 4. Business pravidla

1. **Kapacita:** `isFull = spotsTaken >= capacity`, `spotsLeft = capacity - spotsTaken`.
   `capacity = null` → neomezeno (skryj počítadlo). Bez čekací listiny (MVP; waitlist fáze 2).
2. **`spotsTaken`** se mění **transakčně** při vzniku (`+1`) / zrušení (`-1`) aktivní registrace.
   Zápis registrace **ověří kapacitu v téže transakci** (žádný overbooking při souběhu).
3. **Živá naplněnost:** web čte `spotsTaken`/`capacity` přímo z programu (lehký re-fetch),
   ne stahování registrací.
4. **Dres dle `program.shirtPolicy`:** `optional` (kroužek, +500 Kč), `required` (kemp),
   `none`. Pokud shirt objednán → `shirtSize` povinné.
5. **Cena:** `priceSnapshot = program.price + (wantsShirt ? 500 : 0)` se **zmrazí** na registraci.
   Pozdější změna ceny programu neovlivní existující registrace. Fakturace fáze 2.
6. **Validace registrace** (server, zrcadlí klienta):
   - `personalId` regex `^\d{6}/\d{4}$` (ukládá se **šifrovaně**)
   - `parentPhone` po odstranění mezer `^\+420[0-9]{9}$`
   - `parentEmail` validní e-mail
   - `birthDate` ≤ dnes
   - `consentPersonalData === true` (jinak odmítnout)
   - shirt povinnost dle `shirtPolicy`
   - kapacita: odmítni pokud `isFull`
7. **Publikace:** **jen `Article`** má draft/publish (`publishedAt = null` = koncept).
   `Program` má `status` (`active`/`hidden`/`archived`) — web zobrazuje jen `active`.
8. **Sitemap:** `GET /api/articles/sitemap` vrací všechny publikované články (slug + publishedAt).
9. **Zrušení registrace:** `status=cancelled` (měkké) → dekrementuje `spotsTaken`, zachová audit.
10. **Omezení přístupu (`accessMode`, viz 3B.10):** `unlisted` se nevrací ve výpisu programů;
    `noticeOnly` zobrazí `restrictionNote` (bez kontroly); `code` vyžaduje platný kód při POST
    (jinak `403`). Nová app validuje server-side.

---

## 5. API kontrakt

> **✅ ROZHODNUTO (varianta B): Vlastní čisté API.**
> Nové CMS **nenapodobuje** Strapi. Navrhne se čistý, srozumitelný REST kontrakt (viz níže).
> **Součástí dodávky je PR do webového repa**, který přepíše všech ~8 fetch míst (sekce 5.7)
> na nové API. Nedědíme Strapi bizarnosti (`?populate=`, `?filters[x][$eq]=`, obálka
> `data`/`meta`, `documentId` v query).
>
> **Migrační strategie (snížení rizika):**
> - `documentId` se v DB **zachovává** (kvůli existujícím URL a migraci dat), ale v novém
>   API je to prostě `id` (veřejný identifikátor v cestě). Staré `documentId` = nové `id`.
> - Přechod probíhá **mimo náborovou špičku** (sekce 17), na staging se ověří celý web,
>   pak ostrý cutover.
> - Nové (dashboard, fakturace, admin) staví rovnou na čistém API.

### 5.1 Konvence
- **Bez obálky** `data`/`meta` u jednotlivce. Kolekce vrací `{ items: T[], total, page, pageSize }`.
- Identifikátor v cestě = veřejné `id` (= dřívější `documentId`). Slug u článků.
- Relace: přes `?include=location` apod., konzistentně a dokumentovaně.
- Chyby: `{ error: { code, message } }`, standardní HTTP statusy.
- Filtrování/řazení: čitelné query (`?type=club`, `?sort=-publishedAt`, `?location=<id>`),
  ne Strapi `[$eq]` syntaxe.
- **Naplněnost je součástí objektu `Program`** (`capacity`, `spotsTaken`) — žádný zvláštní
  count endpoint, žádné stahování registrací. Web čte přímo z programu.

### 5.2 Veřejné čtecí endpointy (read-only)
| Metoda | Cesta | Query | Použití na webu |
|--------|-------|-------|-----------------|
| GET | `/api/programs` | `?type=club&status=active&include=location` | výpis kroužků na `/prihlaseni` (seskup dle location na webu) |
| GET | `/api/programs` | `?type=camp&status=active` | výpis kempů na `/prihlaseni` |
| GET | `/api/programs/:id` | `?include=location` | detail `/prihlaseni/[courseId]`, `/prihlaseni/kemp/[campId]` |
| GET | `/api/locations` | `?include=programs` | (volitelné) školky + jejich programy pohromadě |
| GET | `/api/articles` | `?sort=-publishedAt` (jen publikované) | `/aktuality` |
| GET | `/api/articles/:slug` | — | `/aktuality/[slug]` |
| GET | `/api/articles/sitemap` | — | seznam `{slug, publishedAt}` pro sitemap |

> Každý `Program` v odpovědi nese `capacity` + `spotsTaken` → web spočítá `spotsLeft`
> a `isFull` lokálně. **Žádná osobní data se veřejně nevystavují.**

### 5.2b Registrace — čtení (jen admin)
| Metoda | Cesta | Auth | Použití |
|--------|-------|------|---------|
| GET | `/admin/api/registrations` | admin | plná data (filtr `?program=`, `?type=`, `?paymentStatus=`, fulltext) |
| GET | `/admin/api/registrations/:id` | admin | detail |

⚠ **Osobní data registrací (vč. RČ) se veřejně NEVYSTAVUJÍ.** Naplněnost je na `Program`
(`spotsTaken`) → web nikdy nestahuje registrace. Odstraňuje bezpečnostní dluh (sekce 9).

### 5.3 Veřejný zápisový endpoint (registrace z webu) — JEDEN
| Metoda | Cesta | Tělo |
|--------|-------|------|
| POST | `/api/registrations` | `{ programId: "<id>", ...pole registrace (3B.5) }` |

- **Jeden endpoint** pro kroužek i kemp (dřív dva) — liší se jen `programId` a validací dle
  `program.type` / `program.shirtPolicy`.
- Server **musí validovat** dle sekce 4 a při chybě vrátit `{ error: { message } }`.
- **Atomicky** zkontroluje kapacitu, vytvoří registraci, inkrementuje `spotsTaken` (transakce).
- **Rate-limiting / anti-spam** povinné.
- **Access code:** pokud `program.accessMode=code`, tělo musí obsahovat `accessCode`;
  server ověří proti hashi, jinak `403 INVALID_ACCESS_CODE`.
- ⚠ PR do webu: `RegistrationForm.tsx` / `CampRegistrationForm.tsx` sjednotit payload
  (plochý, `programId`), zrušit Strapi obálku `{ data: {...} }`; přidat pole "Přístupový kód"
  když je vyžadován.

### 5.4 Formát času lekce
Program `type=club|school` má `weekday` (1–7) + `time` (`"HH:MM"`) místo ISO datetime.
**PR do webu** přepíše `formatCourseDateTime` v `src/lib/utils.ts` na nový tvar.

### 5.5 Naplněnost
`Program.spotsTaken` + `capacity` jsou přímo v odpovědi. Web počítá `spotsLeft`/`isFull`
lokálně, polling jen znovu-načte program (lehké). **Žádný sken registrací, žádná osobní data.**

### 5.6 Admin endpointy (autentizované)
Plný CRUD:
```
GET/POST/PUT/DELETE  /admin/api/{programs|locations|registrations|articles|children|parents}
POST /admin/api/media                       (upload obrázku)
GET  /admin/api/registrations/export?program=<id>&type=<club|camp>   (CSV/XLSX)
GET  /admin/api/programs/:id/roster.pdf     (docházkový list — fáze 1, sekce 6A.3)
GET  /admin/api/dashboard/stats             (metriky — fáze 1, sekce 6A.1)
POST /admin/auth/login
POST /admin/auth/logout
```

### 5.7 Frontend fetch místa (PR do webu — varianta B)
- `src/app/prihlaseni/page.tsx` → `GET /api/programs?type=club` + `?type=camp` (místo kindergartens/courses/camps)
- `src/app/prihlaseni/[courseId]/page.tsx` + `CourseRegistrationClient.tsx` → `GET /api/programs/:id`
- `src/app/prihlaseni/kemp/[campId]/page.tsx` + `CampRegistrationClient.tsx` → `GET /api/programs/:id`
- `src/components/CourseCard.tsx`, `src/components/CampCard.tsx` → naplněnost z programu (bez pollingu registrací)
- `src/components/RegistrationForm.tsx`, `src/components/CampRegistrationForm.tsx` → `POST /api/registrations` (sjednocený payload)
- `src/lib/articles.ts` → `GET /api/articles`, `/api/articles/:slug` (bez `?populate`, bez media prefix hacku — API vrací plné URL)
- `src/app/sitemap.ts` → `GET /api/articles/sitemap`
- `src/lib/types.ts` → nové typy (`Program`, `Registration`, `Location`, `Article`) místo Strapi typů

---

## 6. Admin frontend — UX požadavky

> **Guiding principle:** uživatel netechnik. Každá obrazovka = 1 jasný úkol. Čeština,
> žádné `id`/`slug`/JSON na očích (skryj do detailu). Velká tlačítka, potvrzení u mazání,
> prázdné stavy s návodem. **Navigace se skládá dle role** (trenér vidí méně než owner — RBAC 7.2).

### 6.1 Přihlášení
- Login e-mail + heslo, "zůstat přihlášen", reset hesla, přijetí pozvánky (nastavení hesla).

### 6.2 Dashboard (úvod)
- Karty: **Nové registrace (dnes/týden)**, počet aktivních programů, blížící se plné/nedoplněné.
- Rychlé akce dle role: "Přidat aktualitu", "Přidat program", "Exportovat registrace".
- Trenér vidí místo toho **"Moje hodiny"** (fáze 3).

### 6.3 Registrace (nejdůležitější sekce, role admin/owner)
- **Filtr podle programu** (typ kroužek/kemp/škola), fulltext (jméno dítěte/rodiče), řazení dle data.
- Tabulka: dítě, rodič, kontakt, program, dres+velikost, datum přihlášení, souhlasy (ikony),
  **stav platby** (fáze 1).
- **Detail** registrace = všechna pole čitelně (vč. RČ, adresy, alergií, poznámky).
- **Export CSV/Excel** aktuálního filtru (kompletní pole — pro fakturaci a docházku).
- Indikátor **naplněnosti** u programu (X/Y z `spotsTaken`/`capacity`, barevně když plno).
- Trenér: vidí jen registrace u **svých** přiřazených programů (scoped, 7.5).

### 6.4 Programy (kurzy, cvičení ve školách, kempy)
Jednotná sekce nad entitou `Program` (sekce 3B.1), odlišená `type`:
- Seznam programů s filtrem podle typu (`club`/`school`/`camp`) a stavu (`active`/`hidden`/`archived`).
- Formulář dle typu:
  - **kroužek/škola:** název, **den v týdnu + čas** (ne raw datetime!), délka lekce, cena, kapacita,
    místo (`Location`), u školy dopo/odpo (`schoolPart`).
  - **kemp:** název, termín od–do (date pickery), cena, kapacita (prázdné = neomezeno), místo.
- **Omezení přístupu** (`accessMode` + `restrictionNote`, sekce 3B.10): přepínač
  veřejný / jen upozornění / kód / nelistovaný + text pro rodiče.
- U programu: počet registrací / kapacita.

### 6.5 Místa (`Location`)
- CRUD míst konání (školky, tělocvičny, sportoviště): název, typ (`kind`), adresa, poznámka.
- Sdílené — jedno místo lze přiřadit více programům.

### 6.6 Aktuality
- Seznam s náhledem coveru, stavem (Publikováno / Koncept), datem.
- Editor: titulek, **slug auto z titulku** (editovatelný, kontrola unikátnosti),
  perex, **rich-text editor** pro obsah (výstup HTML kompatibilní se současným renderem),
  upload cover obrázku (+ alt text), datum publikace, přepínač Publikovat/Koncept.
- Náhled odkazu `hopik4kids.cz/aktuality/<slug>`.
- **Jediná entita s draft/publish** (programy mají jen `status`).

### 6.7 Tým & role (owner/admin)
- Seznam členů: jméno, e-mail, role, stav (`pozván`/`aktivní`/`deaktivován`).
- **Pozvat člena** (e-mail + role) → odešle pozvánku. Změna role, deaktivace, znovu-pozvat.
- Owner: správa ownerů (nelze smazat posledního). Audit log (kdo co dělal).

### 6.8 Obecné UX
- Potvrzení uložení, validační hlášky česky. Mazání = modal s potvrzením.
- Responsivní (mobil). Prázdné stavy: "Zatím žádné registrace — až se někdo přihlásí, objeví se tady."

---

## 6A. Byznysové rozšíření — proč to postavit "navíc"

> Motivace: CMS nemá být jen "editor obsahu", ale **provozní nástroj, který Hopíku šetří
> čas, snižuje chyby a pomáhá vydělat**. Níže seřazeno podle poměru hodnota/úsilí.
> Značení priority: 🟢 rychlá výhra • 🟡 střední • 🔴 větší projekt.

### 6A.1 Provozní / finanční přehledy (dashboard metriky)
Data už v systému jsou — jen je zobrazit. Nulové provozní náklady, okamžitá hodnota.

- 🟢 **Obsazenost & tržby živě:** za každý kurz/kemp `naplněno X/Y`, `% obsazenosti`,
  **potvrzené tržby** (`Σ registrací × cena` + dresy). Součet za sezónu = kolik už "vydělali".
- 🟢 **Predikce tržeb:** `zbývající kapacita × cena` = maximální možná tržba → kolik ještě
  můžou nabrat. Motivuje k marketingu tam, kde je volno.
- 🟢 **Nedoplněné kurzy (alert):** kurzy s obsazeností < prahu (např. 40 %) X dní před startem
  → "tyhle je potřeba dopropagovat, nebo zrušit". Přímo brání ztrátovým kurzům.
- 🟡 **Trend registrací v čase:** graf registrací po dnech/týdnech → vidí špičky (po newsletteru,
  po akci) a kdy nábor stagnuje.
- 🟡 **Dres attach-rate:** % registrací s objednaným dresem → upsell metrika (dres = +500 Kč čistý
  doplňkový příjem). Když klesá, je prostor to líp nabízet.

### 6A.2 Marketing & růst
- 🟡 **Zdroj registrace (UTM / "odkud jste se dozvěděli"):** volitelné pole v přihlášce nebo
  UTM z URL → které kanály (Instagram, škola, doporučení) reálně přivádějí děti.
  Byznysově nejcennější: kam dávat čas/peníze.
- 🟡 **Opakovaní zákazníci (retence):** match dětí podle `personalId`/jména napříč sezónami/kempy
  → kolik % se vrací. Retence = levnější než akvizice; klíčová KPI Hopíka.
- 🟡 **Cross-sell "kroužek → kemp":** seznam dětí z kroužků, které ještě nejsou na kempu
  (a naopak) → cílená nabídka existující bázi (nejlevnější prodej vůbec).
- 🟢 **Kapacitní badge na webu ("Poslední 3 místa!"):** už teď se počítá naplněnost →
  scarcity trigger zvyšuje konverzi přihlášek.
- 🔴 **Waitlist / čekací listina:** když je plno, sbírej zájemce → automaticky nabídni místo
  při uvolnění. Zachytí poptávku, která by jinak utekla ke konkurenci.

### 6A.3 Úspora času správců (nejcennější — čas trenérů)
- 🟢 **Docházkový/kontaktní list (PDF/tisk) na program:** jedním klikem seznam dětí + kontakty
  + alergie + souhlasy → berou s sebou na kemp/lekci. Dnes ruční skládání z e-mailů.
- 🟢 **Rychlý přehled alergií/zdravotních omezení** per program (bezpečnost + zákonná
  odpovědnost, hlavně kempy).
- 🟡 **Stav platby u registrace:** příznak `nezaplaceno / zaplaceno / storno`
  → přehled kdo dluží, filtr "čeká na platbu". Reálně vymáhá peníze.
- 🟡 **Hromadný e-mail účastníkům programu:** "info před startem", změna termínu →
  segmentovaně na rodiče daného programu, bez ručního kopírování adres.

### 6A.4 ~~Škola jako obchodní kanál (B2B lead pipeline)~~ — OUT-OF-SCOPE
> Rozhodnuto (14.3): poptávky škol se **neevidují**, řeší trenéři osobní domluvou; web školy
> nepřihlašuje. Ponecháno jen jako budoucí možnost, pokud vznikne potřeba B2B pipeline.

### 6A.5 Fakturace (generování faktur)
Dnes: po registraci se rodiči ručně posílá faktura e-mailem. Automatizace = velká úspora
času + profesionálnější dojem + přehled o platbách. Data pro fakturu **už existují**
(cena kurzu/kempu, dres +500 Kč, jméno+adresa plátce, e-mail).

**Co fakturovat:** registrace do kurzu/kempu. Plátce = rodič (jméno, adresa, e-mail z registrace).
Položky = kurz/kemp (název + cena) + volitelně dres (500 Kč). Bez DPH, pokud Hopík není
plátce DPH (ověřit — mění to vzhled faktury a náležitosti).

- 🟡 **Auto-generování PDF faktury** při vzniku registrace (nebo tlačítkem v adminu).
  Náležitosti: číslo faktury (řada dle roku, sekvenční), datum vystavení + splatnosti,
  dodavatel (Hopík4Kids — IČO, adresa, č. účtu), odběratel (rodič), položky, celková částka,
  **variabilní symbol** (= číslo faktury) a **QR platba (SPD formát)** pro rychlé zaplacení.
- 🟢 **Napojení na stav platby (6A.3):** faktura vytvořena → `nezaplaceno`; po spárování →
  `zaplaceno`. Přehled neuhrazených = kdo dluží.
- 🟡 **Rozeslání faktury e-mailem** rodiči (příloha PDF) — navazuje na stávající e-mail flow.
- 🟡 **QR kód platby** přímo ve faktuře i v potvrzovacím e-mailu → rodič zaplatí na 2 kliky,
  vyšší a rychlejší platební morálka.
- 🔴 **Automatické párování plateb** (import bankovního výpisu / API banky přes VS) →
  stav `zaplaceno` bez ruční práce. Větší projekt, fáze 2+.
- 🔴 **Upomínky nezaplacených** (X dní po splatnosti auto e-mail). Fáze 2.

**Nová entita `Invoice` (faktura):**

| Pole | Typ | Pozn. |
|------|-----|-------|
| `id` / `documentId` | | |
| `invoiceNumber` | string (unique) | řada dle roku, sekvenční (např. `2026-0042`) = VS |
| `registration` | relation → Registration \| CampRegistration | zdroj |
| `type` | enum `course` \| `camp` | |
| `payerName` | string | z registrace (rodič) |
| `payerAddress` | string | |
| `payerEmail` | email | |
| `items` | JSON / relation | `[{label, qty, unitPrice}]` — kurz/kemp + dres |
| `totalAmount` | number (Kč) | součet |
| `issueDate` | date | vystaveno |
| `dueDate` | date | splatnost (např. +14 dní) |
| `variableSymbol` | string | = invoiceNumber |
| `status` | enum `nezaplaceno` \| `zaplaceno` \| `storno` | |
| `paidAt` | datetime \| null | kdy spárováno |
| `pdfUrl` | string \| null | vygenerované PDF (úložiště) |

**Nastavení dodavatele (jednorázově v adminu):** název, IČO, DIČ (pokud plátce DPH),
adresa, číslo účtu (IBAN pro QR), logo, výchozí splatnost, text patičky.

**Tech:** generování PDF server-side (`@react-pdf/renderer`, `pdfmake` nebo HTML→PDF přes
Puppeteer). QR platba: knihovna pro **SPAYD/SPD** (`Short Payment Descriptor`, český
QR platba standard). Číslování faktur **atomicky** (transakce/DB sekvence — nesmí vzniknout
duplicitní číslo).

> ⚠ **Právní ověření:** náležitosti daňového dokladu vs. neplátce DPH; číselné řady faktur;
> archivace 10 let (zákon). Doporučeno konzultovat s účetní Hopíka před nasazením.

### 6A.6 GDPR & compliance jako feature
- 🟢 **Přehled souhlasů:** kdo dal/nedal media souhlas → bezpečné publikování fotek
  (jinak riziko pokuty). Filtr "děti bez media souhlasu".
- 🟡 **Retence osobních dat:** po skončení sezóny označit/anonymizovat staré registrace
  (RČ nemá ležet věčně) — snižuje GDPR riziko.

### 6A.7 Doporučené MVP z tohoto seznamu
Postavit hned (data existují, minimum práce, maximum hodnoty):
1. Dashboard: obsazenost + potvrzené tržby + predikce + alert nedoplněných kurzů (6A.1).
2. Docházkový/kontaktní list per turnus s alergiemi + souhlasy (6A.3).
3. Příznak stavu platby + filtr (6A.3).

Fakturace = fáze 2. Zbytek (waitlist, retence, UTM, hromadné e-maily, auto-párování plateb,
upomínky, rozvrh, dokumenty) dle priorit dále.

### 6A.8 Rozvrh trenérů & interní dokumenty (BUDOUCNOST — ne v MVP) 📅
> Nástroj pro **trenéry**, ne pro rodiče. Na začátku být nemusí; přidat později.

**A) Rozvrh / kalendář lekcí (school-timetable styl)**
Přehledná tabulka nadcházejících lekcí — jako školní rozvrh: den + časové okno + místo,
platné jen v určitém období (např. říjen–listopad).

- Zdroj dat: `Program` (`type=club|school`) už nese `weekday` + `time` + `location`.
  Chybí jen **platnost v čase** a **délka lekce** → rozšířit `Program` o:
  - `validFrom` / `validTo` (date, null) — období konání (např. 1. 10.–30. 11.)
  - `durationMin` (int) — délka lekce v minutách (pro okno `16:00–16:45`)
- **Zobrazení:** týdenní mřížka (Po–Ne × čas) nebo seznam "co mě čeká tento týden".
  Řádek: `Pondělí 16:00–16:45 · Heřmanka (MŠ Heřmanova Huť)`.
- **Filtr podle trenéra:** vyžaduje přiřazení lekce trenérovi → nová vazba
  `Program.trainerId` (FK → Trainer/User) nebo M:N pokud lekci vede více trenérů.
  Pak "můj rozvrh" = lekce přihlášeného trenéra.
- **Úkoly/poznámky k lekci:** volitelné pole `Program.trainerNote` (text) — co si připravit,
  pomůcky, specifika skupiny. Zobrazí se trenérovi u dané lekce v rozvrhu.
- **Nová entita `Trainer`** (nebo rozšíření admin `User`): `name`, `phone`, `email`, `color`
  (pro odlišení v rozvrhu). Trenér = přihlášený uživatel s omezenými právy (vidí svůj rozvrh
  + dokumenty, needituje obsah).
- 🔴 Rozsah: střední. Hodnota: velká pro provoz (trenéři vědí kde/kdy/co), ale ne nutné pro
  spuštění náboru.

**B) Interní dokumenty & příručky ("do kapsy")**
Úložiště dokumentů pro trenéry — Hopíkovská pravidla, metodická příručka, checklisty, formuláře.

- **Nová entita `Document`:** `title`, `category` (enum: `pravidla` \| `metodika` \|
  `checklist` \| `formular` \| `ostatni`), `file` (FK → Media / upload PDF/obrázek),
  `content` (rich text — pro dokumenty psané přímo v systému, ne jen přílohy),
  `visibility` (enum `trainers` \| `admin`), `order` (řazení), `updatedAt`.
- **Admin UI:** nahrát PDF / napsat dokument, zařadit do kategorie.
- **Trenér UI:** přehledný seznam po kategoriích, otevřít/stáhnout na mobilu ("do kapsy").
  Vyhledávání. Vždy nejnovější verze (verzování volitelné).
- Napojení na rozvrh: u lekce může být odkaz na relevantní dokument (např. "pravidla kempu").
- 🟢 Rozsah: malý (upload + seznam). Hodnota: sjednotí know-how, noví trenéři se rychle zorientují.

**Zařazení:** fáze 3 (po stabilním CMS + provozních featurách). `Document` (B) je levné a lze
předsadit před rozvrh (A), pokud kluci chtějí nejdřív sdílet příručky.

---

## 7. Autentizace, uživatelé & RBAC ⭐

> **Změna oproti původnímu předpokladu:** systém **není** pro 2 lidi natrvalo. Matěj a Petr
> jsou **founders**, ale **nabírají další trenéry** → od začátku počítat s **více uživateli,
> rolemi a správou týmu**. RBAC a user management jsou **součást základu (fáze 0)**, ne dodatek.

### 7.1 Autentizace
- Login e-mail + heslo (hash: argon2/bcrypt). Session cookie (httpOnly) nebo JWT.
- **Onboarding = pozvánka e-mailem:** admin zadá e-mail + roli → systém pošle pozvánku
  s jednorázovým odkazem → nový člen si **sám nastaví heslo**. (Bezpečnější než ruční hesla.)
- Reset hesla e-mailem, "zůstat přihlášen", odhlášení.
- (Budoucnost) 2FA pro admin role.

### 7.2 Role (pevné, pojmenované)
| Role | Popis | Klíčová práva |
|------|-------|---------------|
| `owner` (founder) | Matěj, Petr | **vše** vč. správy uživatelů, rolí, nastavení, fakturace, mazání |
| `admin` | důvěryhodný správce | vše kromě správy vlastníků/kritického nastavení (dle konfigurace) |
| `trainer` | trenér | **svůj rozvrh**, hlášení na hodiny (7.4), interní dokumenty, čtení přiřazených lekcí; **nevidí** citlivá data (RČ) mimo své lekce, needituje obsah/ceny |
| `accountant` *(volitelné, fáze 2)* | účetní | jen faktury, platby, exporty; nevidí obsah webu |
| `viewer` *(volitelné)* | read-only | čtení dashboardu/registrací bez editace |

- **Owner** je speciální admin, který jediný může měnit role ostatních a odebírat/přidávat
  ownery. Vždy musí existovat ≥ 1 owner (nelze smazat posledního).
- Role se dají v čase rozšiřovat (modulární — viz sekce 12A). Start: `owner`, `admin`, `trainer`.

### 7.3 Správa uživatelů (admin UI)
- Seznam členů týmu: jméno, e-mail, role, stav (`pozván` / `aktivní` / `deaktivován`).
- Akce: **pozvat** (e-mail + role), změnit roli, **deaktivovat** (ne smazat — audit),
  znovu odeslat pozvánku.
- Owner/admin vidí, kdo co naposledy dělal (audit log — kdo vytvořil/upravil registraci,
  program, fakturu…).

### 7.4 Hlášení trenérů na hodiny (shift-signup) 📅
Trenéři se **sami hlásí** na lekce/směny (jako směny v práci). Provazuje se s rozvrhem (6A.8 A).

- Admin vytvoří **lekci/směnu** (`Program` s termínem, nebo samostatná instance lekce —
  viz `LessonInstance` níže) s potřebným počtem trenérů.
- Trenér vidí **volné hodiny** a **přihlásí se** → vznikne `ShiftSignup`.
- **Režim potvrzování** (konfigurovatelné): buď (a) přihlášení je rovnou závazné, nebo
  (b) admin schvaluje (`pending → approved/rejected`).
- Trenér vidí "moje hodiny" (přihlášené) + "volné hodiny" (může se přihlásit).
- Konflikty (dva na stejný slot / překryv v čase) systém hlídá.
- Notifikace: trenérovi potvrzení, adminovi info o novém hlášení.

### 7.5 API autorizace
- **Veřejné čtecí API** (`/api/*`): kurzy/kempy/články — bez auth (data nejsou citlivá).
  Naplněnost z `Program.spotsTaken` (žádná osobní data).
- **`/admin/api/*`**: vyžaduje session + kontrolu role na každém endpointu (middleware).
- Registrace s osobními údaji (RČ) — jen `admin`/`owner`; `trainer` vidí osobní data
  **pouze u dětí ve svých přiřazených lekcích** (scoped access), ne globálně.
- Standardně `403` při nedostatečné roli, `401` bez přihlášení.

---

## 8. E-maily
- Dnes: Next.js API routes (`/api/send-registration-email`, `/api/camp-registration-email`,
  `/api/school-registration`) přes Nodemailer + WEDOS SMTP. Posílá potvrzení rodiči i správci.
- **Ponechat na webu** (rozhodnuto, sekce 14.4). CMS jen ukládá data.
- **CMS ale posílá vlastní systémové e-maily:** pozvánky uživatelů, reset hesla, notifikace
  shift-signup → CMS potřebuje vlastní e-mail transport (stejné SMTP / transakční služba).
- School registration poptávka: out-of-scope (rozhodnuto, sekce 14.3).

---

## 9. Bezpečnostní dluh k nápravě
1. **`NEXT_PUBLIC_STRAPI_API_TOKEN` v prohlížeči** → kdokoli může číst všechny registrace
   (osobní údaje, rodná čísla). **Nutné odstranit.** Řešení: veřejné jen agregační počty;
   zápis registrací přes serverovou route s ochranou; plná data jen admin.
2. Chybí **rate-limiting** na POST registrací (spam/abuse).
3. RČ (`personalId`) je citlivý údaj → šifrovat at-rest, logovat přístup, minimalizovat expozici.

---

## 10. Migrace dat
- Export ze Strapi (REST nebo DB dump) → transformace na cílový model (mapování v **sekci 3C**)
  → import do nové DB.
- **Zachovat staré `documentId`** jako nové `Program.id` / `Location.id` / `Article` slug
  (existující URL a redirecty na to spoléhají).
- Zachovat relace dle 3C; extrahovat `Child`/`Parent` z registrací (varianta A).
- Dopočítat `spotsTaken` z aktivních registrací. Přenést media soubory (cover obrázky).
- **Ověřit počty** záznamů před/po migraci.

---

## 11. Nefunkční požadavky
- **Provoz:** jednoduchý deploy (1 app + DB + úložiště obrázků). Levné hostování.
- **Výkon:** čtecí endpointy < 200 ms; naplněnost z `spotsTaken` (index na FK), bez skenů.
- **Web cache:** web čte s ISR (60 s) — API musí zvládat opakované GET.
- **Zálohy:** denní záloha DB + media.
- **Audit:** `createdAt`/`updatedAt` u všech entit + `AuditLog` u zápisových operací (3B.7g).
- **Bezpečnost:** RBAC vynucen na API, RČ šifrováno at-rest, rate-limit na veřejný POST.

---

## 12. Technologická doporučení (nezávazná)
Cíl = lehké, udržovatelné, **rozšiřitelné**, přátelské k netechnikům i k jednomu vývojáři:
- **Backend + admin v jednom:** **Next.js (App Router) + Prisma + Postgres**, admin UI vlastní
  (React + Tailwind, konzistentní s webem). Vlastní řešení dává plnou kontrolu nad RBAC,
  moduly a shift-signup logikou.
  - Alternativa **Payload CMS** je rychlejší start, ale RBAC/shift-signup/rozvrh jsou custom
    logika, kterou stejně budeš psát → u složitějšího produktu se vlastní stack vyplatí víc.
- **DB:** PostgreSQL. **Media:** S3-kompatibilní úložiště nebo lokální disk + CDN.
- **Auth:** Auth.js / Lucia + vlastní RBAC middleware (role check na endpointech).
- **Export:** server-side CSV/XLSX (`exceljs`). **PDF:** `@react-pdf/renderer` / Puppeteer.
- **RBAC:** centrální `can(user, action, resource)` helper + middleware; nikdy nespoléhat
  jen na skrytí v UI — vždy kontrolovat na API.

---

## 12A. Modulární architektura (pro rozšiřitelnost) ⭐

> Systém **poroste** (rozvrh, shift-signup, fakturace, dokumenty, další nápady). Aby to
> nebyla "koule bláta", navrhnout ho **modulárně** od začátku — každý modul samostatný,
> s jasným rozhraním, zapínatelný/rozšiřitelný bez zásahu do jádra.

### 12A.1 Vrstvy
```
┌─────────────────────────────────────────────┐
│ Admin UI (React) — po modulech, dle role     │
├─────────────────────────────────────────────┤
│ API vrstva (/api veřejné, /admin/api RBAC)   │
├─────────────────────────────────────────────┤
│ Moduly (doménová logika, samostatné)         │
│  core · registrations · scheduling ·         │
│  billing · documents · users&rbac            │
├─────────────────────────────────────────────┤
│ Sdílené jádro: DB (Prisma), auth, RBAC,      │
│  audit, e-mail, media, eventy                │
└─────────────────────────────────────────────┘
```

### 12A.2 Moduly (hranice = doména)
| Modul | Obsah | Fáze |
|-------|-------|------|
| **core** | Location, Program, Article, Media, dashboard | 0 |
| **users & rbac** | User, Invitation, role, auth, audit log | 0 |
| **registrations** | Registration, Child, Parent, kapacita, export, stav platby | 0/1 |
| **scheduling** | LessonInstance, ShiftSignup, rozvrh, přiřazení trenérů | 3 |
| **billing** | Invoice, QR platba, párování plateb | 2 |
| **documents** | Document, kategorie, sdílení trenérům | 3 |

- Každý modul: vlastní složka (`modules/<name>/`) s **schématem, service vrstvou, API routami,
  UI**. Závislosti jen na sdíleném jádru, ne mezi moduly napřímo (přes eventy/rozhraní).
- **Feature flags:** modul lze mít vypnutý (nezobrazí se v UI, API neaktivní) → nasazovat po fázích.

### 12A.3 Principy pro rozšiřitelnost
- **RBAC deklarativně:** každý endpoint/akce má definované, které role smí → přidání role
  nebo modulu = přidání pravidel, ne přepis.
- **Eventy/hooky:** doménové události (`registration.created`, `invoice.paid`,
  `shift.approved`) → moduly a e-maily na ně reagují, volně provázané (snadno přidat další).
- **Konzistentní CRUD kontrakt** napříč moduly (stejný tvar list/detail/create/update/delete,
  stránkování, filtry) → predikovatelné API i UI, méně kódu.
- **UI po modulech:** navigace admin UI se skládá z modulů dostupných dané roli (trenér vidí
  jen scheduling+documents, owner vše).
- **Audit napříč:** každá zápisová operace přes jádro → automaticky do `AuditLog`.

### 12A.4 Dopad na fázování
- **Fáze 0 musí položit základ:** DB jádro, auth, **RBAC, users & invitations**, modulární
  kostra, core + registrations. Ne "admin pro 2 lidi" — rovnou multi-user s rolemi.
- Další moduly (billing, scheduling, documents) se **přidávají do hotové kostry** bez refaktoru.

---

## 13. Akceptační kritéria
1. Web hopik4kids.cz funguje po nasazení **dodaného PR** (přepis fetch míst na nové čisté API,
   sekce 5.7) — registrace, výpis kurzů/kempů, kapacita i aktuality fungují beze změny chování.
2. Registrace z webu se ukládají a validují server-side dle sekce 4.
3. Počty naplněnosti kurzů/kempů se na webu zobrazují správně a živě.
4. Aktuality: publikované se zobrazí na `/aktuality`, každá má vlastní URL a je v sitemap;
   koncepty se nezobrazují.
5. Správce (netechnik) zvládne bez pomoci: přidat kurz, přidat kemp, napsat a publikovat
   aktualitu s obrázkem, najít a exportovat registrace.
6. GET registrací (osobní údaje) není přístupný veřejně bez auth.
7. Migrace: stávající data (školky, kurzy, kempy, registrace, články) přenesena vč. relací.
8. **RBAC funguje:** owner může pozvat nového člena e-mailem, přiřadit roli; trenér po přijetí
   pozvánky vidí jen povolené sekce (ne obsah/ceny/cizí osobní data); role se vynucují na API
   (403), ne jen skrytím v UI.
9. **Modularita:** nový modul lze přidat bez zásahu do jádra (kostra dle 12A).

---

## 14. Otevřené otázky (rozhodnout před/při implementaci)
1. ✅ **ROZHODNUTO:** Vlastní čisté API (varianta B) + PR do webu. Viz sekce 5.
2. ✅ **ROZHODNUTO:** `dayAndTime` → strukturovaný `weekday+time` (sekce 5.4).
3. ✅ **ROZHODNUTO:** School-registration leady se **neukládají** (out-of-scope). Poptávky škol
   řeší trenéři osobní domluvou; web dnes školy nepřihlašuje. Zůstává jen e-mailová poptávka.
4. ✅ **ROZHODNUTO:** E-maily zůstávají na webu (Nodemailer). Přesun do CMS až s fakturací (fáze 2).
5. ✅ **ROZHODNUTO:** Draft/publish **jen aktuality**. Kurzy/kempy/lekce = jen aktivní/neaktivní
   (bez publikačního workflow).
6. ✅ **ROZHODNUTO:** Hopík **není plátce DPH** → faktury bez DPH (ověřit s účetní před fází 2).
7. ✅ **ROZHODNUTO:** Fakturace = **fáze 2** (ne v MVP).

---

## 15. Implementační roadmapa (fáze)

**Fáze 0 — Základ (must-have, náhrada Strapi + multi-user platforma):**
- **Modulární kostra** (sekce 12A) + datový model 3B, DB, veřejné čtecí API, zápis registrací
  + server validace, admin CRUD, media upload, aktuality, migrace dat, oprava bezpeč. dluhu (9).
- **RBAC + správa uživatelů** (sekce 7): role owner/admin/trainer, pozvánky e-mailem,
  audit log. Ne "admin pro 2 lidi" — rovnou multi-user.
- *Cíl: web funguje beze změny, Strapi lze vypnout, tým lze rozšiřovat.*

**Fáze 1 — Provozní hodnota (data už existují, málo práce):**
- Dashboard (obsazenost, tržby, predikce, alert nedoplněných kurzů — 6A.1).
- Docházkový/kontaktní list PDF + alergie + souhlasy (6A.3).
- Stav platby + filtr neuhrazených (6A.3).
- Přehled media souhlasů (6A.6).

**Fáze 2 — Fakturace & růst:**
- PDF faktury + QR platba + rozeslání e-mailem (6A.5).
- Hromadné e-maily účastníkům (6A.3).
- Waitlist, UTM/zdroj registrace, retence & cross-sell (6A.2).

**Fáze 3 — Nástroje pro trenéry & automatizace:**
- **Rozvrh trenérů** (kalendář lekcí s obdobím platnosti, "můj rozvrh", poznámky k lekci — 6A.8 A).
- **Hlášení trenérů na hodiny** (shift-signup: LessonInstance + ShiftSignup, schvalování — 7.4).
- **Interní dokumenty & příručky** (Hopíkovská pravidla, metodika, checklisty — 6A.8 B).
- Auto-párování plateb z banky, upomínky, retence/anonymizace dat.
- Přístupový kód ke kurzu (`accessMode=code`) a případně `roster` ověření (3B.10) dle potřeby.

---

## 16. Rizika a mitigace
| Riziko | Dopad | Mitigace |
|--------|-------|----------|
| Rozbití webu při přechodu z Strapi | Výpadek registrací = ztráta tržeb | Vlastní API + **PR do webu** ověřený na staging, paralelní běh, ostrý cutover mimo sezónu náboru |
| Ztráta/nekonzistence `documentId` při migraci | Rozbité URL a relace | Migrační skript zachová `documentId`; ověřit počty záznamů před/po |
| Únik osobních dat (RČ) | GDPR pokuta, ztráta důvěry | Chráněné admin API, šifrování RČ at-rest, žádný token v browseru |
| Duplicitní číslo faktury | Účetní/právní problém | Atomické číslování (DB sekvence/transakce) |
| Špatné náležitosti faktury (DPH) | Neplatný doklad | Ověření s účetní před spuštěním fakturace |
| Správci se v novém UI ztratí | Nepoužívají to, vrátí se k chaosu | UX testy s Matějem/Petrem, onboarding, jednoduchost > featury |
| Sezónnost náboru (špička = zápis mnoha registrací naráz) | Přetížení/kolize kapacity | Rate-limit, index na FK, test zátěže před sezónou |

---

## 17. Sezónnost provozu (kontext pro plánování)
Hopík má výrazně **sezónní cyklus** — timing nasazení je byznysově kritický:
- **Nábor kroužků:** před začátkem školního roku (srpen–září) + pololetí (leden).
- **Nábor kempů:** jaro (nejvíc registrací před létem 2026).
- **Doporučení:** ostrý přechod na nové CMS provést **mimo hlavní náborovou špičku**
  (nejlépe po náboru, ne uprostřed), aby výpadek neohrozil příjmy.
- Dashboard alerty (nedoplněné kurzy) mají největší hodnotu **během** náborového okna.
