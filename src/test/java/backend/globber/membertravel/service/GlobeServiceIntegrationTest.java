package backend.globber.membertravel.service;

import backend.globber.auth.domain.Member;
import backend.globber.auth.domain.constant.AuthProvider;
import backend.globber.auth.domain.constant.Role;
import backend.globber.auth.repository.MemberRepository;
import backend.globber.city.domain.City;
import backend.globber.city.repository.CityRepository;
import backend.globber.membertravel.controller.dto.CityDto;
import backend.globber.membertravel.controller.dto.GlobeSummaryDto;
import backend.globber.membertravel.domain.MemberTravel;
import backend.globber.membertravel.domain.MemberTravelCity;
import backend.globber.membertravel.repository.MemberTravelCityRepository;
import backend.globber.membertravel.repository.MemberTravelRepository;
import backend.globber.support.PostgresTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import({PostgresTestConfig.class})
class GlobeServiceIntegrationTest {

    @Autowired
    private GlobeService globeService;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private CityRepository cityRepository;
    @Autowired
    private MemberTravelRepository memberTravelRepository;
    @Autowired
    private MemberTravelCityRepository memberTravelCityRepository;

    private String testUuid;

    @BeforeEach
    void setUp() {
        // given: 회원 생성
        Member member = Member.of(
                "test@example.com",
                "테스트유저",
                null,
                AuthProvider.KAKAO,
                List.of(Role.ROLE_USER)
        );
        memberRepository.save(member);
        testUuid = member.getUuid();

        City seoul = cityRepository.save(
                City.builder()
                        .cityName("Seoul")
                        .countryName("Korea")
                        .lat(37.5665)
                        .lng(126.9780)
                        .countryCode("KOR")
                        .build()
        );

        City tokyo = cityRepository.save(
                City.builder()
                        .cityName("Tokyo")
                        .countryName("Japan")
                        .lat(35.6895)
                        .lng(139.6917)
                        .countryCode("JPN")
                        .build()
        );

        MemberTravel memberTravel = MemberTravel.builder()
                .member(member)
                .build();
        memberTravelRepository.save(memberTravel);

        memberTravelCityRepository.save(MemberTravelCity.builder()
                .memberTravel(memberTravel)
                .city(seoul)
                .build());

        memberTravelCityRepository.save(MemberTravelCity.builder()
                .memberTravel(memberTravel)
                .city(tokyo)
                .build());
    }

    @Test
    @DisplayName("UUID로 지구본 요약 조회")
    void getGlobe_success() {
        GlobeSummaryDto result = globeService.getGlobe(testUuid);

        // then
        assertThat(result.cityCount()).isEqualTo(2);
        assertThat(result.countryCount()).isEqualTo(2);

        assertThat(result.regions()).hasSize(2);
        assertThat(result.regions())
                .extracting("regionName")
                .containsExactlyInAnyOrder("Korea", "Japan");

        assertThat(result.regions().stream()
                .flatMap(r -> r.cities().stream())
                .map(CityDto::name))
                .containsExactlyInAnyOrder("Seoul", "Tokyo");
    }
}
