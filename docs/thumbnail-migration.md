# Existing HEIC thumbnail migration

## Preservation and execution boundaries

The batch has its own `backend.migration.ThumbnailBatch` entry point. The normal
API never starts a migration. Its image is built with `Dockerfile.thumbnail-batch`;
the production API image does not gain native image tools.

The batch only reads `travels/`. It never overwrites/deletes an original or changes
the DB `photo_code`, display order, dimensions, or other photo metadata. Before conversion
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
   The 640px migration alone does not change the diary's `photoCode` response.
   Use the display migration below to fix existing clients without frontend changes.
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

## Full-size display migration and existing client compatibility

The original 37-photo production migration populated only `thumbnail_code` with
640px WebP keys. The existing frontend still reads `photoCode`, so that alone did
not fix diary photo display. Do not report backend thumbnail completion as UI completion.

Run the same staged workflow with `asset_kind=display`, a new UUID and a newly
built/pinned image containing this feature. Never use the old thumbnail-only image.
Use the same kind, UUID and image ID across prepare/convert/apply/rollback.
`display` selects HEIC photos whose `display_code` is null, including photos that
already have small thumbnails. It preserves the recorded fixed target set.

Conversion creates an auto-oriented WebP at the source resolution (Q90, no resize)
under `thumbnails/heic-display/{run-id}/{photo-id}.webp`. It verifies the output and
enforces the existing 10MiB/60s limits; a decode/size/timeout failure stops the run.
Existing audited original backups with identical source key/ETag/size are reused
after checksum/byte verification. If no such backup exists, one is created with
the original preservation rules. Original HEICs, their backups and the old 640px
thumbnails remain intact.

After apply, **DB** `photo.photo_code` still contains the original key; the new
`photo.display_code` contains the full-size WebP key. **API** diary/photo `photoCode`
returns `display_code ?? photo_code`, so existing frontends use the WebP without
code changes. `thumbnailCode` and city list thumbnails keep their small WebP keys.
An existing photo edit is identified by `photoId`; echoing the response's WebP key
must not replace its DB original key. Normal deletion covers original/small/full
display objects and still excludes independent backups under `migrations/`.

`asset_kind=display, mode=rollback` clears only the exact display references applied
by that run, preserving original and thumbnail references. It does not remove S3
objects. A thumbnail run cannot apply or roll back a display run (and vice versa).
Schema deployment verifies a fresh DB dump before adding `display_code` to an
existing thumbnail-enabled database.

This is an existing-HEIC one-off migration, not a new upload pipeline. Current web
uploads already convert HEIC to JPEG before directly uploading to
`travels/{memberId}/...` in S3; the server issues a presigned URL and persists the
uploaded key. New uploads do not automatically create extra WebPs in this change.

Validate one real DEV image through apply/rollback, then one PROD image before
converting/applying the remaining live references. Confirm the actual public
`/record/5?uuid=e0c9b2` page requests WebP and displays the reported photos.
SQL inspection: add `photo.display_code` and ledger `asset_kind` to the queries above.

## Local verification

Use JDK 21. Run `./gradlew test bootJar`, build the batch image, and test decoding
with a local HEIC input. Never use production configuration for integration tests.
The batch starts no API security beans, JPA entity scanning, or Redis warmup. Each
converter process has a 60s timeout; its container has 768MiB/1 CPU limits. Input
objects and outputs are limited to 10MiB. No AWS credentials are printed in logs.
