package backend.globber.membertravel.service;

import backend.globber.auth.domain.Member;
import backend.globber.auth.domain.constant.AuthProvider;
import backend.globber.auth.domain.constant.Role;
import backend.globber.city.domain.City;
import backend.globber.diary.domain.Diary;
import backend.globber.diary.domain.Photo;
import backend.globber.membertravel.controller.dto.response.TravelRecordWithDiaryResponse;
import backend.globber.membertravel.domain.MemberTravel;
import backend.globber.membertravel.domain.MemberTravelCity;
import backend.globber.support.PostgresTestConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@Import(PostgresTestConfig.class)
class TravelRecordQueryServiceTest {

    @Autowired
    private TravelRecordQueryService travelRecordQueryService;

    @Autowired
    private EntityManager em;

    private Long memberId;

    @BeforeEach
    void setUp() {

        Member member = Member.of(
                "test@kakao",
                "테스트유저",
                "password",
                AuthProvider.KAKAO,
                List.of(Role.ROLE_USER)
        );
        em.persist(member);

        City tokyo = City.builder()
                .cityName("도쿄")
                .countryName("일본")
                .countryCode("JPN")
                .lat(35.6895)
                .lng(139.6917)
                .build();

        City osaka = City.builder()
                .cityName("오사카")
                .countryName("일본")
                .countryCode("JPN")
                .lat(34.6937)
                .lng(135.5023)
                .build();

        em.persist(tokyo);
        em.persist(osaka);

        MemberTravel memberTravel = MemberTravel.builder()
                .member(member)
                .build();
        em.persist(memberTravel);

        MemberTravelCity tokyoMtc = MemberTravelCity.builder()
                .memberTravel(memberTravel)
                .city(tokyo)
                .build();
        MemberTravelCity osakaMtc = MemberTravelCity.builder()
                .memberTravel(memberTravel)
                .city(osaka)
                .build();

        em.persist(tokyoMtc);
        em.persist(osakaMtc);

        Diary tokyoDiary = Diary.builder()
                .memberTravelCity(tokyoMtc)
                .text("도쿄 여행기")
                .emoji("😊")
                .build();
        em.persist(tokyoDiary);

        Photo photo1 = Photo.builder()
                .photoCode("tokyo_1")
                .diary(tokyoDiary)
                .build();
        Photo photo2 = Photo.builder()
                .photoCode("tokyo_2")
                .diary(tokyoDiary)
                .build();
        em.persist(photo1);
        em.persist(photo2);

        em.flush();
        em.clear();

        memberId = member.getId();
    }

    @Test
    @DisplayName("회원 여행 기록 조회 - 국가별/도시별 일기 정상 매핑")
    void testGetRecordsWithDiaries() {
        // when
        TravelRecordWithDiaryResponse response = travelRecordQueryService.getRecordsWithDiaries(memberId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.totalCountriesCounts()).isEqualTo(1);
        assertThat(response.totalCitiesCounts()).isEqualTo(1);
        assertThat(response.totalDiariesCounts()).isEqualTo(1);

        var country = response.records().get(0);
        assertThat(country.countryName()).isEqualTo("일본");
        assertThat(country.countryCode()).isEqualTo("JPN");
        assertThat(country.continent()).isEqualTo(Continent.아시아.name());
        assertThat(country.diaryCount()).isEqualTo(1);

        var tokyo = country.cities().stream()
                .filter(c -> c.name().equals("도쿄"))
                .findFirst()
                .orElseThrow();

        assertThat(tokyo.hasDiary()).isTrue();
        assertThat(tokyo.thumbnailUrls()).containsExactly("tokyo_1", "tokyo_2");

        var osaka = country.cities().stream()
                .filter(c -> c.name().equals("오사카"))
                .findFirst()
                .orElseThrow();

        assertThat(osaka.hasDiary()).isFalse();
        assertThat(osaka.thumbnailUrls()).isEmpty();
    }
}
