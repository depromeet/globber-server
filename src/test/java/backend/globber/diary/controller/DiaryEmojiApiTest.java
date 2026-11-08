package backend.globber.diary.controller;

import static org.hamcrest.Matchers.equalTo;

import backend.globber.auth.domain.Member;
import backend.globber.auth.domain.constant.AuthProvider;
import backend.globber.auth.domain.constant.Role;
import backend.globber.auth.util.JwtTokenProvider;
import backend.globber.city.domain.City;
import backend.globber.city.service.RedisWarmUp;
import backend.globber.diary.domain.Diary;
import backend.globber.membertravel.domain.MemberTravel;
import backend.globber.membertravel.domain.MemberTravelCity;
import backend.globber.support.PostgresTestConfig;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import jakarta.persistence.EntityManager;
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
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({PostgresTestConfig.class})
class DiaryEmojiApiTest {

    private static final String FIRE = "1f525";

    @MockitoBean
    private RedisWarmUp redisWarmUp;

    @LocalServerPort
    private int port;

    @Autowired
    private EntityManager em;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private Long diaryId;
    private String ownerUuid;
    private String ownerToken;
    private String visitorToken;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        transactionTemplate.executeWithoutResult(status -> {
            Member owner = member();
            Member visitor = member();
            City city = City.builder().cityName("서울" + UUID.randomUUID()).countryName("대한민국")
                .countryCode("KOR").lat(37.5).lng(127.0).build();
            MemberTravel travel = MemberTravel.builder().member(owner).build();
            MemberTravelCity mtc = MemberTravelCity.builder().memberTravel(travel).city(city).build();
            em.persist(owner);
            em.persist(visitor);
            em.persist(city);
            em.persist(travel);
            em.persist(mtc);
            Diary diary = Diary.builder().memberTravelCity(mtc).text("여행기록").build();
            em.persist(diary);
            diaryId = diary.getId();
            ownerUuid = owner.getUuid();
            ownerToken = jwtTokenProvider.createAccessToken(owner.getEmail(), List.of(Role.ROLE_USER.name()));
            visitorToken = jwtTokenProvider.createAccessToken(visitor.getEmail(), List.of(Role.ROLE_USER.name()));
        });
    }

    private Member member() {
        String id = UUID.randomUUID().toString();
        return Member.of("emoji-api-" + id + "@example.com", "테스터", "password",
            AuthProvider.KAKAO, List.of(Role.ROLE_USER), id.substring(0, 6));
    }

    private ValidatableResponse post(String action, String token, String body) {
        var request = RestAssured.given().log().all().contentType(ContentType.JSON);
        if (token != null) {
            request.header("Authorization", token);
        }
        if (body != null) {
            request.body(body);
        }
        return request.when()
            .post("/api/v1/diaries/{diaryId}/emojis/" + action, diaryId)
            .then().log().all();
    }

    private String codeBody(String code) {
        return "{\"code\": \"" + code + "\"}";
    }

    @Test
    @DisplayName("로그인하지 않으면 등록과 누르기 모두 401이다")
    void withoutLogin_unauthorized() {
        // when & then
        post("register", null, codeBody(FIRE)).statusCode(401).body("status", equalTo("fail"));
        post("press", null, codeBody(FIRE)).statusCode(401).body("status", equalTo("fail"));
    }

    @Test
    @DisplayName("JSON 바디의 code로 등록하면 서버가 glyph를 만들어 count 1, pressedByMe true로 응답한다")
    void register_jsonBody() {
        // when & then
        post("register", visitorToken, codeBody(FIRE))
            .statusCode(200)
            .body("data.code", equalTo(FIRE))
            .body("data.glyph", equalTo("🔥"))
            .body("data.count", equalTo(1))
            .body("data.pressedByMe", equalTo(true));
    }

    @Test
    @DisplayName("잘못된 code는 400이고, 예전처럼 바디 없이 쿼리로 보내면 400(JSON 빈 바디) 또는 415(JSON 아님)다")
    void register_invalid_badRequest() {
        // when & then
        post("register", visitorToken, codeBody("smile")).statusCode(400).body("status", equalTo("fail"));
        RestAssured.given().log().all()
            .header("Authorization", visitorToken)
            .queryParam("code", FIRE)
            .queryParam("glyph", "🔥")
            .when()
            .post("/api/v1/diaries/{diaryId}/emojis/register", diaryId)
            .then().log().all()
            .statusCode(415)
            .body("status", equalTo("fail"));
        RestAssured.given().log().all()
            .header("Authorization", visitorToken)
            .contentType(ContentType.JSON)
            .queryParam("code", FIRE)
            .when()
            .post("/api/v1/diaries/{diaryId}/emojis/register", diaryId)
            .then().log().all()
            .statusCode(400)
            .body("status", equalTo("fail"));
    }

    @Test
    @DisplayName("누르기는 토글이고 응답에 갱신된 count와 pressedByMe가 온다")
    void press_toggleResponse() {
        // given
        post("register", ownerToken, codeBody(FIRE)).statusCode(200);

        // when & then
        post("press", visitorToken, codeBody(FIRE)).statusCode(200)
            .body("data.count", equalTo(2)).body("data.pressedByMe", equalTo(true));
        post("press", visitorToken, codeBody(FIRE)).statusCode(200)
            .body("data.count", equalTo(1)).body("data.pressedByMe", equalTo(false));
    }

    @Test
    @DisplayName("기록 상세와 기록 목록의 emojis에 pressedByMe가 오고, 비로그인이면 false다")
    void diaryResponses_includePressedByMe() {
        // given
        post("register", visitorToken, codeBody(FIRE)).statusCode(200);

        // when & then: 상세
        RestAssured.given().header("Authorization", visitorToken)
            .when().get("/api/v1/diaries/{diaryId}", diaryId)
            .then().statusCode(200).body("data.emojis[0].pressedByMe", equalTo(true));
        RestAssured.given()
            .when().get("/api/v1/diaries/{diaryId}", diaryId)
            .then().statusCode(200).body("data.emojis[0].pressedByMe", equalTo(false));

        // when & then: 목록
        RestAssured.given().header("Authorization", visitorToken).queryParam("uuid", ownerUuid)
            .when().get("/api/v1/diaries")
            .then().statusCode(200).body("data.diaryResponses[0].diaries[0].emojis[0].pressedByMe", equalTo(true));
        RestAssured.given().header("Authorization", ownerToken).queryParam("uuid", ownerUuid)
            .when().get("/api/v1/diaries")
            .then().statusCode(200).body("data.diaryResponses[0].diaries[0].emojis[0].pressedByMe", equalTo(false));
    }

    @Test
    @DisplayName("이모지 목록 API도 pressedByMe를 포함한다")
    void emojiList_includesPressedByMe() {
        // given
        post("register", visitorToken, codeBody(FIRE)).statusCode(200);

        // when & then
        RestAssured.given().header("Authorization", visitorToken)
            .when().get("/api/v1/diaries/{diaryId}/emojis", diaryId)
            .then().statusCode(200).body("data[0].pressedByMe", equalTo(true));
        RestAssured.given()
            .when().get("/api/v1/diaries/{diaryId}/emojis", diaryId)
            .then().statusCode(200).body("data[0].pressedByMe", equalTo(false));
    }
}
