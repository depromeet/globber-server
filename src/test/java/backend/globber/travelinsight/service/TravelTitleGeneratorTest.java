package backend.globber.travelinsight.service;

import backend.globber.diary.domain.constant.PhotoTag;
import backend.globber.travelinsight.domain.TravelStatistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TravelTitleGeneratorTest {

    private final TravelTitleGenerator generator = new TravelTitleGenerator();

    @Test
    @DisplayName("여행 기록이 없으면 '자유로운 여행자' 반환")
    void generateTitle_noTravel_returnsDefault() {
        // given
        TravelStatistics statistics = TravelStatistics.empty();
        List<String> countryCodes = List.of();

        // when
        String title = generator.generateTitle(statistics, countryCodes);

        // then
        assertThat(title).isEqualTo("자유로운 여행자");
    }

    @Test
    @DisplayName("Lv.3 탐험형 - 국가 15개 이상 -> '대담한' + '탐험가'")
    void generateTitle_level3_explorer() {
        // given: 국가 15개, 도시 35개 (3개 대륙)
        TravelStatistics statistics = TravelStatistics.builder()
                .countryCount(15)
                .cityCount(35)
                .continentCount(0) // 대륙은 countryCodes로 계산
                .photoTagCounts(Map.of(
                        PhotoTag.FOOD, 10L,
                        PhotoTag.SCENERY, 10L,
                        PhotoTag.PEOPLE, 10L
                ))
                .build();

        List<String> countryCodes = Arrays.asList(
                "KOR", "JPN", "CHN", "THA", "VNM", "SGP", "MYS", "IDN", // 아시아 8개
                "USA", "CAN", "MEX", // 북미 3개
                "FRA", "DEU", "ITA", "ESP" // 유럽 4개
        ); // 3개 대륙, 15개 국가

        // when
        String title = generator.generateTitle(statistics, countryCodes);

        // then
        assertThat(title).isEqualTo("대담한 세계 탐험가");
    }

    @Test
    @DisplayName("Lv.2 활동형 + 음식 50% 이상 -> '호기심 많은' + '미식가'")
    void generateTitle_level2_foodie() {
        // given: 국가 8개, 도시 20개 (1개 대륙)
        TravelStatistics statistics = TravelStatistics.builder()
                .countryCount(8)
                .cityCount(20)
                .continentCount(0)
                .photoTagCounts(Map.of(
                        PhotoTag.FOOD, 50L,
                        PhotoTag.SCENERY, 25L,
                        PhotoTag.PEOPLE, 25L
                ))
                .build();

        List<String> countryCodes = Arrays.asList(
                "KOR", "JPN", "CHN", "THA", "VNM", "SGP", "MYS", "IDN"
        ); // 아시아 1개 대륙, 8개 국가

        // when
        String title = generator.generateTitle(statistics, countryCodes);

        // then
        assertThat(title).isEqualTo("호기심 많은 대륙 미식가");
    }

    @Test
    @DisplayName("Lv.1 탐구형 + 인물 50% 이상 -> '열정적인' + '인장가'")
    void generateTitle_level1_portraitist() {
        // given: 국가 3개, 도시 10개
        TravelStatistics statistics = TravelStatistics.builder()
                .countryCount(3)
                .cityCount(10)
                .continentCount(0)
                .photoTagCounts(Map.of(
                        PhotoTag.FOOD, 10L,
                        PhotoTag.SCENERY, 10L,
                        PhotoTag.PEOPLE, 60L
                ))
                .build();

        List<String> countryCodes = Arrays.asList("KOR", "JPN", "CHN");

        // when
        String title = generator.generateTitle(statistics, countryCodes);

        // then
        assertThat(title).isEqualTo("열정적인 문화 인장가");
    }

    @Test
    @DisplayName("Lv.0 깊이형 + 풍경 50% 이상 -> '섬세한' + '감상가'")
    void generateTitle_level0_appreciator() {
        // given: 국가 1개, 도시 5개
        TravelStatistics statistics = TravelStatistics.builder()
                .countryCount(1)
                .cityCount(5)
                .continentCount(0)
                .photoTagCounts(Map.of(
                        PhotoTag.FOOD, 10L,
                        PhotoTag.SCENERY, 60L,
                        PhotoTag.PEOPLE, 10L
                ))
                .build();

        List<String> countryCodes = List.of("KOR");

        // when
        String title = generator.generateTitle(statistics, countryCodes);

        // then
        assertThat(title).isEqualTo("섬세한 문화 감상가");
    }

    @Test
    @DisplayName("균형형 (모든 태그 50% 미만) -> '기록가'")
    void generateTitle_balanced_chronicler() {
        // given: 국가 8개, 도시 20개, 균형잡힌 사진
        TravelStatistics statistics = TravelStatistics.builder()
                .countryCount(8)
                .cityCount(20)
                .continentCount(0)
                .photoTagCounts(Map.of(
                        PhotoTag.FOOD, 30L,
                        PhotoTag.SCENERY, 35L,
                        PhotoTag.PEOPLE, 35L
                ))
                .build();

        List<String> countryCodes = Arrays.asList(
                "KOR", "JPN", "CHN", "THA", "VNM", "SGP", "MYS", "IDN"
        );

        // when
        String title = generator.generateTitle(statistics, countryCodes);

        // then
        assertThat(title).isEqualTo("호기심 많은 대륙 기록가");
    }

    @Test
    @DisplayName("국가 10개 이상 -> '세계' 스코프")
    void generateTitle_scope_world() {
        // given: 국가 12개, 2개 대륙
        TravelStatistics statistics = TravelStatistics.builder()
                .countryCount(12)
                .cityCount(25)
                .continentCount(0)
                .photoTagCounts(Map.of())
                .build();

        List<String> countryCodes = Arrays.asList(
                "KOR", "JPN", "CHN", "THA", "VNM", "SGP", "MYS", "IDN", // 아시아 8개
                "USA", "CAN", "MEX", "BRA" // 아메리카 4개
        );

        // when
        String title = generator.generateTitle(statistics, countryCodes);

        // then
        assertThat(title).contains("세계");
    }

    @Test
    @DisplayName("3개 대륙 이상 -> '세계' 스코프")
    void generateTitle_scope_world_by_continent() {
        // given: 국가 9개, 3개 대륙
        TravelStatistics statistics = TravelStatistics.builder()
                .countryCount(9)
                .cityCount(20)
                .continentCount(0)
                .photoTagCounts(Map.of())
                .build();

        List<String> countryCodes = Arrays.asList(
                "KOR", "JPN", "CHN", // 아시아 3개
                "USA", "CAN", "MEX", // 북미 3개
                "FRA", "DEU", "ITA" // 유럽 3개
        );

        // when
        String title = generator.generateTitle(statistics, countryCodes);

        // then
        assertThat(title).contains("세계");
    }

    @Test
    @DisplayName("1개 대륙, 4~9개 국가 -> '대륙' 스코프")
    void generateTitle_scope_continent() {
        // given: 국가 6개, 1개 대륙
        TravelStatistics statistics = TravelStatistics.builder()
                .countryCount(6)
                .cityCount(15)
                .continentCount(0)
                .photoTagCounts(Map.of())
                .build();

        List<String> countryCodes = Arrays.asList(
                "KOR", "JPN", "CHN", "THA", "VNM", "SGP"
        );

        // when
        String title = generator.generateTitle(statistics, countryCodes);

        // then
        assertThat(title).contains("대륙");
    }

    @Test
    @DisplayName("1개 국가 집중 -> '문화' 스코프")
    void generateTitle_scope_culture() {
        // given: 국가 1개, 도시 5개
        TravelStatistics statistics = TravelStatistics.builder()
                .countryCount(1)
                .cityCount(5)
                .continentCount(0)
                .photoTagCounts(Map.of())
                .build();

        List<String> countryCodes = List.of("KOR");

        // when
        String title = generator.generateTitle(statistics, countryCodes);

        // then
        assertThat(title).contains("문화");
    }

    @Test
    @DisplayName("사진이 없는 경우 -> '기록가'")
    void generateTitle_noPhotos_chronicler() {
        // given: 국가 5개, 도시 15개, 사진 없음
        TravelStatistics statistics = TravelStatistics.builder()
                .countryCount(5)
                .cityCount(15)
                .continentCount(0)
                .photoTagCounts(Map.of())
                .build();

        List<String> countryCodes = Arrays.asList(
                "KOR", "JPN", "CHN", "THA", "VNM"
        );

        // when
        String title = generator.generateTitle(statistics, countryCodes);

        // then
        assertThat(title).isEqualTo("호기심 많은 대륙 기록가");
    }

    @Test
    @DisplayName("국가 코드가 null이면 대륙 수 0으로 계산")
    void generateTitle_nullCountryCodes_continentCountZero() {
        // given
        TravelStatistics statistics = TravelStatistics.builder()
                .countryCount(5)
                .cityCount(15)
                .continentCount(0)
                .photoTagCounts(Map.of())
                .build();

        // when
        String title = generator.generateTitle(statistics, null);

        // then
        assertThat(title).contains("문화"); // 대륙 수 0이면 문화
    }

    @Test
    @DisplayName("매핑되지 않은 국가 코드는 무시")
    void generateTitle_unmappedCountryCodes_ignored() {
        // given
        TravelStatistics statistics = TravelStatistics.builder()
                .countryCount(5)
                .cityCount(15)
                .continentCount(0)
                .photoTagCounts(Map.of())
                .build();

        List<String> countryCodes = Arrays.asList(
                "KOR", "JPN", "XXX", "YYY", "ZZZ" // XXX, YYY, ZZZ는 매핑 안됨
        );

        // when
        String title = generator.generateTitle(statistics, countryCodes);

        // then
        // 실제 매핑된 국가는 2개(아시아 1개 대륙)이므로 문화
        assertThat(title).contains("문화");
    }
}
