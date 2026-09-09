package backend.globber.city.service;

import backend.globber.city.domain.City;
import backend.globber.city.repository.CityRepository;
import backend.globber.city.repository.cache.RankingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisWarmUp {

    private final CityRepository cityRepository;
    private final RankingRepository rankingRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void synchronizeCities() {
        try {
            int score = CityInfo.values().length;

            for (CityInfo info : CityInfo.values()) {
                City city = City.builder()
                        .cityName(info.getCityName())
                        .countryName(info.getCountryName())
                        .lat(info.getLat())
                        .lng(info.getLng())
                        .countryCode(info.getCountryCode())
                        .build();

                cityRepository.upsertCity(
                        city.getCityName(),
                        city.getCountryName(),
                        city.getLat(),
                        city.getLng(),
                        city.getCountryCode()
                );

                int currentScore = score;
                cityRepository.findByCountryNameAndCityName(city.getCountryName(), city.getCityName())
                        .ifPresent(savedCity -> {
                            Double existingScore = rankingRepository.getScore(savedCity);
                            double synchronizedScore = existingScore != null ? existingScore : currentScore;

                            rankingRepository.setScore(savedCity, synchronizedScore);
                            log.debug("'{}' ({}) Redis에 점수 {}로 동기화 완료",
                                    savedCity.getCityName(), savedCity.getCountryName(), synchronizedScore);
                        });
                score--;
            }

            log.info("도시 데이터를 DB 및 Redis와 성공적으로 동기화했습니다. (총 {}건)", CityInfo.values().length);

        } catch (Exception e) {
            log.error("도시 데이터 동기화 중 오류 발생: {}", e.getMessage(), e);
        }
    }
}
