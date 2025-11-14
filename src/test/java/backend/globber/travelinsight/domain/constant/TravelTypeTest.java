package backend.globber.travelinsight.domain.constant;

import static org.assertj.core.api.Assertions.assertThat;

import backend.globber.diary.domain.constant.PhotoTag;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TravelTypeTest {

    @Test
    @DisplayName("Lv.3이면 무조건 탐험가")
    void shouldReturnExplorer_WhenLevelThree() {
        TravelType type = TravelType.determineType(TravelLevel.LEVEL_3, Map.of());

        assertThat(type).isEqualTo(TravelType.EXPLORER);
    }

    @Test
    @DisplayName("음식 비율이 50% 이상이면 미식가")
    void shouldReturnFoodie_WhenFoodDominant() {
        Map<PhotoTag, Long> counts = Map.of(
            PhotoTag.FOOD, 6L,
            PhotoTag.PEOPLE, 3L,
            PhotoTag.SCENERY, 1L
        );

        TravelType type = TravelType.determineType(TravelLevel.LEVEL_2, counts);

        assertThat(type).isEqualTo(TravelType.FOODIE);
    }

    @Test
    @DisplayName("인물 비율이 50% 이상이면 인장가")
    void shouldReturnPortraitist_WhenPeopleDominant() {
        Map<PhotoTag, Long> counts = Map.of(
            PhotoTag.FOOD, 2L,
            PhotoTag.PEOPLE, 5L,
            PhotoTag.SCENERY, 1L
        );

        TravelType type = TravelType.determineType(TravelLevel.LEVEL_1, counts);

        assertThat(type).isEqualTo(TravelType.PORTRAITIST);
    }

    @Test
    @DisplayName("풍경 비율이 50% 이상이면 감상가")
    void shouldReturnAppreciator_WhenSceneryDominant() {
        Map<PhotoTag, Long> counts = Map.of(
            PhotoTag.FOOD, 1L,
            PhotoTag.PEOPLE, 1L,
            PhotoTag.SCENERY, 6L
        );

        TravelType type = TravelType.determineType(TravelLevel.LEVEL_0, counts);

        assertThat(type).isEqualTo(TravelType.APPRECIATOR);
    }

    @Test
    @DisplayName("균형형이면 기록가")
    void shouldReturnChronicler_WhenBalanced() {
        Map<PhotoTag, Long> counts = Map.of(
            PhotoTag.FOOD, 3L,
            PhotoTag.PEOPLE, 3L,
            PhotoTag.SCENERY, 4L
        );

        TravelType type = TravelType.determineType(TravelLevel.LEVEL_2, counts);

        assertThat(type).isEqualTo(TravelType.CHRONICLER);
    }

    @Test
    @DisplayName("사진이 없으면 기록가")
    void shouldReturnChronicler_WhenNoPhotos() {
        TravelType type = TravelType.determineType(TravelLevel.LEVEL_1, Map.of());

        assertThat(type).isEqualTo(TravelType.CHRONICLER);
    }
}

