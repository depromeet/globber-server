package backend.migration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

class ThumbnailMigrationTest {
    private JdbcTemplate jdbc;
    private S3Client s3;
    private ThumbnailMigration batch;
    private final String run = UUID.randomUUID().toString();
    private final byte[] webp = "RIFF1234WEBPtest".getBytes(StandardCharsets.US_ASCII);

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1"));
        jdbc.execute("create table photo (id bigint primary key, photo_code varchar(255), thumbnail_code varchar(1024), display_code varchar(1024))");
        jdbc.execute("""
            create table thumbnail_migration_item (
                run_id varchar(36), bucket varchar(255), photo_id bigint, source_key varchar(255),
                source_etag varchar(255), source_size bigint, backup_key varchar(255),
                thumbnail_key varchar(255), state varchar(20) default 'PLANNED',
                asset_kind varchar(20) default 'thumbnail',
                updated_at timestamp default now(), primary key (run_id,photo_id))
            """);
        jdbc.update("insert into photo values (1,'travels/0/a.HEIC',null,null)");
        s3 = mock(S3Client.class);
        batch = new ThumbnailMigration(jdbc, s3, "globber-dev");
        when(s3.headObject(any(HeadObjectRequest.class))).thenAnswer(call -> {
            String key = ((HeadObjectRequest) call.getArgument(0)).key();
            return HeadObjectResponse.builder().eTag("etag").contentLength((long) webp.length)
                .contentType(key.endsWith(".webp") ? "image/webp" : "image/heic")
                .metadata(Map.of("source-etag", "etag", "source-key", "travels/0/a.HEIC",
                    "sha256", ThumbnailMigration.sha256(webp))).build();
        });
        when(s3.getObjectAsBytes(any(GetObjectRequest.class)))
            .thenReturn(ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), webp));
    }

    @Test
    void dryRunNeverWritesDatabaseOrObjects() throws Exception {
        batch.run("dry-run", run, 1);
        assertThat(jdbc.queryForObject("select count(*) from thumbnail_migration_item", Long.class)).isZero();
        assertThat(thumbnail()).isNull();
        verify(s3).headObject(any(HeadObjectRequest.class));
        verifyNoMoreInteractions(s3);
    }

    @Test
    void applyAndRollbackPreserveOriginalAndOtherPhotos() throws Exception {
        prepareConverted();
        jdbc.update("insert into photo values (2,'travels/0/b.jpg','thumbnails/unrelated.webp',null)");
        batch.run("apply", run, 1);
        assertThat(thumbnail()).endsWith("/1.webp");
        batch.run("apply", run, 1); // idempotent
        batch.run("rollback", run, 1);
        assertThat(thumbnail()).isNull();
        assertThat(jdbc.queryForObject("select photo_code from photo where id=1", String.class)).isEqualTo("travels/0/a.HEIC");
        assertThat(jdbc.queryForObject("select thumbnail_code from photo where id=2", String.class)).isEqualTo("thumbnails/unrelated.webp");
        assertThat(state()).isEqualTo("ROLLED_BACK");
        batch.run("rollback", run, 1);
    }

    @Test
    void applyRefusesConcurrentPhotoEditAndRollsBackAuditState() throws Exception {
        prepareConverted();
        jdbc.update("update photo set thumbnail_code='someone-elses.webp' where id=1");
        assertThatThrownBy(() -> batch.run("apply", run, 1)).isInstanceOf(IllegalStateException.class);
        assertThat(thumbnail()).isEqualTo("someone-elses.webp");
        assertThat(state()).isEqualTo("CONVERTED");
    }

    @Test
    void refusesWrongBucketAndUnconvertedApply() throws Exception {
        batch.run("prepare", run, 1);
        assertThatThrownBy(() -> batch.run("apply", run, 1)).isInstanceOf(IllegalStateException.class);
        jdbc.update("update thumbnail_migration_item set bucket='globber-prod'");
        assertThatThrownBy(() -> batch.run("convert", run, 1)).hasMessage("Bucket mismatch");
        assertThat(thumbnail()).isNull();
    }

    @Test
    void rollbackRefusesOverwritingNewerThumbnail() throws Exception {
        prepareConverted();
        batch.run("apply", run, 1);
        jdbc.update("update photo set thumbnail_code='newer.webp' where id=1");
        assertThatThrownBy(() -> batch.run("rollback", run, 1)).isInstanceOf(IllegalStateException.class);
        assertThat(thumbnail()).isEqualTo("newer.webp");
        assertThat(state()).isEqualTo("APPLIED");
    }

    @Test
    void prepareIsImmutableAndSourceChangesBlockApply() throws Exception {
        prepareConverted();
        assertThatThrownBy(() -> batch.run("prepare", run, 1)).isInstanceOf(IllegalStateException.class);
        when(s3.headObject(any(HeadObjectRequest.class))).thenReturn(
            HeadObjectResponse.builder().eTag("changed").contentLength(1L).build());
        assertThatThrownBy(() -> batch.run("apply", run, 1)).hasMessageContaining("Original changed");
        assertThat(thumbnail()).isNull();
    }

    @Test
    void fullSizeRunReusesBackupAndRollsBackOnlyDisplayReference() throws Exception {
        prepareConverted();
        batch.run("apply", run, 1);
        String originalThumbnail = thumbnail();
        String backup = jdbc.queryForObject("select backup_key from thumbnail_migration_item where run_id=?", String.class, run);
        var displayBatch = new ThumbnailMigration(jdbc, s3, "globber-dev", "display");
        String displayRun = UUID.randomUUID().toString();
        displayBatch.run("prepare", displayRun, 1000);
        assertThat(jdbc.queryForObject("select backup_key from thumbnail_migration_item where run_id=?", String.class, displayRun))
            .isEqualTo(backup);
        jdbc.update("update thumbnail_migration_item set state='CONVERTED' where run_id=?", displayRun);
        assertThatThrownBy(() -> batch.run("apply", displayRun, 1)).hasMessageContaining("Asset kind mismatch");
        displayBatch.run("apply", displayRun, 1);
        assertThat(jdbc.queryForObject("select display_code from photo where id=1", String.class))
            .isEqualTo("thumbnails/heic-display/" + displayRun + "/1.webp");
        displayBatch.run("rollback", displayRun, 1);
        assertThat(jdbc.queryForObject("select display_code from photo where id=1", String.class)).isNull();
        assertThat(thumbnail()).isEqualTo(originalThumbnail);
        assertThat(jdbc.queryForObject("select photo_code from photo where id=1", String.class)).isEqualTo("travels/0/a.HEIC");
    }

    @Test
    void displayRunRefusesReplacingNewerDisplayReference() throws Exception {
        var displayBatch = new ThumbnailMigration(jdbc, s3, "globber-dev", "display");
        displayBatch.run("prepare", run, 1);
        jdbc.update("update thumbnail_migration_item set state='CONVERTED' where run_id=?", run);
        jdbc.update("update photo set display_code='newer.webp' where id=1");
        assertThatThrownBy(() -> displayBatch.run("apply", run, 1)).hasMessageContaining("Photo changed");
        assertThat(jdbc.queryForObject("select display_code from photo where id=1", String.class)).isEqualTo("newer.webp");
        assertThat(state()).isEqualTo("CONVERTED");
    }

    private void prepareConverted() throws Exception {
        batch.run("prepare", run, 1);
        jdbc.update("update thumbnail_migration_item set state='CONVERTED'");
    }

    private String thumbnail() {
        return jdbc.queryForObject("select thumbnail_code from photo where id=1", String.class);
    }

    private String state() {
        return jdbc.queryForObject("select state from thumbnail_migration_item where photo_id=1", String.class);
    }
}
