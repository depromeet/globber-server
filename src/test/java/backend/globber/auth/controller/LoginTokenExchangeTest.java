package backend.globber.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

import backend.globber.auth.domain.Member;
import backend.globber.auth.domain.constant.AuthProvider;
import backend.globber.auth.domain.constant.Role;
import backend.globber.auth.repository.MemberRepository;
import backend.globber.auth.service.TokenService;
import backend.globber.auth.util.JwtTokenProvider;
import backend.globber.auth.util.LoginCodeStore;
import backend.globber.auth.util.LoginCodeStore.LoginResult;
import backend.globber.city.service.RedisWarmUp;
import backend.globber.support.PostgresTestConfig;
import backend.globber.support.RedisTestConfig;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = RedisTestConfig.Initializer.class)
@Import({PostgresTestConfig.class, RedisTestConfig.class})
@DirtiesContext
class LoginTokenExchangeTest {

    @MockitoBean
    private RedisWarmUp redisWarmUp;

    @LocalServerPort
    private int port;

    @Autowired
    private LoginCodeStore loginCodeStore;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private Member member;
    private String accessToken;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        member = memberRepository.save(Member.of(
            "login-code-" + UUID.randomUUID() + "@example.com", "테스트유저", "password",
            AuthProvider.KAKAO, List.of(Role.ROLE_USER), UUID.randomUUID().toString().substring(0, 6)));
        accessToken = jwtTokenProvider.createAccessToken(member.getEmail(), List.of(Role.ROLE_USER.name()));
    }

    private String body(String code) {
        return "{\"code\": \"" + code + "\"}";
    }

    @Test
    @DisplayName("일회용 code를 교환하면 accessToken·memberId·uuid·firstLogin을 돌려주고, 같은 code는 다시 쓸 수 없다")
    void exchange_onceOnly() {
        // given
        String code = loginCodeStore.issue(new LoginResult(accessToken, member.getId(), member.getUuid(), true));

        // when & then
        RestAssured.given().log().all()
            .contentType(ContentType.JSON).body(body(code))
            .when().post("/auth/token")
            .then().log().all()
            .statusCode(200)
            .body("status", equalTo("success"))
            .body("data.accessToken", equalTo(accessToken))
            .body("data.memberId", equalTo(member.getId().intValue()))
            .body("data.uuid", equalTo(member.getUuid()))
            .body("data.firstLogin", equalTo(true));

        RestAssured.given().contentType(ContentType.JSON).body(body(code))
            .when().post("/auth/token")
            .then().statusCode(401).body("status", equalTo("fail"));
    }

    @Test
    @DisplayName("없는 code는 401, code가 비어 있으면 400이다")
    void exchange_invalid() {
        // when & then
        RestAssured.given().contentType(ContentType.JSON).body(body("unknown-code"))
            .when().post("/auth/token")
            .then().statusCode(401).body("status", equalTo("fail"));
        RestAssured.given().contentType(ContentType.JSON).body("{}")
            .when().post("/auth/token")
            .then().statusCode(400).body("status", equalTo("fail"));
    }

    @Test
    @DisplayName("GET /id는 삭제되었다")
    void memberIdEndpoint_removed() {
        // when & then
        RestAssured.given().header("Authorization", accessToken)
            .when().get("/id")
            .then().statusCode(404);
    }

    @Test
    @DisplayName("Redis의 RefreshToken은 리프레시 토큰 수명(jwt.refresh_expiration)만큼 보관한다")
    void refreshToken_ttlMatchesRefreshExpiration() {
        // when
        tokenService.updateRefreshToken(member.getEmail(), jwtTokenProvider.createRefreshToken());

        // then: 테스트(local) 설정의 refresh_expiration은 8640000ms = 8640초
        // 저장소는 이메일을 키로 저장한다
        Long ttl = redisTemplate.getExpire(member.getEmail());
        assertThat(ttl).isBetween(8_600L, 8_640L);
    }
}
