package backend.globber.membertravel.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder
public record CityRecordDto(
        @Schema(description = "도시 ID", example = "1")
        Long id,

        @Schema(description = "도시 이름", example = "도쿄")
        String name,

        @Schema(description = "이 도시에 기록이 1개 이상 있는지 여부", example = "true")
        boolean hasDiary,                 // 일기 존재 여부

        @Schema(description = "이 도시 기록의 대표 사진 목록 (최대 3개)")
        List<String> thumbnailUrls        // 대표 사진 목록
) {
}
