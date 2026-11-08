package backend.globber.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import backend.globber.auth.domain.Member;
import backend.globber.auth.domain.RefreshToken;
import backend.globber.auth.domain.constant.AuthProvider;
import backend.globber.auth.domain.constant.Role;
import backend.globber.auth.repository.MemberRepository;
import backend.globber.auth.repository.RefreshTokenRedisRepository;
import backend.globber.auth.util.AccessTokenBlacklist;
import backend.globber.auth.util.JwtTokenProvider;
import backend.globber.city.service.RedisWarmUp;
import backend.globber.support.PostgresTestConfig;
import backend.globber.support.RedisTestConfig;
import io.restassured.RestAssured;
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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = RedisTestConfig.Initializer.class)
@Import({PostgresTestConfig.class, RedisTestConfig.class})
@DirtiesContext
class LogoutBlacklistTest {

    @MockitoBean
    private RedisWarmUp redisWarmUp;

    @LocalServerPort
    private int port;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RefreshTokenRedisRepository refreshTokenRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private AccessTokenBlacklist accessTokenBlacklist;

    private Member member;
    private String token;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        member = memberRepository.save(Member.of(
            "logout-" + UUID.randomUUID() + "@example.com",
            "테스트유저",
            "password",
            AuthProvider.KAKAO,
            List.of(Role.ROLE_USER),
            UUID.randomUUID().toString().substring(0, 6)
        ));
        token = jwtTokenProvider.createAccessToken(member.getEmail(), List.of(Role.ROLE_USER.name()));
    }

    private void saveRefreshToken() {
        refreshTokenRepository.save(RefreshToken.of(member.getEmail(), jwtTokenProvider.createRefreshToken()));
    }

    private void logoutOk() {
        RestAssured.given().log().all()
            .header("Authorization", token)
            .when()
            .post("/logout")
            .then().log().all()
            .statusCode(200);
    }

    private void assertRejected(String path) {
        RestAssured.given().log().all()
            .header("Authorization", token)
            .when()
            .get(path)
            .then().log().all()
            .statusCode(401)
            .body("status", equalTo("fail"));
    }

    @Test
    @DisplayName("로그아웃한 액세스 토큰으로 다시 호출하면 401을 반환한다")
    void afterLogout_sameTokenRejected() {
        // given
        saveRefreshToken();

        // when
        logoutOk();

        // then
        assertRejected("/api/v1/member-travels/listViews");
        assertRejected("/api/v1/bookmarks");
    }

    @Test
    @DisplayName("Redis에 RefreshToken이 없어도 로그아웃은 200이고 쿠키를 만료시킨다")
    void logout_withoutRefreshToken_ok() {
        // when & then
        RestAssured.given().log().all()
            .header("Authorization", token)
            .when()
            .post("/logout")
            .then().log().all()
            .statusCode(200)
            .header("Set-Cookie", containsString("RefreshToken="))
            .header("Set-Cookie", containsString("Max-Age=0"));
        assertThat(accessTokenBlacklist.contains(rawToken())).isTrue();
    }

    @Test
    @DisplayName("탈퇴하면 그 액세스 토큰을 블랙리스트에 넣어 다시 쓸 수 없다")
    void afterWithdraw_tokenBlacklisted() {
        // given
        saveRefreshToken();

        // when
        RestAssured.given().log().all()
            .header("Authorization", token)
            .when()
            .delete("/withdraw")
            .then().log().all()
            .statusCode(200);

        // then
        assertThat(accessTokenBlacklist.contains(rawToken())).isTrue();
        assertRejected("/api/v1/member-travels/listViews");
    }

    private String rawToken() {
        return token.substring("Bearer ".length());
    }
}
