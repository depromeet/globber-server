package backend.globber.auth.util;

import static org.assertj.core.api.Assertions.assertThat;

import backend.globber.city.service.RedisWarmUp;
import backend.globber.support.PostgresTestConfig;
import backend.globber.support.RedisTestConfig;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ContextConfiguration(initializers = RedisTestConfig.Initializer.class)
@Import({PostgresTestConfig.class, RedisTestConfig.class})
class AccessTokenBlacklistTest {

    @MockitoBean
    private RedisWarmUp redisWarmUp;

    @Autowired
    private AccessTokenBlacklist blacklist;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    @DisplayName("남은 만료 시간만큼만 보관하고, 원문이 아니라 해시를 키로 쓴다")
    void add_storesHashWithRemainingTtl() {
        // given
        String token = "token-" + UUID.randomUUID();
        Date expiration = new Date(System.currentTimeMillis() + 120_000);

        // when
        blacklist.add(token, expiration);

        // then
        assertThat(blacklist.contains(token)).isTrue();
        assertThat(redisTemplate.keys("auth:blacklist:*")).noneMatch(key -> key.contains(token));
        String key = AccessTokenBlacklist.keyOf(token);
        assertThat(redisTemplate.getExpire(key)).isBetween(1L, 120L);
    }

    @Test
    @DisplayName("이미 만료된 토큰은 보관하지 않는다")
    void add_expiredToken_notStored() {
        // given
        String token = "expired-" + UUID.randomUUID();

        // when
        blacklist.add(token, new Date(System.currentTimeMillis() - 1_000));

        // then
        assertThat(blacklist.contains(token)).isFalse();
    }

    @Test
    @DisplayName("블랙리스트에 없는 토큰은 false다")
    void contains_unknown_false() {
        // when & then
        assertThat(blacklist.contains("unknown-" + UUID.randomUUID())).isFalse();
    }
}
