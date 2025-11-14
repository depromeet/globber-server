package backend.globber.travelinsight.domain.constant;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TravelLevelTest {

    @ParameterizedTest(name = "국가 {0}, 도시 {1} => {2}")
    @MethodSource("levelArguments")
    void determineLevel_ShouldMatchSpecification(int countryCount, int cityCount, TravelLevel expectedLevel) {
        TravelLevel level = TravelLevel.determineLevel(countryCount, cityCount);

        assertThat(level).isEqualTo(expectedLevel);
    }

    private static Stream<Arguments> levelArguments() {
        return Stream.of(
            Arguments.of(15, 0, TravelLevel.LEVEL_3),
            Arguments.of(0, 30, TravelLevel.LEVEL_3),
            Arguments.of(5, 5, TravelLevel.LEVEL_2),
            Arguments.of(1, 20, TravelLevel.LEVEL_2),
            Arguments.of(3, 10, TravelLevel.LEVEL_1),
            Arguments.of(2, 14, TravelLevel.LEVEL_1),
            Arguments.of(1, 5, TravelLevel.LEVEL_0),
            Arguments.of(0, 3, TravelLevel.LEVEL_0)
        );
    }
}

