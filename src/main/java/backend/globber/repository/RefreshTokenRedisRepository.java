package backend.globber.repository;

import backend.globber.domain.RefreshToken;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Repository
public class RefreshTokenRedisRepository {
    private final RedisTemplate<String, RefreshToken> redisTemplate;

    public RefreshTokenRedisRepository(@Qualifier("redisTemplate1") RedisTemplate<String, RefreshToken> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // 시간제한 10분
    public void save(RefreshToken refreshToken) {
        redisTemplate.opsForValue().set(refreshToken.getEmail(), refreshToken, 30, TimeUnit.DAYS);
    }

    public Optional<RefreshToken> findById(String email) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(email));
    }

    public void delete(RefreshToken refreshToken) {
        redisTemplate.delete(refreshToken.getEmail());
    }
}
