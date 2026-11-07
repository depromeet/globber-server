package backend.globber.city.service;

import backend.globber.city.controller.dto.RecommendResponse;
import backend.globber.city.domain.City;
import backend.globber.city.repository.CityRepository;
import backend.globber.city.repository.cache.RankingRepository;
import backend.globber.support.PostgresTestConfig;
import backend.globber.support.RedisTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ContextConfiguration(initializers = RedisTestConfig.Initializer.class)
@Import({PostgresTestConfig.class, RedisTestConfig.class})
@Transactional
class CityServiceIntegrationTest {

    @MockitoBean
    private RedisWarmUp redisWarmUp;

    @Autowired
    private CityService cityService;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private RankingRepository rankingRepository;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // Redis 컨테이너는 모든 테스트 컨텍스트가 함께 쓰므로, 다른 테스트가 남긴 인기 도시 랭킹을 지우고 시작한다
    @BeforeEach
    void clearRanking() {
        stringRedisTemplate.delete(List.of("search_ranking", "city:data"));
    }


    @Test
    @DisplayName("인기 도시 조회 시 limit 개수만큼 반환된다")
    void getTopCities_limitTest() {
        List<City> cities = IntStream.rangeClosed(1, 50).mapToObj(i -> new City(null, "City" + i, "Country" + i, 123.12, 123.12, "KOR")).toList();
        cityRepository.saveAll(cities);
        cities.forEach(rankingRepository::incrementScore);

        int limit = 10;

        RecommendResponse response = cityService.getTopCities(limit);

        assertThat(response.cityResponseList()).hasSize(limit);
    }


    @Test
    @DisplayName("Redis에 데이터가 없으면 DB에서 limit 개수 조회한다")
    void getTopCities_fallbackToDb() {
        List<City> cities = IntStream.rangeClosed(1, 50).mapToObj(i -> new City(null, "City" + i, "Country" + i, 123.12, 123.12, "KOR")).toList();
        cityRepository.saveAll(cities);

        int limit = 5;

        RecommendResponse response = cityService.getTopCities(limit);

        assertThat(response.cityResponseList()).hasSize(limit);
    }

    @Test
    @DisplayName("Redis에 데이터가 없으면 DB에서 limit 개수 조회한다 (모든 필드 검증)")
    void getTopCities_fallbackToDb_fullFields() {
        // given
        List<City> cities = IntStream.rangeClosed(1, 5).mapToObj(i -> City.builder().cityId(null).cityName("City" + i).countryName("Country" + i).lat(123.12).lng(456.78).countryCode("KOR").build()).toList();
        cityRepository.saveAll(cities);


        int limit = 5;

        // when
        RecommendResponse response = cityService.getTopCities(limit);

        // then
        assertThat(response.cityResponseList()).hasSize(limit);

        // DB는 다른 테스트와 함께 쓰므로 어떤 도시가 뽑힐지는 단정하지 않고, 응답의 모든 필드가 DB 값과 같은지 확인한다
        assertThat(response.cityResponseList()).allSatisfy(dto -> {
            City saved = cityRepository.findById(dto.cityId()).orElseThrow();
            assertThat(dto.cityName()).isEqualTo(saved.getCityName());
            assertThat(dto.countryName()).isEqualTo(saved.getCountryName());
            assertThat(dto.lat()).isEqualTo(saved.getLat());
            assertThat(dto.lng()).isEqualTo(saved.getLng());
            assertThat(dto.countryCode()).isEqualTo(saved.getCountryCode());
        });
    }

}
