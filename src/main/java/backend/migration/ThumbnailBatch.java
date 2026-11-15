package backend.migration;

import backend.globber.config.S3Config;
import javax.sql.DataSource;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/** Separate entry point: no API component scan, Redis warmup, JPA or web security. */
@Configuration(proxyBeanMethods = false)
@Import({DataSourceAutoConfiguration.class, S3Config.class})
public class ThumbnailBatch {
    public static void main(String[] args) throws Exception {
        try (var context = new SpringApplicationBuilder(ThumbnailBatch.class)
            .web(WebApplicationType.NONE).run(args)) {
            var env = context.getEnvironment();
            new ThumbnailMigration(new JdbcTemplate(context.getBean(DataSource.class)),
                context.getBean(software.amazon.awssdk.services.s3.S3Client.class),
                env.getRequiredProperty("aws.s3.bucket-name"))
                .run(env.getProperty("migration.mode", "dry-run"),
                    env.getRequiredProperty("migration.run-id"),
                    Integer.parseInt(env.getProperty("migration.limit", "1")));
        }
    }
}
