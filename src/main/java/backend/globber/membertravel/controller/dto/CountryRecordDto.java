package backend.globber.membertravel.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder
public record CountryRecordDto(
        @Schema(description = "나라 이름", example = "일본")
        String countryName,

        @Schema(description = "나라 코드 (ISO 3166-1 Alpha-3)", example = "JPN")
        String countryCode,

        @Schema(description = "대륙", example = "ASIA")
        String continent,

        @Schema(description = "이 나라의 방문 도시들에 있는 기록 수의 합", example = "3")
        long diaryCount,

        @Schema(description = "이 나라의 방문 도시 목록 (기록 유무와 관계없음)")
        List<CityRecordDto> cities
) {
}
