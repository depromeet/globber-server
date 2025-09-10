package backend.globber.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Repository
@Slf4j
public class MailCertRedisRepository {
    private final RedisTemplate<String, String> redisTemplate;

    public MailCertRedisRepository(@Qualifier("redisTemplate2") RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void save(String email, String uuid) {
        log.info(email+" "+uuid);
        redisTemplate.opsForValue().set(email, uuid, 600, TimeUnit.SECONDS);
    }

    public Optional<String> findByEmail(String email) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(email));
    }

    public void update(String email, String uuid) {
        redisTemplate.opsForValue().set(email, uuid, 600, TimeUnit.SECONDS);
    }
    public void delete(String email) {
        redisTemplate.delete(email);
    }
}
