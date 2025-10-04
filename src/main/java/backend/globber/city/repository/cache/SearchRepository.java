package backend.globber.city.repository.cache;

import backend.globber.city.controller.dto.SearchResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

@Repository
public class SearchRepository {

    private final RedisTemplate<String, SearchResult> redisTemplate;

    public SearchRepository(
            @Qualifier("searchResultRedisTemplate") RedisTemplate<String, SearchResult> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public SearchResult get(final String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void set(final String key, final SearchResult value, final int ttlMinutes) {
        redisTemplate.opsForValue().set(key, value, ttlMinutes, TimeUnit.MINUTES);
    }
}
