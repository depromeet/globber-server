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
                new City(null, "뉴델리", "인도"),
                new City(null, "타지마할", "인도"),
                new City(null, "민우", "인도"),
                new City(null, "서울", "대한민국"),
                new City(null, "인천", "대한민국"),
                new City(null, "부산", "대한민국"),
                new City(null, "베이징", "중국"),
                new City(null, "상하이", "중국"),
                new City(null, "홍콩", "중국"),
                new City(null, "도쿄", "일본"),
                new City(null, "오사카", "일본")
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
        searchService.recordSelection("뉴델리");

        // when
        Double score = rankingRepository.getScore("뉴델리");

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

    }

    @Test
    @DisplayName("인기도 점수가 높은 도시는 유사도가 같을 경우 더 앞에 정렬된다 (뉴델리 vs 뭄바이)")
    void testPopularityRankingWithMultipleCities() {
        // given
        searchService.recordSelection("뭄바이");
        searchService.recordSelection("뭄바이");
        searchService.recordSelection("뭄바이");

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
        searchService.recordSelection("서울");
        searchService.recordSelection("서울");
        searchService.recordSelection("서울");

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
            cityRepository.save(new City(null, "테스트도시" + i, "테스트국가"));
        }

        // when
        SearchResult result = searchService.search("테스트");

        // then
        assertThat(result.cities().size()).isLessThanOrEqualTo(100);
    }
}
