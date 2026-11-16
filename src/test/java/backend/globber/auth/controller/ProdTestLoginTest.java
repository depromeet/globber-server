package backend.globber.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

import backend.globber.auth.domain.Member;
import backend.globber.auth.domain.constant.AuthProvider;
import backend.globber.auth.domain.constant.Role;
import backend.globber.auth.repository.MemberRepository;
import backend.globber.city.service.RedisWarmUp;
import backend.globber.support.PostgresTestConfig;
import backend.globber.support.RedisTestConfig;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import java.util.List;
import java.util.UUID;
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
 * 운영 프로필에서 테스트 로그인 키가 설정된 경우.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.jpa.hibernate.ddl-auto=create",
    "management.health.mail.enabled=false",
    "app.test-login.key=" + ProdTestLoginTest.KEY,
    "app.test-accounts[0].email=" + ProdTestLoginTest.TEST_EMAIL,
    "app.test-accounts[0].nickname=운영테스트"
})
@ActiveProfiles("prod")
@ContextConfiguration(initializers = RedisTestConfig.Initializer.class)
@Import({PostgresTestConfig.class, RedisTestConfig.class})
@DirtiesContext
class ProdTestLoginTest {

    static final String KEY = "test-login-secret-key-for-test";
    static final String TEST_EMAIL = "prod-test-account@globber.test";

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

    private ExtractableResponse<Response> login(String key, String email) {
        var request = RestAssured.given().contentType(ContentType.JSON)
            .body("{\"email\": \"" + email + "\"}");
        if (key != null) {
            request.header("X-Test-Login-Key", key);
        }
        return request.when().post("/auth/test-login").then().extract();
    }

    @Test
    @DisplayName("키가 설정된 운영에서는 설정한 테스트 계정이 기동할 때 자동으로 만들어진다")
    void testAccountCreatedOnStartup() {
        assertThat(memberRepository.existsByEmail(TEST_EMAIL)).isTrue();
    }

    @Test
    @DisplayName("키와 테스트 계정이 맞으면 액세스 토큰만 본문으로 주고, 쿠키·Authorization 헤더는 주지 않는다")
    void success_accessTokenOnly() {
        // when
        ExtractableResponse<Response> response = login(KEY, TEST_EMAIL);

        // then
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.header("Set-Cookie")).isNull();
        assertThat(response.header("Authorization")).isNull();
        assertThat(response.jsonPath().getString("status")).isEqualTo("success");
        assertThat(response.jsonPath().getString("data.refreshToken")).isNull();
        String accessToken = response.jsonPath().getString("data.accessToken");
        assertThat(accessToken).startsWith("Bearer ");

        // 발급한 토큰으로 인증이 필요한 API를 호출할 수 있다
        RestAssured.given().header("Authorization", accessToken)
            .when().get("/api/v1/profiles/me")
            .then().statusCode(200);
    }

    @Test
    @DisplayName("키가 없거나 틀리면 401이다")
    void missingOrWrongKey_unauthorized() {
        assertThat(login(null, TEST_EMAIL).statusCode()).isEqualTo(401);
        assertThat(login("wrong-key", TEST_EMAIL).statusCode()).isEqualTo(401);
        assertThat(login("", TEST_EMAIL).statusCode()).isEqualTo(401);
        assertThat(login("wrong-key", TEST_EMAIL).jsonPath().getString("status")).isEqualTo("fail");
    }

    @Test
    @DisplayName("키가 맞아도 설정된 테스트 계정이 아니면 실제 사용자로 로그인할 수 없다(403)")
    void realUser_forbidden() {
        // given: 실제 사용자
        String realUserEmail = UUID.randomUUID() + "@kakao";
        memberRepository.save(Member.of(realUserEmail, "실사용자", null, AuthProvider.KAKAO,
            List.of(Role.ROLE_USER), UUID.randomUUID().toString().substring(0, 6)));

        // when
        ExtractableResponse<Response> response = login(KEY, realUserEmail);

        // then
        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.jsonPath().getString("status")).isEqualTo("fail");
        assertThat(response.jsonPath().getString("data.accessToken")).isNull();
    }

    @Test
    @DisplayName("email이 비어 있으면 400이다")
    void blankEmail_badRequest() {
        RestAssured.given().contentType(ContentType.JSON).header("X-Test-Login-Key", KEY).body("{}")
            .when().post("/auth/test-login")
            .then().statusCode(400).body("status", equalTo("fail"));
    }

    @Test
    @DisplayName("API 문서에 노출하지 않는다")
    void hiddenFromApiDocs() {
        String docs = RestAssured.given().when().get("/v3/api-docs/GLOBBER API")
            .then().statusCode(200).extract().asString();
        assertThat(docs).doesNotContain("/auth/test-login");
    }
}
