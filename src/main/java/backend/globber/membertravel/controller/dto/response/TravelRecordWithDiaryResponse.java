package backend.globber.membertravel.controller.dto.response;

import backend.globber.membertravel.controller.dto.CountryRecordDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder
public record TravelRecordWithDiaryResponse(
        @Schema(description = "방문한 나라 수 (records에 포함된 나라 수, 기록 유무와 관계없음)", example = "2")
        long totalCountriesCounts,

        @Schema(description = "방문한 도시 수 (records에 포함된 모든 도시 수, 기록 유무와 관계없음)", example = "3")
        long totalCitiesCounts,

        @Schema(description = "기록 수 (나라별 diaryCount의 합, 방문 도시 목록에 없는 도시의 기록은 제외)", example = "5")
        long totalDiariesCounts,

        @Schema(description = "나라별 방문 기록 목록")
        List<CountryRecordDto> records
) {
}
