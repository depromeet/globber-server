package backend.globber.diary.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import backend.globber.auth.domain.Member;
import backend.globber.auth.domain.constant.AuthProvider;
import backend.globber.auth.domain.constant.Role;
import backend.globber.auth.util.JwtTokenProvider;
import backend.globber.city.domain.City;
import backend.globber.city.service.RedisWarmUp;
import backend.globber.diary.domain.Diary;
import backend.globber.diary.domain.Photo;
import backend.globber.membertravel.domain.MemberTravel;
import backend.globber.membertravel.domain.MemberTravelCity;
import backend.globber.support.PostgresTestConfig;
import backend.globber.support.TestEntityFactory;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import jakarta.persistence.EntityManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(PostgresTestConfig.class)
class DiaryControllerTest {

    private static final String ORIGINAL_TEXT = "원래 본문";

    @MockitoBean
    private RedisWarmUp redisWarmUp;

    @LocalServerPort
    private int port;

    @Autowired
    private EntityManager em;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String prefix;
    private String accessToken;
    private Long cityId;
    private Long memberTravelCityId;
    private Long diaryId;
    private Long otherDiaryId;
    private Long otherPhotoId;
    private final Map<String, Long> photoIds = new HashMap<>();

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        prefix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String email = prefix + "@example.com";

        // given: 기록(사진 A,B,C)과 같은 회원의 다른 기록(사진 OTHER)
        transactionTemplate.executeWithoutResult(status -> {
            Member member = Member.of(email, "테스터", "password", AuthProvider.KAKAO,
                List.of(Role.ROLE_USER), prefix.substring(0, 6));
            City city = TestEntityFactory.createCity("도시" + prefix, "대한민국", "KOR", 37.5, 127.0);
            MemberTravel memberTravel = TestEntityFactory.createMemberTravel(member);
            MemberTravelCity mtc = TestEntityFactory.createMemberTravelCity(memberTravel, city);
            em.persist(member);
            em.persist(city);
            em.persist(memberTravel);
            em.persist(mtc);

            Diary diary = TestEntityFactory.createDiary(mtc, ORIGINAL_TEXT);
            Diary other = TestEntityFactory.createDiary(mtc, "다른 기록");
            em.persist(diary);
            em.persist(other);

            List<String> names = List.of("A", "B", "C");
            for (int i = 0; i < names.size(); i++) {
                Photo photo = photo(diary, code(names.get(i)), i);
                em.persist(photo);
                photoIds.put(names.get(i), photo.getId());
            }
            Photo otherPhoto = photo(other, code("OTHER"), 0);
            em.persist(otherPhoto);

            cityId = city.getCityId();
            memberTravelCityId = mtc.getId();
            diaryId = diary.getId();
            otherDiaryId = other.getId();
            otherPhotoId = otherPhoto.getId();
        });

        accessToken = jwtTokenProvider.createAccessToken(email, List.of(Role.ROLE_USER.name()));
    }

    @Test
    @DisplayName("추가·수정·삭제가 섞인 수정을 한 번에 반영하고 배열 순서대로 저장한다")
    void updateDiary_mixedChanges() {
        // given: B 삭제, C 메타데이터 수정, D 추가, 순서 [C, D, A]
        Map<String, Object> body = diaryBody("수정한 본문", List.of(
            photoBody(photoIds.get("C"), code("C"), "바뀐 장소"),
            photoBody(null, code("D"), null),
            photoBody(photoIds.get("A"), code("A"), null)));

        // when & then
        put(diaryId, body)
            .statusCode(200)
            .body("data.text", equalTo("수정한 본문"))
            .body("data.photos.photoCode", contains(code("C"), code("D"), code("A")))
            .body("data.photos[0].placeName", equalTo("바뀐 장소"));

        assertThat(photoRows(diaryId)).containsExactly(code("C") + ":0", code("D") + ":1", code("A") + ":2");
        getDetail(diaryId).body("data.photos.photoCode", contains(code("C"), code("D"), code("A")));
    }

    @Test
    @DisplayName("빈 사진 목록으로 수정하면 그 기록의 사진만 모두 삭제한다")
    void updateDiary_emptyPhotos_deletesAll() {
        // given
        Map<String, Object> body = diaryBody("사진 없는 본문", List.of());

        // when & then
        put(diaryId, body)
            .statusCode(200)
            .body("data.photos", empty());

        assertThat(photoRows(diaryId)).isEmpty();
        assertThat(photoRows(otherDiaryId)).containsExactly(code("OTHER") + ":0");
    }

    @Test
    @DisplayName("기존 사진을 빼고 같은 photoCode로 새 사진을 추가하면 새 id로 배열 순서대로 저장한다")
    void updateDiary_replaceWithSamePhotoCode() {
        // given: 기존 B를 목록에서 빼고, B의 photoCode로 새 사진을 추가한다
        Map<String, Object> body = diaryBody("교체한 본문", List.of(
            photoBody(null, code("B"), null),
            photoBody(photoIds.get("A"), code("A"), null),
            photoBody(photoIds.get("C"), code("C"), null)));

        // when & then
        put(diaryId, body)
            .statusCode(200)
            .body("data.photos.photoCode", contains(code("B"), code("A"), code("C")));

        assertThat(photoRows(diaryId)).containsExactly(code("B") + ":0", code("A") + ":1", code("C") + ":2");
        List<Long> newIds = jdbcTemplate.queryForList(
            "select id from photo where photo_code = ?", Long.class, code("B"));
        assertThat(newIds).hasSize(1).doesNotContain(photoIds.get("B"));
    }

    @Test
    @DisplayName("기록 생성 시 사진 배열 순서를 저장하고, 수정으로 바꾼 순서대로 조회된다")
    void createDiary_savesPhotoOrder() {
        // given
        Map<String, Object> body = diaryBody("새 기록", List.of(
            photoBody(null, code("X"), null),
            photoBody(null, code("Y"), null),
            photoBody(null, code("Z"), null)));

        // when
        ValidatableResponse created = RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", accessToken)
            .body(body)
            .when()
            .post("/api/v1/diaries")
            .then()
            .statusCode(200)
            .body("data.photos.photoCode", contains(code("X"), code("Y"), code("Z")));
        Long newDiaryId = created.extract().jsonPath().getLong("data.diaryId");
        List<Long> ids = created.extract().jsonPath().getList("data.photos.photoId", Long.class);

        // then
        assertThat(photoRows(newDiaryId)).containsExactly(code("X") + ":0", code("Y") + ":1", code("Z") + ":2");

        // when: 순서만 [Z, X, Y]로 변경
        put(newDiaryId, diaryBody("새 기록", List.of(
            photoBody(ids.get(2), code("Z"), null),
            photoBody(ids.get(0), code("X"), null),
            photoBody(ids.get(1), code("Y"), null))))
            .statusCode(200);

        // then
        getDetail(newDiaryId).body("data.photos.photoCode", contains(code("Z"), code("X"), code("Y")));
    }

    @Test
    @DisplayName("다른 기록의 photoId가 오면 404를 반환하고 같은 요청의 변경을 모두 롤백한다")
    void updateDiary_foreignPhotoId_rollsBack() {
        // given: 본문 변경, A·B·C 삭제, 새 사진 추가와 함께 다른 기록의 사진을 보낸다
        Map<String, Object> body = diaryBody("바뀌면 안 되는 본문", List.of(
            photoBody(null, code("D"), null),
            photoBody(otherPhotoId, code("OTHER"), null)));

        // when & then
        put(diaryId, body).statusCode(404);
        assertUnchanged();
    }

    @Test
    @DisplayName("삭제를 반영한 뒤 새 사진 저장이 실패하면 전체를 롤백한다")
    void updateDiary_failureAfterDelete_rollsBack() {
        // given: B·C 삭제 후, 다른 기록이 쓰는 photoCode로 새 사진을 추가해 저장 중에 실패시킨다
        Map<String, Object> body = diaryBody("바뀌면 안 되는 본문", List.of(
            photoBody(photoIds.get("A"), code("A"), "바뀌면 안 되는 장소"),
            photoBody(null, code("OTHER"), null)));

        // when & then
        put(diaryId, body).statusCode(not(200));
        assertUnchanged();
    }

    @Test
    @DisplayName("사진이 4장 이상이면 수정과 생성 모두 400을 반환하고 아무것도 바꾸지 않는다")
    void tooManyPhotos_rejected() {
        // given
        List<Map<String, Object>> photos = List.of(
            photoBody(photoIds.get("A"), code("A"), null),
            photoBody(photoIds.get("B"), code("B"), null),
            photoBody(photoIds.get("C"), code("C"), null),
            photoBody(null, code("D"), null));

        // when & then: 수정
        put(diaryId, diaryBody("바뀌면 안 되는 본문", photos)).statusCode(400);
        assertUnchanged();

        // when & then: 생성
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", accessToken)
            .body(diaryBody("새 기록", List.of(
                photoBody(null, code("N1"), null),
                photoBody(null, code("N2"), null),
                photoBody(null, code("N3"), null),
                photoBody(null, code("N4"), null))))
            .when()
            .post("/api/v1/diaries")
            .then()
            .statusCode(400);
        Integer diaryCount = jdbcTemplate.queryForObject(
            "select count(*) from diary where member_travel_city_id = ?", Integer.class, memberTravelCityId);
        assertThat(diaryCount).isEqualTo(2);
    }

    @Test
    @DisplayName("삭제한 사진 개별 API는 더 이상 동작하지 않는다")
    void removedPhotoApis_notAvailable() {
        RestAssured.given().contentType(ContentType.JSON).header("Authorization", accessToken)
            .body(photoBody(null, code("D"), null))
            .when().post("/api/v1/diaries/photo/{diaryId}", diaryId)
            .then().statusCode(not(200));
        RestAssured.given().contentType(ContentType.JSON).header("Authorization", accessToken)
            .body(List.of(photoBody(photoIds.get("A"), code("A"), "바뀌면 안 되는 장소")))
            .when().put("/api/v1/diaries/photo/{diaryId}", diaryId)
            .then().statusCode(not(200));
        RestAssured.given().header("Authorization", accessToken)
            .when().delete("/api/v1/diaries/photo/{diaryId}/{photoId}", diaryId, photoIds.get("B"))
            .then().statusCode(not(200));

        assertUnchanged();
    }

    private void assertUnchanged() {
        String text = jdbcTemplate.queryForObject("select text from diary where id = ?", String.class, diaryId);
        assertThat(text).isEqualTo(ORIGINAL_TEXT);
        assertThat(photoRows(diaryId)).containsExactly(code("A") + ":0", code("B") + ":1", code("C") + ":2");
        assertThat(photoRows(otherDiaryId)).containsExactly(code("OTHER") + ":0");
        Integer placeNames = jdbcTemplate.queryForObject(
            "select count(*) from photo where diary_id = ? and place_name is not null", Integer.class, diaryId);
        assertThat(placeNames).isZero();
    }

    private List<String> photoRows(Long targetDiaryId) {
        return jdbcTemplate.query(
            "select photo_code, display_order from photo where diary_id = ? order by display_order, id",
            (rs, rowNum) -> rs.getString("photo_code") + ":" + rs.getInt("display_order"),
            targetDiaryId);
    }

    private ValidatableResponse put(Long targetDiaryId, Map<String, Object> body) {
        return RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", accessToken)
            .body(body)
            .when()
            .put("/api/v1/diaries/{diaryId}", targetDiaryId)
            .then();
    }

    private ValidatableResponse getDetail(Long targetDiaryId) {
        return RestAssured.given()
            .when()
            .get("/api/v1/diaries/{diaryId}", targetDiaryId)
            .then()
            .statusCode(200);
    }

    private Map<String, Object> diaryBody(String text, List<Map<String, Object>> photos) {
        Map<String, Object> body = new HashMap<>();
        body.put("cityId", cityId);
        body.put("text", text);
        body.put("photos", photos);
        return body;
    }

    private Map<String, Object> photoBody(Long photoId, String photoCode, String placeName) {
        Map<String, Object> body = new HashMap<>();
        if (photoId != null) {
            body.put("photoId", photoId);
        }
        body.put("photoCode", photoCode);
        body.put("width", 100);
        body.put("height", 100);
        body.put("takenMonth", "202510");
        if (placeName != null) {
            body.put("placeName", placeName);
        }
        return body;
    }

    private Photo photo(Diary diary, String photoCode, int displayOrder) {
        return Photo.builder()
            .photoCode(photoCode)
            .width(100L)
            .height(100L)
            .displayOrder(displayOrder)
            .diary(diary)
            .build();
    }

    private String code(String name) {
        return prefix + "/" + name;
    }
}
