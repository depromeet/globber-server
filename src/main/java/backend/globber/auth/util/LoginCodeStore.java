package backend.globber.auth.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 로그인 성공 결과를 일회용 code로 잠시 보관한다. 리다이렉트 URL에는 code만 싣고,
 * 프론트가 POST /auth/token으로 code를 한 번 교환해 결과를 받는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginCodeStore {

    private static final String KEY_PREFIX = "auth:login-code:";
    private static final Duration TTL = Duration.ofSeconds(60);
    private static final int CODE_BYTES = 32;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    public record LoginResult(String accessToken, Long memberId, String uuid, boolean firstLogin) {
    }

    public String issue(LoginResult result) {
        byte[] bytes = new byte[CODE_BYTES];
        secureRandom.nextBytes(bytes);
        String code = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        try {
            redisTemplate.opsForValue().set(KEY_PREFIX + code, objectMapper.writeValueAsString(result), TTL);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("로그인 결과를 저장하지 못했습니다.", e);
        }
        return code;
    }

    /**
     * code로 로그인 결과를 꺼낸다. 조회와 삭제를 한 번에 해서 같은 code는 한 번만 쓸 수 있다.
     */
    public Optional<LoginResult> consume(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        String json = redisTemplate.opsForValue().getAndDelete(KEY_PREFIX + code);
        if (json == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, LoginResult.class));
        } catch (JsonProcessingException e) {
            log.error("로그인 결과를 읽지 못함", e);
            return Optional.empty();
        }
    }
}
