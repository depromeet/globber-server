package backend.globber.support;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(PostgresTestConfig.class)
class PostgresIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void testPostgresConnection() throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // 간단한 쿼리 실행
            stmt.execute("CREATE TABLE IF NOT EXISTS test_table (id SERIAL PRIMARY KEY, name VARCHAR(255))");
            stmt.execute("INSERT INTO test_table (name) VALUES ('hello')");
            stmt.execute("INSERT INTO test_table (name) VALUES ('world')");

            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM test_table");
            rs.next();
            int count = rs.getInt(1);

            assertThat(count).isEqualTo(2);
        }
    }
}