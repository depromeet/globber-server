package backend.globber.city.repository.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
@RequiredArgsConstructor
public class RankingRepository {

    private final StringRedisTemplate redisTemplate;

    private static final String RANKING_KEY = "search_ranking";

    /**
     * 특정 키워드의 점수를 1 증가시킴
     */
    public void incrementScore(String keyword) {
        redisTemplate.opsForZSet().incrementScore(RANKING_KEY, keyword, 1);
    }

    public Double getScore(String keyword) {
        return redisTemplate.opsForZSet().score(RANKING_KEY, keyword);
    }

    /**
     * 여러 키워드의 점수를 한 번에 조회
     */
    public Map<String, Double> getScores(List<String> keywords) {
        Map<String, Double> scores = new HashMap<>();
        for (String keyword : keywords) {
            Double score = redisTemplate.opsForZSet().score(RANKING_KEY, keyword);
            scores.put(keyword, score != null ? score : 0.0);
        }
        return scores;
    }

    public List<String> getTopCities(int limit) {
        return Optional.ofNullable(
                        redisTemplate.opsForZSet().reverseRange(RANKING_KEY, 0, limit - 1)
                ).orElse(Set.of())
                .stream()
                .toList();
    }
}
