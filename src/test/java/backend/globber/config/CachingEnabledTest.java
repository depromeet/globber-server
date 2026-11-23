package backend.globber.config;

import static org.assertj.core.api.Assertions.assertThat;

import backend.globber.auth.domain.Member;
import backend.globber.auth.domain.constant.AuthProvider;
import backend.globber.auth.domain.constant.Role;
import backend.globber.auth.repository.MemberRepository;
import backend.globber.city.domain.City;
import backend.globber.city.repository.CityRepository;
import backend.globber.city.service.RedisWarmUp;
import backend.globber.membertravel.service.MemberTravelService;
import backend.globber.support.PostgresTestConfig;
import backend.globber.support.RedisTestConfig;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * 테스트 전용 @EnableCaching 없이, 애플리케이션 설정만으로 캐시가 동작하는지 확인한다.
 */
@SpringBootTest
@ContextConfiguration(initializers = RedisTestConfig.Initializer.class)
@Import({RedisTestConfig.class, PostgresTestConfig.class})
class CachingEnabledTest {

    @MockitoBean
    private RedisWarmUp redisWarmUp;

    @Autowired
    private MemberTravelService memberTravelService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    @Qualifier("memberTravelCacheManager")
    private CacheManager memberTravelCacheManager;

    @Test
    @DisplayName("내 여행지 조회 결과가 memberTravels 캐시에 저장된다")
    void memberTravelRetrieve_isCached() {
        // given
        Member member = memberRepository.save(Member.of(
            "cache-" + UUID.randomUUID() + "@example.com", "캐시테스트", "password",
            AuthProvider.KAKAO, List.of(Role.ROLE_USER), UUID.randomUUID().toString().substring(0, 6)));

        // when
        memberTravelService.retrieveMemberTravel(member.getId());

        // then
        assertThat(memberTravelCacheManager.getCache("memberTravels").get(member.getId())).isNotNull();
    }

    @Test
    @DisplayName("캐시가 켜져도 국가 코드·도시 이름 조회는 반복 호출해도 정상 동작한다")
    void cityLookup_worksWithCachingEnabled() {
        // given
        String cityName = "캐시도시-" + UUID.randomUUID().toString().substring(0, 8);
        cityRepository.save(City.builder()
            .cityName(cityName).countryName("대한민국").countryCode("KOR").lat(37.5).lng(127.0).build());

        // when & then
        assertThat(cityRepository.findByCountryCodeAndCityName("KOR", cityName)).isPresent();
        assertThat(cityRepository.findByCountryCodeAndCityName("KOR", cityName)).isPresent();
    }
}
