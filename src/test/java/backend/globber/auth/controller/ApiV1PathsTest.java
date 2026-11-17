package backend.globber.auth.controller;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

import backend.globber.city.service.RedisWarmUp;
import backend.globber.support.PostgresTestConfig;
import backend.globber.support.RedisTestConfig;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * GLB-298: 모든 API를 /api/v1 아래로 모은다. 예전 경로는 더 이상 동작하지 않는다.
 * 카카오 콜백(/login/oauth2/code/*), actuator, Swagger는 그대로 둔다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = RedisTestConfig.Initializer.class)
@Import({PostgresTestConfig.class, RedisTestConfig.class})
class ApiV1PathsTest {

    @MockitoBean
    private RedisWarmUp redisWarmUp;

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    @DisplayName("인증 관련 API는 /api/v1/auth 아래에서 동작하고 예전 경로는 없다")
    void authEndpoints_movedUnderApiV1() {
        // when & then: 새 경로는 컨트롤러까지 도달한다(인증·코드가 없어서 401)
        RestAssured.given().contentType(ContentType.JSON).body("{\"code\":\"unknown\"}")
            .when().post("/api/v1/auth/token").then().statusCode(401);
        RestAssured.given().when().post("/api/v1/auth/reissue").then().statusCode(not(anyOf(is(404), is(405))));
        RestAssured.given().when().post("/api/v1/auth/logout").then().statusCode(401);
        RestAssured.given().when().delete("/api/v1/auth/withdraw").then().statusCode(401);

        // 예전 경로는 없다
        RestAssured.given().contentType(ContentType.JSON).body("{\"code\":\"unknown\"}")
            .when().post("/auth/token").then().statusCode(anyOf(is(404), is(405)));
        RestAssured.given().when().post("/reissue").then().statusCode(anyOf(is(404), is(405)));
        RestAssured.given().when().post("/logout").then().statusCode(anyOf(is(404), is(405)));
        RestAssured.given().when().delete("/withdraw").then().statusCode(anyOf(is(404), is(405)));
    }

    @Test
    @DisplayName("개발용 테스트 로그인은 /api/v1/dev/test-login에서 동작한다")
    void devTestLogin_movedUnderApiV1() {
        // when & then
        RestAssured.given().queryParam("email", "not-a-test-account@example.com")
            .when().post("/api/v1/dev/test-login").then().statusCode(not(anyOf(is(404), is(405))));
        RestAssured.given().queryParam("email", "not-a-test-account@example.com")
            .when().post("/dev/test-login").then().statusCode(anyOf(is(404), is(405)));
    }

    @Test
    @DisplayName("카카오 로그인 시작은 /api/v1/oauth2/authorization/kakao이고 예전 경로는 없다")
    void oauthAuthorization_movedUnderApiV1() {
        // when & then
        RestAssured.given().redirects().follow(false)
            .when().get("/api/v1/oauth2/authorization/kakao")
            .then().statusCode(302).header("Location", containsString("kauth.kakao.com"));
        RestAssured.given().redirects().follow(false)
            .when().get("/oauth2/authorization/kakao")
            .then().statusCode(404);
    }

    @Test
    @DisplayName("API 문서에 나오는 모든 경로는 /api/v1로 시작한다")
    void documentedPaths_allUnderApiV1() {
        // when & then
        RestAssured.given().when().get("/v3/api-docs/GLOBBER API").then().statusCode(200)
            .body("paths.keySet()", everyItem(startsWith("/api/v1/")));
    }
}
