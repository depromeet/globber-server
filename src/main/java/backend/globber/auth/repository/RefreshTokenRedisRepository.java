package backend.globber.auth.repository;

import backend.globber.auth.domain.RefreshToken;
import java.util.Optional;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RefreshTokenRedisRepository {

    private final RedisTemplate<String, RefreshToken> redisTemplate;

    // 리프레시 토큰 수명(밀리초). Redis 보관 기간을 JWT 만료·쿠키 Max-Age와 같게 맞춘다
    private final long refreshExpirationMillis;

    public RefreshTokenRedisRepository(
        @Qualifier("redisTemplate1") RedisTemplate<String, RefreshToken> redisTemplate,
        @Value("${jwt.refresh_expiration}") long refreshExpirationMillis) {
        this.redisTemplate = redisTemplate;
        this.refreshExpirationMillis = refreshExpirationMillis;
    }

    public void save(RefreshToken refreshToken) {
        redisTemplate.opsForValue().set(refreshToken.getEmail(), refreshToken, Duration.ofMillis(refreshExpirationMillis));
    }

    public Optional<RefreshToken> findById(String email) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(email));
    }

    public void delete(RefreshToken refreshToken) {
        redisTemplate.delete(refreshToken.getEmail());
    }
}
