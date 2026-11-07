package backend.globber.membertravel.controller;

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
import backend.globber.membertravel.domain.MemberTravel;
import backend.globber.membertravel.domain.MemberTravelCity;
import backend.globber.membertravel.repository.MemberTravelCityRepository;
import backend.globber.membertravel.repository.MemberTravelRepository;
import backend.globber.support.PostgresTestConfig;
import backend.globber.support.RedisTestConfig;
import io.restassured.RestAssured;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = RedisTestConfig.Initializer.class)
@Import({RedisTestConfig.class, PostgresTestConfig.class, MemberTravelDeleteAcceptanceTest.CachingTestConfig.class})
class MemberTravelDeleteAcceptanceTest {

    private static final String NOT_IN_MY_TRAVEL_MESSAGE = "내 여행지에 해당 도시가 없습니다.";

    // 운영 설정에는 @EnableCaching이 없으므로 캐시 무효화를 검증하기 위해 이 테스트 컨텍스트에서만 캐싱을 켠다.
    @TestConfiguration
    @EnableCaching
    static class CachingTestConfig {
    }

    @MockitoBean
    private RedisWarmUp redisWarmUp;

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

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    @Qualifier("memberTravelCacheManager")
    private CacheManager memberTravelCacheManager;

    private Member me;
    private MemberTravel myTravel;
    private City deleteTarget;
    private City remaining;
    private String accessToken;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;

        me = saveMember();
        deleteTarget = saveCity();
        remaining = saveCity();
        myTravel = saveMemberTravel(me, deleteTarget, remaining);
        accessToken = jwtTokenProvider.createAccessToken(me.getEmail(), List.of(Role.ROLE_USER.name()));
    }

    @Test
    @DisplayName("내 여행지의 도시를 cityId로 삭제하면 204를 반환하고 해당 도시만 삭제된다")
    void deleteMemberTravel_noContent() {
        // when
        RestAssured.given().log().all()
                .header("Authorization", accessToken)
                .when()
                .delete("/api/v1/member-travels/{cityId}", deleteTarget.getCityId())
                .then().log().all()
                .statusCode(204);

        // then
        assertThat(memberTravelCityRepository.existsByMemberTravel_IdAndCity_CityId(
                myTravel.getId(), deleteTarget.getCityId())).isFalse();
        assertThat(memberTravelCityRepository.existsByMemberTravel_IdAndCity_CityId(
                myTravel.getId(), remaining.getCityId())).isTrue();
    }

    @Test
    @DisplayName("다른 회원의 여행지에만 있는 cityId로 삭제하면 404를 반환하고 그 회원의 여행지는 유지된다")
    void deleteMemberTravel_otherMembersCity_notFound() {
        // given
        Member other = saveMember();
        City othersCity = saveCity();
        MemberTravel othersTravel = saveMemberTravel(other, othersCity);

        // when & then
        RestAssured.given().log().all()
                .header("Authorization", accessToken)
                .when()
                .delete("/api/v1/member-travels/{cityId}", othersCity.getCityId())
                .then().log().all()
                .statusCode(404)
                .body("status", equalTo("fail"))
                .body("data", equalTo(NOT_IN_MY_TRAVEL_MESSAGE));

        assertThat(memberTravelCityRepository.existsByMemberTravel_IdAndCity_CityId(
                othersTravel.getId(), othersCity.getCityId())).isTrue();
        assertThat(memberTravelCityRepository.findByMemberTravel_Id(myTravel.getId())).hasSize(2);
    }

    @Test
    @DisplayName("존재하지 않는 cityId로 삭제하면 404를 반환한다")
    void deleteMemberTravel_nonExistentCity_notFound() {
        // given
        Long nonExistentCityId = Long.MAX_VALUE;

        // when & then
        RestAssured.given().log().all()
                .header("Authorization", accessToken)
                .when()
                .delete("/api/v1/member-travels/{cityId}", nonExistentCityId)
                .then().log().all()
                .statusCode(404)
                .body("status", equalTo("fail"))
                .body("data", equalTo(NOT_IN_MY_TRAVEL_MESSAGE));

        assertThat(memberTravelCityRepository.findByMemberTravel_Id(myTravel.getId())).hasSize(2);
    }

    @Test
    @DisplayName("여행 기록이 없는 회원이 삭제하면 404를 반환한다")
    void deleteMemberTravel_noMemberTravel_notFound() {
        // given
        Member noTravelMember = saveMember();
        String noTravelToken = jwtTokenProvider.createAccessToken(
                noTravelMember.getEmail(), List.of(Role.ROLE_USER.name()));

        // when & then
        RestAssured.given().log().all()
                .header("Authorization", noTravelToken)
                .when()
                .delete("/api/v1/member-travels/{cityId}", deleteTarget.getCityId())
                .then().log().all()
                .statusCode(404)
                .body("status", equalTo("fail"));
    }

    @Test
    @DisplayName("조회 후 삭제하면 여행 기록 캐시에 남지 않고 다시 조회할 때 삭제된 도시가 없다")
    void deleteMemberTravel_evictsMemberTravelsCache() {
        // given
        assertThat(getMyCityIds()).containsExactlyInAnyOrder(deleteTarget.getCityId(), remaining.getCityId());
        Cache cache = memberTravelCacheManager.getCache("memberTravels");
        assertThat(cache).isNotNull();
        assertThat(cache.get(me.getId())).isNotNull();

        // when
        RestAssured.given().log().all()
                .header("Authorization", accessToken)
                .when()
                .delete("/api/v1/member-travels/{cityId}", deleteTarget.getCityId())
                .then().log().all()
                .statusCode(204);

        // then
        assertThat(cache.get(me.getId())).isNull();
        assertThat(getMyCityIds()).containsExactly(remaining.getCityId());
    }

    private List<Long> getMyCityIds() {
        return RestAssured.given().log().all()
                .header("Authorization", accessToken)
                .when()
                .get("/api/v1/member-travels")
                .then().log().all()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("data.travels[0].cities.cityId", Long.class);
    }

    private Member saveMember() {
        return memberRepository.save(Member.of(
                "delete-test-" + UUID.randomUUID() + "@example.com",
                "삭제테스트유저",
                "password",
                AuthProvider.KAKAO,
                List.of(Role.ROLE_USER),
                UUID.randomUUID().toString().substring(0, 6)
        ));
    }

    private City saveCity() {
        return cityRepository.save(City.builder()
                .cityName("삭제테스트-" + UUID.randomUUID().toString().substring(0, 8))
                .countryName("대한민국")
                .countryCode("KOR")
                .lat(37.5665)
                .lng(126.9780)
                .build());
    }

    private MemberTravel saveMemberTravel(Member member, City... cities) {
        MemberTravel memberTravel = memberTravelRepository.save(MemberTravel.builder()
                .member(member)
                .build());

        memberTravelCityRepository.saveAll(List.of(cities).stream()
                .map(city -> MemberTravelCity.builder()
                        .memberTravel(memberTravel)
                        .city(city)
                        .build())
                .toList());
        return memberTravel;
    }
}
