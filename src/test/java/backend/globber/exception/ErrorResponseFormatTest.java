package backend.globber.exception;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;

import backend.globber.auth.domain.Member;
import backend.globber.auth.domain.constant.AuthProvider;
import backend.globber.auth.domain.constant.Role;
import backend.globber.auth.repository.MemberRepository;
import backend.globber.auth.util.JwtTokenProvider;
import backend.globber.city.service.RedisWarmUp;
import backend.globber.support.PostgresTestConfig;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({PostgresTestConfig.class})
@DirtiesContext
class ErrorResponseFormatTest {

    private static final String INVALID_VALUE = "잘못된 요청 값입니다.";

    @MockitoBean
    private RedisWarmUp redisWarmUp;

    @LocalServerPort
    private int port;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String token;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        Member member = memberRepository.save(Member.of(
            "error-format-" + UUID.randomUUID() + "@example.com",
            "테스트유저",
            "password",
            AuthProvider.KAKAO,
            List.of(Role.ROLE_USER),
            UUID.randomUUID().toString().substring(0, 6)
        ));
        token = jwtTokenProvider.createAccessToken(member.getEmail(), List.of(Role.ROLE_USER.name()));
    }

    @Test
    @DisplayName("Authorization 헤더 없이 보호 API를 부르면 401과 ApiResponse 형식을 반환한다")
    void missingAuthorizationHeader_unauthorized() {
        // when & then
        RestAssured.given().log().all()
            .when()
            .get("/api/v1/member-travels/listViews")
            .then().log().all()
            .statusCode(401)
            .body("status", equalTo("fail"))
            .body("data", equalTo("로그인이 필요합니다."));
    }

    @Test
    @DisplayName("필수 파라미터가 없으면 400과 ApiResponse 형식을 반환한다")
    void missingRequestParameter_badRequest() {
        // when & then
        RestAssured.given().log().all()
            .when()
            .get("/api/v1/diaries")
            .then().log().all()
            .statusCode(400)
            .body("status", equalTo("fail"))
            .body("data", containsString("uuid"));
    }

    @Test
    @DisplayName("경로 변수 타입이 맞지 않으면 400과 고정 문구를 반환한다")
    void pathVariableTypeMismatch_badRequest() {
        // when & then
        RestAssured.given().log().all()
            .when()
            .get("/api/v1/diaries/abc")
            .then().log().all()
            .statusCode(400)
            .body("status", equalTo("fail"))
            .body("data", equalTo(INVALID_VALUE));
    }

    @Test
    @DisplayName("enum 파라미터 값이 잘못되면 400과 고정 문구를 반환하고 내부 패키지명을 노출하지 않는다")
    void enumParameterMismatch_badRequestWithoutInternals() {
        // when & then
        RestAssured.given().log().all()
            .header("Authorization", token)
            .queryParam("sort", "zzz")
            .when()
            .get("/api/v1/bookmarks")
            .then().log().all()
            .statusCode(400)
            .body("status", equalTo("fail"))
            .body("data", equalTo(INVALID_VALUE))
            .body(not(containsString("backend.globber")));
    }

    @Test
    @DisplayName("@Valid 검증에 실패하면 400과 DTO에 작성된 검증 메시지를 반환한다")
    void validationFailure_badRequestWithFieldMessage() {
        // when & then
        RestAssured.given().log().all()
            .header("Authorization", token)
            .contentType(ContentType.JSON)
            .body("{\"targetMemberId\": -1}")
            .when()
            .post("/api/v1/bookmarks")
            .then().log().all()
            .statusCode(400)
            .body("status", equalTo("fail"))
            .body("data", equalTo("회원 ID는 양수여야 합니다."));
    }

    @Test
    @DisplayName("JSON 본문을 읽을 수 없으면 400과 고정 문구를 반환한다")
    void unreadableBody_badRequest() {
        // when & then
        RestAssured.given().log().all()
            .header("Authorization", token)
            .contentType(ContentType.JSON)
            .body("{")
            .when()
            .post("/api/v1/bookmarks")
            .then().log().all()
            .statusCode(400)
            .body("status", equalTo("fail"))
            .body("data", equalTo(INVALID_VALUE));
    }

    @Test
    @DisplayName("지원하지 않는 메서드로 호출하면 405와 ApiResponse 형식을 반환한다")
    void methodNotSupported_methodNotAllowed() {
        // when & then
        RestAssured.given().log().all()
            .when()
            .patch("/api/v1/cities/favorites")
            .then().log().all()
            .statusCode(405)
            .body("status", equalTo("fail"));
    }

    @Test
    @DisplayName("없는 경로를 호출하면 404와 ApiResponse 형식을 반환한다")
    void noHandler_notFound() {
        // when & then
        RestAssured.given().log().all()
            .when()
            .get("/api/v1/does-not-exist")
            .then().log().all()
            .statusCode(404)
            .body("status", equalTo("fail"));
    }
}
