package backend.globber.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;
import java.time.Duration;
import java.util.Map;

@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestConfig {

    private static final DockerImageName IMAGE = DockerImageName
            .parse("symdit/postgresql15-bigm:latest")
            .asCompatibleSubstituteFor("postgres");

    private static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(IMAGE)
                    .withDatabaseName("mydb")
                    .withUsername("testuser")
                    .withPassword("testpassword")
                    .withStartupTimeout(Duration.ofMinutes(3))
                    .withTmpFs(Map.of("/var/lib/postgresql/data", "rw"))
                    .withInitScript("schema.sql");

    static {
        postgres.start();
    }

    @Bean
    @Primary
    public DataSource dataSource() {
        return DataSourceBuilder.create()
                .url(postgres.getJdbcUrl())
                .username(postgres.getUsername())
                .password(postgres.getPassword())
                .driverClassName(postgres.getDriverClassName())
                .build();
    }
}
