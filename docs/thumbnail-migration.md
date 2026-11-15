# Existing HEIC thumbnail migration

## Preservation and execution boundaries

The batch has its own `backend.migration.ThumbnailBatch` entry point. The normal
API never starts a migration. Its image is built with `Dockerfile.thumbnail-batch`;
the production API image does not gain native image tools.

The batch only reads `travels/`. It never overwrites/deletes an original or changes
`photo_code`, display order, dimensions, or other photo metadata. Before conversion
it makes an independent backup under `migrations/heic/{run-id}/originals/{photo-id}.heic`.
The normal application's S3 deleter does not accept that prefix, so this backup
survives subsequent diary deletion. No lifecycle or public-access settings are changed.
Generated WebP objects use `thumbnails/heic/{run-id}/{photo-id}.webp`.
Conditional PUT prevents overwrites. A retry verifies metadata, SHA-256 and exact bytes.

Schema changes are additive. CD verifies a PostgreSQL dump before initially adding
`photo.thumbnail_code`. Dumps are retained in `globber-thumbnail-backups` beside the
runner workspace, outside checkout/clean. Verify a restore into an isolated database
before production rollout; `pg_restore --list` alone checks readability, not a full restore.
The ledger has no photo FK so deletion does not erase the migration's recovery record.

## Gradual rollout

For the first rollout, GitHub only registers workflow_dispatch files present on
the default branch. Until the manual workflow reaches main, DEV proxy refresh
calls `scripts/verify-thumbnail-dev.sh`: a fixed one-off run ID verifies one live
HEIC and always rolls back its reference. A completed ROLLED_BACK run is skipped
on later deploys. If DEV has no referenced HEIC, it reports that gap without
seeding user data; do not claim live conversion verification in that case.
The script builds from the running DEV jar, preserving build identity. Both
backup objects and diagnostic build files are retained.

1. Merge into develop. DEV CD runs the backup/schema step before stopping the old API.
   Backup or SQL failure prevents the API deployment. Confirm the development health check.
2. Use Actions **HEIC thumbnail migration**, develop branch, `stage=dev`.
   Generate a UUID once (`uuidgen | tr '[:upper:]' '[:lower:]'`). Start with `limit=1`.
3. Run `mode=dry-run`: only SELECT and S3 HEAD; no ledger or object writes.
   Save the content-addressed `sha256:...` image ID printed in the job summary.
   Supply it as `image_id` for every subsequent phase, including rollback. The
   workflow binds the UUID to that exact image using a stopped pin container.
   It never starts that container; it retains the image through CD image pruning.
   A changed image or a runner without that image fails before batch execution.
   Preserve the pin container until recovery is no longer needed. Do not rebuild
   a missing image and assume it is identical; recover the exact saved image first.
4. Run `prepare` with the same UUID: records a fixed selection, bucket, original
   key, ETag and size. A UUID cannot be prepared twice. Maximum plan size is 1000.
5. Run `convert`: copies the original into the backup prefix, decodes HEIC into a
   WebP (640px bounding box, Q82), verifies it, and records CONVERTED. Photo rows
   still use their previous references. Inspect the generated image and its actual
   URL/permissions before proceeding. No ACL changes are made by the batch.
6. Run `apply`: checks source stability and backup/thumbnail checksums, then atomically
   updates `thumbnail_code` and the ledger per photo. Concurrent edits/deletion stop
   the run without replacing the user's newer data. Completed items are skipped on retry.
7. Check city record `thumbnailUrls` and diary `photos[].thumbnailCode` in the DEV API.
   Photo responses retain the original `photoCode`. Frontend must use `thumbnailCode`
   for previews, falling back to `photoCode`. Full-size HEIC display is separate work.
8. Test `rollback`, verify the original keys and other photos are unchanged. Repeat
   with a new UUID if a new canary is needed. Keep all backups and ledger entries.
9. After DEV is verified, promote the same changes to main. Repeat the sequence
   with `stage=prod`, **a new UUID**, `limit=1`. Verify one production thumbnail before
   preparing the remaining photos under another UUID. Count live DB references;
   the historical S3 object count is not the migration target count.

No automatic migration is triggered by deployment. The workflow requires explicit
stage, mode and run ID and only runs from develop (dev) / main (prod). One batch per
environment can run at a time. The recorded set is not expanded on convert/apply.

## Rollback and recovery

Run `mode=rollback` with the affected run ID in the same environment. It clears only
references that were applied by that run and still exactly match its photo/source/
thumbnail keys. A changed/deleted photo stops for manual review; it is never recreated.
WebP objects, original backups, dumps and ledger entries are retained. There is no
bulk `UPDATE photo SET thumbnail_code=NULL` and no S3 cleanup operation.

SQL for inspecting a run (substitute the UUID):

```sql
SELECT run_id, bucket, photo_id, source_key, source_etag, backup_key, thumbnail_key, state
FROM thumbnail_migration_item WHERE run_id = '<uuid>' ORDER BY photo_id;
```

If an original was subsequently removed by a user, clearing a derived reference
does not restore a deleted file or photo row. Its independent backup is retained;
review that user's deletion intent before any manual recovery. Restoring a DB dump
over the live database is not part of automated rollback.

## Local verification

Use JDK 21. Run `./gradlew test bootJar`, build the batch image, and test decoding
with a local HEIC input. Never use production configuration for integration tests.
The batch starts no API security beans, JPA entity scanning, or Redis warmup. Each
converter process has a 60s timeout; its container has 768MiB/1 CPU limits. Input
objects and outputs are limited to 10MiB. No AWS credentials are printed in logs.
