package backend.globber.city.service;

import backend.globber.city.controller.dto.SearchResponse;
import backend.globber.city.controller.dto.SearchResult;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ContextConfiguration(initializers = RedisTestConfig.Initializer.class)
@Import({PostgresTestConfig.class, RedisTestConfig.class})
class SearchServiceIntegrationTest {

    @Autowired
    private SearchService searchService;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private RankingRepository rankingRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS pg_bigm");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_city_name_bigm ON city USING gin (city_name gin_bigm_ops)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_country_name_bigm ON city USING gin (country_name gin_bigm_ops)");

        cityRepository.deleteAll();
        cityRepository.saveAll(List.of(
                City.builder().cityName("뉴델리").countryName("인도")
                        .lat(28.6139).lng(77.2090).countryCode("IND").build(),
                City.builder().cityName("타지마할").countryName("인도")
                        .lat(27.1751).lng(78.0421).countryCode("IND").build(),
                City.builder().cityName("민우").countryName("인도")
                        .lat(28.7041).lng(77.1025).countryCode("IND").build(), // 임시 좌표
                City.builder().cityName("서울").countryName("대한민국")
                        .lat(37.5665).lng(126.9780).countryCode("KOR").build(),
                City.builder().cityName("인천").countryName("대한민국")
                        .lat(37.4563).lng(126.7052).countryCode("KOR").build(),
                City.builder().cityName("부산").countryName("대한민국")
                        .lat(35.1796).lng(129.0756).countryCode("KOR").build(),
                City.builder().cityName("베이징").countryName("중국")
                        .lat(39.9042).lng(116.4074).countryCode("CHN").build(),
                City.builder().cityName("상하이").countryName("중국")
                        .lat(31.2304).lng(121.4737).countryCode("CHN").build(),
                City.builder().cityName("홍콩").countryName("중국")
                        .lat(22.3193).lng(114.1694).countryCode("CHN").build(),
                City.builder().cityName("도쿄").countryName("일본")
                        .lat(35.6762).lng(139.6503).countryCode("JPN").build(),
                City.builder().cityName("오사카").countryName("일본")
                        .lat(34.6937).lng(135.5023).countryCode("JPN").build()
        ));
    }

    @Test
    @DisplayName("pg_bigm 인덱스가 생성되어 있다")
    void testPgBigmIndexExists() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_indexes WHERE indexname = 'idx_city_name_bigm'",
                Integer.class
        );

        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("검색 결과는 캐시에 저장되어 이후 캐시 히트 시 DB를 조회하지 않는다")
    void testSearchCacheHit() {
        // given
        searchService.search("일본");

        // when
        SearchResult cachedResult = searchService.search("일본");

        // then
        assertThat(cachedResult.cities()).hasSize(2); // 도쿄, 오사카
    }

    @Test
    @DisplayName("최종 선택이 반영되면 랭킹 점수가 증가한다")
    void testRecordSelection() {
        // given
        City city = new City(1L, "뉴델리", "인도", 123.12, 123.12, "IND");
        searchService.recordSelection(city);

        // when
        Double score = rankingRepository.getScore(city);

        // then
        assertThat(score).isNotNull();
        assertThat(score).isGreaterThan(0.0);
    }

    @Test
    @DisplayName("검색어 '인도' 입력 시 유사도가 높은 도시가 나온다 (뉴델리, 뭄바이)")
    void testSimilarityRankingMultipleCities() {
        // when
        SearchResult result = searchService.search("인도");

        // then
        List<String> names = result.cities().stream()
                .map(SearchResponse::cityName)
                .toList();

        System.out.println(names);
        assertThat(names).contains("뉴델리", "민우", "타지마할");
        assertThat(names).doesNotContain("서울");
    }

    @Test
    @DisplayName("인기도 점수가 높은 도시는 유사도가 같을 경우 더 앞에 정렬된다 (뉴델리 vs 뭄바이)")
    void testPopularityRankingWithMultipleCities() {

        City city = new City(1L, "뉴델리", "인도", 123.12, 123.12, "IND");
        // given
        searchService.recordSelection(city);
        searchService.recordSelection(city);
        searchService.recordSelection(city);

        // when
        SearchResult result = searchService.search("인도");

        // then
        List<String> names = result.cities().stream()
                .map(SearchResponse::cityName)
                .toList();

        // 뭄바이 인기도 점수를 올렸으므로 뉴델리보다 앞에 와야 함
        assertThat(names.indexOf("뭄바이"))
                .isLessThan(names.indexOf("뉴델리"));

        assertThat(names.indexOf("뭄바이"))
                .isLessThan(names.indexOf("뉴델리"));
    }

    @Test
    @DisplayName("인기도 점수가 높은 도시는 유사도가 같을 경우 더 앞에 정렬된다 2 (서울 vs 부산 vs 인천)")
    void testPopularityRankingWithMultipleCities2() {
        // given
        City city = new City(1L, "서울", "대한민국", 123.12, 123.12, "KOR");
        searchService.recordSelection(city);
        searchService.recordSelection(city);
        searchService.recordSelection(city);

        // when
        SearchResult result = searchService.search("대한민국");

        // then
        List<String> names = result.cities().stream()
                .map(SearchResponse::cityName)
                .toList();

        assertThat(names.indexOf("서울"))
                .isLessThan(names.indexOf("부산"));

        assertThat(names.indexOf("서울"))
                .isLessThan(names.indexOf("인천"));
    }

    @Test
    @DisplayName("검색 결과는 최대 100개까지만 반환된다")
    void testResultLimit() {
        // given
        for (int i = 0; i < 200; i++) {
            cityRepository.save(
                    City.builder()
                            .cityName("테스트도시" + i)
                            .countryName("테스트국가")
                            .lat(0.0)
                            .lng(0.0)
                            .countryCode("TST")
                            .build()
            );
        }

        // when
        SearchResult result = searchService.search("테스트");

        // then
        assertThat(result.cities().size()).isLessThanOrEqualTo(100);
    }

    @Test
    @DisplayName("레디스에 40개 도시를 저장하고 인기도 순으로 Top 20이 반환된다")
    void testPopularCitiesTop20() {
        // given: DB 초기화 후 40개 저장
        cityRepository.deleteAll();

        List<City> cities = IntStream.rangeClosed(1, 40)
                .mapToObj(i -> new City(null, "City" + i, "Country" + i, 123.12, 123.12, "KOR"))
                .toList();
        cityRepository.saveAll(cities);

        City city10 = cities.get(9);
        City city5 = cities.get(4);
        City city20 = cities.get(19);


        IntStream.range(0, 40).forEach(i -> searchService.recordSelection(cities.get(i)));

        IntStream.range(0, 10).forEach(i -> searchService.recordSelection(city10));
        IntStream.range(0, 5).forEach(i -> searchService.recordSelection(city5));
        IntStream.range(0, 3).forEach(i -> searchService.recordSelection(city20));

        // when
        SearchResult result = searchService.getPopularCities(20);

        // then: Top-20 사이즈 확인
        assertThat(result.cities()).hasSize(20);

        // 인기도 순서 검증
        List<String> names = result.cities().stream()
                .map(SearchResponse::cityName)
                .toList();

        assertThat(names.get(0)).isEqualTo("City10");
        assertThat(names.get(1)).isEqualTo("City5");
        assertThat(names.get(2)).isEqualTo("City20");
    }


}
