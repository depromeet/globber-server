package backend.globber.travelinsight.domain;

import backend.globber.diary.domain.constant.PhotoTag;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class TravelStatistics {

    private final int countryCount;      // 방문한 국가 수
    private final int cityCount;         // 방문한 도시 수
    private final int continentCount;    // 방문한 대륙 수
    private final Map<PhotoTag, Long> photoTagCounts; // 사진 태그별 개수

    /**
     * 기본값 (여행 기록이 없는 경우)
     */
    public static TravelStatistics empty() {
        return TravelStatistics.builder()
            .countryCount(0)
            .cityCount(0)
            .continentCount(0)
            .photoTagCounts(Map.of())
            .build();
    }

    /**
     * 여행 기록이 있는지 확인
     */
    public boolean hasTravel() {
        return countryCount > 0 || cityCount > 0;
    }
}
