package backend.globber.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;

@Testcontainers
@TestConfiguration
public class PostgresTestConfig {

    @Container
    private static final PostgreSQLContainer<?> postgresContainer =
            new PostgreSQLContainer<>("postgres:15.3")
                    .withDatabaseName("mydb")
                    .withUsername("testuser")
                    .withPassword("testpassword")
                    .withCommand("-c timezone=Asia/Seoul");

    static {
        postgresContainer.start();
    }

    @Bean
    @Primary
    public DataSource dataSource() {
        return DataSourceBuilder.create()
                .url(postgresContainer.getJdbcUrl())
                .username(postgresContainer.getUsername())
                .password(postgresContainer.getPassword())
                .driverClassName(postgresContainer.getDriverClassName())
                .build();
    }
}
