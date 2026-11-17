package backend.globber.membertravel.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record GlobeSummaryDto(
        Long memberId,
        String nickname,
        int cityCount,
        int countryCount,
        List<RegionDto> regions,
        @Schema(description = "지구본 대표 사진 전체 URL. 없으면 null")
        String thumbnailUrl
) {
}
