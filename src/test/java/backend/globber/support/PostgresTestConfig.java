package backend.globber.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import java.time.Duration;
import java.util.Map;

@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestConfig {

    private static final int MAX_POOL_SIZE = 5;

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
                    .withInitScript("schema.sql")
                    // 테스트 클래스마다 Spring 컨텍스트가 따로 캐시되어 연결이 쌓이므로 최대 연결 수를 늘린다
                    .withCommand("postgres", "-c", "fsync=off", "-c", "max_connections=300");

    static {
        postgres.start();
    }

    @Bean
    @Primary
    public DataSource dataSource() {
        HikariDataSource dataSource = DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .url(postgres.getJdbcUrl())
                .username(postgres.getUsername())
                .password(postgres.getPassword())
                .driverClassName(postgres.getDriverClassName())
                .build();
        // 캐시된 컨텍스트마다 연결을 10개씩 잡아 두지 않도록 작게, 쓰지 않는 연결은 바로 반납한다
        dataSource.setMaximumPoolSize(MAX_POOL_SIZE);
        dataSource.setMinimumIdle(0);
        dataSource.setIdleTimeout(10_000);
        return dataSource;
    }
}
