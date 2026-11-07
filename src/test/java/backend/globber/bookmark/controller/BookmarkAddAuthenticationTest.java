package backend.globber.bookmark.controller;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({PostgresTestConfig.class})
class BookmarkAddAuthenticationTest {

    @MockitoBean
    private RedisWarmUp redisWarmUp;

    @LocalServerPort
    private int port;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private Member target;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        target = saveMember();
    }

    private Member saveMember() {
        return memberRepository.save(Member.of(
            "bookmark-" + UUID.randomUUID() + "@example.com",
            "테스트유저",
            "password",
            AuthProvider.KAKAO,
            List.of(Role.ROLE_USER),
            UUID.randomUUID().toString().substring(0, 6)
        ));
    }

    private String body() {
        return "{\"targetMemberId\": " + target.getId() + "}";
    }

    @Test
    @DisplayName("비로그인으로 북마크를 추가하면 401을 반환하고 리다이렉트 헤더와 쿠키를 내려주지 않는다")
    void addBookmark_withoutToken_unauthorized() {
        // when & then
        RestAssured.given().log().all()
            .contentType(ContentType.JSON)
            .body(body())
            .when()
            .post("/api/v1/bookmarks")
            .then().log().all()
            .statusCode(401)
            .body("status", equalTo("fail"))
            .body("data", equalTo("로그인이 필요합니다."))
            .header("X-Redirect-URL", nullValue())
            .header("Set-Cookie", anyOf(nullValue(), not(containsString("pendingBookmarkId"))));
    }

    @Test
    @DisplayName("로그인한 사용자는 북마크를 추가할 수 있다")
    void addBookmark_withToken_ok() {
        // given
        Member me = saveMember();
        String token = jwtTokenProvider.createAccessToken(me.getEmail(), List.of(Role.ROLE_USER.name()));

        // when & then
        RestAssured.given().log().all()
            .header("Authorization", token)
            .contentType(ContentType.JSON)
            .body(body())
            .when()
            .post("/api/v1/bookmarks")
            .then().log().all()
            .statusCode(200)
            .body("status", equalTo("success"));
    }
}
