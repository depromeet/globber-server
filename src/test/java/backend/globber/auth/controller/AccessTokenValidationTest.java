package backend.globber.auth.controller;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import backend.globber.auth.domain.Member;
import backend.globber.auth.domain.RefreshToken;
import backend.globber.auth.domain.constant.AuthProvider;
import backend.globber.auth.domain.constant.Role;
import backend.globber.auth.repository.MemberRepository;
import backend.globber.auth.repository.RefreshTokenRedisRepository;
import backend.globber.auth.util.JwtTokenProvider;
import backend.globber.city.service.RedisWarmUp;
import backend.globber.support.PostgresTestConfig;
import backend.globber.support.RedisTestConfig;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.restassured.RestAssured;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
class AccessTokenValidationTest {

    private static final String BEARER = "Bearer ";
    private static final String FORGED_SECRET =
        Base64.getEncoder().encodeToString("forged-secret-forged-secret-forged-secret-1234".getBytes());

    @MockitoBean
    private RedisWarmUp redisWarmUp;

    @LocalServerPort
    private int port;

    @Value("${jwt.secret}")
    private String secretKey;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RefreshTokenRedisRepository refreshTokenRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private Member member;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        member = memberRepository.save(Member.of(
            "token-" + UUID.randomUUID() + "@example.com",
            "테스트유저",
            "password",
            AuthProvider.KAKAO,
            List.of(Role.ROLE_USER),
            UUID.randomUUID().toString().substring(0, 6)
        ));
    }

    // Bearer 접두사가 붙은 정상 토큰 (서버가 발급하는 형식 그대로)
    private String validBearerToken() {
        return jwtTokenProvider.createAccessToken(member.getEmail(), List.of(Role.ROLE_USER.name()));
    }

    private String rawJwt(String secret, Date expiration) {
        Date issuedAt = new Date(expiration.getTime() - 60_000);
        return Jwts.builder()
            .setSubject(member.getEmail())
            .claim("role", List.of(Role.ROLE_USER.name()))
            .setIssuedAt(issuedAt)
            .setExpiration(expiration)
            .signWith(SignatureAlgorithm.HS256, secret)
            .compact();
    }

    private String expiredRawJwt() {
        return rawJwt(secretKey, new Date(System.currentTimeMillis() - 60_000));
    }

    private String forgedRawJwt() {
        return rawJwt(FORGED_SECRET, new Date(System.currentTimeMillis() + 600_000));
    }

    private void assertUnauthorized(String authorization, String path) {
        RestAssured.given().log().all()
            .header("Authorization", authorization)
            .when()
            .get(path)
            .then().log().all()
            .statusCode(401)
            .body("status", equalTo("fail"));
    }

    @Nested
    @DisplayName("CommonService로 회원을 찾는 API (listViews)")
    class CommonServicePath {

        private static final String PATH = "/api/v1/member-travels/listViews";

        @Test
        @DisplayName("Bearer 접두사가 붙은 정상 토큰은 200을 반환한다")
        void validBearer_ok() {
            // given
            String token = validBearerToken();

            // when & then
            RestAssured.given().log().all()
                .header("Authorization", token)
                .when()
                .get(PATH)
                .then().log().all()
                .statusCode(200);
        }

        @Test
        @DisplayName("Bearer 접두사가 없는 정상 토큰은 401을 반환한다")
        void rawValidToken_unauthorized() {
            // given
            String token = validBearerToken().substring(BEARER.length());

            // when & then
            assertUnauthorized(token, PATH);
        }

        @Test
        @DisplayName("Bearer 접두사가 없는 만료 토큰은 401을 반환한다")
        void rawExpiredToken_unauthorized() {
            assertUnauthorized(expiredRawJwt(), PATH);
        }

        @Test
        @DisplayName("Bearer 접두사가 없는 위조 토큰은 401을 반환한다")
        void rawForgedToken_unauthorized() {
            assertUnauthorized(forgedRawJwt(), PATH);
        }

        @Test
        @DisplayName("Bearer가 아닌 형식의 헤더는 401을 반환한다")
        void basicScheme_unauthorized() {
            assertUnauthorized("Basic abc", PATH);
        }

        @Test
        @DisplayName("Bearer 접두사가 붙은 만료 토큰은 401을 반환한다")
        void bearerExpiredToken_unauthorized() {
            assertUnauthorized(BEARER + expiredRawJwt(), PATH);
        }

        @Test
        @DisplayName("Bearer 접두사가 붙은 위조 토큰은 401을 반환한다")
        void bearerForgedToken_unauthorized() {
            assertUnauthorized(BEARER + forgedRawJwt(), PATH);
        }
    }

    @Nested
    @DisplayName("TokenService로 회원을 찾는 API (북마크 목록)")
    class TokenServicePath {

        private static final String PATH = "/api/v1/bookmarks";

        @Test
        @DisplayName("Bearer 접두사가 붙은 정상 토큰은 200을 반환한다")
        void validBearer_ok() {
            // given
            String token = validBearerToken();

            // when & then
            RestAssured.given().log().all()
                .header("Authorization", token)
                .when()
                .get(PATH)
                .then().log().all()
                .statusCode(200);
        }

        @Test
        @DisplayName("Bearer 접두사가 없는 정상 토큰은 401을 반환한다")
        void rawValidToken_unauthorized() {
            assertUnauthorized(validBearerToken().substring(BEARER.length()), PATH);
        }

        @Test
        @DisplayName("Bearer가 아닌 형식의 헤더는 401을 반환한다")
        void basicScheme_unauthorized() {
            assertUnauthorized("Basic abc", PATH);
        }
    }

    @Nested
    @DisplayName("로그아웃 (/logout)")
    class Logout {

        @Test
        @DisplayName("Bearer 접두사가 붙은 정상 토큰으로 로그아웃할 수 있다")
        void validBearer_ok() {
            // given
            refreshTokenRepository.save(RefreshToken.of(member.getEmail(), jwtTokenProvider.createRefreshToken()));

            // when & then
            RestAssured.given().log().all()
                .header("Authorization", validBearerToken())
                .when()
                .post("/logout")
                .then().log().all()
                .statusCode(200);
        }

        @Test
        @DisplayName("Bearer 접두사가 없는 정상 토큰으로는 로그아웃할 수 없다")
        void rawValidToken_unauthorized() {
            // given
            refreshTokenRepository.save(RefreshToken.of(member.getEmail(), jwtTokenProvider.createRefreshToken()));

            // when & then
            RestAssured.given().log().all()
                .header("Authorization", validBearerToken().substring(BEARER.length()))
                .when()
                .post("/logout")
                .then().log().all()
                .statusCode(401)
                .body("status", equalTo("fail"));
        }
    }

    @Nested
    @DisplayName("토큰 재발급 (/reissue)")
    class Reissue {

        private String saveRefreshToken() {
            String refreshToken = jwtTokenProvider.createRefreshToken();
            refreshTokenRepository.save(RefreshToken.of(member.getEmail(), refreshToken));
            return refreshToken;
        }

        @Test
        @DisplayName("Bearer 접두사가 붙은 만료 액세스 토큰과 유효한 리프레시 토큰으로 재발급할 수 있다")
        void expiredBearerAccessToken_reissued() {
            // given
            String refreshToken = saveRefreshToken();

            // when & then
            RestAssured.given().log().all()
                .header("Authorization", BEARER + expiredRawJwt())
                .cookie("RefreshToken", refreshToken)
                .when()
                .post("/reissue")
                .then().log().all()
                .statusCode(200)
                .body("data.accessToken", notNullValue());
        }

        @Test
        @DisplayName("Bearer 접두사가 없는 만료 액세스 토큰으로는 재발급할 수 없다")
        void rawExpiredAccessToken_unauthorized() {
            // given
            String refreshToken = saveRefreshToken();

            // when & then
            RestAssured.given().log().all()
                .header("Authorization", expiredRawJwt())
                .cookie("RefreshToken", refreshToken)
                .when()
                .post("/reissue")
                .then().log().all()
                .statusCode(401)
                .body("status", equalTo("fail"));
        }

        @Test
        @DisplayName("서명이 위조된 액세스 토큰으로는 재발급할 수 없다")
        void forgedAccessToken_unauthorized() {
            // given
            String refreshToken = saveRefreshToken();

            // when & then
            RestAssured.given().log().all()
                .header("Authorization", BEARER + forgedRawJwt())
                .cookie("RefreshToken", refreshToken)
                .when()
                .post("/reissue")
                .then().log().all()
                .statusCode(401)
                .body("status", equalTo("fail"));
        }
    }
}
