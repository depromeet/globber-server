#!/usr/bin/env bash
set -euo pipefail
# This script is called only by DEV proxy refresh. Its fixed ledger ID makes it
# a one-off canary, not a migration on every deployment.
run_id=18718718-7187-4187-8187-187187187187
query() {
  docker exec postgres_dev sh -c 'exec psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Atc "$1"' sh "$1"
}
count=$(query "SELECT count(*) FROM thumbnail_migration_item WHERE run_id='$run_id'")
if [[ "$count" == 1 && "$(query "SELECT state FROM thumbnail_migration_item WHERE run_id='$run_id'")" == ROLLED_BACK ]]; then
  echo 'DEV thumbnail canary already verified and rolled back; skipping.'
  exit 0
fi
if [[ "$count" == 0 && "$(query "SELECT count(*) FROM photo WHERE thumbnail_code IS NULL AND lower(photo_code) LIKE '%.heic'")" == 0 ]]; then
  echo 'DEV has no referenced HEIC photos. No data was seeded or modified; a live conversion still needs verification.'
  exit 0
fi
work_dir=$(mktemp -d)
mkdir -p "$work_dir/build/libs"
docker cp dev:/app.jar "$work_dir/build/libs/app-SNAPSHOT.jar"
cp Dockerfile.thumbnail-batch "$work_dir/Dockerfile"
docker build -t globber-thumbnail-batch:dev-canary "$work_dir"
batch() {
  docker run --rm --network dev_network --memory 768m --cpus 1 \
    globber-thumbnail-batch:dev-canary --spring.profiles.active=dev \
    --migration.run-id="$run_id" --migration.limit=1 --migration.mode="$1"
}
batch dry-run
if [[ "$count" == 0 ]]; then batch prepare; fi
# Always attempt reference rollback on an error, including failed public URL verification.
# All S3 objects and backup artifacts are retained. No original is overwritten.
trap 'batch rollback' EXIT
batch convert
target=$(query "SELECT thumbnail_key FROM thumbnail_migration_item WHERE run_id='$run_id'")
curl --fail --silent --show-error --head --max-time 20 \
  "https://globber-dev.s3.ap-northeast-2.amazonaws.com/$target"
batch apply
test "$(query "SELECT count(*) FROM photo p JOIN thumbnail_migration_item m ON m.photo_id=p.id AND p.photo_code=m.source_key AND p.thumbnail_code=m.thumbnail_key WHERE m.run_id='$run_id'")" == 1
batch rollback
trap - EXIT
test "$(query "SELECT state FROM thumbnail_migration_item WHERE run_id='$run_id'")" == ROLLED_BACK
echo 'DEV thumbnail canary converted, verified, applied and rolled back; original preserved.'
# Keep this run's Docker build context for diagnosis, rather than deleting artifacts.
echo "Canary build context: $work_dir"
