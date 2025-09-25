package backend.globber.support;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ContextConfiguration(initializers = RedisTestConfig.Initializer.class)
@Import(RedisTestConfig.class)
class RedisIntegrationTest {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void testRedisSetAndGet() {
        redisTemplate.opsForValue().set("test1", "test2");

        String value = redisTemplate.opsForValue().get("test1");

        assertThat(value).isEqualTo("test2");
    }
}
