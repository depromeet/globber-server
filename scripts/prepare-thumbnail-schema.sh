#!/usr/bin/env bash
set -euo pipefail
stage=${1:?dev or prod required}
case "$stage" in dev|prod) ;; *) exit 2 ;; esac
db_container="postgres_${stage}"
# No production credentials in arguments or logs; use the DB container's own environment.
schema_present=$(docker exec "$db_container" sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Atc "SELECT count(*) FROM information_schema.columns WHERE table_schema = '\''public'\'' AND table_name = '\''photo'\'' AND column_name IN ('\''thumbnail_code'\'', '\''display_code'\'')"')
if [[ "$schema_present" == 2 ]]; then
  echo 'Thumbnail column already present; checking additive audit schema.'
else
  backup_dir="$(dirname "${GITHUB_WORKSPACE:-$PWD}")/globber-thumbnail-backups"
  mkdir -p "$backup_dir"
  chmod 700 "$backup_dir"
  backup_file=$(mktemp "$backup_dir/${stage}-before-thumbnail-$(date -u +%Y%m%dT%H%M%SZ).XXXXXX.dump")
  (umask 077; docker exec "$db_container" sh -c 'exec pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc' > "$backup_file")
  test -s "$backup_file"
  docker exec -i "$db_container" pg_restore --list < "$backup_file" > /dev/null
  echo "Verified database backup: $backup_file (retain for rollback)"
fi
docker exec -i "$db_container" sh -c 'exec psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"' \
  < scripts/sql/thumbnail-migration.sql
