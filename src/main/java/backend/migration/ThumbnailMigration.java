package backend.migration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

/** Only creates objects under migration-specific prefixes. Never deletes an S3 object. */
public class ThumbnailMigration {
    private static final Logger log = LoggerFactory.getLogger(ThumbnailMigration.class);
    private static final long MAX_BYTES = 10 * 1024 * 1024;
    private final JdbcTemplate jdbc;
    private final S3Client s3;
    private final String bucket;
    private final String assetKind;
    private final String column;

    public ThumbnailMigration(JdbcTemplate jdbc, S3Client s3, String bucket) {
        this(jdbc, s3, bucket, "thumbnail");
    }

    public ThumbnailMigration(JdbcTemplate jdbc, S3Client s3, String bucket, String assetKind) {
        this.jdbc = jdbc;
        this.s3 = s3;
        this.bucket = bucket;
        this.assetKind = assetKind;
        // SQL 컬럼에는 설정값을 직접 쓰지 않고 허용한 두 식별자만 사용한다.
        this.column = switch (assetKind) {
            case "thumbnail" -> "thumbnail_code";
            case "display" -> "display_code";
            default -> throw new IllegalArgumentException("Unknown asset kind: " + assetKind);
        };
    }

    public void run(String mode, String runId, int limit) throws Exception {
        if (!UUID.fromString(runId).toString().equals(runId) || limit < 1 || limit > 1000) {
            throw new IllegalArgumentException("run-id must be a canonical UUID; limit must be 1..1000");
        }
        switch (mode) {
            case "dry-run" -> plan(runId, limit, false);
            case "prepare" -> plan(runId, limit, true);
            case "convert", "apply", "rollback" -> {
                // Work on a fixed recorded set. Never rediscover or expand targets during execution.
                var items = jdbc.query("select * from thumbnail_migration_item where run_id=? order by photo_id",
                    (rs, row) -> {
                        if (!assetKind.equals(rs.getString("asset_kind"))) {
                            throw new IllegalStateException("Asset kind mismatch; use the recorded kind");
                        }
                        return new Item(rs.getLong("photo_id"), rs.getString("source_key"),
                        rs.getString("source_etag"), rs.getLong("source_size"), rs.getString("backup_key"),
                        rs.getString("thumbnail_key"), rs.getString("state"), rs.getString("bucket"));
                    }, runId);
                if (items.isEmpty()) throw new IllegalStateException("No prepared items for run " + runId);
                for (Item item : items) {
                    if (!bucket.equals(item.bucket())) throw new IllegalStateException("Bucket mismatch");
                    if (mode.equals("convert") && item.state().equals("PLANNED")) convert(runId, item);
                    if (mode.equals("apply")) apply(runId, item);
                    if (mode.equals("rollback")) rollback(runId, item);
                }
            }
            default -> throw new IllegalArgumentException("Unknown migration.mode: " + mode);
        }
        log.info("Migration finished: mode={}, runId={}, assetKind={}", mode, runId, assetKind);
    }

    private void plan(String runId, int limit, boolean persist) {
        if (persist && jdbc.queryForObject("select count(*) from thumbnail_migration_item where run_id=?",
            Long.class, runId) != 0) throw new IllegalStateException("Use a new run ID; existing plan is immutable");
        var targets = jdbc.query("select id, photo_code from photo where " + column
            + " is null and lower(photo_code) like '%.heic' order by id limit ?",
            (rs, row) -> Map.entry(rs.getLong(1), rs.getString(2)), limit);
        // Validate all sources before persisting any plan.
        var items = targets.stream().map(target -> {
            String source = target.getValue();
            if (!source.startsWith("travels/") || !source.toLowerCase(Locale.ROOT).endsWith(".heic")) {
                throw new IllegalArgumentException("Unexpected source: " + source);
            }
            var head = head(source);
            if (head.contentLength() < 1 || head.contentLength() > MAX_BYTES) {
                throw new IllegalStateException("Source exceeds input limit: " + source);
            }
            String prefix = "migrations/heic/" + runId + "/";
            String backup = prefix + "originals/" + target.getKey() + ".heic";
            if (assetKind.equals("display")) {
                // 원본이 같다는 기록이 있는 기존 백업만 재사용한다. 실제 내용은 convert/apply에서 검증한다.
                var backups = jdbc.queryForList("""
                    select backup_key from thumbnail_migration_item
                    where bucket=? and photo_id=? and source_key=? and source_etag=? and source_size=?
                      and state in ('CONVERTED','APPLIED','ROLLED_BACK') order by run_id limit 1
                    """, String.class, bucket, target.getKey(), source, head.eTag(), head.contentLength());
                if (!backups.isEmpty()) backup = backups.getFirst();
            }
            return new Item(target.getKey(), source, head.eTag(), head.contentLength(),
                backup, "thumbnails/heic" + (assetKind.equals("display") ? "-display" : "")
                + "/" + runId + "/" + target.getKey() + ".webp", "PLANNED", bucket);
        }).toList();
        transaction().executeWithoutResult(status -> {
            for (Item item : items) {
                log.info("{} photoId={} source={} target={}", persist ? "PLAN" : "DRY RUN",
                    item.id(), item.source(), item.thumbnail());
                if (persist) jdbc.update("""
                    insert into thumbnail_migration_item
                    (run_id,bucket,photo_id,source_key,source_etag,source_size,backup_key,thumbnail_key,asset_kind)
                    values (?,?,?,?,?,?,?,?,?)
                    """, runId, bucket, item.id(), item.source(), item.etag(), item.size(),
                    item.backup(), item.thumbnail(), assetKind);
            }
        });
        log.info("Targets={}, limit={}; compare DB count before choosing a larger limit", items.size(), limit);
    }

    private void convert(String runId, Item item) throws Exception {
        assertSource(item);
        Path dir = Files.createTempDirectory("globber-heic-");
        try {
            Path source = dir.resolve("source.heic");
            s3.getObject(GetObjectRequest.builder().bucket(bucket).key(item.source()).ifMatch(item.etag()).build(), source);
            if (Files.size(source) != item.size()) throw new IllegalStateException("Source size changed");
            // Independent backup survives later normal deletion of a diary's original image.
            putOnce(item.backup(), source, "image/heic", item);
            Path output = dir.resolve("thumbnail.webp");
            if (assetKind.equals("display")) {
                // autorot는 축소 없이 방향을 바로잡는다. 브라우저 표시에 사용할 원본 해상도 WebP.
                execute(dir, "vips", "autorot", source.toString(), output + "[Q=90]");
            } else {
                execute(dir, "vips", "thumbnail", source.toString(), output + "[Q=82,strip]", "640", "--size", "down");
            }
            execute(dir, "vipsheader", output.toString());
            validateWebp(Files.readAllBytes(output));
            putOnce(item.thumbnail(), output, "image/webp", item);
            verifyDerived(item);
            jdbc.update("update thumbnail_migration_item set state='CONVERTED',updated_at=now() where run_id=? and photo_id=? and state='PLANNED'",
                runId, item.id());
            log.info("Converted photoId={}; original and DB reference preserved", item.id());
        } finally {
            try (var paths = Files.walk(dir)) {
                for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            }
        }
    }

    private void apply(String runId, Item item) {
        if (item.state().equals("APPLIED")) return;
        if (!item.state().equals("CONVERTED")) throw new IllegalStateException("Convert and review first: " + item.id());
        assertSource(item);
        verifyDerived(item);
        transaction().executeWithoutResult(status -> {
            lock(runId, item);
            int changed = jdbc.update("update photo set " + column + "=? where id=? and photo_code=? and "
                + column + " is null", item.thumbnail(), item.id(), item.source());
            if (changed != 1) throw new IllegalStateException("Photo changed/deleted; refusing apply: " + item.id());
            jdbc.update("update thumbnail_migration_item set state='APPLIED',updated_at=now() where run_id=? and photo_id=?",
                runId, item.id());
        });
    }

    private void rollback(String runId, Item item) {
        if (!item.state().equals("APPLIED")) return;
        transaction().executeWithoutResult(status -> {
            lock(runId, item);
            int changed = jdbc.update("update photo set " + column + "=null where id=? and photo_code=? and "
                + column + "=?", item.id(), item.source(), item.thumbnail());
            if (changed != 1) throw new IllegalStateException("Photo changed/deleted; manual review needed: " + item.id());
            jdbc.update("update thumbnail_migration_item set state='ROLLED_BACK',updated_at=now() where run_id=? and photo_id=?",
                runId, item.id());
        });
    }

    private void lock(String runId, Item item) {
        String state = jdbc.queryForObject("select state from thumbnail_migration_item where run_id=? and photo_id=? for update",
            String.class, runId, item.id());
        if (!item.state().equals(state)) throw new IllegalStateException("Concurrent migration; rerun after it finishes");
    }

    private TransactionTemplate transaction() {
        return new TransactionTemplate(new DataSourceTransactionManager(java.util.Objects.requireNonNull(jdbc.getDataSource())));
    }

    private void assertSource(Item item) {
        var head = head(item.source());
        if (!item.etag().equals(head.eTag()) || item.size() != head.contentLength()) {
            throw new IllegalStateException("Original changed since plan: " + item.id());
        }
    }

    private void verifyDerived(Item item) {
        verify(item.backup(), "image/heic", item);
        verify(item.thumbnail(), "image/webp", item);
    }

    private void verify(String key, String type, Item item) {
        var head = head(key);
        if (!type.equals(head.contentType()) || head.contentLength() < 1
            || head.contentLength() > MAX_BYTES
            || !item.etag().equals(head.metadata().get("source-etag"))
            || !item.source().equals(head.metadata().get("source-key"))) {
            throw new IllegalStateException("Unexpected existing object; refusing reuse: " + key);
        }
        byte[] data = s3.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(key)
            .ifMatch(head.eTag()).build()).asByteArray();
        if (!sha256(data).equals(head.metadata().get("sha256"))) {
            throw new IllegalStateException("Object checksum mismatch: " + key);
        }
        if (type.equals("image/webp")) validateWebp(data);
    }

    private HeadObjectResponse head(String key) {
        return s3.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
    }

    private void putOnce(String key, Path file, String type, Item item) {
        try {
            s3.putObject(PutObjectRequest.builder().bucket(bucket).key(key).contentType(type)
                .metadata(Map.of("source-etag", item.etag(), "source-key", item.source(),
                    "sha256", sha256(Files.readAllBytes(file))))
                .overrideConfiguration(c -> c.putHeader("If-None-Match", "*"))
                .build(), RequestBody.fromFile(file));
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        } catch (S3Exception e) {
            if (e.statusCode() != 412) throw e;
            verify(key, type, item);
            // Exact byte comparison makes retries refuse an unrelated/corrupted existing object.
            try {
                byte[] existing = s3.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(key).build()).asByteArray();
                if (!java.util.Arrays.equals(existing, Files.readAllBytes(file))) {
                    throw new IllegalStateException("Existing object bytes differ: " + key);
                }
            } catch (java.io.IOException io) { throw new java.io.UncheckedIOException(io); }
        }
    }

    static void validateWebp(byte[] data) {
        if (data.length < 12 || data.length > MAX_BYTES
            || !new String(data, 0, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("RIFF")
            || !new String(data, 8, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("WEBP")) {
            throw new IllegalStateException("Invalid WebP output");
        }
    }

    static String sha256(byte[] data) {
        try {
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(data));
        } catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    private static void execute(Path dir, String... command) throws Exception {
        Path logFile = dir.resolve("converter.log");
        Process process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(logFile.toFile()).start();
        try {
            if (!process.waitFor(Duration.ofSeconds(60).toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly().waitFor();
                throw new IllegalStateException("Image conversion timed out");
            }
            if (process.exitValue() != 0) throw new IllegalStateException("Converter failed: " + Files.readString(logFile));
        } finally {
            if (process.isAlive()) process.destroyForcibly().waitFor();
        }
    }

    record Item(long id, String source, String etag, long size, String backup, String thumbnail, String state, String bucket) {}
}
