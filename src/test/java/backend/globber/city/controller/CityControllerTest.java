package backend.globber.city.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import backend.globber.city.domain.City;
import backend.globber.city.repository.CityRepository;
import backend.globber.city.service.RedisWarmUp;
import backend.globber.support.PostgresTestConfig;
import backend.globber.support.RedisTestConfig;
import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.DigestUtils;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = RedisTestConfig.Initializer.class)
@Import({PostgresTestConfig.class, RedisTestConfig.class})
class CityControllerTest {

    private static final String CITIES_URL = "/api/v1/cities";

    @MockitoBean
    private RedisWarmUp redisWarmUp;

    @LocalServerPort
    private int port;

    @Autowired
    private CityRepository cityRepository;

    // 테스트마다 다른 검색어를 써서 Redis 검색 캐시와 기존 데이터의 간섭을 막는다.
    private String token;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        token = "검사" + UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    @DisplayName("검색어 파라미터가 없으면 400과 실패 응답을 반환한다")
    void search_missingKeyword_badRequest() {
        // given
        // when & then
        RestAssured.given()
            .when()
            .get(CITIES_URL)
            .then()
            .statusCode(400)
            .body("status", equalTo("fail"))
            .body("data", equalTo("검색어를 입력해 주세요."));
    }

    @Test
    @DisplayName("API 문서에서 도시 검색의 keyword 파라미터를 필수로 표시한다")
    void apiDocs_keywordRequired() {
        // given
        // when & then
        RestAssured.given()
            .when()
            .get("/v3/api-docs")
            .then()
            .statusCode(200)
            .body("paths['/api/v1/cities'].get.parameters.find { it.name == 'keyword' }.required", equalTo(true));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @DisplayName("검색어가 비어 있거나 공백뿐이면 400과 실패 응답을 반환한다")
    void search_blankKeyword_badRequest(String keyword) {
        // given
        // when & then
        RestAssured.given()
            .queryParam("keyword", keyword)
            .when()
            .get(CITIES_URL)
            .then()
            .statusCode(400)
            .body("status", equalTo("fail"));
    }

    @Test
    @DisplayName("검색어의 앞뒤 공백을 제거해 검색하고 ApiResponse로 감싸 반환한다")
    void search_stripsKeywordAndWrapsResponse() {
        // given
        cityRepository.save(city(token + "시티"));

        // when & then
        RestAssured.given()
            .queryParam("keyword", "  " + token + "  ")
            .when()
            .get(CITIES_URL)
            .then()
            .statusCode(200)
            .body("status", equalTo("success"))
            .body("data.cities", hasSize(1))
            .body("data.cities[0].cityName", equalTo(token + "시티"));
    }

    @Test
    @DisplayName("검색 결과는 최대 20개까지만 반환한다")
    void search_limitsResultsTo20() {
        // given
        cityRepository.saveAll(IntStream.rangeClosed(1, 25)
            .mapToObj(i -> city(token + i))
            .toList());

        // when & then
        RestAssured.given()
            .queryParam("keyword", token)
            .when()
            .get(CITIES_URL)
            .then()
            .statusCode(200)
            .body("status", equalTo("success"))
            .body("data.cities", hasSize(20))
            .body("data.cities.cityName", everyItem(containsString(token)));
    }

    @Test
    @DisplayName("인기 여행지 조회는 ApiResponse로 감싸고 기본 보안 캐시 헤더를 유지한다")
    void favorites_wrapsResponseAndKeepsNoCacheHeaders() {
        // given
        cityRepository.save(city(token + "인기"));

        // when & then
        RestAssured.given()
            .queryParam("limit", 5)
            .when()
            .get(CITIES_URL + "/favorites")
            .then()
            .statusCode(200)
            .body("status", equalTo("success"))
            .body("data.cityResponseList", notNullValue())
            .header("Cache-Control", containsString("no-store"))
            .header("Pragma", equalTo("no-cache"));
    }

    @Test
    @DisplayName("전체 도시 조회는 ApiResponse의 data 배열로 반환하고 1일 공개 캐시 헤더와 ETag를 붙인다")
    void all_returnsCitiesWithCacheHeaders() {
        // given
        cityRepository.save(city(token + "전체"));

        // when & then
        RestAssured.given()
            .when()
            .get(CITIES_URL + "/all")
            .then()
            .statusCode(200)
            .body("status", equalTo("success"))
            .body("data.cityName", hasItem(token + "전체"))
            .header("Cache-Control", containsString("public"))
            .header("Cache-Control", containsString("max-age=86400"))
            .header("Cache-Control", not(containsString("no-store")))
            .header("Cache-Control", not(containsString("no-cache")))
            .header("Pragma", nullValue())
            .header("Expires", nullValue())
            .header("ETag", notNullValue());
    }

    @Test
    @DisplayName("전체 도시 조회의 ETag는 실제 응답 본문 바이트의 md5다")
    void all_eTagIsMd5OfResponseBody() {
        // given
        cityRepository.save(city(token + "본문"));

        // when
        ExtractableResponse<Response> response = getAll();

        // then
        String expected = "\"" + DigestUtils.md5DigestAsHex(response.asByteArray()) + "\"";
        assertThat(response.header("ETag")).isEqualTo(expected);
    }

    @Test
    @DisplayName("전체 도시 조회는 UTF-16BE를 요청해도 UTF-8 본문으로 응답해 ETag가 본문 바이트의 md5와 같다")
    void all_utf16Accept_eTagMatchesBody() {
        // given
        cityRepository.save(city(token + "인코딩"));
        String defaultETag = getAll().header("ETag");

        // when
        ExtractableResponse<Response> response = RestAssured.given()
            .header("Accept", "application/json;charset=UTF-16BE")
            .when()
            .get(CITIES_URL + "/all")
            .then()
            .statusCode(200)
            .extract();

        // then
        String expected = "\"" + DigestUtils.md5DigestAsHex(response.asByteArray()) + "\"";
        assertThat(response.header("ETag")).isEqualTo(expected).isEqualTo(defaultETag);
    }

    @Test
    @DisplayName("전체 도시 조회에 같은 ETag로 If-None-Match를 보내면 본문 없이 304를 반환한다")
    void all_sameETag_notModified() {
        // given
        cityRepository.save(city(token + "전체"));
        String eTag = getAll().header("ETag");

        // when
        ExtractableResponse<Response> response = RestAssured.given()
            .header("If-None-Match", eTag)
            .when()
            .get(CITIES_URL + "/all")
            .then()
            .statusCode(304)
            .extract();

        // then
        assertThat(response.asString()).isEmpty();
        assertThat(response.header("ETag")).isEqualTo(eTag);
        assertThat(response.header("Cache-Control")).contains("public", "max-age=86400").doesNotContain("no-store");
    }

    @Test
    @DisplayName("전체 도시 조회에 다른 ETag로 If-None-Match를 보내면 200과 전체 목록을 반환한다")
    void all_differentETag_ok() {
        // given
        cityRepository.save(city(token + "전체"));

        // when & then
        RestAssured.given()
            .header("If-None-Match", "\"stale-etag\"")
            .when()
            .get(CITIES_URL + "/all")
            .then()
            .statusCode(200)
            .body("status", equalTo("success"))
            .body("data.cityName", hasItem(token + "전체"));
    }

    @Test
    @DisplayName("도시가 추가되면 전체 도시 조회의 ETag가 바뀌어 이전 ETag로도 200을 반환한다")
    void all_cityAdded_eTagChanges() {
        // given
        String previousETag = getAll().header("ETag");
        cityRepository.save(city(token + "추가"));

        // when
        ExtractableResponse<Response> response = RestAssured.given()
            .header("If-None-Match", previousETag)
            .when()
            .get(CITIES_URL + "/all")
            .then()
            .statusCode(200)
            .extract();

        // then
        assertThat(response.header("ETag")).isNotEqualTo(previousETag);
        assertThat(response.jsonPath().getList("data.cityName", String.class)).contains(token + "추가");
    }

    private ExtractableResponse<Response> getAll() {
        return RestAssured.given()
            .when()
            .get(CITIES_URL + "/all")
            .then()
            .statusCode(200)
            .extract();
    }

    private City city(String cityName) {
        return City.builder()
            .cityName(cityName)
            .countryName("테스트국가")
            .lat(0.0)
            .lng(0.0)
            .countryCode("TST")
            .build();
    }
}
