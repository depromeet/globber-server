package backend.globber.profile.controller;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import backend.globber.auth.domain.Member;
import backend.globber.auth.domain.constant.AuthProvider;
import backend.globber.auth.domain.constant.Role;
import backend.globber.auth.repository.MemberRepository;
import backend.globber.auth.util.JwtTokenProvider;
import backend.globber.city.service.RedisWarmUp;
import backend.globber.support.PostgresTestConfig;
import backend.globber.support.RedisTestConfig;
import io.restassured.RestAssured;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = RedisTestConfig.Initializer.class)
@Import({PostgresTestConfig.class, RedisTestConfig.class})
class ProfileEmailExposureTest {

    @MockitoBean
    private RedisWarmUp redisWarmUp;

    @LocalServerPort
    private int port;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    private String tokenFor(String email) {
        memberRepository.save(Member.of(email, "테스터", null, AuthProvider.KAKAO, List.of(Role.ROLE_USER),
            UUID.randomUUID().toString().substring(0, 6)));
        return jwtTokenProvider.createAccessToken(email, List.of(Role.ROLE_USER.name()));
    }

    @Test
    @DisplayName("카카오 로그인으로 만든 '회원번호@kakao' 이메일은 내 프로필 응답에서 null이다")
    void syntheticKakaoEmail_hidden() {
        // given: 카카오 로그인은 '{카카오 회원번호}@kakao'를 로그인 식별자로 저장한다
        String syntheticEmail = ThreadLocalRandom.current().nextLong(1_000_000_000L, 9_999_999_999L) + "@kakao";
        String token = tokenFor(syntheticEmail);

        // when & then
        RestAssured.given().header("Authorization", token)
            .when().get("/api/v1/profiles/me")
            .then().statusCode(200)
            .body("data.email", nullValue())
            .body("data.nickname", equalTo("테스터"));
        // 로그인 식별자(DB 값)는 그대로다
        org.assertj.core.api.Assertions.assertThat(memberRepository.existsByEmail(syntheticEmail)).isTrue();
    }

    @Test
    @DisplayName("실제 이메일 주소는 내 프로필 응답에 그대로 준다")
    void realEmail_kept() {
        // given
        String realEmail = "real-" + UUID.randomUUID() + "@example.com";
        String token = tokenFor(realEmail);

        // when & then
        RestAssured.given().header("Authorization", token)
            .when().get("/api/v1/profiles/me")
            .then().statusCode(200)
            .body("data.email", equalTo(realEmail));
    }
}
