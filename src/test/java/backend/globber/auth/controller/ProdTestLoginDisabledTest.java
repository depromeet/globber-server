package backend.globber.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

import backend.globber.auth.repository.MemberRepository;
import backend.globber.city.service.RedisWarmUp;
import backend.globber.support.PostgresTestConfig;
import backend.globber.support.RedisTestConfig;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * 운영 프로필에서 테스트 로그인 키가 설정되지 않은 경우(기본 상태).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.jpa.hibernate.ddl-auto=create",
    "management.health.mail.enabled=false",
    "app.test-login.key=",
    "app.test-accounts[0].email=" + ProdTestLoginDisabledTest.TEST_EMAIL,
    "app.test-accounts[0].nickname=운영테스트"
})
@ActiveProfiles("prod")
@ContextConfiguration(initializers = RedisTestConfig.Initializer.class)
@Import({PostgresTestConfig.class, RedisTestConfig.class})
@DirtiesContext
class ProdTestLoginDisabledTest {

    static final String TEST_EMAIL = "prod-test-disabled@globber.test";

    @MockitoBean
    private RedisWarmUp redisWarmUp;

    @LocalServerPort
    private int port;

    @Autowired
    private MemberRepository memberRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    @DisplayName("키 설정이 비어 있으면 어떤 키를 보내도 API가 없는 것처럼 404다")
    void keyNotConfigured_notFound() {
        for (String key : new String[]{"", "anything"}) {
            RestAssured.given().contentType(ContentType.JSON).header("X-Test-Login-Key", key)
                .body("{\"email\": \"" + TEST_EMAIL + "\"}")
                .when().post("/auth/test-login")
                .then().statusCode(404).body("status", equalTo("fail"))
                .body("data", equalTo("요청한 경로를 찾을 수 없습니다."));
        }
        // 본문이 비거나 없어도 400이 아니라 404다(API 존재를 드러내지 않는다)
        RestAssured.given().contentType(ContentType.JSON).body("{}")
            .when().post("/auth/test-login").then().statusCode(404);
        RestAssured.given().contentType(ContentType.JSON)
            .when().post("/auth/test-login").then().statusCode(404);
    }

    @Test
    @DisplayName("키 설정이 비어 있으면 운영에서는 테스트 계정을 만들지 않는다")
    void keyNotConfigured_noTestAccountCreated() {
        assertThat(memberRepository.existsByEmail(TEST_EMAIL)).isFalse();
    }
}
