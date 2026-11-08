package backend.globber.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class PostgresTestConfigTest {

    @Test
    @DisplayName("테스트 DB는 최대 연결 수를 늘리고, 컨텍스트별 연결 풀은 작게 쓴다")
    void connectionLimits() {
        // given
        DataSource dataSource = new PostgresTestConfig().dataSource();

        // when
        Integer maxConnections = new JdbcTemplate(dataSource)
            .queryForObject("select current_setting('max_connections')::int", Integer.class);

        // then
        assertThat(maxConnections).isGreaterThanOrEqualTo(300);
        assertThat(dataSource).isInstanceOf(HikariDataSource.class);
        assertThat(((HikariDataSource) dataSource).getMaximumPoolSize()).isLessThanOrEqualTo(5);
        ((HikariDataSource) dataSource).close();
    }
}
