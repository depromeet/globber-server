package backend.globber.city.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

import backend.globber.auth.domain.Member;
import backend.globber.auth.domain.constant.AuthProvider;
import backend.globber.auth.domain.constant.Role;
import backend.globber.auth.repository.MemberRepository;
import backend.globber.auth.util.JwtTokenProvider;
import backend.globber.city.domain.City;
import backend.globber.city.repository.CityRepository;
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
import org.springframework.test.util.ReflectionTestUtils;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({PostgresTestConfig.class})
// 같은 설정의 TravelInsightControllerTest가 이 컨텍스트를 재사용하면 고정 도시 데이터가 충돌하므로 분리한다
@DirtiesContext
class CityAdminAuthorizationTest {

    private static final String UPDATE_BODY = """
        {"cityName":"수정된도시","countryName":"한국","lat":37.5,"lng":127.0,"countryCode":"KOR"}
        """;

    @MockitoBean
    private RedisWarmUp redisWarmUp;

    @LocalServerPort
    private int port;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private City city;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        city = cityRepository.save(City.builder()
            .cityName("원래도시" + UUID.randomUUID())
            .countryName("한국")
            .countryCode("KOR")
            .lat(37.5)
            .lng(127.0)
            .build());
    }

    private Member saveMember(Role role) {
        return memberRepository.save(Member.of(
            "city-auth-" + UUID.randomUUID() + "@example.com",
            "테스트유저",
            "password",
            AuthProvider.KAKAO,
            List.of(role),
            UUID.randomUUID().toString().substring(0, 6)
        ));
    }

    private String adminToken() {
        Member admin = saveMember(Role.ROLE_ADMIN);
        return jwtTokenProvider.createAccessToken(admin.getEmail(), List.of(Role.ROLE_ADMIN.name()));
    }

    // DB 역할은 ROLE_USER지만 토큰에는 ROLE_ADMIN이 남아 있는 경우 (권한 회수 후 기존 토큰)
    private String revokedAdminToken() {
        Member user = saveMember(Role.ROLE_USER);
        return jwtTokenProvider.createAccessToken(user.getEmail(), List.of(Role.ROLE_ADMIN.name()));
    }

    @Test
    @DisplayName("관리자는 도시를 수정할 수 있다")
    void updateCity_admin_ok() {
        // given
        String token = adminToken();

        // when & then
        RestAssured.given().log().all()
            .header("Authorization", token)
            .contentType(ContentType.JSON)
            .body(UPDATE_BODY)
            .when()
            .put("/api/v1/cities/{cityId}", city.getCityId())
            .then().log().all()
            .statusCode(200)
            .body("data.cityName", equalTo("수정된도시"));
    }

    @Test
    @DisplayName("관리자는 도시를 삭제할 수 있다")
    void deleteCity_admin_ok() {
        // given
        String token = adminToken();

        // when
        RestAssured.given().log().all()
            .header("Authorization", token)
            .when()
            .delete("/api/v1/cities/{cityId}", city.getCityId())
            .then().log().all()
            .statusCode(200);

        // then
        assertThat(cityRepository.findById(city.getCityId())).isEmpty();
    }

    @Test
    @DisplayName("DB 역할이 일반 회원이면 토큰에 관리자 역할이 있어도 도시를 수정할 수 없다")
    void updateCity_revokedAdmin_forbidden() {
        // given
        String token = revokedAdminToken();

        // when
        RestAssured.given().log().all()
            .header("Authorization", token)
            .contentType(ContentType.JSON)
            .body(UPDATE_BODY)
            .when()
            .put("/api/v1/cities/{cityId}", city.getCityId())
            .then().log().all()
            .statusCode(403);

        // then
        assertThat(cityRepository.findById(city.getCityId()))
            .get().extracting(City::getCityName).isEqualTo(city.getCityName());
    }

    @Test
    @DisplayName("DB 역할이 일반 회원이면 토큰에 관리자 역할이 있어도 도시를 삭제할 수 없다")
    void deleteCity_revokedAdmin_forbidden() {
        // given
        String token = revokedAdminToken();

        // when
        RestAssured.given().log().all()
            .header("Authorization", token)
            .when()
            .delete("/api/v1/cities/{cityId}", city.getCityId())
            .then().log().all()
            .statusCode(403);

        // then
        assertThat(cityRepository.findById(city.getCityId())).isPresent();
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 도시를 삭제할 수 없다")
    void deleteCity_noToken_unauthorized() {
        // given: Authorization 헤더 없음

        // when
        RestAssured.given().log().all()
            .when()
            .delete("/api/v1/cities/{cityId}", city.getCityId())
            .then().log().all()
            .statusCode(401);

        // then
        assertThat(cityRepository.findById(city.getCityId())).isPresent();
    }

    @Test
    @DisplayName("Bearer 접두사가 없는 토큰으로는 도시를 삭제할 수 없다")
    void deleteCity_rawToken_unauthorized() {
        // given
        String rawToken = jwtTokenProvider.fromHeader(adminToken());

        // when
        RestAssured.given().log().all()
            .header("Authorization", rawToken)
            .when()
            .delete("/api/v1/cities/{cityId}", city.getCityId())
            .then().log().all()
            .statusCode(401);

        // then
        assertThat(cityRepository.findById(city.getCityId())).isPresent();
    }

    @Test
    @DisplayName("만료된 관리자 토큰으로는 도시를 삭제할 수 없다")
    void deleteCity_expiredToken_unauthorized() {
        // given
        Member admin = saveMember(Role.ROLE_ADMIN);
        JwtTokenProvider expiredTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(expiredTokenProvider, "secretKey",
            ReflectionTestUtils.getField(jwtTokenProvider, "secretKey"));
        ReflectionTestUtils.setField(expiredTokenProvider, "accessTokenExpiration", -1000L);
        String expiredToken = expiredTokenProvider.createAccessToken(admin.getEmail(), List.of(Role.ROLE_ADMIN.name()));

        // when
        RestAssured.given().log().all()
            .header("Authorization", expiredToken)
            .when()
            .delete("/api/v1/cities/{cityId}", city.getCityId())
            .then().log().all()
            .statusCode(401);

        // then
        assertThat(cityRepository.findById(city.getCityId())).isPresent();
    }
}
