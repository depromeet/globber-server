package backend.globber.config;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import backend.globber.city.service.RedisWarmUp;
import backend.globber.support.PostgresTestConfig;
import backend.globber.support.RedisTestConfig;
import io.restassured.RestAssured;
import io.restassured.config.DecoderConfig;
import io.restassured.config.RestAssuredConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = RedisTestConfig.Initializer.class)
@Import({PostgresTestConfig.class, RedisTestConfig.class})
class ResponseCompressionTest {

    // 응답 본문을 풀지 않고 Content-Encoding 헤더를 그대로 확인한다
    private static final RestAssuredConfig RAW = RestAssured.config()
            .decoderConfig(DecoderConfig.decoderConfig().noContentDecoders());

    @MockitoBean
    private RedisWarmUp redisWarmUp;

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    @DisplayName("1KB가 넘는 JSON 응답은 Accept-Encoding: gzip이면 gzip으로 압축한다")
    void largeJson_gzipped() {
        // when & then: API 명세(application/json, 수십 KB)
        RestAssured.given().config(RAW)
                .header("Accept-Encoding", "gzip")
                .when().get("/v3/api-docs")
                .then()
                .statusCode(200)
                .header("Content-Encoding", equalTo("gzip"));
    }

    @Test
    @DisplayName("gzip을 요청하지 않거나 응답이 작으면 압축하지 않는다")
    void notRequestedOrSmall_notGzipped() {
        // when & then
        RestAssured.given().config(RAW)
                .when().get("/v3/api-docs")
                .then()
                .statusCode(200)
                .header("Content-Encoding", nullValue());
        RestAssured.given().config(RAW)
                .header("Accept-Encoding", "gzip")
                .when().get("/actuator/health")
                .then()
                .statusCode(200)
                .header("Content-Encoding", nullValue());
    }
}
