package backend.globber.city.repository.cache;

import backend.globber.city.controller.dto.SearchResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

@Repository
public class CacheRepository {

    private final RedisTemplate<String, SearchResult> redisTemplate;

    public CacheRepository(
            @Qualifier("searchResultRedisTemplate") RedisTemplate<String, SearchResult> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public SearchResult get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void set(String key, SearchResult value, int ttlMinutes) {
        redisTemplate.opsForValue().set(key, value, ttlMinutes, TimeUnit.MINUTES);
    }
}
