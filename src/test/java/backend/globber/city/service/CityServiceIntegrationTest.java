package backend.globber.city.service;

import backend.globber.city.controller.dto.RecommendResponse;
import backend.globber.city.domain.City;
import backend.globber.city.repository.CityRepository;
import backend.globber.city.repository.cache.RecommendedCityListRepository;
import backend.globber.support.PostgresTestConfig;
import backend.globber.support.RedisTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ContextConfiguration(initializers = RedisTestConfig.Initializer.class)
@Import({RedisTestConfig.class, PostgresTestConfig.class})
class CityServiceIntegrationTest {

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private RecommendedCityListRepository recommendedCityListRepository;

    @Autowired
    private CityService cityService;

    @BeforeEach
    void setUp() {
        // DB에 20개 City 데이터 삽입
        List<City> cities = IntStream.rangeClosed(1, 20)
                .mapToObj(i -> {
                    City c = new City();
                    try {
                        var cityNameField = City.class.getDeclaredField("cityName");
                        cityNameField.setAccessible(true);
                        cityNameField.set(c, "City" + i);

                        var countryField = City.class.getDeclaredField("countryName");
                        countryField.setAccessible(true);
                        countryField.set(c, "Country" + i);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    return c;
                }).toList();
        cityRepository.saveAll(cities);
    }

    @Test
    @DisplayName("캐시에 없으면 DB에서 조회 후 Redis에 저장된다")
    void testCacheMiss() {
        // when
        RecommendResponse result = cityService.getRecommendedCities();

        // then
        assertThat(result.cityResponseList()).hasSize(20);

        // Redis에 데이터 저장되었는지 확인
        RecommendResponse cached = recommendedCityListRepository.getRecommendedCities("recommended:city:list");
        assertThat(cached.cityResponseList()).hasSize(20);
    }

    @Test
    @DisplayName("캐시에 있으면 DB를 조회하지 않고 Redis에서 조회한다")
    void testCacheHit() {
        // given: 첫 번째 호출로 캐시 저장
        cityService.getRecommendedCities();

        // when: 두 번째 호출
        RecommendResponse result = cityService.getRecommendedCities();

        // then
        assertThat(result.cityResponseList()).hasSize(20);
        RecommendResponse cached = recommendedCityListRepository.getRecommendedCities("recommended:city:list");
        assertThat(cached.cityResponseList()).hasSize(20);
    }
}
