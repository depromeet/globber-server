package backend.globber.city.service;

import static org.assertj.core.api.Assertions.assertThat;

import backend.globber.auth.repository.MemberRepository;
import backend.globber.city.controller.dto.CityResponse;
import backend.globber.city.repository.CityRepository;
import backend.globber.city.repository.cache.RankingRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CityServiceETagTest {

    @Mock
    private CityRepository cityRepository;

    @Mock
    private RankingRepository rankingRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private CityService cityService;

    private final CityResponse paris = new CityResponse(1L, "파리", "프랑스", 48.8566, 2.3522, "FRA");
    private final CityResponse tokyo = new CityResponse(2L, "도쿄", "일본", 35.6762, 139.6503, "JPN");

    @Test
    @DisplayName("같은 도시 목록이면 따옴표로 감싼 같은 ETag를 만든다")
    void generateCitiesETag_sameContent() {
        // given
        List<CityResponse> cities = List.of(paris, tokyo);

        // when
        String first = cityService.generateCitiesETag(cities);
        String second = cityService.generateCitiesETag(List.of(paris, tokyo));

        // then
        assertThat(first).isEqualTo(second);
        assertThat(first).matches("^\"[0-9a-f]{32}\"$");
    }

    @Test
    @DisplayName("도시가 추가·수정·삭제되면 ETag가 바뀐다")
    void generateCitiesETag_changedContent() {
        // given
        String original = cityService.generateCitiesETag(List.of(paris, tokyo));
        CityResponse renamedTokyo = new CityResponse(2L, "도쿄도", "일본", 35.6762, 139.6503, "JPN");
        CityResponse seoul = new CityResponse(3L, "서울", "대한민국", 37.5665, 126.9780, "KOR");

        // when
        String added = cityService.generateCitiesETag(List.of(paris, tokyo, seoul));
        String updated = cityService.generateCitiesETag(List.of(paris, renamedTokyo));
        String deleted = cityService.generateCitiesETag(List.of(paris));

        // then
        assertThat(List.of(added, updated, deleted)).doesNotContain(original).doesNotHaveDuplicates();
    }
}
