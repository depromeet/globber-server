package backend.globber.city.service;

import backend.globber.city.domain.City;
import backend.globber.city.repository.CityRepository;
import backend.globber.city.repository.cache.RankingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RedisWarmUpTest {

    @Mock
    private CityRepository cityRepository;

    @Mock
    private RankingRepository rankingRepository;

    @InjectMocks
    private RedisWarmUp redisWarmUp;

    @Test
    @DisplayName("기존 Redis 데이터가 있어도 전체 도시를 동기화하고 기존 점수는 유지한다")
    void synchronizeCitiesPreservesExistingScores() {
        City savedCity = City.builder()
                .cityId(1L)
                .cityName("기존 도시")
                .countryName("기존 국가")
                .lat(0.0)
                .lng(0.0)
                .countryCode("TST")
                .build();

        given(cityRepository.findByCountryNameAndCityName(anyString(), anyString()))
                .willReturn(Optional.of(savedCity));
        given(rankingRepository.getScore(any(City.class))).willReturn(100.0);

        redisWarmUp.synchronizeCities();

        int cityCount = CityInfo.values().length;
        verify(cityRepository, times(cityCount))
                .upsertCity(anyString(), anyString(), anyDouble(), anyDouble(), anyString());
        verify(rankingRepository, times(cityCount)).setScore(eq(savedCity), eq(100.0));
    }
}
