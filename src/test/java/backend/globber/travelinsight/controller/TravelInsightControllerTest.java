package backend.globber.travelinsight.controller;

import backend.globber.auth.domain.Member;
import backend.globber.auth.domain.constant.AuthProvider;
import backend.globber.auth.domain.constant.Role;
import backend.globber.auth.repository.MemberRepository;
import backend.globber.city.domain.City;
import backend.globber.city.repository.CityRepository;
import backend.globber.membertravel.domain.MemberTravel;
import backend.globber.membertravel.domain.MemberTravelCity;
import backend.globber.membertravel.repository.MemberTravelCityRepository;
import backend.globber.membertravel.repository.MemberTravelRepository;
import backend.globber.support.PostgresTestConfig;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({PostgresTestConfig.class})
class TravelInsightControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberTravelRepository memberTravelRepository;

    @Autowired
    private MemberTravelCityRepository memberTravelCityRepository;

    @Autowired
    private CityRepository cityRepository;

    private Member member;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;

        member = memberRepository.save(getMember());

        List<City> cities = getCities();

        MemberTravel memberTravel = memberTravelRepository.save(MemberTravel.builder()
                .member(member)
                .build());

        memberTravelCityRepository.saveAll(
                cities.stream()
                        .map(city -> MemberTravelCity.builder()
                                .memberTravel(memberTravel)
                                .city(city)
                                .build())
                        .toList());
    }

    private static @NotNull Member getMember() {
        return Member.of(
                "test" + System.currentTimeMillis() + "@example.com",
                "테스트유저",
                "password",
                AuthProvider.KAKAO,
                List.of(Role.ROLE_USER)
        );
    }

    private @NotNull List<City> getCities() {
        return cityRepository.saveAll(List.of(
                City.builder()
                        .cityName("교토")
                        .countryName("일본")
                        .countryCode("JPN")
                        .lat(35.0116)
                        .lng(135.7681)
                        .build(),
                City.builder()
                        .cityName("도쿄")
                        .countryName("일본")
                        .countryCode("JPN")
                        .lat(35.6762)
                        .lng(139.6503)
                        .build(),
                City.builder()
                        .cityName("방콕")
                        .countryName("태국")
                        .countryCode("THA")
                        .lat(13.7563)
                        .lng(100.5018)
                        .build()
        ));
    }

    @Test
    @DisplayName("여행 인사이트 조회 성공시 OK 반환한다")
    void aiInsight_Ok() {
        RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .when()
                .get("/api/v1/travel-insights/{memberId}", member.getId())
                .then().log().all()
                .statusCode(200)
                .body("status", equalTo("success"))
                .body("data.title", notNullValue());
    }

//  @Test
//  @DisplayName("여행 기록 없는 회원은 초보자 인사이트를 반환한다")
//  void aiInsight_default() {
//    Member newMember = memberRepository.save(Member.of(
//        "new@example.com",
//        "신규유저",
//        "password",
//        AuthProvider.KAKAO,
//        List.of(Role.ROLE_USER)
//    ));
//
//    RestAssured.given().log().all()
//        .contentType(ContentType.JSON)
//        .when()
//        .get("/api/v1/travel-insights/{memberId}", newMember.getId())
//        .then().log().all()
//        .statusCode(200)
//        .body("status", equalTo("success"))
//        .body("data.title", equalTo("여행 초보자"));
//  }
}
