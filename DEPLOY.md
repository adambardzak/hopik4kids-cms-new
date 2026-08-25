# Deploy — Hopík4Kids CMS

## Přehled

| Část | Kam | URL |
|------|-----|-----|
| Backend API | VPS (46.62.209.17), Docker + nginx + TLS | `https://api.hopik4kids.cz` |
| Admin frontend | Vercel (GitHub auto-deploy) | `https://admin.hopik4kids.cz` |
| Veřejný web | jinde (`hopik4kids.cz`) | volá `api.hopik4kids.cz` |
| Databáze | VPS, izolovaný Postgres kontejner (interní) | — |

## DNS (nutné)

```
api.hopik4kids.cz.    A   46.62.209.17          # backend na VPS
admin.hopik4kids.cz.  →   Vercel (CNAME dle Vercelu)   # PŘESMĚROVAT z VPS na Vercel
```

## Backend (VPS)

Kód: `~/hopik-cms` (git clone z GitHubu). Secrets: `~/hopik-cms/.env` (NENÍ v gitu).

```bash
cd ~/hopik-cms
git pull
docker compose -f docker-compose.prod.yml up -d --build   # deploy / update
docker logs -f hopik-cms-backend                            # log
curl -s localhost:18095/actuator/health                     # health
```

- Backend: `127.0.0.1:18095` → nginx `api.hopik4kids.cz` (TLS certbot).
- Postgres: interní (docker síť), data ve volume `hopik_cms_pgdata`.
- Media: volume `hopik_cms_media` → `/data/media`.

### TLS (po DNS)
```bash
sudo certbot --nginx -d api.hopik4kids.cz
```

## Admin frontend (Vercel)

1. Vercel → New Project → import GitHub repo `adambardzak/hopik4kids-cms-new`.
2. **Root Directory: `frontend`** (monorepo!).
3. Framework: Next.js (auto).
4. Environment Variables:
   - `BACKEND_URL = https://api.hopik4kids.cz`   (server-side BFF cíl)
5. Deploy. Pak Domains → přidat `admin.hopik4kids.cz` + nastavit DNS dle Vercelu.

## Secrets (uložené mimo git, v ~/hopik-cms/.env)

- `PERSONAL_ID_KEY`, `JWT_SECRET`, `REGISTRATION_API_KEY`, `DB_PASSWORD`, `BOOTSTRAP_OWNER_PASSWORD`
- **REGISTRATION_API_KEY** musí dostat i veřejný web (`hopik4kids.cz`) jako server-only env,
  aby mohl posílat registrace na `POST /api/registrations` (hlavička `X-Registration-Key`).

## Owneři (první přihlášení)

Vytvořeni v DB. Hesla změnit po prvním loginu.
