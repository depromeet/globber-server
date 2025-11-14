package backend.globber.travelinsight.domain;

import static org.assertj.core.api.Assertions.assertThat;

import backend.globber.diary.domain.constant.PhotoTag;
import backend.globber.travelinsight.domain.constant.TravelLevel;
import backend.globber.travelinsight.domain.constant.TravelScope;
import backend.globber.travelinsight.domain.constant.TravelType;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TravelTitleComposerTest {

    private final TravelTitleComposer composer = new TravelTitleComposer();

    private static Stream<Arguments> titleCases() {
        return Stream.of(
            Arguments.of(25, 16, 1, counts(50, 50, 50), "대담한 세계 탐험가"),
            Arguments.of(30, 12, 3, counts(10, 20, 60), "대담한 세계 탐험가"),
            Arguments.of(20, 8, 1, counts(5, 11, 4), "호기심 많은 대륙 인상가"),
            Arguments.of(9, 3, 1, counts(2, 2, 7), "열정적인 로컬 감상가"),
//            Arguments.of(9, 3, 1, counts(2, 2, 7), "열정적인 대륙 감상가"),
            Arguments.of(6, 1, 1, counts(1, 6, 3), "섬세한 로컬 인상가"),
            Arguments.of(25, 10, 2, counts(45, 30, 25), "호기심 많은 세계 기록가"),
//            Arguments.of(25, 10, 2, counts(45, 30, 25), "대담한 세계 탐험가"),
            Arguments.of(12, 4, 1, counts(12, 5, 3), "열정적인 대륙 미식가"),
            Arguments.of(8, 2, 1, counts(3, 3, 3), "열정적인 로컬 기록가"),
//            Arguments.of(8, 2, 1, counts(3, 3, 3), "열정적인 대륙 기록가"),
            Arguments.of(4, 1, 1, counts(1, 2, 7), "섬세한 로컬 감상가"),
            Arguments.of(18, 6, 1, counts(3, 5, 2), "호기심 많은 대륙 인상가"),
//            Arguments.of(10, 3, 1, counts(11, 5, 4), "열정적인 대륙 미식가")
            Arguments.of(10, 3, 1, counts(11, 5, 4), "열정적인 로컬 미식가")
        );
    }

    private static Map<PhotoTag, Long> counts(long food, long people, long scenery) {
        return Map.of(
            PhotoTag.FOOD, food,
            PhotoTag.PEOPLE, people,
            PhotoTag.SCENERY, scenery
        );
    }

    @ParameterizedTest(name = "[{0}도시/{1}국가/{2}대륙] => {4}")
    @MethodSource("titleCases")
    @DisplayName("여행 타이틀 조합 테스트")
    void shouldComposeExpectedTitle(
        int cityCount,
        int countryCount,
        int continentCount,
        Map<PhotoTag, Long> photoCounts,
        String expectedTitle
    ) {
        TravelLevel level = TravelLevel.determineLevel(countryCount, cityCount);
        TravelScope scope = TravelScope.determineScope(countryCount, continentCount);
        TravelType type = TravelType.determineType(level, photoCounts);

        String title = composer.compose(level, scope, type);

        assertThat(title).isEqualTo(expectedTitle);
    }
}

