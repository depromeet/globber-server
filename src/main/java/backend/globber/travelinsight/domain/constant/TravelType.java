package backend.globber.travelinsight.domain.constant;

import backend.globber.diary.domain.constant.PhotoTag;
import lombok.Getter;

import java.util.Map;

@Getter
public enum TravelType {
    EXPLORER("탐험가"),   // 여행 규모가 너무 큰 사람 (Lv.3)
    FOODIE("미식가"),     // 음식 비율 >= 50%
    PORTRAITIST("인장가"), // 인물 비율 >= 50%
    APPRECIATOR("감상가"), // 풍경 비율 >= 50%
    CHRONICLER("기록가");  // 균형형

    private final String typeName;

    TravelType(String typeName) {
        this.typeName = typeName;
    }

    /**
     * 여행 타입 결정
     * @param level 여행 레벨
     * @param photoTagCounts 사진 태그별 개수
     * @return TravelType
     */
    public static TravelType determineType(TravelLevel level, Map<PhotoTag, Long> photoTagCounts) {
        // (1) 여행 규모가 너무 큰 사람 (Lv.3)
        if (level == TravelLevel.LEVEL_3) {
            return EXPLORER;
        }

        // 전체 사진 개수 계산 (NONE 제외)
        long totalPhotos = photoTagCounts.getOrDefault(PhotoTag.FOOD, 0L)
                + photoTagCounts.getOrDefault(PhotoTag.SCENERY, 0L)
                + photoTagCounts.getOrDefault(PhotoTag.PEOPLE, 0L);

        // 사진이 없는 경우 기본값
        if (totalPhotos == 0) {
            return CHRONICLER;
        }

        // 각 카테고리 비율 계산
        double foodRatio = (photoTagCounts.getOrDefault(PhotoTag.FOOD, 0L) * 100.0) / totalPhotos;
        double peopleRatio = (photoTagCounts.getOrDefault(PhotoTag.PEOPLE, 0L) * 100.0) / totalPhotos;
        double sceneryRatio = (photoTagCounts.getOrDefault(PhotoTag.SCENERY, 0L) * 100.0) / totalPhotos;

        // 비율에 따라 타입 결정 (우선순위: 음식 > 인물 > 풍경)
        if (foodRatio >= 50.0) {
            return FOODIE;
        }
        if (peopleRatio >= 50.0) {
            return PORTRAITIST;
        }
        if (sceneryRatio >= 50.0) {
            return APPRECIATOR;
        }

        // 균형형 (어느 것도 50% 이상이 아닌 경우)
        return CHRONICLER;
    }
}
