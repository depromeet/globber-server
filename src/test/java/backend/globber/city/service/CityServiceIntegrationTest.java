package backend.globber.city.service;

import backend.globber.city.controller.dto.PagedRecommendResponse;
import backend.globber.city.domain.City;
import backend.globber.city.repository.CityRepository;
import backend.globber.city.repository.cache.RankingRepository;
import backend.globber.support.PostgresTestConfig;
import backend.globber.support.RedisTestConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ContextConfiguration(initializers = RedisTestConfig.Initializer.class)
@Import({PostgresTestConfig.class, RedisTestConfig.class})
class CityServiceIntegrationTest {

    @Autowired
    private CityService cityService;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private RankingRepository rankingRepository;


    @Test
    @DisplayName("인기 도시 조회 시 limit 개수만큼 반환된다")
    void getTopCities_limitTest() {
        List<City> cities = IntStream.rangeClosed(1, 50)
                .mapToObj(i -> new City(null, "City" + i, "Country" + i, 123.12, 123.12, "KOR"))
                .toList();
        cityRepository.saveAll(cities);
        cities.forEach(rankingRepository::incrementScore);

        Pageable pageable = PageRequest.of(0, 10);
        PagedRecommendResponse response = cityService.getTopCities(pageable);

        assertThat(response.cityResponseList()).hasSize(10);
        assertThat(response.totalElements()).isEqualTo(50);
        assertThat(response.totalPages()).isEqualTo(5);

    }


    @Test
    @DisplayName("Redis에 데이터가 없으면 DB에서 limit 개수 조회한다")
    void getTopCities_fallbackToDb() {
        List<City> cities = IntStream.rangeClosed(1, 50)
                .mapToObj(i -> new City(null, "City" + i, "Country" + i, 123.12, 123.12, "KOR"))
                .toList();
        cityRepository.saveAll(cities);

        Pageable pageable = PageRequest.of(0, 10);
        PagedRecommendResponse response = cityService.getTopCities(pageable);

        assertThat(response.cityResponseList()).hasSize(10);
        assertThat(response.totalElements()).isEqualTo(50);
        assertThat(response.totalPages()).isEqualTo(5);
    }
}
