# Hopík4Kids CMS

Custom CMS + operational platform for **Hopík4Kids** (children's sports program, Plzeň),
replacing the previous Strapi. It exposes a clean public REST API for the marketing website
(`hopik4kids.cz`) and an admin console with RBAC for the team.

See [`prd.md`](./prd.md) for the authoritative product spec and [`AGENTS.md`](./AGENTS.md) for
build conventions.

## Architecture

Monorepo, two deployables + one database:

```
├── backend/    # Spring Boot (Java 21) — REST API, business logic, RBAC, persistence
├── frontend/   # Next.js (App Router, TS, Tailwind) — admin console
└── prd.md      # Product spec (authoritative)
```

- **Backend:** Spring Boot + PostgreSQL. Flyway migrations, JWT auth, RBAC, `personalId`
  encrypted at-rest, rate-limited public registration endpoint.
- **Frontend:** Next.js admin UI. JWT stored in an httpOnly cookie; backend is the source of
  truth for authorization.
- **DB:** PostgreSQL. Schema versioned via Flyway (`backend/src/main/resources/db/migration`).

### API split

- `/api/**` — public, read-only for the website (programs, articles) + registration POST.
  No personal data; capacity comes from `Program.spotsTaken`.
- `/admin/api/**` — authenticated, RBAC-checked. Full CRUD, registrations with personal data.

## Local development

Requires JDK 21, Node 22, Docker.

```bash
# 1. Postgres
docker compose up -d postgres

# 2. Backend (from backend/) — http://localhost:8080
./mvnw spring-boot:run        # or: mvn spring-boot:run

# 3. Frontend admin (from frontend/) — http://localhost:3000
npm install
npm run dev
```

On first start the backend seeds a bootstrap owner (`owner@hopik4kids.cz` / `changeme123`
in dev). **Change this immediately.**

Health check: `GET http://localhost:8080/actuator/health`.

## Production

Activate the prod profile and provide all secrets via environment (see [`.env.example`](./.env.example)).
The prod profile has **no default secrets** — the app fails fast if any are missing, and
refuses to start with known dev defaults.

```bash
SPRING_PROFILES_ACTIVE=prod   # backend
```

Both apps ship a multi-stage `Dockerfile` (backend: JRE runtime, non-root; frontend: Next
standalone).
