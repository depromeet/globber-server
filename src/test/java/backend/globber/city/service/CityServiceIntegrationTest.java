package backend.globber.city.service;

import backend.globber.city.controller.dto.CityResponse;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ContextConfiguration(initializers = RedisTestConfig.Initializer.class)
@Import({PostgresTestConfig.class, RedisTestConfig.class})
@Transactional
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
        List<City> cities = IntStream.rangeClosed(1, 50).mapToObj(i -> new City(null, "City" + i, "Country" + i, 123.12, 123.12, "KOR")).toList();
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
        List<City> cities = IntStream.rangeClosed(1, 50).mapToObj(i -> new City(null, "City" + i, "Country" + i, 123.12, 123.12, "KOR")).toList();
        cityRepository.saveAll(cities);

        Pageable pageable = PageRequest.of(0, 10);
        PagedRecommendResponse response = cityService.getTopCities(pageable);

        assertThat(response.cityResponseList()).hasSize(10);
        assertThat(response.totalElements()).isEqualTo(50);
        assertThat(response.totalPages()).isEqualTo(5);
    }

    @Test
    @DisplayName("Redis에 데이터가 없으면 DB에서 페이징된 결과와 모든 필드가 올바르게 조회된다")
    void getTopCities_fallbackToDb_fullFields_withPaging() {
        // given
        List<City> cities = IntStream.rangeClosed(1, 15)
                .mapToObj(i -> City.builder()
                        .cityId(null)
                        .cityName("City" + i)
                        .countryName("Country" + i)
                        .lat(123.12)
                        .lng(456.78)
                        .countryCode("KOR")
                        .build())
                .toList();
        cityRepository.saveAll(cities);

        Pageable pageable = PageRequest.of(1, 5); // 2번째 페이지, size=5

        // when
        PagedRecommendResponse response = cityService.getTopCities(pageable);

        // then
        assertThat(response.cityResponseList()).hasSize(5);
        assertThat(response.totalElements()).isEqualTo(15);
        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(5);

        // 필드 값 검증 (해당 페이지 데이터)
        var expectedCities = cities.subList(5, 10);
        var expectedNames = expectedCities.stream().map(City::getCityName).toList();
        var expectedCountries = expectedCities.stream().map(City::getCountryName).toList();

        assertThat(response.cityResponseList().stream().map(CityResponse::cityName))
                .containsExactlyInAnyOrderElementsOf(expectedNames);

        assertThat(response.cityResponseList().stream().map(CityResponse::countryName))
                .containsExactlyInAnyOrderElementsOf(expectedCountries);

        assertThat(response.cityResponseList()).allSatisfy(dto -> {
            assertThat(dto.lat()).isEqualTo(123.12);
            assertThat(dto.lng()).isEqualTo(456.78);
            assertThat(dto.countryCode()).isEqualTo("KOR");
        });
    }


}
