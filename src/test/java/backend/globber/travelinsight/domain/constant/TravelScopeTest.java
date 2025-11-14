package backend.globber.travelinsight.domain.constant;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TravelScopeTest {

    private static Stream<Arguments> scopeArguments() {
        return Stream.of(
            Arguments.of(10, 1, TravelScope.WORLD),
            Arguments.of(2, 3, TravelScope.WORLD),
            Arguments.of(4, 1, TravelScope.CONTINENT),
            Arguments.of(9, 1, TravelScope.CONTINENT),
            Arguments.of(3, 2, TravelScope.LOCAL),
            Arguments.of(1, 1, TravelScope.LOCAL)
        );
    }

    @ParameterizedTest(name = "국가 {0}, 대륙 {1} => {2}")
    @MethodSource("scopeArguments")
    void determineScope_ShouldFollowRules(int countryCount, int continentCount, TravelScope expectedScope) {
        TravelScope scope = TravelScope.determineScope(countryCount, continentCount);

        assertThat(scope).isEqualTo(expectedScope);
    }
}

