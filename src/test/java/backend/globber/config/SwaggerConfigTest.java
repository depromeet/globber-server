package backend.globber.config;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

import backend.globber.city.service.RedisWarmUp;
import backend.globber.support.PostgresTestConfig;
import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({PostgresTestConfig.class})
@DirtiesContext
class SwaggerConfigTest {

    @MockitoBean
    private RedisWarmUp redisWarmUp;

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @ParameterizedTest
    @ValueSource(strings = {"/v3/api-docs", "/v3/api-docs/GLOBBER API"})
    @DisplayName("API 명세의 서버 목록은 운영, 개발, 로컬 서버 순서로 노출된다")
    void apiDocs_servers(String apiDocsPath) {
        // when
        ValidatableResponse response = RestAssured.given()
                .when()
                .get(apiDocsPath)
                .then();

        // then
        response.statusCode(200)
                .body("servers", hasSize(3))
                .body("servers.url", contains(
                        "https://globber.world/api",
                        "https://dev.globber.world/api",
                        "http://localhost:8080"))
                .body("servers.description", contains("운영 서버", "개발 서버", "로컬 서버"))
                .body("servers.url", not(hasItem("/")))
                .body("servers.url", not(hasItem("https://globber.store")))
                .body("servers.url", not(hasItem("https://globber-dev.store")));
    }
}
