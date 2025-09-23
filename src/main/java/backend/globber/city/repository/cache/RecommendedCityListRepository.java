package backend.globber.city.repository.cache;

import backend.globber.city.controller.dto.RecommendResponse;
import backend.globber.city.domain.City;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RecommendedCityListRepository {

    @Qualifier("recommendResponseRedisTemplate")
    private final RedisTemplate<String, RecommendResponse> redisTemplate;

    private static final long TTL = 30; // 30일

    public void saveRecommendedCities(String key, List<City> cities) {
        RecommendResponse list = RecommendResponse.toResponse(cities);
        redisTemplate.opsForValue().set(key, list, TTL, TimeUnit.DAYS);
    }

    public RecommendResponse getRecommendedCities(String key) {
        return redisTemplate.opsForValue().get(key);
    }
}