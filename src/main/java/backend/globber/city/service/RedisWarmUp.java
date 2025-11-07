package backend.globber.city.service;

import backend.globber.city.domain.City;
import backend.globber.city.repository.CityRepository;
import backend.globber.city.repository.cache.RankingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisWarmUp {

    private final CityRepository cityRepository;
    private final RankingRepository rankingRepository;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String RANKING_KEY = "search_ranking";
    private static final String CITY_DATA_KEY = "city:data";

    @EventListener(ApplicationReadyEvent.class)
    public void restoreIfEmpty() {
        try {
            Long rankingCount = stringRedisTemplate.opsForZSet().zCard(RANKING_KEY);
            Long cityDataCount = stringRedisTemplate.opsForHash().size(CITY_DATA_KEY);

            boolean hasRankingData = rankingCount != null && rankingCount > 0;
            boolean hasCityData = cityDataCount > 0;

            if (hasRankingData && hasCityData) {
                log.info("Redis에 이미 데이터가 존재합니다. 초기화를 건너뜁니다. (rankingCount={}, cityDataCount={})",
                        rankingCount, cityDataCount);
                return;
            }

            log.warn("Redis가 비어 있습니다. Top20 도시 데이터를 DB 및 Redis에 복원합니다.");
            int score = CityInfo.values().length;

            for (CityInfo info : CityInfo.values()) {
                // City 엔티티 생성
                City city = City.builder()
                        .cityName(info.getCityName())
                        .countryName(info.getCountryName())
                        .lat(info.getLat())
                        .lng(info.getLng())
                        .countryCode(info.getCountryCode())
                        .build();

                // DB에 upsert
                cityRepository.upsertCity(
                        city.getCityName(),
                        city.getCountryName(),
                        city.getLat(),
                        city.getLng(),
                        city.getCountryCode()
                );

                // Redis 랭킹 점수 등록
                int currentScore = score;
                cityRepository.findByCountryNameAndCityName(city.getCountryName(), city.getCityName())
                        .ifPresent(savedCity -> {
                            rankingRepository.setScore(savedCity, currentScore);
                            log.debug("'{}' ({}) Redis에 점수 {}로 등록 완료",
                                    savedCity.getCityName(), savedCity.getCountryName(), currentScore);
                        });
                score--;
            }

            log.info("Top20 도시 데이터를 Redis에 성공적으로 복원했습니다. (총 {}건)", CityInfo.values().length);

        } catch (Exception e) {
            log.error("Redis 복원 중 오류 발생: {}", e.getMessage(), e);
        }
    }
}
