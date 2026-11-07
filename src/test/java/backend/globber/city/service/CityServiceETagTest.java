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

    @Test
    @DisplayName("구분자 문자를 포함해 필드 경계만 옮겨진 목록이면 ETag가 다르다")
    void generateCitiesETag_separatorInValue() {
        // given
        CityResponse cityNameWithSeparator =
            new CityResponse(1L, "파리\u001F프랑스", "국가", 48.8566, 2.3522, "FRA");
        CityResponse countryNameWithSeparator =
            new CityResponse(1L, "파리", "프랑스\u001F국가", 48.8566, 2.3522, "FRA");
        CityResponse recordSeparatorInCountryCode =
            new CityResponse(1L, "파리", "프랑스", 48.8566, 2.3522, "FRA\u001E2");
        CityResponse splitRecords = new CityResponse(2L, "파리", "프랑스", 48.8566, 2.3522, "FRA");

        // when
        String first = cityService.generateCitiesETag(List.of(cityNameWithSeparator));
        String second = cityService.generateCitiesETag(List.of(countryNameWithSeparator));
        String third = cityService.generateCitiesETag(List.of(recordSeparatorInCountryCode));
        String fourth = cityService.generateCitiesETag(List.of(paris, splitRecords));

        // then
        assertThat(List.of(first, second, third, fourth)).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("null 필드와 \"null\" 문자열 필드는 서로 다른 ETag를 만든다")
    void generateCitiesETag_nullAndNullString() {
        // given
        CityResponse nullCityName = new CityResponse(1L, null, "프랑스", 48.8566, 2.3522, "FRA");
        CityResponse nullStringCityName = new CityResponse(1L, "null", "프랑스", 48.8566, 2.3522, "FRA");

        // when
        String nullETag = cityService.generateCitiesETag(List.of(nullCityName));
        String nullStringETag = cityService.generateCitiesETag(List.of(nullStringCityName));

        // then
        assertThat(nullETag).isNotEqualTo(nullStringETag);
        assertThat(nullETag).matches("^\"[0-9a-f]{32}\"$");
    }
}
