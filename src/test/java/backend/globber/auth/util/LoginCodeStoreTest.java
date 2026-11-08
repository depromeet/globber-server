package backend.globber.auth.util;

import static org.assertj.core.api.Assertions.assertThat;

import backend.globber.auth.util.LoginCodeStore.LoginResult;
import backend.globber.city.service.RedisWarmUp;
import backend.globber.support.PostgresTestConfig;
import backend.globber.support.RedisTestConfig;
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
class LoginCodeStoreTest {

    @MockitoBean
    private RedisWarmUp redisWarmUp;

    @Autowired
    private LoginCodeStore store;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private final LoginResult result = new LoginResult("Bearer access", 7L, "abc123", true);

    @Test
    @DisplayName("발급한 code로 로그인 결과를 한 번만 꺼낼 수 있다")
    void issueAndConsumeOnce() {
        // given
        String code = store.issue(result);

        // when & then
        assertThat(store.consume(code)).contains(result);
        assertThat(store.consume(code)).isEmpty();
    }

    @Test
    @DisplayName("code는 추측하기 어려운 길이이고 60초 동안만 보관한다")
    void codeIsRandomAndShortLived() {
        // when
        String code = store.issue(result);

        // then
        assertThat(code).hasSizeGreaterThanOrEqualTo(32).isNotEqualTo(store.issue(result));
        assertThat(redisTemplate.getExpire("auth:login-code:" + code)).isBetween(1L, 60L);
    }

    @Test
    @DisplayName("없는 code나 빈 값은 비어 있다")
    void unknownCode_empty() {
        // when & then
        assertThat(store.consume("unknown")).isEmpty();
        assertThat(store.consume(null)).isEmpty();
        assertThat(store.consume("")).isEmpty();
    }
}
