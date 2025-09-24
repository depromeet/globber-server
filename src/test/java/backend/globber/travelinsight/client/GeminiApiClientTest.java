package backend.globber.travelinsight.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import backend.globber.exception.spec.GeminiException;
import backend.globber.membertravel.controller.dto.TravelCityDto;
import backend.globber.membertravel.controller.dto.response.MemberTravelAllResponse;
import backend.globber.membertravel.controller.dto.response.MemberTravelResponse;
import backend.globber.support.PostgresTestConfig;
import backend.globber.travelinsight.controller.dto.response.TravelInsightResponse;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;


@SpringBootTest
@Import({PostgresTestConfig.class})
class GeminiApiClientTest {

    @Autowired
    private GeminiApiClient geminiApiClient;

    @Test
    @DisplayName("아시아 여행 데이터로 AI 인사이트 생성")
    void shouldCreateInsight_WithAsianTravelData() {
        // given
        List<TravelCityDto> cities = List.of(
            new TravelCityDto(1L, "교토", "일본", "JPN", 35.0116, 135.7681),
            new TravelCityDto(2L, "도쿄", "일본", "JPN", 35.6762, 139.6503),
            new TravelCityDto(3L, "방콕", "태국", "THA", 13.7563, 100.5018)
        );
        MemberTravelResponse travelResponse = new MemberTravelResponse(cities);
        MemberTravelAllResponse travels = new MemberTravelAllResponse(1L, List.of(travelResponse));

        // when
        TravelInsightResponse response = geminiApiClient.createTitle(travels);

        // then
        assertThat(response).isNotNull();
        assertThat(response.title()).isNotBlank();
        System.out.println("아시아 여행 인사이트: " + response.title());
    }

    @Test
    @DisplayName("유럽 여행 데이터로 AI 인사이트 생성")
    void shouldCreateInsight_WithEuropeanTravelData() {
        // given
        List<TravelCityDto> cities = List.of(
            new TravelCityDto(4L, "파리", "프랑스", "FRA", 48.8566, 2.3522),
            new TravelCityDto(5L, "로마", "이탈리아", "ITA", 41.9028, 12.4964),
            new TravelCityDto(6L, "베를린", "독일", "DEU", 52.5200, 13.4050)
        );
        MemberTravelResponse travelResponse = new MemberTravelResponse(cities);
        MemberTravelAllResponse travels = new MemberTravelAllResponse(1L, List.of(travelResponse));

        // when
        TravelInsightResponse response = geminiApiClient.createTitle(travels);

        // then
        assertThat(response.title()).isNotBlank();
        System.out.println("유럽 여행 인사이트: " + response.title());
    }

    @Test
    @DisplayName("다양한 대륙 여행 데이터로 AI 인사이트 생성")
    void shouldCreateInsight_WithMultiContinentData() {
        // given
        List<TravelCityDto> cities = List.of(
            new TravelCityDto(7L, "서울", "한국", "KOR", 37.5665, 126.9780),
            new TravelCityDto(8L, "뉴욕", "미국", "USA", 40.7128, -74.0060),
            new TravelCityDto(9L, "시드니", "호주", "AUS", -33.8688, 151.2093),
            new TravelCityDto(10L, "리우데자네이루", "브라질", "BRA", -22.9068, -43.1729)
        );
        MemberTravelResponse travelResponse = new MemberTravelResponse(cities);
        MemberTravelAllResponse travels = new MemberTravelAllResponse(1L, List.of(travelResponse));

        // when
        TravelInsightResponse response = geminiApiClient.createTitle(travels);

        // then
        assertThat(response.title()).isNotBlank();
        System.out.println("글로벌 여행 인사이트: " + response.title());
    }

    @Test
    @DisplayName("단일 국가 여행 데이터로 AI 인사이트 생성")
    void shouldCreateInsight_WithSingleCountryData() {
        // given
        List<TravelCityDto> cities = List.of(
            new TravelCityDto(2L, "도쿄", "일본", "JPN", 35.6762, 139.6503),
            new TravelCityDto(11L, "오사카", "일본", "JPN", 34.6937, 135.5023),
            new TravelCityDto(1L, "교토", "일본", "JPN", 35.0116, 135.7681)
        );
        MemberTravelResponse travelResponse = new MemberTravelResponse(cities);
        MemberTravelAllResponse travels = new MemberTravelAllResponse(1L, List.of(travelResponse));

        // when
        TravelInsightResponse response = geminiApiClient.createTitle(travels);

        // then
        assertThat(response.title()).isNotBlank();
        System.out.println("일본 집중 여행 인사이트: " + response.title());
    }

    @Test
    @DisplayName("잘못된 API 키로 호출 시 예외 발생")
    void shouldThrowException_WithInvalidApiKey() {
        // given
        GeminiApiClient invalidClient = new GeminiApiClient(
            "https://api.google.com/gemini",
            "INVALID_KEY"
        );

        // given
        List<TravelCityDto> cities = List.of(
            new TravelCityDto(1L, "교토", "일본", "JPN", 35.0116, 135.7681),
            new TravelCityDto(2L, "도쿄", "일본", "JPN", 35.6762, 139.6503),
            new TravelCityDto(3L, "방콕", "태국", "THA", 13.7563, 100.5018)
        );
        MemberTravelResponse travelResponse = new MemberTravelResponse(cities);
        MemberTravelAllResponse travels = new MemberTravelAllResponse(1L, List.of(travelResponse));

        // when & then
        assertThatThrownBy(() -> invalidClient.createTitle(travels))
            .isInstanceOf(GeminiException.class);
    }
}
