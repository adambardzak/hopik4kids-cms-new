# AGENTS.md — Hopík4Kids Custom CMS

Guidance for AI agents and developers working in this repository.

> **Read `prd.md` first.** It is the authoritative product spec (data model, API contract,
> RBAC, modules, phasing). This file describes *how* to build; `prd.md` describes *what*.
> When in doubt, `prd.md` wins. Section references below (e.g. "3B", "7", "12A") point to `prd.md`.

---

## 1. What this project is

Custom CMS + operational platform for **Hopík4Kids** (children's sports program, Plzeň),
replacing their current Strapi. It:
1. Exposes a **clean REST API** for the public marketing site (`hopik4kids.cz`, separate repo
   `../hopik4kids`).
2. Provides an **admin UI with RBAC** for a growing team (founders + trainers).
3. Is built **modularly** to grow (schedule, invoicing, documents…).

**Do not** confuse this with the public website — that lives in `../hopik4kids` and is only
consumed as an API client. Changes there are delivered as a separate PR (see prd.md §5.7).

---

## 2. Architecture & stack

Monorepo, two deployables + one database:

```
hopik4kids-custom-cms/
├── backend/     # Spring Boot (Java) — REST API, business logic, RBAC, persistence
├── frontend/    # Next.js (App Router, TS) — admin UI (+ public API is served by backend)
├── prd.md       # Product spec (authoritative)
└── AGENTS.md
```

- **Backend:** Spring Boot + PostgreSQL. Owns the domain, API, auth, RBAC, migrations.
- **Frontend:** Next.js (App Router, React, TypeScript, Tailwind) — admin console only.
  Talks to backend over REST. The **public marketing site is a different repo** and calls
  the same backend's public endpoints.
- **DB:** PostgreSQL. Schema versioned via migrations (Flyway or Liquibase — pick one, be consistent).
- **Auth:** session or JWT issued by backend; frontend stores it httpOnly. RBAC enforced
  **on the backend**, never only hidden in UI.

### API split (prd.md §5)
- `/api/**` — **public**, read-only for the website (programs, articles) + registration POST.
  No personal data exposed; capacity comes from `Program.spotsTaken`.
- `/admin/api/**` — **authenticated**, RBAC-checked. Full CRUD, registrations with personal data.

---

## 3. Domain model (prd.md §3B — target model)

Build to **§3B**, not §3 (§3 is the old Strapi model, kept only as migration reference).
Core entities:

- `Program` — anything you can register for (`type: club | school | camp`). Unifies old
  Course + Camp. Carries `capacity`, `spotsTaken`, `shirtPolicy`, `status`, `accessMode`.
- `Location` — venue/kindergarten (shared, optional on program).
- `Registration` — **one** entity for club & camp signups (was two in Strapi).
- `Child` + `Parent` — deduplicated personal data (variant A, recommended).
- `Article` — news post (only entity with draft/publish via `publishedAt`).
- `Media` — uploads.
- `User`, `Invitation`, `AuditLog` — auth/RBAC/audit (phase 0).
- `LessonInstance`, `ShiftSignup`, `Document` — phase 3.

### Non-negotiable rules (prd.md §3B.9, §4)
- `spotsTaken` is mutated **transactionally** on registration create/cancel; capacity checked
  in the same transaction (no overbooking). Never compute capacity by scanning registrations.
- `priceSnapshot` freezes price on the registration (price changes don't rewrite history).
- `personalId` (Czech RČ) is **sensitive** → encrypt at-rest, never expose on public API,
  restrict access by role.
- Soft-delete registrations (`status=cancelled`), keep audit trail.
- Validation mirrors the website form (prd.md §4): `personalId` `^\d{6}/\d{4}$`,
  phone `^\+420[0-9]{9}$`, email, `birthDate <= today`, `consentPersonalData === true`.

---

## 4. RBAC (prd.md §7)

Fixed roles: `owner`, `admin`, `trainer` (+ later `accountant`, `viewer`). Rules:
- Enforce on **every** `/admin/api` endpoint via a central authorization layer
  (e.g. Spring Security + method-level checks). UI hiding is not security.
- `trainer` sees personal data **only** for children in their assigned programs (scoped).
- At least one `owner` must always exist (cannot delete the last one).
- New members onboarded via **e-mail invitation** (invite → set own password), never
  admin-typed passwords.
- Every write operation writes to `AuditLog`.

---

## 5. Modular structure (prd.md §12A)

Organize by **domain module**, not by technical layer only. Modules: `core`, `users-rbac`,
`registrations`, `scheduling`, `billing`, `documents`. Each module owns its schema slice,
service logic, API, and (frontend) its UI + navigation entry.

- Modules depend only on the **shared kernel** (DB, auth, RBAC, audit, email, media, events),
  not on each other directly — communicate via domain events / interfaces.
- New modules must be addable **without touching the kernel** or other modules.
- Support feature flags so a module can ship disabled.

When adding a feature, ask: *which module does this belong to?* Put it there. Don't leak
domain logic into controllers or the kernel.

---

## 6. Conventions

### Backend (Java / Spring Boot)
- Package by module: `cz.hopik4kids.cms.<module>` with `domain`, `service`, `web` (controllers),
  `repository` sub-packages.
- DTOs at the API boundary — never expose JPA entities directly (esp. anything with `personalId`).
- Public API response shape (prd.md §5.1): no Strapi-style `data`/`meta` wrapper. Collections
  return `{ items, total, page, pageSize }`. Errors `{ error: { code, message } }`.
- Path identifiers use the public `id` (= old `documentId`, preserved on migration). Articles by `slug`.
- Migrations for every schema change (Flyway/Liquibase). Never edit a released migration.
- Validation server-side, mirroring prd.md §4. Return `403` on role failure, `401` unauthenticated.

### Frontend (Next.js admin)
- App Router, TypeScript, Tailwind. Czech UI copy (`lang="cs"`), no jargon/IDs shown to users.
- Navigation composed from modules available to the current role.
- Never trust the client for authorization — backend is the source of truth.
- Keep it approachable for non-technical users (founders + trainers): big buttons, confirms on
  delete, helpful empty states.

### General
- Match existing code style in each subproject; do not reformat unrelated code.
- Only commit/push when explicitly asked. Never commit secrets (DB creds, SMTP, JWT keys).
- Keep the public website (`../hopik4kids`) in mind: breaking the API contract breaks
  registrations = lost revenue. Coordinate contract changes with the website PR (prd.md §5.7).

---

## 7. Commands

> Fill in exact commands once tooling is scaffolded. Expected shape:

```bash
# Backend (from backend/)
./mvnw spring-boot:run        # or ./gradlew bootRun
./mvnw test                   # run tests
./mvnw verify                 # build + verify

# Frontend (from frontend/)
npm run dev                   # dev server
npm run build                 # production build
npm run lint                  # lint

# Database (local)
docker compose up -d postgres # local Postgres
# migrations run on backend startup (Flyway/Liquibase)
```

Keep this section updated as scaffolding lands.

---

## 8. Phasing (prd.md §15) — build order

- **Phase 0 (now):** modular skeleton + §3B data model + migrations, public read API +
  registration POST, **RBAC + users + invitations**, admin CRUD, media, articles, data
  migration from Strapi, fix the security debt (§9). Ship the website PR (§5.7).
- **Phase 1:** dashboard metrics, attendance/contact PDF, payment status.
- **Phase 2:** invoicing (PDF + QR/SPAYD), bulk emails, retention/UTM.
- **Phase 3:** trainer schedule, shift-signup, internal documents.

Do not build later-phase features before Phase 0 is solid unless explicitly asked.

---

## 9. Security & compliance checklist (prd.md §9)

- [ ] No API token in the browser (the current Strapi mistake — do not repeat).
- [ ] `personalId` encrypted at-rest; never on public API.
- [ ] Rate-limit the public registration POST.
- [ ] RBAC enforced on backend for every admin endpoint.
- [ ] Audit log on writes.
- [ ] GDPR: media-consent visibility, retention/anonymization of old registrations (phase 2/3).
- [ ] Invoices (phase 2): confirm Hopík is **not** a VAT payer; number series atomic; 10y archive.
