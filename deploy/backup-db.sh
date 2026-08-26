#!/usr/bin/env bash
# Daily PostgreSQL backup for the Hopík4Kids CMS, with rotation.
# - Dumps the DB from the running docker compose 'postgres' service (gzip-compressed).
# - Keeps DAILY_KEEP daily backups; on Sundays also writes a weekly copy (WEEKLY_KEEP kept).
# Safe to run repeatedly. Intended to be triggered by cron.
set -euo pipefail

CMS_DIR="/home/deploy/hopik-cms"
BACKUP_DIR="/home/deploy/backups/hopik-cms"
DAILY_KEEP=14
WEEKLY_KEEP=8
COMPOSE="docker compose -f ${CMS_DIR}/docker-compose.prod.yml"

mkdir -p "${BACKUP_DIR}/daily" "${BACKUP_DIR}/weekly"

# Load DB credentials from the (gitignored) prod env file.
set -a
# shellcheck disable=SC1091
source "${CMS_DIR}/.env"
set +a

TS="$(date +%Y%m%d-%H%M%S)"
DAILY_FILE="${BACKUP_DIR}/daily/hopik-cms-${TS}.sql.gz"
LOG="${BACKUP_DIR}/backup.log"

log() { echo "$(date '+%Y-%m-%d %H:%M:%S') $*" >> "${LOG}"; }

log "START backup -> ${DAILY_FILE}"

# Dump and compress. pg_dump exit status is checked via PIPESTATUS.
if ${COMPOSE} exec -T postgres pg_dump -U "${DB_USER}" "${DB_NAME}" | gzip > "${DAILY_FILE}"; then
  # Guard against truncated/empty dumps.
  SIZE="$(stat -c%s "${DAILY_FILE}")"
  if [ "${SIZE}" -lt 1000 ]; then
    log "ERROR dump too small (${SIZE} bytes) — removing ${DAILY_FILE}"
    rm -f "${DAILY_FILE}"
    exit 1
  fi
  log "OK daily backup ${SIZE} bytes"
else
  log "ERROR pg_dump failed — removing partial ${DAILY_FILE}"
  rm -f "${DAILY_FILE}"
  exit 1
fi

# On Sunday (day 7) keep a weekly copy.
if [ "$(date +%u)" -eq 7 ]; then
  cp "${DAILY_FILE}" "${BACKUP_DIR}/weekly/hopik-cms-weekly-${TS}.sql.gz"
  log "OK weekly copy created"
fi

# Rotation: keep newest N, delete the rest.
rotate() {
  local dir="$1" keep="$2"
  local files
  files="$(ls -1t "${dir}"/*.sql.gz 2>/dev/null | tail -n +$((keep + 1)) || true)"
  if [ -n "${files}" ]; then
    echo "${files}" | xargs -r rm -f
    log "rotated ${dir} (kept ${keep})"
  fi
}
rotate "${BACKUP_DIR}/daily" "${DAILY_KEEP}"
rotate "${BACKUP_DIR}/weekly" "${WEEKLY_KEEP}"

log "DONE"
