package backend.globber.membertravel.controller.dto;

import java.util.List;

public record GlobeSummaryDto(
        String nickname,
        int cityCount,
        int countryCount,
        List<RegionDto> regions,
        String thumbnailUrl
) {
}
