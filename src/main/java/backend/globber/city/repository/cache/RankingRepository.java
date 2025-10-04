package backend.globber.city.repository.cache;

import backend.globber.city.domain.City;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
@RequiredArgsConstructor
public class RankingRepository {

    private final StringRedisTemplate zsetTemplate;

    @Qualifier("searchTopCityRedisTemplate")
    private final RedisTemplate<String, City> redisTemplate;

    private static final String RANKING_KEY = "search_ranking";
    private static final String CITY_DATA_KEY = "city:data";
    private static final String CITY_MEMBER_PREFIX = "city:";


    public void incrementScore(final City city) {
        if (city == null || city.getCityId() == null) return;
        String member = CITY_MEMBER_PREFIX + city.getCityId();

        zsetTemplate.opsForZSet().incrementScore(RANKING_KEY, member, 1);
        redisTemplate.opsForHash().put(CITY_DATA_KEY, city.getCityId().toString(), city);
    }

    public Double getScore(final City city) {
        String member = "city:" + city.getCityId();
        return zsetTemplate.opsForZSet().score(RANKING_KEY, member);
    }

    public Map<Long, Double> getScores(final List<City> cities) {
        Map<Long, Double> scores = new HashMap<>();
        for (City city : cities) {
            String member = "city:" + city.getCityId();
            Double score = zsetTemplate.opsForZSet().score(RANKING_KEY, member);
            scores.put(city.getCityId(), score != null ? score : 0.0);
        }
        return scores;
    }

    public List<City> getTopCities(final int limit) {
        if (limit <= 0) return List.of();
        Set<String> members = zsetTemplate.opsForZSet()
                .reverseRange(RANKING_KEY, 0, limit - 1);

        if (members == null || members.isEmpty()) {
            return List.of();
        }

        return members.stream()
                .map(member -> {
                    String cityId = member.replace(CITY_MEMBER_PREFIX, "");
                    return (City) redisTemplate.opsForHash().get(CITY_DATA_KEY, cityId);
                })
                .filter(Objects::nonNull)
                .toList();
    }
}

