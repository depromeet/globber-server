package backend.migration;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ThumbnailBatchBootstrapTest {
    @Test
    void startsWithoutApiJpaRedisOrWebSecurity() {
        new ApplicationContextRunner().withUserConfiguration(ThumbnailBatch.class)
            .withPropertyValues("spring.datasource.url=jdbc:h2:mem:batch-bootstrap",
                "aws.s3.region=ap-northeast-2", "aws.credentials.access-key=test",
                "aws.credentials.secret-key=test")
            .run(context -> {
                assertThat(context).hasNotFailed().hasSingleBean(javax.sql.DataSource.class)
                    .hasSingleBean(software.amazon.awssdk.services.s3.S3Client.class)
                    .doesNotHaveBean(backend.globber.city.service.RedisWarmUp.class)
                    .doesNotHaveBean(backend.globber.auth.config.SecurityConfig.class)
                    .doesNotHaveBean(jakarta.persistence.EntityManagerFactory.class);
            });
    }
}
