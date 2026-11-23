package backend.globber.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@TestConfiguration
@Testcontainers
public class RedisTestConfig {

    private static final int REDIS_PORT = 6379;

    @Container
    private static final GenericContainer<?> redisContainer =
            new GenericContainer<>("redis:latest")
                    .withExposedPorts(REDIS_PORT)
                    .waitingFor(Wait.forListeningPort());

    static {
        redisContainer.start();
    }

    public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext context) {
            TestPropertyValues.of(
                    "spring.data.redis.host=" + redisContainer.getHost(),
                    "spring.data.redis.port=" + redisContainer.getMappedPort(REDIS_PORT)
            ).applyTo(context.getEnvironment());
            clearCacheEntries();
        }

        // 컨텍스트마다 DB를 새로 만들어(ddl-auto: create) id가 다시 1부터 시작하므로,
        // 다른 컨텍스트가 같은 Redis에 남긴 캐시(cache:*)가 새 데이터와 섞이지 않게 지운다.
        private static void clearCacheEntries() {
            try {
                var result = redisContainer.execInContainer("redis-cli", "EVAL",
                        "for _, key in ipairs(redis.call('KEYS', 'cache:*')) do redis.call('DEL', key) end return 1", "0");
                if (result.getExitCode() != 0) {
                    throw new IllegalStateException("Redis 캐시 정리 실패: " + result.getStderr());
                }
            } catch (java.io.IOException e) {
                throw new IllegalStateException("Redis 캐시 정리 실패", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Redis 캐시 정리 중단", e);
            }
        }
    }
}
