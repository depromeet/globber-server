package backend.globber.city.service;

import static org.assertj.core.api.Assertions.assertThat;

import backend.globber.auth.repository.MemberRepository;
import backend.globber.city.controller.dto.CityResponse;
import backend.globber.city.repository.CityRepository;
import backend.globber.city.repository.cache.RankingRepository;
import backend.globber.common.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.util.DigestUtils;

@ExtendWith(MockitoExtension.class)
class CityServiceETagTest {

    @Mock
    private CityRepository cityRepository;

    @Mock
    private RankingRepository rankingRepository;

    @Mock
    private MemberRepository memberRepository;

    // Spring Boot가 HTTP 응답 직렬화에 쓰는 ObjectMapper와 같은 기본 설정으로 만든다.
    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

    private CityService cityService;

    private final CityResponse paris = new CityResponse(1L, "파리", "프랑스", 48.8566, 2.3522, "FRA");
    private final CityResponse tokyo = new CityResponse(2L, "도쿄", "일본", 35.6762, 139.6503, "JPN");

    @BeforeEach
    void setUp() {
        cityService = new CityService(cityRepository, rankingRepository, memberRepository, objectMapper);
    }

    @Test
    @DisplayName("같은 응답이면 따옴표로 감싼 같은 ETag를 만든다")
    void generateETag_sameBody() {
        // given
        ApiResponse<List<CityResponse>> body = ApiResponse.success(List.of(paris, tokyo));

        // when
        String first = cityService.generateETag(body);
        String second = cityService.generateETag(ApiResponse.success(List.of(paris, tokyo)));

        // then
        assertThat(first).isEqualTo(second);
        assertThat(first).matches("^\"[0-9a-f]{32}\"$");
    }

    @Test
    @DisplayName("ETag는 응답을 ObjectMapper로 직렬화한 JSON 바이트의 md5다")
    void generateETag_md5OfSerializedBody() throws Exception {
        // given
        ApiResponse<List<CityResponse>> body = ApiResponse.success(List.of(paris, tokyo));
        String expected = "\"" + DigestUtils.md5DigestAsHex(objectMapper.writeValueAsBytes(body)) + "\"";

        // when
        String eTag = cityService.generateETag(body);

        // then
        assertThat(eTag).isEqualTo(expected);
    }

    @Test
    @DisplayName("응답의 필드 하나라도 바뀌면 ETag가 바뀐다")
    void generateETag_changedField() {
        // given
        String original = cityService.generateETag(ApiResponse.success(List.of(paris)));

        // when
        List<String> changed = List.of(
            cityService.generateETag(ApiResponse.success(List.of(
                new CityResponse(9L, "파리", "프랑스", 48.8566, 2.3522, "FRA")))),
            cityService.generateETag(ApiResponse.success(List.of(
                new CityResponse(1L, "파리시", "프랑스", 48.8566, 2.3522, "FRA")))),
            cityService.generateETag(ApiResponse.success(List.of(
                new CityResponse(1L, "파리", "프랑스공화국", 48.8566, 2.3522, "FRA")))),
            cityService.generateETag(ApiResponse.success(List.of(
                new CityResponse(1L, "파리", "프랑스", 48.8567, 2.3522, "FRA")))),
            cityService.generateETag(ApiResponse.success(List.of(
                new CityResponse(1L, "파리", "프랑스", 48.8566, 2.3523, "FRA")))),
            cityService.generateETag(ApiResponse.success(List.of(
                new CityResponse(1L, "파리", "프랑스", 48.8566, 2.3522, "FRX")))),
            cityService.generateETag(ApiResponse.fail(List.of(paris)))
        );

        // then
        assertThat(changed).doesNotContain(original).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("도시가 추가·삭제되거나 순서가 바뀌면 ETag가 바뀐다")
    void generateETag_changedList() {
        // given
        String original = cityService.generateETag(ApiResponse.success(List.of(paris, tokyo)));
        CityResponse seoul = new CityResponse(3L, "서울", "대한민국", 37.5665, 126.9780, "KOR");

        // when
        String added = cityService.generateETag(ApiResponse.success(List.of(paris, tokyo, seoul)));
        String deleted = cityService.generateETag(ApiResponse.success(List.of(paris)));
        String reordered = cityService.generateETag(ApiResponse.success(List.of(tokyo, paris)));

        // then
        assertThat(List.of(added, deleted, reordered)).doesNotContain(original).doesNotHaveDuplicates();
    }
}
