package backend.globber.auth.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Date;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 로그아웃·탈퇴한 액세스 토큰을 남은 만료 시간 동안 Redis에 보관해 다시 쓰지 못하게 한다.
 * 토큰 원문 대신 SHA-256 해시를 키로 쓴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccessTokenBlacklist {

    private static final String KEY_PREFIX = "auth:blacklist:";

    private final StringRedisTemplate redisTemplate;

    public void add(String rawToken, Date expiration) {
        long remainingMillis = expiration.getTime() - System.currentTimeMillis();
        if (remainingMillis <= 0) {
            return;
        }
        redisTemplate.opsForValue().set(keyOf(rawToken), "1", Duration.ofMillis(remainingMillis));
    }

    /**
     * Redis 장애 시에는 모든 인증이 막히지 않도록 false를 돌려주고 ERROR 로그를 남긴다.
     */
    public boolean contains(String rawToken) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(keyOf(rawToken)));
        } catch (RuntimeException e) {
            log.error("액세스 토큰 블랙리스트 조회 실패", e);
            return false;
        }
    }

    static String keyOf(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return KEY_PREFIX + HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("토큰 해시를 만들지 못했습니다.", e);
        }
    }
}
